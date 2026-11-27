package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the records of the expressions computed by the cases of a
 * {@link SymbolicStore} (see
 * {@link SymbolicStore#computing(SymbolicExpression, ProgramPoint)} and
 * {@link SymbolicStore#restrictTo(ProgramPoint, SymbolicExpression)} and
 * {@link SymbolicStore#restrictTo(SymbolicExpression, ProgramPoint)}).
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

	private static BinaryExpression positive() {
		return new BinaryExpression(BoolType.INSTANCE, X, ZERO, ComparisonGt.INSTANCE, LOC);
	}

	private static BinaryExpression nonPositive() {
		return new BinaryExpression(BoolType.INSTANCE, X, ZERO, ComparisonLe.INSTANCE, LOC);
	}

	/**
	 * A join keeps the expressions computed by the cases of both stores: a case
	 * where a point computed nothing known is kept by the restrictions to that
	 * point, which are then not exact.
	 */
	@Test
	public void joinsKeepTheExpressionsOfBothSides() {
		Constant one = integer(1);
		SymbolicStore computed = identity().assume(positive(), pp).computing(one, pp);
		SymbolicStore other = identity().assume(nonPositive(), pp).computing(integer(2), pp);
		SymbolicStore both = computed.lub(other);
		assertEquals(1, both.restrictTo(pp, one).getCases().size());
		assertTrue(both.restrictsExactly(pp, one));

		// the other side computed nothing at the point
		SymbolicStore missing = computed.lub(identity().assume(nonPositive(), pp));
		assertEquals(2, missing.restrictTo(pp, one).getCases().size());
		assertFalse(missing.restrictsExactly(pp, one));
	}

	/**
	 * When a join exceeds the maximum number of cases, the merged case computed
	 * any of the expressions of the cases: restrictions keep it, and are not
	 * exact.
	 */
	@Test
	public void mergedCasesComputedAnyExpression() {
		SymbolicStore bounded = SymbolicStore.identity(factory, 2);
		Constant one = integer(1);
		SymbolicStore a = bounded.assume(positive(), pp).computing(one, pp);
		SymbolicStore b = bounded.assume(nonPositive(), pp).computing(integer(2), pp);
		SymbolicStore c = bounded.assume(positive(), pp).assume(nonPositive(), pp).computing(integer(3), pp);
		SymbolicStore merged = a.lub(b).lub(c);
		assertEquals(1, merged.getCases().size());
		assertEquals(merged, merged.restrictTo(pp, one));
		assertFalse(merged.restrictsExactly(pp, one));
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
		assertSame(store, store.restrictTo(plus(one, X), Set.of(pp)));
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
		assertEquals(1, store.restrictTo(plus(one, X), Set.of(pp)).getCases().size());
		// an equal constant is not the computed one
		assertEquals(2, store.restrictTo(plus(integer(1), X), Set.of(pp)).getCases().size());

		BinaryExpression first = plus(X, one), second = plus(X, two);
		store = branches(identity(), first, second);
		// an expression equal to a computed one, not an identifier or a
		// constant, stands for it
		assertEquals(1, store.restrictTo(plus(X, integer(1)), Set.of(pp)).getCases().size());
	}

	/**
	 * The stores losing all values are top stores, and their cases are tracked
	 * again from the following computations.
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
	 * An expression equal to a computed one stands for it only if it is
	 * evaluated by the point computing it or by one of its operands: an equal
	 * expression evaluated elsewhere is another computation (e.g., the second
	 * {@code a + 1} of {@code ((a if c else b) + 1) * (a + 1)}), and
	 * restricting to the cases of the first one would drop executions. Unknown
	 * values are recognized only by identity.
	 */
	@Test
	public void equalExpressionsComputedElsewhereDoNotRestrict() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		CFG cfg = new CFG(new CodeMemberDescriptor(LOC, program, false, "m"));
		UnresolvedCall inner = new UnresolvedCall(cfg, new SourceCodeLocation("test", 2, 1), CallType.STATIC, "t",
				"g");
		UnresolvedCall outer = new UnresolvedCall(cfg, new SourceCodeLocation("test", 3, 1), CallType.STATIC, "t",
				"f", inner);
		UnresolvedCall other = new UnresolvedCall(cfg, new SourceCodeLocation("test", 4, 1), CallType.STATIC, "t",
				"h");
		SymbolicStore store = branches(identity(), plus(X, integer(1)), plus(X, integer(2)), inner);
		// evaluated by the point using the operand that computed it
		assertEquals(1, store.restrictTo(plus(X, integer(1)), outer).getCases().size());
		// evaluated elsewhere, or by an unknown point
		assertEquals(2, store.restrictTo(plus(X, integer(1)), other).getCases().size());
		assertEquals(2, store.restrictTo(plus(X, integer(1)), (ProgramPoint) null).getCases().size());

		PushAny unknown = new PushAny(Int32Type.INSTANCE, LOC);
		store = branches(identity(), unknown, integer(2), inner);
		// another unknown value of the same type is equal, but not the same
		assertEquals(2, store.restrictTo(new PushAny(Int32Type.INSTANCE, LOC), outer).getCases().size());
		assertEquals(1, store.restrictTo(unknown, other).getCases().size());
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
	 * A case whose computed expressions are unknown, among cases whose ones are
	 * known, stays unknown: it might have computed any expression, and it is
	 * never dropped.
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

	/**
	 * A restriction is exact when, in each case it keeps, the point computed
	 * only the given expression.
	 */
	@Test
	public void restrictionsOfBranchesAreExact() {
		Constant one = integer(1);
		Constant two = integer(2);
		SymbolicStore store = branches(identity(), one, two);
		assertTrue(store.restrictsExactly(pp, one));
		assertTrue(store.restrictsExactly(pp, two));
	}

	/**
	 * A restriction is not exact if a case it keeps computed also other
	 * expressions at the point, or nothing known.
	 */
	@Test
	public void restrictionsKeepingOtherCasesAreNotExact() {
		Constant one = integer(1);
		Constant two = integer(2);
		// a single case computing both expressions
		SymbolicStore both = identity().computing(one, pp).computing(two, pp);
		assertFalse(both.restrictsExactly(pp, one));
		assertFalse(both.restrictsExactly(pp, two));
		// a case where the point computed nothing known
		SymbolicStore known = identity()
				.assume(new BinaryExpression(BoolType.INSTANCE, X, ZERO, ComparisonGt.INSTANCE, LOC), pp)
				.computing(one, pp);
		SymbolicStore other = identity()
				.assume(new BinaryExpression(BoolType.INSTANCE, X, ZERO, ComparisonLe.INSTANCE, LOC), pp);
		assertFalse(known.lub(other).restrictsExactly(pp, one));
		// a store without tracked expressions
		assertFalse(branches(identity(), one, two).restrictsExactly(null, one));
	}
}
