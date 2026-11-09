package it.unive.lisa.triage.smt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * An {@link SmtSolver} that communicates with an external solver process
 * through the SMT-LIB 2 textual interface, sending commands on its standard
 * input and reading responses from its standard output. The process is started
 * when the solver is created, and its creation fails with an
 * {@link SmtSolverUnavailableException} if the executable cannot be found.
 * Factories for common solvers are provided by {@link #z3()} and
 * {@link #cvc5()}: the executables they run are expected to be in the path of
 * the current environment, unless a different path is set through
 * {@link #setZ3Path(String)} and {@link #setCvc5Path(String)}.<br>
 * <br>
 * Each satisfiability check is bounded by a timeout, communicated to the
 * solver: checks exceeding it yield {@link SmtResult#UNKNOWN}. If the solver
 * does not answer within a grace period after the timeout, its process is
 * killed, the check yields {@link SmtResult#UNKNOWN}, and the solver can no
 * longer be used.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SmtLibProcessSolver
		implements
		SmtSolver {

	/**
	 * The default timeout of satisfiability checks, in milliseconds.
	 */
	public static final long DEFAULT_TIMEOUT = 10_000;

	/**
	 * The time, in milliseconds, that the solver is given to answer after the
	 * timeout has expired, before its process is killed.
	 */
	public static final long GRACE_PERIOD = 5_000;

	private static final Object EOF = new Object();

	private static volatile String z3Path = null;

	private static volatile String cvc5Path = null;

	private final List<String> command;

	private final long timeout;

	private final Process process;

	private final Writer input;

	private final BlockingQueue<Object> responses;

	private final Thread reader;

	private boolean closed;

	/**
	 * The number of scopes currently open.
	 */
	private int depth;

	/**
	 * The equalities defining the constants of
	 * {@link #defineConstant(String, Term)}, each with the depth of the scope
	 * where it is asserted: when that scope is closed, the equality is
	 * asserted again in the enclosing one, so that it holds in all later
	 * queries.
	 */
	private final List<Definition> definitions = new ArrayList<>();

	private static final class Definition {

		private final Term equality;

		private int depth;

		private Definition(
				Term equality,
				int depth) {
			this.equality = equality;
			this.depth = depth;
		}
	}

	/**
	 * Why the last satisfiability check is unknown, when the solver has been
	 * killed because it did not answer in time ({@code null} otherwise).
	 */
	private String killed;

	/**
	 * The commands that have been sent, but whose answers have not been read
	 * yet: commands answering {@code success} are pipelined, and their answers
	 * are read before the next request answering something else.
	 */
	private final Deque<String> pending = new ArrayDeque<>();

	/**
	 * The commands initializing the solver, sent again after each reset.
	 */
	private final List<String> initialization;

	/**
	 * Sets the path to the z3 executable used by {@link #z3()},
	 * {@link #z3(long)}, and {@link #isZ3Available()}. If no path is set (or if
	 * it is set to {@code null}), z3 is expected to be available as {@code z3}
	 * in the path of the current environment.
	 *
	 * @param path the path to the z3 executable, or {@code null} to use the one
	 *                 in the path of the current environment
	 */
	public static void setZ3Path(
			String path) {
		z3Path = path;
	}

	/**
	 * Sets the path to the cvc5 executable used by {@link #cvc5()},
	 * {@link #cvc5(long)}, and {@link #isCvc5Available()}. If no path is set
	 * (or if it is set to {@code null}), cvc5 is expected to be available as
	 * {@code cvc5} in the path of the current environment.
	 *
	 * @param path the path to the cvc5 executable, or {@code null} to use the
	 *                 one in the path of the current environment
	 */
	public static void setCvc5Path(
			String path) {
		cvc5Path = path;
	}

	private static String z3Executable() {
		String path = z3Path;
		return path == null ? "z3" : path;
	}

	private static String cvc5Executable() {
		String path = cvc5Path;
		return path == null ? "cvc5" : path;
	}

	/**
	 * Builds a solver running z3 (see {@link #setZ3Path(String)}), with
	 * {@link #DEFAULT_TIMEOUT} as timeout.
	 *
	 * @return the solver
	 *
	 * @throws SmtSolverUnavailableException if z3 cannot be started
	 */
	public static SmtLibProcessSolver z3() {
		return z3(DEFAULT_TIMEOUT);
	}

	/**
	 * Builds a solver running z3 (see {@link #setZ3Path(String)}).
	 *
	 * @param timeoutMillis the timeout of satisfiability checks, in
	 *                          milliseconds
	 *
	 * @return the solver
	 *
	 * @throws SmtSolverUnavailableException if z3 cannot be started
	 */
	public static SmtLibProcessSolver z3(
			long timeoutMillis) {
		return new SmtLibProcessSolver(
				List.of(z3Executable(), "-in", "-smt2"),
				List.of("(set-option :timeout " + timeoutMillis + ")"),
				timeoutMillis);
	}

	/**
	 * Builds a solver running cvc5 (see {@link #setCvc5Path(String)}), with
	 * {@link #DEFAULT_TIMEOUT} as timeout.
	 *
	 * @return the solver
	 *
	 * @throws SmtSolverUnavailableException if cvc5 cannot be started
	 */
	public static SmtLibProcessSolver cvc5() {
		return cvc5(DEFAULT_TIMEOUT);
	}

	/**
	 * Builds a solver running cvc5 (see {@link #setCvc5Path(String)}).
	 *
	 * @param timeoutMillis the timeout of satisfiability checks, in
	 *                          milliseconds
	 *
	 * @return the solver
	 *
	 * @throws SmtSolverUnavailableException if cvc5 cannot be started
	 */
	public static SmtLibProcessSolver cvc5(
			long timeoutMillis) {
		return new SmtLibProcessSolver(
				List.of(cvc5Executable(), "--lang=smt2", "--incremental", "--tlimit-per=" + timeoutMillis),
				List.of(),
				timeoutMillis);
	}

	/**
	 * Yields {@code true} if z3 (see {@link #setZ3Path(String)}) can be started
	 * in the current environment.
	 *
	 * @return whether or not z3 is available
	 */
	public static boolean isZ3Available() {
		return isAvailable(z3Executable());
	}

	/**
	 * Yields {@code true} if cvc5 (see {@link #setCvc5Path(String)}) can be
	 * started in the current environment.
	 *
	 * @return whether or not cvc5 is available
	 */
	public static boolean isCvc5Available() {
		return isAvailable(cvc5Executable());
	}

	/**
	 * Yields {@code true} if the given executable can be started, by running it
	 * with the {@code --version} option.
	 */
	private static boolean isAvailable(
			String executable) {
		try {
			Process p = new ProcessBuilder(executable, "--version").redirectErrorStream(true).start();
			p.getInputStream().readAllBytes();
			return p.waitFor(GRACE_PERIOD, TimeUnit.MILLISECONDS) && p.exitValue() == 0;
		} catch (IOException e) {
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * Builds the solver, starting its process.
	 *
	 * @param command        the command starting the solver, that must read
	 *                           SMT-LIB 2 commands from its standard input
	 * @param initialization solver-specific commands to send right after the
	 *                           solver has started (e.g., to set the timeout)
	 * @param timeoutMillis  the timeout of satisfiability checks, in
	 *                           milliseconds
	 *
	 * @throws SmtSolverUnavailableException if the process cannot be started
	 */
	public SmtLibProcessSolver(
			List<String> command,
			List<String> initialization,
			long timeoutMillis) {
		this.command = List.copyOf(command);
		this.timeout = timeoutMillis;
		this.initialization = List.copyOf(initialization);
		try {
			this.process = new ProcessBuilder(command).redirectErrorStream(true).start();
		} catch (IOException e) {
			throw new SmtSolverUnavailableException("Cannot start SMT solver with command " + command, e);
		}
		this.input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
		this.responses = new LinkedBlockingQueue<>();
		BufferedReader output = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
		this.reader = new Thread(() -> readResponses(output), "smt-solver-reader");
		this.reader.setDaemon(true);
		this.reader.start();

		try {
			// print-success makes every command answer, so that errors are
			// reported synchronously
			command("(set-option :print-success true)");
			initialize();
			sync();
		} catch (SmtException e) {
			close();
			throw new SmtSolverUnavailableException("The SMT solver started with " + command
					+ " cannot be initialized", e);
		}
	}

	private void initialize() {
		command("(set-option :produce-models true)");
		command("(set-option :produce-unsat-cores true)");
		command("(set-option :global-declarations true)");
		for (String init : initialization)
			command(init);
		command("(set-logic ALL)");
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * The solver is reset through the {@code reset} command, and initialized
	 * again with the same options it has been started with.
	 */
	@Override
	public boolean reset() {
		depth = 0;
		definitions.clear();
		command("(reset)");
		// options are reset as well
		command("(set-option :print-success true)");
		initialize();
		sync();
		return true;
	}

	@Override
	public Term define(
			String name,
			Term body) {
		Term constant = Term.constant(name, body.getSort());
		command("(define-fun " + constant.toSmtLib() + " () " + body.getSort().toSmtLib() + " " + body.toSmtLib()
				+ ")");
		return constant;
	}

	@Override
	public Term defineConstant(
			String name,
			Term body) {
		Term constant = declare(name, body.getSort());
		Term equality = Term.eq(constant, body);
		assertTerm(equality);
		definitions.add(new Definition(equality, depth));
		return constant;
	}

	private void readResponses(
			BufferedReader output) {
		try {
			SExpression response;
			while ((response = SExpression.read(output)) != null)
				responses.add(response);
		} catch (IOException | SmtException e) {
			responses.add(e);
		}
		responses.add(EOF);
	}

	@Override
	public Sort declareSort(
			String name) {
		Sort sort = Sort.uninterpreted(name);
		command("(declare-sort " + sort.toSmtLib() + " 0)");
		return sort;
	}

	@Override
	public Sort declareDatatype(
			Datatype datatype) {
		command(datatype.toSmtLibDeclaration());
		return datatype.getSort();
	}

	@Override
	public Term declare(
			String name,
			Sort sort) {
		Term constant = Term.constant(name, sort);
		command("(declare-const " + constant.toSmtLib() + " " + sort.toSmtLib() + ")");
		return constant;
	}

	@Override
	public String declareFunction(
			String name,
			List<Sort> arguments,
			Sort result) {
		String symbol = Term.symbol(name);
		StringBuilder sb = new StringBuilder("(declare-fun ").append(symbol).append(" (");
		for (int k = 0; k < arguments.size(); k++)
			sb.append(k == 0 ? "" : " ").append(arguments.get(k).toSmtLib());
		sb.append(") ").append(result.toSmtLib()).append(")");
		command(sb.toString());
		return symbol;
	}

	@Override
	public void assertTerm(
			Term term) {
		checkBoolean(term);
		command("(assert " + term.toSmtLib() + ")");
	}

	@Override
	public void assertNamed(
			String name,
			Term term) {
		checkBoolean(term);
		command("(assert (! " + term.toSmtLib() + " :named " + Term.symbol(name) + "))");
	}

	private static void checkBoolean(
			Term term) {
		if (!term.getSort().equals(Sort.BOOL))
			throw new IllegalArgumentException("Only boolean terms can be asserted: " + term);
	}

	@Override
	public void push() {
		command("(push 1)");
		depth++;
	}

	@Override
	public void pop() {
		if (closed && killed != null)
			// the solver has been killed while checking: its scopes are gone
			// with it
			return;
		command("(pop 1)");
		depth--;
		// the constants defined in the closed scope are still declared, and
		// still equal to their terms
		for (Definition def : definitions)
			if (def.depth > depth) {
				assertTerm(def.equality);
				def.depth = depth;
			}
	}

	@Override
	public SmtResult check() {
		SExpression response;
		try {
			response = query("(check-sat)");
		} catch (SolverTimeoutException e) {
			killed = "timeout: " + e.getMessage();
			return SmtResult.UNKNOWN;
		}
		if (response.isAtom())
			switch (response.getAtom()) {
			case "sat":
				return SmtResult.SAT;
			case "unsat":
				return SmtResult.UNSAT;
			case "unknown":
				return SmtResult.UNKNOWN;
			default:
				break;
			}
		throw new SmtException("Unexpected response to check-sat: " + response);
	}

	@Override
	public Map<Term, String> getValues(
			Collection<Term> terms) {
		Map<Term, String> values = new LinkedHashMap<>();
		// constants need no query, and each term is asked once
		Set<Term> asked = new LinkedHashSet<>();
		for (Term term : terms)
			if (term.equals(Term.TRUE) || term.equals(Term.FALSE))
				values.put(term, term.toSmtLib());
			else
				asked.add(term);
		if (asked.isEmpty())
			return values;

		StringBuilder cmd = new StringBuilder("(get-value (");
		for (Term term : asked)
			cmd.append(term.toSmtLib()).append(" ");
		cmd.setLength(cmd.length() - 1);
		cmd.append("))");

		SExpression response = query(cmd.toString());
		if (response.isAtom() || response.getChildren().size() != asked.size())
			throw new SmtException("Unexpected response to get-value: " + response);

		// pairs are returned in the same order of the request
		Iterator<Term> it = asked.iterator();
		for (SExpression pair : response.getChildren()) {
			if (pair.isAtom() || pair.getChildren().size() != 2)
				throw new SmtException("Unexpected response to get-value: " + response);
			values.put(it.next(), pair.getChildren().get(1).toString());
		}
		return values;
	}

	@Override
	public List<String> getUnsatCore() {
		SExpression response = query("(get-unsat-core)");
		if (response.isAtom())
			throw new SmtException("Unexpected response to get-unsat-core: " + response);
		List<String> core = new ArrayList<>();
		for (SExpression name : response.getChildren())
			core.add(unquote(name.toString()));
		return core;
	}

	@Override
	public String getReasonUnknown() {
		if (closed && killed != null)
			return killed;
		SExpression response = query("(get-info :reason-unknown)");
		if (response.isAtom() || response.getChildren().size() != 2)
			throw new SmtException("Unexpected response to get-info: " + response);
		return unquote(response.getChildren().get(1).toString());
	}


	private static String unquote(
			String text) {
		if (text.length() >= 2
				&& ((text.startsWith("|") && text.endsWith("|")) || (text.startsWith("\"") && text.endsWith("\""))))
			return text.substring(1, text.length() - 1).replace("\"\"", "\"");
		return text;
	}

	/**
	 * Sends a command that is expected to answer with {@code success}, without
	 * waiting for the answer (see {@link #sync()}).
	 */
	private void command(
			String cmd) {
		if (closed)
			throw new SmtException("The SMT solver has been closed");
		try {
			input.write(cmd);
			input.write("\n");
		} catch (IOException e) {
			close();
			throw new SmtException("Cannot send command to the SMT solver: " + abbreviate(cmd), e);
		}
		pending.add(abbreviate(cmd));
	}

	/**
	 * Reads the answers of the commands sent through {@link #command(String)},
	 * throwing an exception for the first one that did not succeed, after all
	 * the answers have been read (so that the solver can still be used).
	 */
	private void sync() {
		flush();
		SmtException failure = null;
		while (!pending.isEmpty()) {
			String cmd = pending.removeFirst();
			try {
				SExpression response = next(cmd);
				if (failure == null && (!response.isAtom() || !response.getAtom().equals("success")))
					failure = new SmtException("Unexpected response to " + cmd + ": " + response);
			} catch (SolverTimeoutException e) {
				pending.clear();
				throw e;
			} catch (SmtException e) {
				if (closed) {
					pending.clear();
					throw e;
				}
				if (failure == null)
					failure = e;
			}
		}
		if (failure != null)
			throw failure;
	}

	private void flush() {
		if (closed)
			throw new SmtException("The SMT solver has been closed");
		try {
			input.flush();
		} catch (IOException e) {
			close();
			throw new SmtException("Cannot send commands to the SMT solver", e);
		}
	}

	/**
	 * Sends a request and waits for its answer, after the ones of the pending
	 * commands.
	 */
	private SExpression query(
			String cmd) {
		sync();
		try {
			input.write(cmd);
			input.write("\n");
			input.flush();
		} catch (IOException e) {
			close();
			throw new SmtException("Cannot send command to the SMT solver: " + abbreviate(cmd), e);
		}
		return next(abbreviate(cmd));
	}

	/**
	 * Waits for the next answer of the solver, to the given (abbreviated)
	 * command.
	 */
	private SExpression next(
			String cmd) {
		Object response;
		try {
			response = responses.poll(timeout + GRACE_PERIOD, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			close();
			throw new SmtException("Interrupted while waiting for the SMT solver", e);
		}

		if (response == null) {
			// the solver is unresponsive: no point in asking it to exit
			process.destroyForcibly();
			close();
			throw new SolverTimeoutException("The SMT solver did not answer to " + cmd + " within "
					+ (timeout + GRACE_PERIOD) + "ms, and has been killed");
		}

		if (response == EOF) {
			close();
			throw new SmtException("The SMT solver terminated unexpectedly while executing " + cmd);
		}

		if (response instanceof Exception) {
			close();
			throw new SmtException("Cannot read the response of the SMT solver to " + cmd, (Exception) response);
		}

		SExpression sexpr = (SExpression) response;
		if (!sexpr.isAtom()
				&& !sexpr.getChildren().isEmpty()
				&& sexpr.getChildren().get(0).isAtom()
				&& sexpr.getChildren().get(0).getAtom().equals("error"))
			throw new SmtException("The SMT solver reported an error while executing " + cmd + ": "
					+ (sexpr.getChildren().size() > 1 ? unquote(sexpr.getChildren().get(1).toString()) : sexpr));
		return sexpr;
	}

	@Override
	public void close() {
		if (closed)
			return;
		closed = true;
		try {
			input.write("(exit)\n");
			input.flush();
		} catch (IOException e) {
			// the process might be already dead
		}
		try {
			if (!process.waitFor(GRACE_PERIOD, TimeUnit.MILLISECONDS))
				process.destroyForcibly();
		} catch (InterruptedException e) {
			process.destroyForcibly();
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public String toString() {
		return "SMT solver " + command;
	}

	/**
	 * Raised when the solver does not answer in time.
	 */
	private static class SolverTimeoutException
			extends
			SmtException {

		private static final long serialVersionUID = 1L;

		private SolverTimeoutException(
				String message) {
			super(message);
		}
	}

	/**
	 * The maximum length of the commands reported in the messages of
	 * exceptions.
	 */
	private static final int MAX_REPORTED_LENGTH = 200;

	private static String abbreviate(
			String cmd) {
		if (cmd.length() <= MAX_REPORTED_LENGTH)
			return cmd;
		return cmd.substring(0, MAX_REPORTED_LENGTH) + "... (" + cmd.length() + " characters)";
	}
}
