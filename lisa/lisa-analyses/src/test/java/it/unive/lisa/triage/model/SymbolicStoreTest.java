package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the records of the expressions computed by the cases of a
 * {@link SymbolicStore} (see
 * {@link SymbolicStore#computing(SymbolicExpression, ProgramPoint)} and
 * {@link SymbolicStore#restrictTo(ProgramPoint, SymbolicExpression)}).
 */
public class SymbolicStoreTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("test", 1, 1);

	private static final Variable X = new Variable(Int32Type.INSTANCE, "x", LOC);

	private static final Constant ZERO = new Constant(Int32Type.INSTANCE, 0, LOC);

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private FreshValueFactory factory;

	@BeforeEach
	public void setup() {
		factory = new FreshValueFactory(new InputSourceResolver(List.of(), null));
	}

	// a store with the cases of the given one where x > 0, computing the
	// first expression, and where x <= 0, computing the second one
	private SymbolicStore branches(
			SymbolicStore store,
			SymbolicExpression positive,
			SymbolicExpression other) {
		SymbolicStore a = store.assume(new BinaryExpression(BoolType.INSTANCE, X, ZERO, ComparisonGt.INSTANCE, LOC),
				pp).computing(positive, pp);
		SymbolicStore b = store.assume(new BinaryExpression(BoolType.INSTANCE, X, ZERO, ComparisonLe.INSTANCE, LOC),
				pp).computing(other, pp);
		return a.lub(b);
	}

	/**
	 * Each case computes its own expression: an operation over one of them
	 * applies to the cases computing it.
	 */
	@Test
	public void operationsApplyToTheCasesComputingTheirOperands() {
		Constant one = new Constant(Int32Type.INSTANCE, 1, LOC);
		Constant two = new Constant(Int32Type.INSTANCE, 2, LOC);
		SymbolicStore store = branches(SymbolicStore.identity(factory, SymbolicStore.DEFAULT_MAX_CASES), one, two);
		assertEquals(2, store.getCases().size());
		assertEquals(1, store.restrictTo(pp, one).getCases().size());
		assertEquals(1, store.restrictTo(pp, two).getCases().size());
	}

	/**
	 * Once the computed expressions of all the cases are unknown (e.g., after
	 * all values are lost), they are tracked again from the following
	 * computations, as from the identity.
	 */
	@Test
	public void computedExpressionsAreTrackedAgainOnceAllAreUnknown() {
		Constant one = new Constant(Int32Type.INSTANCE, 1, LOC);
		Constant two = new Constant(Int32Type.INSTANCE, 2, LOC);
		SymbolicStore lost = SymbolicStore.identity(factory, SymbolicStore.DEFAULT_MAX_CASES).top();
		SymbolicStore store = branches(lost, one, two);
		assertEquals(2, store.getCases().size());
		assertEquals(1, store.restrictTo(pp, one).getCases().size());
		assertEquals(1, store.restrictTo(pp, two).getCases().size());
	}

	/**
	 * A case whose computed expressions are unknown, among cases whose ones
	 * are known, stays unknown: it might have computed any expression, and it
	 * is never dropped.
	 */
	@Test
	public void unknownCasesAmongKnownOnesAreKept() {
		Constant one = new Constant(Int32Type.INSTANCE, 1, LOC);
		Constant two = new Constant(Int32Type.INSTANCE, 2, LOC);
		SymbolicStore known = SymbolicStore.identity(factory, SymbolicStore.DEFAULT_MAX_CASES)
				.assume(new BinaryExpression(BoolType.INSTANCE, X, ZERO, ComparisonGt.INSTANCE, LOC), pp);
		SymbolicStore unknown = SymbolicStore.identity(factory, SymbolicStore.DEFAULT_MAX_CASES).top();
		SymbolicStore store = known.lub(unknown).computing(one, pp);
		assertEquals(2, store.getCases().size());
		// the unknown case might have computed one
		assertEquals(2, store.restrictTo(pp, one).getCases().size());
		assertEquals(2, store.restrictTo(pp, two).getCases().size());
	}
}
