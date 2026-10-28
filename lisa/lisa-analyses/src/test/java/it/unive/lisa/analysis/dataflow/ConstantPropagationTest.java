package it.unive.lisa.analysis.dataflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.ConstantPropagation.CP;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMod;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ConstantPropagationTest {

	private final Type intType = Int32Type.INSTANCE;

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final Variable x = new Variable(intType, "x", pp.getLocation());
	private final Variable y = new Variable(intType, "y", pp.getLocation());

	private final ConstantPropagation domain = new ConstantPropagation();

	private final DefiniteSet<CP> emptyState = domain.makeLattice();

	private Constant mkConst(
			int v) {
		return new Constant(intType, v, pp.getLocation());
	}

	private BinaryExpression mkBin(
			it.unive.lisa.symbolic.value.operator.binary.BinaryOperator op,
			it.unive.lisa.symbolic.value.ValueExpression left,
			it.unive.lisa.symbolic.value.ValueExpression right) {
		return new BinaryExpression(intType, left, right, op, pp.getLocation());
	}

	@Test
	public void assigningAConstantGeneratesItsValue()
			throws SemanticException {
		Set<CP> gen = domain.gen(emptyState, x, mkConst(5), pp);
		assertEquals(1, gen.size());
	}

	@Test
	public void assigningANonConstantExpressionGeneratesNothing()
			throws SemanticException {
		// y has no known constant value in the empty state
		assertEquals(Set.of(), domain.gen(emptyState, x, y, pp));
	}

	@Test
	public void additionOfTwoConstantsIsFoldedCorrectly()
			throws SemanticException {
		CP result = domain.gen(emptyState, x, mkBin(NumericNonOverflowingAdd.INSTANCE, mkConst(2), mkConst(3)), pp)
				.iterator().next();
		CP expected = domain.gen(emptyState, x, mkConst(5), pp).iterator().next();
		assertEquals(expected, result);
	}

	@Test
	public void subtractionOfTwoConstantsIsFoldedCorrectly()
			throws SemanticException {
		CP result = domain.gen(emptyState, x, mkBin(NumericNonOverflowingSub.INSTANCE, mkConst(10), mkConst(4)), pp)
				.iterator().next();
		CP expected = domain.gen(emptyState, x, mkConst(6), pp).iterator().next();
		assertEquals(expected, result);
	}

	@Test
	public void multiplicationOfTwoConstantsIsFoldedCorrectly()
			throws SemanticException {
		CP result = domain.gen(emptyState, x, mkBin(NumericNonOverflowingMul.INSTANCE, mkConst(3), mkConst(4)), pp)
				.iterator().next();
		CP expected = domain.gen(emptyState, x, mkConst(12), pp).iterator().next();
		assertEquals(expected, result);
	}

	@Test
	public void divisionOfTwoNonZeroConstantsIsFoldedCorrectly()
			throws SemanticException {
		CP result = domain.gen(emptyState, x, mkBin(NumericNonOverflowingDiv.INSTANCE, mkConst(12), mkConst(4)), pp)
				.iterator().next();
		CP expected = domain.gen(emptyState, x, mkConst(3), pp).iterator().next();
		assertEquals(expected, result);
	}

	// SUSPECTED BUG (Evaluator.visit(BinaryExpression), DivisionOperator
	// branch): the zero-check tests the LEFT operand ("left == 0 ? null :
	// ...") instead of the RIGHT one. Dividing zero by a non-zero constant is
	// a perfectly well-defined operation (0 / 5 == 0) and should be folded,
	// not treated as "unknown". This test is expected to fail against the
	// current implementation, which wrongly returns no generated element.
	@Test
	public void dividingZeroByANonZeroConstantIsFoldedToZero()
			throws SemanticException {
		Set<CP> gen = domain.gen(emptyState, x, mkBin(NumericNonOverflowingDiv.INSTANCE, mkConst(0), mkConst(5)), pp);
		CP expected = domain.gen(emptyState, x, mkConst(0), pp).iterator().next();
		assertEquals(Set.of(expected), gen);
	}

	// SUSPECTED BUG (same root cause as above): since the zero-check guards
	// the wrong operand, dividing by an actual zero right operand is never
	// guarded at all, so evaluating "5 / 0" reaches a raw integer division
	// and throws ArithmeticException instead of yielding "unknown" (no
	// generated element). This test is expected to fail (with an unexpected
	// ArithmeticException) against the current implementation.
	@Test
	public void dividingByZeroGeneratesNothingAndDoesNotThrow()
			throws SemanticException {
		Set<CP> gen = domain.gen(emptyState, x, mkBin(NumericNonOverflowingDiv.INSTANCE, mkConst(5), mkConst(0)), pp);
		assertEquals(Set.of(), gen);
	}

	@Test
	public void moduloByZeroGeneratesNothing()
			throws SemanticException {
		assertEquals(Set.of(),
				domain.gen(emptyState, x, mkBin(NumericNonOverflowingMod.INSTANCE, mkConst(5), mkConst(0)), pp));
	}

	@Test
	public void moduloOfTwoNonZeroConstantsIsFoldedCorrectly()
			throws SemanticException {
		CP result = domain.gen(emptyState, x, mkBin(NumericNonOverflowingMod.INSTANCE, mkConst(7), mkConst(3)), pp)
				.iterator().next();
		CP expected = domain.gen(emptyState, x, mkConst(1), pp).iterator().next();
		assertEquals(expected, result);
	}

	@Test
	public void negationOfAConstantIsFoldedCorrectly()
			throws SemanticException {
		UnaryExpression neg = new UnaryExpression(intType, mkConst(5), NumericNegation.INSTANCE, pp.getLocation());
		CP result = domain.gen(emptyState, x, neg, pp).iterator().next();
		CP expected = domain.gen(emptyState, x, mkConst(-5), pp).iterator().next();
		assertEquals(expected, result);
	}

	@Test
	public void negationOfANonConstantGeneratesNothing()
			throws SemanticException {
		UnaryExpression neg = new UnaryExpression(intType, y, NumericNegation.INSTANCE, pp.getLocation());
		assertEquals(Set.of(), domain.gen(emptyState, x, neg, pp));
	}

	@Test
	public void copyingAnAlreadyKnownConstantPropagatesItsValue()
			throws SemanticException {
		CP xIsFive = domain.gen(emptyState, x, mkConst(5), pp).iterator().next();
		DefiniteSet<CP> state = emptyState.update(Set.of(), Set.of(xIsFive));

		// y = x, where x is already known to be 5
		CP result = domain.gen(state, y, x, pp).iterator().next();
		CP expected = domain.gen(emptyState, y, mkConst(5), pp).iterator().next();
		assertEquals(expected, result);
	}

	@Test
	public void nonAssigningEvaluationNeverGeneratesAnything()
			throws SemanticException {
		assertEquals(Set.of(), domain.gen(emptyState, mkConst(5), pp));
	}

	@Test
	public void reassigningAnIdentifierKillsItsPreviousConstant()
			throws SemanticException {
		CP xIsFive = domain.gen(emptyState, x, mkConst(5), pp).iterator().next();
		CP yIsThree = domain.gen(emptyState, y, mkConst(3), pp).iterator().next();
		DefiniteSet<CP> state = emptyState.update(Set.of(), Set.of(xIsFive, yIsThree));

		assertEquals(Set.of(xIsFive), domain.kill(state, x, mkConst(9), pp));
	}

	@Test
	public void nonAssigningEvaluationNeverKillsAnything()
			throws SemanticException {
		CP xIsFive = domain.gen(emptyState, x, mkConst(5), pp).iterator().next();
		DefiniteSet<CP> state = emptyState.update(Set.of(), Set.of(xIsFive));

		assertEquals(Set.of(), domain.kill(state, mkConst(1), pp));
	}

	@Test
	public void reassignmentEndToEndReplacesTheOldConstant()
			throws SemanticException {
		DefiniteSet<CP> state = emptyState;
		state = state.update(domain.kill(state, x, mkConst(5), pp), domain.gen(state, x, mkConst(5), pp));
		assertEquals(1, state.getDataflowElements().size());

		state = state.update(domain.kill(state, x, mkConst(7), pp), domain.gen(state, x, mkConst(7), pp));
		CP expected = domain.gen(emptyState, x, mkConst(7), pp).iterator().next();
		assertEquals(Set.of(expected), state.getDataflowElements());
	}

	@Test
	public void twoConstantPropagationElementsWithSameIdAndValueAreEqual()
			throws SemanticException {
		CP first = domain.gen(emptyState, x, mkConst(5), pp).iterator().next();
		CP second = domain.gen(emptyState, x, mkConst(5), pp).iterator().next();
		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode());
	}

	@Test
	public void twoConstantPropagationElementsWithDifferentValuesAreNotEqual()
			throws SemanticException {
		CP five = domain.gen(emptyState, x, mkConst(5), pp).iterator().next();
		CP six = domain.gen(emptyState, x, mkConst(6), pp).iterator().next();
		assertTrue(!five.equals(six));
	}

}
