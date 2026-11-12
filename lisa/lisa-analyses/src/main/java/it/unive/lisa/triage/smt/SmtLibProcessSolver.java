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

	/**
	 * The alternative command checking satisfiability, tried first for queries
	 * involving floating point numbers, or {@code null}.
	 */
	private final String floatCheck;

	/**
	 * The alternative command checking satisfiability, tried for queries not
	 * involving floating point numbers when the standard one cannot decide, or
	 * {@code null} if {@link #floatCheck} is used for them as well.
	 */
	private final String otherCheck;

	/**
	 * Whether floating point numbers have been sent to the solver since it has
	 * been started or reset.
	 */
	private boolean floats;

	private final Process process;

	private final Writer input;

	private final BlockingQueue<Object> responses;

	private final Thread reader;

	private boolean closed;

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
	 * The relaxation of the queries over floating point numbers to real
	 * numbers, used to look for models of the queries that cannot be decided
	 * (see {@link #relaxedCheck()}), or {@code null} if queries are not
	 * relaxed.
	 */
	private RealRelaxation relaxation;

	/**
	 * The command checking the satisfiability of relaxed queries.
	 */
	private String relaxedCheck;

	/**
	 * The command checking the satisfiability of queries whose floating point
	 * constants have been fixed to the values of a model of their relaxation.
	 */
	private String fixedCheck;

	/**
	 * Whether the last satisfiability check has been decided through the
	 * relaxation, in a scope opened by {@link #relaxedCheck()} that is closed
	 * together with the one of the query.
	 */
	private boolean fixed;

	/**
	 * Why the last satisfiability check is unknown, asked before trying the
	 * relaxation (whose checks replace it in the solver), or {@code null}.
	 */
	private String reasonBeforeRelaxation;

	/**
	 * The number of queries decided through the relaxation.
	 */
	private int relaxations;

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
		SmtLibProcessSolver solver = new SmtLibProcessSolver(
				List.of(z3Executable(), "-in", "-smt2"),
				// each check might try two strategies (see check())
				List.of("(set-option :timeout " + timeoutMillis / 2 + ")"),
				timeoutMillis,
				z3FloatCheck(timeoutMillis / 2),
				z3SimplifyingCheck(timeoutMillis / 2));
		// undecided queries over floating point numbers are relaxed (see
		// relaxedCheck()), with a quarter of the timeout for each of the two
		// further checks
		solver.relaxation = new RealRelaxation();
		solver.relaxedCheck = z3SimplifyingCheck(timeoutMillis / 4);
		solver.fixedCheck = z3FloatCheck(timeoutMillis / 4);
		return solver;
	}

	/**
	 * The command checking satisfiability with z3 of queries involving floating
	 * point numbers: queries are first simplified (solving equalities and
	 * eliminating unconstrained terms, so that, e.g., the values read from the
	 * heap model and wrapped in values of unknown kind often become plain
	 * numbers), and then solved by bit-blasting if they are made of floating
	 * point numbers and bit-vectors only, and by the general solver otherwise.
	 * Without the simplifications, the general solver often fails on queries
	 * mixing floating point numbers with the other theories, while the general
	 * solver is often better at other queries (e.g., strings), so that both are
	 * tried (see {@link #check()}).
	 */
	private static String z3FloatCheck(
			long timeoutMillis) {
		return "(check-sat-using (try-for (then simplify propagate-values solve-eqs elim-uncnstr (or-else qffp smt)) "
				+ timeoutMillis + "))";
	}

	/**
	 * The command checking satisfiability with z3 of queries not involving
	 * floating point numbers, when the standard command cannot decide: the same
	 * simplifications of {@link #z3FloatCheck(long)}, followed by the general
	 * solver (bit-blasting would only spend the time of the check before
	 * failing). The simplifications often decide queries over the heap model
	 * that the general solver alone does not (e.g., the strings stored in the
	 * fields of new objects).
	 */
	private static String z3SimplifyingCheck(
			long timeoutMillis) {
		return "(check-sat-using (try-for (then simplify propagate-values solve-eqs elim-uncnstr smt) "
				+ timeoutMillis + "))";
	}

	/**
	 * The standard command checking satisfiability.
	 */
	private static final String CHECK_SAT = "(check-sat)";

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
		this(command, initialization, timeoutMillis, null);
	}

	/**
	 * Builds a solver running the given command, that must accept SMT-LIB 2
	 * commands from its standard input, and that checks satisfiability with the
	 * standard command and, if it cannot decide, with the given
	 * (solver-specific) one, if any, that is instead tried first for queries
	 * involving floating point numbers (see {@link #check()}).
	 *
	 * @param command        the command line starting the solver, that reads
	 *                           SMT-LIB 2 commands from its standard input
	 * @param initialization solver-specific commands to send right after the
	 *                           solver has started (e.g., to set the timeout)
	 * @param timeoutMillis  the timeout of satisfiability checks, in
	 *                           milliseconds
	 * @param floatCheck     the alternative command checking satisfiability,
	 *                           tried first for queries involving floating
	 *                           point numbers, or {@code null}
	 *
	 * @throws SmtSolverUnavailableException if the process cannot be started
	 */
	public SmtLibProcessSolver(
			List<String> command,
			List<String> initialization,
			long timeoutMillis,
			String floatCheck) {
		this(command, initialization, timeoutMillis, floatCheck, null);
	}

	/**
	 * Builds a solver running the given command, as
	 * {@link #SmtLibProcessSolver(List, List, long, String)}, with a different
	 * alternative command for queries not involving floating point numbers.
	 *
	 * @param command        the command line starting the solver, that reads
	 *                           SMT-LIB 2 commands from its standard input
	 * @param initialization solver-specific commands to send right after the
	 *                           solver has started (e.g., to set the timeout)
	 * @param timeoutMillis  the timeout of satisfiability checks, in
	 *                           milliseconds
	 * @param floatCheck     the alternative command checking satisfiability,
	 *                           tried first for queries involving floating
	 *                           point numbers, or {@code null}
	 * @param otherCheck     the alternative command checking satisfiability,
	 *                           tried for the other queries when the standard
	 *                           one cannot decide, or {@code null} to use
	 *                           {@code floatCheck} for them as well
	 *
	 * @throws SmtSolverUnavailableException if the process cannot be started
	 */
	public SmtLibProcessSolver(
			List<String> command,
			List<String> initialization,
			long timeoutMillis,
			String floatCheck,
			String otherCheck) {
		this.floatCheck = floatCheck;
		this.otherCheck = otherCheck;
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
		command("(reset)");
		floats = false;
		fixed = false;
		if (relaxation != null)
			relaxation.reset();
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
		if (relaxation != null)
			relaxation.define(constant.toSmtLib(), body.getSort(), body.toSmtLib());
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
		if (relaxation != null)
			relaxation.declareDatatype(datatype);
		return datatype.getSort();
	}

	@Override
	public Term declare(
			String name,
			Sort sort) {
		Term constant = Term.constant(name, sort);
		command("(declare-const " + constant.toSmtLib() + " " + sort.toSmtLib() + ")");
		if (relaxation != null)
			relaxation.declareConstant(constant.toSmtLib(), sort);
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
		if (relaxation != null)
			relaxation.declareFunction(symbol, arguments, result);
		return symbol;
	}

	@Override
	public void assertTerm(
			Term term) {
		checkBoolean(term);
		unfix();
		command("(assert " + term.toSmtLib() + ")");
		if (relaxation != null)
			relaxation.assertion(term.toSmtLib());
	}

	@Override
	public void assertNamed(
			String name,
			Term term) {
		checkBoolean(term);
		unfix();
		command("(assert (! " + term.toSmtLib() + " :named " + Term.symbol(name) + "))");
		if (relaxation != null)
			relaxation.assertion(term.toSmtLib());
	}

	private static void checkBoolean(
			Term term) {
		if (!term.getSort().equals(Sort.BOOL))
			throw new IllegalArgumentException("Only boolean terms can be asserted: " + term);
	}

	@Override
	public void push() {
		unfix();
		command("(push 1)");
		if (relaxation != null)
			relaxation.push();
	}

	@Override
	public void pop() {
		if (closed && killed != null)
			// the solver has been killed while checking: its scopes are gone
			// with it
			return;
		unfix();
		command("(pop 1)");
		if (relaxation != null)
			relaxation.pop();
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * If an alternative command has been given, it is used when the standard
	 * one cannot decide, and it is tried first if floating point numbers have
	 * been sent to the solver. Queries without floating point numbers use the
	 * alternative command for them, if one has been given.
	 */
	@Override
	public SmtResult check() {
		reasonBeforeRelaxation = null;
		unfix();
		if (floatCheck == null)
			return check(CHECK_SAT);
		String first = floats ? floatCheck : CHECK_SAT;
		String second = floats ? CHECK_SAT : otherCheck != null ? otherCheck : floatCheck;
		SmtResult result = check(first);
		if (result != SmtResult.UNKNOWN || killed != null)
			return result;
		result = check(second);
		if (result != SmtResult.UNKNOWN || killed != null || relaxation == null)
			return result;
		// queries without floating point constants are not relaxed
		return relaxedCheck();
	}

	/**
	 * Looks for a model of the current assertions, that the solver could not
	 * decide, through their relaxation over real numbers (see
	 * {@link RealRelaxation}): the relaxed assertions are checked in a new
	 * scope, and if they are satisfiable the floating point constants of the
	 * original assertions are fixed to the values of their copies in the model,
	 * in another scope where the original assertions are checked again. If
	 * they are satisfiable, the scope is left open until the one of the query
	 * is closed, so that the model can be read, and the query is decided as
	 * satisfiable. Otherwise, the query stays undecided, and the solver is left
	 * as it was. Relaxed queries are never unsatisfiable, as the relaxation is
	 * not an over-approximation.
	 *
	 * @return {@link SmtResult#SAT} if a model has been found,
	 *             {@link SmtResult#UNKNOWN} otherwise
	 */
	SmtResult relaxedCheck() {
		reasonBeforeRelaxation = getReasonUnknown();
		RealRelaxation.Relaxed relaxed = relaxation.relax();
		if (relaxed == null)
			return SmtResult.UNKNOWN;
		List<String> fixes = new ArrayList<>();
		command("(push 1)");
		try {
			for (String cmd : relaxed.getCommands())
				command(cmd);
			if (check(relaxedCheck) == SmtResult.SAT) {
				StringBuilder cmd = new StringBuilder("(get-value (");
				for (RealRelaxation.Pin pin : relaxed.getPins())
					cmd.append(pin.getRelaxed()).append(" ");
				cmd.setLength(cmd.length() - 1);
				SExpression response = query(cmd.append("))").toString());
				List<RealRelaxation.Pin> pins = relaxed.getPins();
				for (int i = 0; i < pins.size() && i < response.getChildren().size(); i++) {
					String fix = pins.get(i).fix(response.getChildren().get(i).getChildren().get(1).toString());
					if (fix != null)
						fixes.add(fix);
				}
			}
		} catch (SmtException e) {
			if (killed != null)
				return SmtResult.UNKNOWN;
			// the relaxation is not understood by the solver: it is not tried
			// again
			relaxation = null;
		} finally {
			if (killed == null)
				command("(pop 1)");
		}
		if (fixes.isEmpty() || killed != null)
			return SmtResult.UNKNOWN;
		command("(push 1)");
		for (String fix : fixes)
			command(fix);
		if (check(fixedCheck) == SmtResult.SAT) {
			fixed = true;
			relaxations++;
			return SmtResult.SAT;
		}
		if (killed == null)
			command("(pop 1)");
		return SmtResult.UNKNOWN;
	}

	/**
	 * Closes the scope where the floating point constants have been fixed by
	 * {@link #relaxedCheck()}, if it is open: it must be closed before the
	 * scope of the query is changed (or checked again), as its assertions only
	 * hold for the model found through the relaxation.
	 */
	private void unfix() {
		if (fixed) {
			fixed = false;
			command("(pop 1)");
		}
	}

	/**
	 * Yields the number of queries that the solver could not decide, but whose
	 * models have been found through their relaxation over real numbers (see
	 * {@link #relaxedCheck()}).
	 *
	 * @return the number of queries
	 */
	public int getRelaxations() {
		return relaxations;
	}

	private SmtResult check(
			String command) {
		SExpression response;
		try {
			response = query(command);
		} catch (SolverTimeoutException e) {
			killed = "timeout: " + e.getMessage();
			return SmtResult.UNKNOWN;
		} catch (SmtException e) {
			// z3 sometimes reports the cancellation of a check whose time is
			// over as an error
			if (!closed && e.getMessage() != null && e.getMessage().endsWith("canceled"))
				return SmtResult.UNKNOWN;
			throw e;
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
	public boolean isAlive() {
		return !closed;
	}

	@Override
	public String getReasonUnknown() {
		if (closed && killed != null)
			return killed;
		if (reasonBeforeRelaxation != null)
			return reasonBeforeRelaxation;
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
		if (!floats && (cmd.contains("FloatingPoint") || cmd.contains("(fp ") || cmd.contains("to_fp")))
			floats = true;
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
