package it.unive.lisa.triage.smt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RealRelaxation}, and for its use in
 * {@link SmtLibProcessSolver#relaxedCheck()} (skipped if z3 is not available).
 */
public class RealRelaxationTest {

	private static final Term RNE = Term.value("RNE", Sort.ROUNDING_MODE);

	private static Datatype value() {
		Map<String, List<Datatype.Field>> constructors = new LinkedHashMap<>();
		constructors.put("V.Int", List.of(new Datatype.Field("V.i", Sort.INT)));
		constructors.put("V.F64", List.of(new Datatype.Field("V.f", Sort.FLOAT64)));
		return new Datatype("V", constructors);
	}

	@Test
	public void floatingPointNumbersBecomeRealNumbers() {
		RealRelaxation r = new RealRelaxation();
		Datatype v = value();
		r.declareDatatype(v);
		r.declareConstant("x", Sort.FLOAT64);
		r.declareConstant("n", Sort.INT);
		r.declareConstant("w", v.getSort());
		Term x = Term.constant("x", Sort.FLOAT64);
		Term sum = Term.app("fp.add", Sort.FLOAT64, RNE, x, FloatingPoints.literal(1.5));
		r.define("d", Sort.FLOAT64, sum.toSmtLib());
		r.assertion(Term.app("fp.lt", Sort.BOOL, Term.constant("d", Sort.FLOAT64), FloatingPoints.literal(-2.0))
				.toSmtLib());
		r.assertion(Term.app(">", Sort.BOOL, Term.constant("n", Sort.INT), Term.integer(0)).toSmtLib());
		r.assertion(Term.app("fp.isNaN", Sort.BOOL, x).toSmtLib());
		r.assertion(Term.app("fp.leq", Sort.BOOL, v.select("V.f", Term.constant("w", v.getSort())), x).toSmtLib());

		RealRelaxation.Relaxed relaxed = r.relax();
		assertNotNull(relaxed);
		String all = String.join("\n", relaxed.getCommands());
		assertTrue(all.contains("(declare-const x!real Real)"), all);
		assertTrue(all.contains("(define-fun d!real () Real (+ x!real 1.5))"), all);
		assertTrue(all.contains("(assert (< d!real (- 2.0)))"), all);
		// constants that do not involve floating point numbers are shared
		assertTrue(all.contains("(assert (> n 0))"), all);
		assertFalse(all.contains("n!real"), all);
		// datatypes holding floating point numbers are copied
		assertTrue(all.contains("(declare-datatypes ((V!real 0)) (((V.Int!real (V.i!real Int)) "
				+ "(V.F64!real (V.f!real Real)))))"), all);
		assertTrue(all.contains("(assert (<= (V.f!real w!real) x!real))"), all);
		// floating point numbers are never NaNs
		assertTrue(all.contains("(assert false)"), all);
		assertEquals(1, relaxed.getPins().size());
		assertEquals("x", relaxed.getPins().get(0).getOriginal());

		// copies are declared once
		RealRelaxation.Relaxed again = r.relax();
		assertNotNull(again);
		assertFalse(String.join("\n", again.getCommands()).contains("declare"));
		assertEquals(1, again.getPins().size());
	}

	@Test
	public void assertionsWithoutFloatingPointNumbersAreNotRelaxed() {
		RealRelaxation r = new RealRelaxation();
		r.declareConstant("n", Sort.INT);
		r.assertion(Term.app(">", Sort.BOOL, Term.constant("n", Sort.INT), Term.integer(0)).toSmtLib());
		assertNull(r.relax());
	}

	@Test
	public void scopes() {
		RealRelaxation r = new RealRelaxation();
		r.declareConstant("x", Sort.FLOAT64);
		Term x = Term.constant("x", Sort.FLOAT64);
		r.push();
		r.assertion(Term.app("fp.isZero", Sort.BOOL, x).toSmtLib());
		assertNotNull(r.relax());
		r.pop();
		assertNull(r.relax());
	}

	@Test
	public void valuesAreRoundedToFloatingPointNumbers() {
		RealRelaxation r = new RealRelaxation();
		r.declareConstant("x", Sort.FLOAT64);
		r.assertion(Term.app("fp.isZero", Sort.BOOL, Term.constant("x", Sort.FLOAT64)).toSmtLib());
		RealRelaxation.Pin pin = r.relax().getPins().get(0);
		assertEquals("(assert (= x " + FloatingPoints.literal(-0.25).toSmtLib() + "))", pin.fix("(- (/ 1.0 4.0))"));
		assertEquals("(assert (= x " + FloatingPoints.literal(1.0 / 3).toSmtLib() + "))", pin.fix("(/ 1.0 3.0)"));
		assertNull(pin.fix("(root-obj (+ (^ x 2) (- 2)) 1)"));
	}

	@Test
	public void relaxedModelsAreCheckedOnTheOriginalQuery() {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		try (SmtLibProcessSolver s = SmtLibProcessSolver.z3()) {
			Term x = s.declare("x", Sort.FLOAT64);
			Term sq = Term.app("fp.mul", Sort.FLOAT64, RNE, x, x);
			s.push();
			s.assertTerm(Term.app("fp.gt", Sort.BOOL, sq, FloatingPoints.literal(2.0)));
			s.assertTerm(Term.app("fp.lt", Sort.BOOL, x, FloatingPoints.literal(3.0)));
			assertEquals(SmtResult.SAT, s.relaxedCheck());
			double vx = FloatingPoints.parse(s.getValues(List.of(x)).get(x), Sort.FLOAT64).doubleValue();
			assertTrue(vx * vx > 2.0 && vx < 3.0, Double.toString(vx));
			assertEquals(1, s.getRelaxations());
			// further assertions are not affected by the values of the model
			s.assertTerm(Term.app("fp.isNegative", Sort.BOOL, x));
			assertEquals(SmtResult.SAT, s.check());
			s.pop();
			// the scopes are balanced
			s.push();
			s.assertTerm(Term.app("fp.isNaN", Sort.BOOL, x));
			assertEquals(SmtResult.SAT, s.check());
			s.assertTerm(Term.not(Term.app("fp.isNaN", Sort.BOOL, x)));
			assertEquals(SmtResult.UNSAT, s.check());
			s.pop();
			assertEquals(SmtResult.SAT, s.check());

			// queries whose relaxation has no model stay undecided
			s.push();
			s.assertTerm(Term.app("fp.isInfinite", Sort.BOOL, x));
			assertEquals(SmtResult.UNKNOWN, s.relaxedCheck());
			s.pop();
		}
	}
}
