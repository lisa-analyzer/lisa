package it.unive.lisa.triage.smt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SmtLibProcessSolver}. Tests are skipped for solvers that
 * are not available in the current environment.
 */
public class SmtLibProcessSolverTest {

	private static final Map<String, Supplier<SmtSolver>> SOLVERS = Map.of(
			"z3", SmtLibProcessSolver::z3,
			"cvc5", SmtLibProcessSolver::cvc5);

	/**
	 * Runs the given test on each available solver, skipping the test if none
	 * is available.
	 */
	private static void onEachSolver(
			Consumer<SmtSolver> test) {
		boolean any = false;
		for (Map.Entry<String, Supplier<SmtSolver>> entry : SOLVERS.entrySet())
			if (SmtLibProcessSolver.isAvailable(entry.getKey())) {
				any = true;
				try (SmtSolver s = entry.getValue().get()) {
					test.accept(s);
				}
			}
		assumeTrue(any, "No SMT solver is available");
	}

	@Test
	public void satWithModel() {
		onEachSolver(s -> {
			Term x = s.declare("x", Sort.INT);
			Term y = s.declare("a strange.name", Sort.INT);
			s.assertTerm(Term.app(">", Sort.BOOL, x, Term.integer(3)));
			s.assertTerm(Term.eq(y, Term.app("-", Sort.INT, Term.integer(0), x)));
			assertEquals(SmtResult.SAT, s.check());
			Map<Term, String> values = s.getValues(List.of(x, y));
			long vx = Long.parseLong(values.get(x));
			assertTrue(vx > 3);
			assertEquals("(- " + vx + ")", values.get(y));
		});
	}

	@Test
	public void pushPopAndUnsatCore() {
		onEachSolver(s -> {
			Term x = s.declare("x", Sort.INT);
			s.assertNamed("positive", Term.app(">", Sort.BOOL, x, Term.integer(0)));
			s.assertNamed("irrelevant", Term.app("<", Sort.BOOL, x, Term.integer(100)));
			s.push();
			s.assertNamed("negative", Term.app("<", Sort.BOOL, x, Term.integer(0)));
			assertEquals(SmtResult.UNSAT, s.check());
			assertEquals(Set.of("positive", "negative"), Set.copyOf(s.getUnsatCore()));
			s.pop();
			assertEquals(SmtResult.SAT, s.check());
		});
	}

	@Test
	public void stringsAndBitVectors() {
		onEachSolver(s -> {
			Term str = s.declare("s", Sort.STRING);
			s.assertTerm(Term.app("str.prefixof", Sort.BOOL, Term.string("ad\"min"), str));
			s.assertTerm(Term.eq(Term.app("str.len", Sort.INT, str), Term.integer(6)));
			Term bv = s.declare("b", Sort.bitVector(8));
			// 127 + 1 overflows to -128 in two's complement
			s.assertTerm(Term.eq(bv, Term.app("bvadd", Sort.bitVector(8), Term.bitVector(127, 8),
					Term.bitVector(1, 8))));
			assertEquals(SmtResult.SAT, s.check());
			Map<Term, String> values = s.getValues(List.of(str, bv));
			assertTrue(values.get(str).startsWith("\"ad\"\"min"), values.get(str));
			assertTrue(values.get(bv).equals("#x80") || values.get(bv).equals("#b10000000"), values.get(bv));
		});
	}

	@Test
	public void errorsDoNotBreakTheSolver() {
		onEachSolver(s -> {
			Term x = s.declare("x", Sort.INT);
			// undeclared constant
			assertThrows(SmtException.class,
					() -> s.assertTerm(Term.app(">", Sort.BOOL, Term.constant("y", Sort.INT), x)));
			s.assertTerm(Term.app(">", Sort.BOOL, x, Term.integer(0)));
			assertEquals(SmtResult.SAT, s.check());
		});
	}

	@Test
	public void timeoutYieldsUnknown() {
		assumeTrue(SmtLibProcessSolver.isAvailable("z3"), "z3 is not available");
		try (SmtSolver s = SmtLibProcessSolver.z3("z3", 200)) {
			// x^3 + y^3 = z^3 has no positive solution, and nonlinear
			// integer arithmetic is undecidable: the solver gives up
			Term x = s.declare("x", Sort.INT);
			Term y = s.declare("y", Sort.INT);
			Term z = s.declare("z", Sort.INT);
			Term zero = Term.integer(0);
			s.assertTerm(Term.and(
					Term.app(">", Sort.BOOL, x, zero),
					Term.app(">", Sort.BOOL, y, zero),
					Term.app(">", Sort.BOOL, z, zero)));
			s.assertTerm(Term.eq(
					Term.app("+", Sort.INT, Term.app("*", Sort.INT, x, x, x), Term.app("*", Sort.INT, y, y, y)),
					Term.app("*", Sort.INT, z, z, z)));
			assertEquals(SmtResult.UNKNOWN, s.check());
			assertFalse(s.getReasonUnknown().isEmpty());
		}
	}

	@Test
	public void unavailableSolver() {
		assertFalse(SmtLibProcessSolver.isAvailable("this-solver-does-not-exist"));
		assertThrows(SmtSolverUnavailableException.class,
				() -> SmtLibProcessSolver.z3("this-solver-does-not-exist", 1000));
	}

	@Test
	public void closedSolver() {
		assumeTrue(SmtLibProcessSolver.isAvailable("z3"), "z3 is not available");
		SmtSolver s = SmtLibProcessSolver.z3();
		s.close();
		assertThrows(SmtException.class, s::check);
		// closing twice is harmless
		s.close();
	}
}
