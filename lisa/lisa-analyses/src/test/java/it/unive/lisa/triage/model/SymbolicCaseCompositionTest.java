package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SymbolicCase#then(SymbolicCase, FreshValueFactory, long)}:
 * the composition of two cases must describe the same transition as the case
 * built by performing the operations of both, one after the other.
 */
public class SymbolicCaseCompositionTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("test", 1, 1);

	private static final Variable X = new Variable(Int32Type.INSTANCE, "x", LOC);

	private static final Variable Y = new Variable(Int32Type.INSTANCE, "y", LOC);

	private static final Variable Z = new Variable(Int32Type.INSTANCE, "z", LOC);

	private static final ScopeToken TOKEN = new ScopeToken(() -> LOC);

	private FreshValueFactory factory;

	@BeforeEach
	public void setup() {
		factory = new FreshValueFactory(new InputSourceResolver(List.of(), null));
	}

	private static Constant integer(
			int value) {
		return new Constant(Int32Type.INSTANCE, value, LOC);
	}

	private static BinaryExpression plus(
			SymbolicExpression l,
			SymbolicExpression r) {
		return new BinaryExpression(Int32Type.INSTANCE, l, r, NumericNonOverflowingAdd.INSTANCE, LOC);
	}

	private static BinaryExpression times(
			SymbolicExpression l,
			SymbolicExpression r) {
		return new BinaryExpression(Int32Type.INSTANCE, l, r, NumericNonOverflowingMul.INSTANCE, LOC);
	}

	private static BinaryExpression gt(
			SymbolicExpression l,
			SymbolicExpression r) {
		return new BinaryExpression(BoolType.INSTANCE, l, r, ComparisonGt.INSTANCE, LOC);
	}

	private SymbolicCase compose(
			SymbolicCase first,
			SymbolicCase second) {
		SymbolicCase c = first.then(second, factory, 1000);
		assertNotNull(c);
		return c;
	}

	@Test
	public void valuesAreComposed() {
		SymbolicCase first = SymbolicCase.IDENTITY.assign(X, plus(Y, integer(1)), factory, null);
		SymbolicCase second = SymbolicCase.IDENTITY.assume(gt(X, integer(5)), factory, null)
				.assign(Z, times(X, integer(2)), factory, null);
		SymbolicCase sequential = first.assume(gt(X, integer(5)), factory, null)
				.assign(Z, times(X, integer(2)), factory, null);
		SymbolicCase composed = compose(first, second);
		assertEquals(sequential.getPathCondition(), composed.getPathCondition());
		assertEquals(sequential.getUpdates(), composed.getUpdates());
		assertEquals(plus(Y, integer(1)), composed.getUpdates().get(X));
		assertEquals(times(plus(Y, integer(1)), integer(2)), composed.getUpdates().get(Z));
		assertFalse(composed.isUnconstrained(Y));
		assertEquals(Y, composed.preIdentifier(Y));
	}

	@Test
	public void laterUpdatesWin() {
		SymbolicCase first = SymbolicCase.IDENTITY.assign(X, integer(1), factory, null);
		SymbolicCase second = SymbolicCase.IDENTITY.assign(X, plus(X, integer(2)), factory, null);
		assertEquals(plus(integer(1), integer(2)), compose(first, second).getUpdates().get(X));
	}

	@Test
	public void forgottenIdentifiersAreUnconstrained() {
		// forgotten by the first case and read by the second one: the same
		// unknown value is read everywhere
		SymbolicCase first = SymbolicCase.IDENTITY.forget(Set.of(X));
		SymbolicCase second = SymbolicCase.IDENTITY.assign(Y, X, factory, null).assign(Z, X, factory, null);
		SymbolicCase composed = compose(first, second);
		assertTrue(composed.getUpdates().get(Y) instanceof SummaryValue);
		assertEquals(composed.getUpdates().get(Y), composed.getUpdates().get(Z));
		assertTrue(composed.isUnconstrained(X));

		// forgotten by the second case: the update of the first one is lost
		first = SymbolicCase.IDENTITY.assign(X, integer(1), factory, null);
		second = SymbolicCase.IDENTITY.forget(Set.of(X));
		composed = compose(first, second);
		assertNull(composed.getUpdates().get(X));
		assertTrue(composed.isUnconstrained(X));

		// forgotten by the first case and assigned by the second one
		first = SymbolicCase.IDENTITY.forget(Set.of(X));
		second = SymbolicCase.IDENTITY.assign(X, Y, factory, null);
		composed = compose(first, second);
		assertEquals(Y, composed.getUpdates().get(X));
		assertFalse(composed.isUnconstrained(X));
	}

	@Test
	public void scopingsAreComposed() throws Exception {
		Identifier outX = (Identifier) X.pushScope(TOKEN, null);
		assertTrue(outX instanceof OutOfScopeIdentifier);
		// the first case assigns x, the second one pushes a scope (as when
		// entering a callee) and assigns a variable of the new scope
		SymbolicCase first = SymbolicCase.IDENTITY.assign(X, integer(3), factory, null);
		SymbolicCase second = SymbolicCase.IDENTITY.scope(TOKEN, true).assign(Z, integer(4), factory, null);
		SymbolicCase sequential = first.scope(TOKEN, true).assign(Z, integer(4), factory, null);
		SymbolicCase composed = compose(first, second);
		assertEquals(sequential.getUpdates(), composed.getUpdates());
		// x is out of scope, with the value given by the first case
		assertEquals(integer(3), composed.getUpdates().get(outX));
		// y is out of scope, with its initial value
		Identifier outY = (Identifier) Y.pushScope(TOKEN, null);
		assertFalse(composed.isUnconstrained(outY));
		assertEquals(Y, composed.preIdentifier(outY));
		// popping the scope afterwards restores the names
		SymbolicCase back = compose(composed, SymbolicCase.IDENTITY.scope(TOKEN, false));
		assertEquals(integer(3), back.getUpdates().get(X));
		assertEquals(Y, back.preIdentifier(Y));
		// the variable of the popped scope is dropped: z is the one of the
		// outer scope, never written
		assertFalse(back.getUpdates().containsKey(Z));
		assertEquals(Z, back.preIdentifier(Z));
		SymbolicCase sequentialBack = sequential.scope(TOKEN, false);
		assertEquals(sequentialBack.getUpdates(), back.getUpdates());
	}

	@Test
	public void havocsAreComposed() {
		SymbolicCase first = SymbolicCase.IDENTITY.assign(X, integer(1), factory, null);
		SymbolicCase composed = compose(first, SymbolicCase.IDENTITY.withHavocAll().assign(Y, integer(2), factory,
				null));
		assertTrue(composed.isUnconstrained(X));
		assertTrue(composed.isUnconstrained(Z));
		assertEquals(integer(2), composed.getUpdates().get(Y));
		// the heap is unconstrained after the composition if it is after
		// either case
		Identifier field = HeapModel.field("f");
		Identifier other = HeapModel.field("g");
		composed = compose(SymbolicCase.IDENTITY.withHeapHavoc(Set.of(field, other)),
				SymbolicCase.IDENTITY.withHeapHavoc(Set.of(field)));
		assertFalse(composed.isUnconstrained(field));
		assertTrue(composed.isUnconstrained(other));
		assertFalse(composed.isUnconstrained(X));
	}

	@Test
	public void largeCompositionsAreRejected() {
		SymbolicCase first = SymbolicCase.IDENTITY.assign(X, plus(Y, Y), factory, null);
		SymbolicCase second = SymbolicCase.IDENTITY.assign(X, plus(X, X), factory, null);
		SymbolicCase composed = first;
		for (int i = 0; i < 20 && composed != null; i++)
			composed = composed.then(second, factory, 100);
		// each composition doubles the size of the value of x
		assertNull(composed);
	}
}
