package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.Program;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.TestLanguageFeatures;
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
		return branches(store, positive, other, pp);
	}

	// as above, the expressions being computed at the given point
	private SymbolicStore branches(
			SymbolicStore store,
			SymbolicExpression positive,
			SymbolicExpression other,
			ProgramPoint point) {
		SymbolicStore a = store.assume(new BinaryExpression(BoolType.INSTANCE, X, ZERO, ComparisonGt.INSTANCE, LOC),
				pp).computing(positive, point);
		SymbolicStore b = store.assume(new BinaryExpression(BoolType.INSTANCE, X, ZERO, ComparisonLe.INSTANCE, LOC),
				pp).computing(other, point);
		return a.lub(b);
	}

	private SymbolicStore identity() {
		return SymbolicStore.identity(factory, SymbolicStore.DEFAULT_MAX_CASES);
	}

	private static Constant integer(
			int value) {
		return new Constant(Int32Type.INSTANCE, value, LOC);
	}

	private static BinaryExpression plus(
			SymbolicExpression left,
			SymbolicExpression right) {
		return new BinaryExpression(Int32Type.INSTANCE, left, right, NumericNonOverflowingAdd.INSTANCE, LOC);
	}

	/**
	 * Recording an expression that a point already computed yields the same
	 * store.
	 */
	@Test
	public void computingAgainChangesNothing() {
		Constant one = integer(1);
		SymbolicStore once = identity().computing(one, pp);
		assertSame(once, once.computing(one, pp));
	}

	/**
	 * A store with a single case is never restricted: if a point computed an
	 * expression in some case, it computed it in that one.
	 */
	@Test
	public void singleCasesAreNotRestricted() {
		Constant one = integer(1);
		SymbolicStore store = identity().computing(one, pp);
		assertSame(store, store.restrictTo(pp, integer(2)));
		assertSame(store, store.restrictTo(plus(one, X)));
	}

	/**
	 * An operation over an expression is restricted to the cases computing its
	 * sub-expressions: by identity, or by equality for expressions that are
	 * neither constants nor identifiers.
	 */
	@Test
	public void operationsAreRestrictedThroughTheirSubExpressions() {
		Constant one = integer(1);
		Constant two = integer(2);
		SymbolicStore store = branches(identity(), one, two);
		// by identity, within a larger expression
		assertEquals(1, store.restrictTo(plus(one, X)).getCases().size());
		// an equal constant is not the computed one
		assertEquals(2, store.restrictTo(plus(integer(1), X)).getCases().size());

		BinaryExpression first = plus(X, one), second = plus(X, two);
		store = branches(identity(), first, second);
		// an expression equal to a computed one, not an identifier or a
		// constant, stands for it
		assertEquals(1, store.restrictTo(plus(X, integer(1))).getCases().size());
	}

	/**
	 * The stores losing all values are top stores, and their cases are
	 * tracked again from the following computations.
	 */
	@Test
	public void lostStoresAreTopAndTrackedAgain() {
		SymbolicStore top = identity().top();
		SymbolicStore lost = identity().lost();
		assertTrue(top.isTop());
		assertTrue(lost.isTop());
		Constant one = integer(1);
		SymbolicStore store = branches(lost, one, integer(2));
		assertEquals(1, store.restrictTo(pp, one).getCases().size());
	}

	/**
	 * A call resolved from a call of the program computes on its behalf: the
	 * expressions computed by the call of the program restrict the operations
	 * of the resolved one.
	 */
	@Test
	public void resolvedCallsStandForTheirSources() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		CFG cfg = new CFG(new CodeMemberDescriptor(LOC, program, false, "m"));
		UnresolvedCall source = new UnresolvedCall(cfg, new SourceCodeLocation("test", 2, 1), CallType.STATIC, "t",
				"f");
		CFGCall resolved = new CFGCall(cfg, new SourceCodeLocation("test", 3, 1), CallType.STATIC, "t", "f",
				List.of());
		resolved.setSource(source);
		Constant one = integer(1);
		SymbolicStore store = branches(identity(), one, integer(2), source);
		assertEquals(1, store.restrictTo(resolved, one).getCases().size());
		// computing at the resolved call records it for the source
		store = branches(identity(), one, integer(2), resolved);
		assertEquals(1, store.restrictTo(source, one).getCases().size());
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
