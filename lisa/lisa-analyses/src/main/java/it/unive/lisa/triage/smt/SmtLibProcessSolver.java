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
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
	 * The timeout of the processes checking relaxed queries, in milliseconds.
	 */
	private long relaxedTimeout;

	/**
	 * The command checking the satisfiability of queries whose floating point
	 * constants have been fixed to the values of a model of their relaxation.
	 */
	private String fixedCheck;

	/**
	 * The solver that decided the last satisfiability check through the
	 * relaxation (see {@link #relaxedCheck()}), holding its model until the
	 * scope of the query is changed, or {@code null}.
	 */
	private SmtLibProcessSolver fixed;

	/**
	 * The declarations and definitions sent to the solver since it has been
	 * started or reset, recorded if queries are relaxed, to send them to the
	 * solvers checking the queries with fixed floating point constants (see
	 * {@link #relaxedCheck()}).
	 */
	private final List<String> declarations = new ArrayList<>();

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
	 * The assertion commands sent in each open scope (the first being the
	 * outermost, that is never closed), recorded if queries are relaxed, to
	 * rebuild the innermost scope (see {@link #relaxedCheck()}).
	 */
	private final List<List<String>> asserted = new ArrayList<>(List.of(new ArrayList<>()));

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
		solver.relaxedTimeout = timeoutMillis / 4;
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
		unfix();
		declarations.clear();
		evaluators.clear();
		if (relaxation != null)
			relaxation.reset();
		asserted.clear();
		asserted.add(new ArrayList<>());
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
		declaration("(define-fun " + constant.toSmtLib() + " () " + body.getSort().toSmtLib() + " " + body.toSmtLib()
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
		declaration("(declare-sort " + sort.toSmtLib() + " 0)");
		if (relaxation != null)
			relaxation.declareSort(sort.toSmtLib());
		return sort;
	}

	@Override
	public Sort declareDatatype(
			Datatype datatype) {
		declaration(datatype.toSmtLibDeclaration());
		if (relaxation != null)
			relaxation.declareDatatype(datatype);
		return datatype.getSort();
	}

	@Override
	public Term declare(
			String name,
			Sort sort) {
		Term constant = Term.constant(name, sort);
		declaration("(declare-const " + constant.toSmtLib() + " " + sort.toSmtLib() + ")");
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
		declaration(sb.toString());
		if (relaxation != null)
			relaxation.declareFunction(symbol, arguments, result);
		return symbol;
	}

	@Override
	public void assertTerm(
			Term term) {
		checkBoolean(term);
		unfix();
		String cmd = "(assert " + term.toSmtLib() + ")";
		command(cmd);
		if (relaxation != null) {
			relaxation.assertion(term.toSmtLib());
			asserted.get(asserted.size() - 1).add(cmd);
		}
	}

	@Override
	public void assertNamed(
			String name,
			Term term) {
		checkBoolean(term);
		unfix();
		String cmd = "(assert (! " + term.toSmtLib() + " :named " + Term.symbol(name) + "))";
		command(cmd);
		if (relaxation != null) {
			relaxation.assertion(term.toSmtLib());
			asserted.get(asserted.size() - 1).add(cmd);
		}
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
		if (relaxation != null) {
			relaxation.push();
			asserted.add(new ArrayList<>());
		}
	}

	@Override
	public void pop() {
		if (closed && killed != null)
			// the solver has been killed while checking: its scopes are gone
			// with it
			return;
		unfix();
		command("(pop 1)");
		if (relaxation != null) {
			relaxation.pop();
			if (asserted.size() > 1)
				asserted.remove(asserted.size() - 1);
		}
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
	 * {@link RealRelaxation}): the relaxed assertions are checked by another
	 * process running the same solver (as solvers might not stop in time on
	 * relaxed problems, that are often non-linear, and killing this one would
	 * lose its state), and if they are satisfiable the floating point
	 * constants of the original assertions are fixed to the values of their
	 * copies in the model, and the original assertions are checked again by
	 * another process. If they are satisfiable, the query is decided as
	 * satisfiable, and the model is read from that process until the scope of
	 * the query is changed. Otherwise, the query stays undecided. This solver
	 * is never changed (z3 might not answer to commands changing the scopes
	 * right after an undecided check). Relaxed queries are never
	 * unsatisfiable, as the relaxation is not an over-approximation.
	 *
	 * @return {@link SmtResult#SAT} if a model has been found,
	 *             {@link SmtResult#UNKNOWN} otherwise
	 */
	SmtResult relaxedCheck() {
		reasonBeforeRelaxation = getReasonUnknown();
		RealRelaxation.Relaxed relaxed = relaxation.relax();
		if (relaxed == null)
			return SmtResult.UNKNOWN;
		List<RealRelaxation.Application> applications = relaxed.getApplications();
		List<String> fixes = new ArrayList<>();
		// the helper is killed if it does not answer within the time of the
		// relaxed check, and the grace period
		try (SmtLibProcessSolver helper = new SmtLibProcessSolver(command, initialization, relaxedTimeout)) {
			for (String cmd : relaxed.getCommands())
				helper.command(cmd);
			// moderate values are preferred, as the cancellations of large
			// real numbers are usually lost when rounding them
			for (RealRelaxation.Pin pin : relaxed.getPins())
				helper.command("(assert (and (<= " + RealRelaxation.real(-MAX_RELAXED_MAGNITUDE) + " "
						+ pin.getRelaxed() + ") (<= " + pin.getRelaxed() + " "
						+ RealRelaxation.real(MAX_RELAXED_MAGNITUDE) + ")))");
			// the functions that can be computed on concrete values are
			// approximated by the linear interpolation of samples (if they
			// have one argument), refined with their values at the points of
			// the models, until the models agree with them
			Map<String, TreeMap<Double, Double>> samples = new HashMap<>();
			for (RealRelaxation.Application app : applications)
				if (app.getRelaxedArguments().size() == 1)
					samples.computeIfAbsent(app.getFunction(), f -> initialSamples(evaluators.get(f)));
			long deadline = System.currentTimeMillis() + relaxedTimeout;
			for (int round = 0;; round++) {
				helper.command("(push 1)");
				for (RealRelaxation.Application app : applications)
					if (app.getRelaxedArguments().size() == 1)
						helper.command("(assert (= " + app.getRelaxed() + " "
								+ interpolation(samples.get(app.getFunction()), app.getRelaxedArguments().get(0))
								+ "))");
				if (helper.check(relaxedCheck) != SmtResult.SAT) {
					helper.command("(pop 1)");
					break;
				}
				List<RealRelaxation.Pin> pins = relaxed.getPins();
				List<String> terms = new ArrayList<>();
				for (RealRelaxation.Pin pin : pins)
					terms.add(pin.getRelaxed());
				List<String> values = helper.values(terms);
				fixes.clear();
				for (int i = 0; i < pins.size(); i++) {
					String fix = pins.get(i).fix(values.get(i));
					if (fix != null)
						fixes.add(fix);
				}
				List<Mismatch> mismatches;
				try {
					mismatches = round >= MAX_RELAXED_REFINEMENTS || System.currentTimeMillis() >= deadline
							? Collections.emptyList()
							: relaxedMismatches(helper, applications);
				} catch (SmtException e) {
					// the values cannot be read: the last model is used
					mismatches = Collections.emptyList();
				}
				helper.command("(pop 1)");
				if (mismatches.isEmpty())
					break;
				for (Mismatch m : mismatches)
					if (m.points.size() == 1)
						samples.get(m.application.getFunction()).put(m.arguments.get(0).doubleValue(), m.actual);
					else
						helper.command("(assert (= " + m.application.relaxedAt(m.points) + " "
								+ RealRelaxation.real(m.actual) + "))");
			}
		} catch (SmtException e) {
			// e.g., the relaxation is not understood by the solver
			return SmtResult.UNKNOWN;
		}
		if (fixes.isEmpty())
			return SmtResult.UNKNOWN;
		// the query with fixed constants is checked by another process as
		// well, since z3 might not answer to further commands changing the
		// scopes right after an undecided check
		SmtLibProcessSolver checker = new SmtLibProcessSolver(command, initialization, relaxedTimeout);
		try {
			for (String cmd : declarations)
				checker.command(cmd);
			for (List<String> scope : asserted)
				for (String cmd : scope)
					checker.command(cmd);
			for (String fix : fixes)
				checker.command(fix);
			if (checker.check(fixedCheck) == SmtResult.SAT) {
				// any model is a model of the query, where the functions are
				// uninterpreted, but the ones where they have their actual
				// values are preferred
				for (int round = 0; round < MAX_REFINEMENTS; round++) {
					List<String> facts;
					try {
						facts = fixedFacts(checker, applications);
					} catch (SmtException e) {
						// the values cannot be read: the model is kept
						break;
					}
					if (facts.isEmpty())
						break;
					checker.command("(push 1)");
					for (String fact : facts)
						checker.command(fact);
					if (checker.check(fixedCheck) != SmtResult.SAT) {
						checker.command("(pop 1)");
						if (checker.check(fixedCheck) != SmtResult.SAT)
							throw new SmtException("The model of the query has been lost");
						break;
					}
				}
				fixed = checker;
				relaxations++;
				return SmtResult.SAT;
			}
		} catch (SmtException e) {
			// the query stays undecided
		}
		checker.close();
		return SmtResult.UNKNOWN;
	}

	/**
	 * The maximum number of times the values of the functions that can be
	 * computed on concrete values are learned from the models of a query with
	 * fixed floating point constants (see {@link #relaxedCheck()}).
	 */
	private static final int MAX_REFINEMENTS = 5;

	/**
	 * The maximum number of times the values of the functions that can be
	 * computed on concrete values are learned from the models of a relaxed
	 * query (see {@link #relaxedCheck()}), within the time of the relaxed
	 * check: each model usually applies the functions to new points.
	 */
	private static final int MAX_RELAXED_REFINEMENTS = 50;

	/**
	 * The maximum magnitude of the values of the floating point constants in
	 * the models of relaxed queries (see {@link #relaxedCheck()}).
	 */
	private static final double MAX_RELAXED_MAGNITUDE = 1e6;

	/**
	 * Yields the values of the given terms in the current model.
	 */
	private List<String> values(
			List<String> terms) {
		List<String> values = new ArrayList<>();
		if (terms.isEmpty())
			return values;
		SExpression response = query("(get-value (" + String.join(" ", terms) + "))");
		if (response.isAtom() || response.getChildren().size() != terms.size())
			throw new SmtException("Unexpected response to get-value: " + response);
		for (SExpression pair : response.getChildren())
			values.add(pair.getChildren().get(1).toString());
		return values;
	}

	/**
	 * An application of a function that can be computed on concrete values,
	 * whose value in a model of a relaxed problem is not the actual one.
	 */
	private static final class Mismatch {

		private final RealRelaxation.Application application;

		private final List<String> points;

		private final List<Number> arguments;

		private final double actual;

		private Mismatch(
				RealRelaxation.Application application,
				List<String> points,
				List<Number> arguments,
				double actual) {
			this.application = application;
			this.points = points;
			this.arguments = arguments;
			this.actual = actual;
		}
	}

	/**
	 * Yields the applications of the functions that can be computed on
	 * concrete values whose values in the current model of the given relaxed
	 * problem are not the actual ones (only for rational points and finite
	 * values).
	 */
	private List<Mismatch> relaxedMismatches(
			SmtLibProcessSolver helper,
			List<RealRelaxation.Application> applications) {
		List<String> terms = new ArrayList<>();
		for (RealRelaxation.Application app : applications) {
			terms.addAll(app.getRelaxedArguments());
			terms.add(app.getRelaxed());
		}
		List<String> values = helper.values(terms);
		List<Mismatch> mismatches = new ArrayList<>();
		int next = 0;
		for (RealRelaxation.Application app : applications) {
			int arity = app.getRelaxedArguments().size();
			List<String> points = values.subList(next, next + arity);
			String current = values.get(next + arity);
			next += arity + 1;
			try {
				Evaluator evaluator = evaluators.get(app.getFunction());
				List<Number> args = new ArrayList<>();
				for (int i = 0; i < arity; i++) {
					BigDecimal arg = RealRelaxation.rational(points.get(i));
					if (arg == null)
						break;
					args.add(evaluator.arguments.get(i).equals(Sort.FLOAT32) ? (Number) arg.floatValue()
							: (Number) arg.doubleValue());
				}
				if (args.size() != arity)
					continue;
				double actual = evaluator.function.apply(args).doubleValue();
				if (Double.isNaN(actual) || Double.isInfinite(actual))
					continue;
				BigDecimal model = RealRelaxation.rational(current);
				if (model != null
						&& Math.abs(model.doubleValue() - actual) <= 1e-9 * Math.max(1, Math.abs(actual)))
					continue;
				mismatches.add(new Mismatch(app, new ArrayList<>(points), args, actual));
			} catch (RuntimeException e) {
				// e.g., values that are not numbers: the application is
				// ignored
			}
		}
		return mismatches;
	}

	/**
	 * Yields the initial samples of the given function of one argument: its
	 * finite values on a grid of points, dense around zero.
	 */
	private static TreeMap<Double, Double> initialSamples(
			Evaluator evaluator) {
		List<Double> points = new ArrayList<>();
		for (int i = -20; i <= 20; i++)
			points.add(i / 2.0);
		for (int k = 4; k <= 10; k++) {
			points.add(Math.pow(2, k));
			points.add(-Math.pow(2, k));
		}
		TreeMap<Double, Double> samples = new TreeMap<>();
		for (double p : points)
			try {
				Number arg = evaluator.arguments.get(0).equals(Sort.FLOAT32) ? (Number) (float) p : (Number) p;
				double v = evaluator.function.apply(List.of(arg)).doubleValue();
				if (!Double.isNaN(v) && !Double.isInfinite(v))
					samples.put(p, v);
			} catch (RuntimeException e) {
				// the point is not sampled
			}
		return samples;
	}

	/**
	 * Yields the linear interpolation of the given samples at the given real
	 * term (constant out of the sampled range), or the term itself if there
	 * are no samples.
	 */
	private static String interpolation(
			TreeMap<Double, Double> samples,
			String x) {
		if (samples.isEmpty())
			return x;
		List<Map.Entry<Double, Double>> points = new ArrayList<>(samples.entrySet());
		String result = RealRelaxation.real(points.get(points.size() - 1).getValue());
		for (int i = points.size() - 2; i >= 0; i--) {
			double x0 = points.get(i).getKey(), y0 = points.get(i).getValue();
			double x1 = points.get(i + 1).getKey(), y1 = points.get(i + 1).getValue();
			String segment = "(+ " + RealRelaxation.real(y0) + " (* " + RealRelaxation.real((y1 - y0) / (x1 - x0))
					+ " (- " + x + " " + RealRelaxation.real(x0) + ")))";
			result = "(ite (<= " + x + " " + RealRelaxation.real(x1) + ") " + segment + " " + result + ")";
		}
		return "(ite (<= " + x + " " + RealRelaxation.real(points.get(0).getKey()) + ") "
				+ RealRelaxation.real(points.get(0).getValue()) + " " + result + ")";
	}

	/**
	 * Yields the assertions stating the actual values of the functions that
	 * can be computed on concrete values at the points where the current model
	 * of the original problem applies them with different values.
	 */
	private List<String> fixedFacts(
			SmtLibProcessSolver checker,
			List<RealRelaxation.Application> applications) {
		List<String> terms = new ArrayList<>();
		for (RealRelaxation.Application app : applications) {
			terms.addAll(app.getOriginalArguments());
			terms.add(app.getOriginal());
		}
		List<String> values = checker.values(terms);
		List<String> facts = new ArrayList<>();
		int next = 0;
		for (RealRelaxation.Application app : applications) {
			int arity = app.getOriginalArguments().size();
			List<String> points = values.subList(next, next + arity);
			String current = values.get(next + arity);
			next += arity + 1;
			try {
				Evaluator evaluator = evaluators.get(app.getFunction());
				List<Number> args = new ArrayList<>();
				for (int i = 0; i < arity; i++)
					args.add(FloatingPoints.parse(points.get(i), evaluator.arguments.get(i)));
				Number actual = evaluator.function.apply(args);
				Number model = FloatingPoints.parse(current, evaluator.result);
				if (Double.compare(model.doubleValue(), actual.doubleValue()) == 0)
					continue;
				Term literal = evaluator.result.equals(Sort.FLOAT32) ? FloatingPoints.literal(actual.floatValue())
						: FloatingPoints.literal(actual.doubleValue());
				facts.add("(assert (= (" + app.getFunction() + " " + String.join(" ", points) + ") "
						+ literal.toSmtLib() + "))");
			} catch (RuntimeException e) {
				// e.g., values that are not numbers: the application is
				// ignored
			}
		}
		return facts;
	}

	/**
	 * A function that can be computed on concrete values (see
	 * {@link #evaluable(String, List, Sort, Function)}).
	 */
	private static final class Evaluator {

		private final List<Sort> arguments;

		private final Sort result;

		private final Function<List<Number>, Number> function;

		private Evaluator(
				List<Sort> arguments,
				Sort result,
				Function<List<Number>, Number> function) {
			this.arguments = arguments;
			this.result = result;
			this.function = function;
		}
	}

	/**
	 * The functions that can be computed on concrete values, recorded if
	 * queries are relaxed.
	 */
	private final Map<String, Evaluator> evaluators = new HashMap<>();

	@Override
	public void evaluable(
			String function,
			List<Sort> arguments,
			Sort result,
			Function<List<Number>, Number> evaluator) {
		if (relaxation != null
				&& evaluators.putIfAbsent(function, new Evaluator(List.copyOf(arguments), result, evaluator)) == null)
			relaxation.markEvaluable(function);
	}


	/**
	 * Forgets the solver that decided the last check through the relaxation
	 * (see {@link #relaxedCheck()}), if any: its model is the one of the query
	 * until the scope of the query is changed (or checked again).
	 */
	private void unfix() {
		if (fixed != null) {
			fixed.close();
			fixed = null;
		}
	}

	/**
	 * Sends a declaration or definition, recording it (and sending it to the
	 * solver holding the model of the last check, if any) if queries are
	 * relaxed (see {@link #relaxedCheck()}).
	 */
	private void declaration(
			String cmd) {
		command(cmd);
		if (relaxation != null) {
			declarations.add(cmd);
			if (fixed != null)
				fixed.command(cmd);
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
		if (fixed != null)
			// the model of the last check
			return fixed.getValues(terms);
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
		unfix();
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
