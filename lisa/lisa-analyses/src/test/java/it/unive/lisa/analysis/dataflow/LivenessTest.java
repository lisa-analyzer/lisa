package it.unive.lisa.analysis.dataflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.Liveness.Liv;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class LivenessTest {

	private final Type intType = Int32Type.INSTANCE;

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final Variable x = new Variable(intType, "x", pp.getLocation());
	private final Variable y = new Variable(intType, "y", pp.getLocation());
	private final Variable z = new Variable(intType, "z", pp.getLocation());

	private final Liveness domain = new Liveness();

	private final PossibleSet<Liv> emptyState = domain.makeLattice();

	private BinaryExpression yPlusZ() {
		return new BinaryExpression(intType, y, z, NumericNonOverflowingAdd.INSTANCE, pp.getLocation());
	}

	@Test
	public void assigningAnExpressionMakesItsIdentifiersLive()
			throws SemanticException {
		// x = y + z: y and z become live, but x itself does not (this is an
		// assignment TO x, not a read of it)
		Set<Liv> gen = domain.gen(emptyState, x, yPlusZ(), pp);
		assertEquals(2, gen.size());
		assertTrue(gen.stream().noneMatch(l -> l.getInvolvedIdentifiers().contains(x)));
	}

	@Test
	public void assigningAConstantMakesNothingLive()
			throws SemanticException {
		assertEquals(Set.of(), domain.gen(emptyState, x, new Constant(intType, 1, pp.getLocation()), pp));
	}

	@Test
	public void evaluatingAnExpressionWithoutAssigningMakesItsIdentifiersLive()
			throws SemanticException {
		Set<Liv> gen = domain.gen(emptyState, yPlusZ(), pp);
		assertEquals(2, gen.size());
	}

	@Test
	public void assigningToAnIdentifierKillsItsLiveness()
			throws SemanticException {
		Set<Liv> kill = domain.kill(emptyState, x, yPlusZ(), pp);
		assertEquals(1, kill.size());
		assertTrue(kill.iterator().next().getInvolvedIdentifiers().contains(x));
	}

	@Test
	public void killDependsOnlyOnTheAssignedIdentifierNotOnTheExpression()
			throws SemanticException {
		Set<Liv> killWithCompoundExpr = domain.kill(emptyState, x, yPlusZ(), pp);
		Set<Liv> killWithConstant = domain.kill(emptyState, x, new Constant(intType, 1, pp.getLocation()), pp);
		assertEquals(killWithCompoundExpr, killWithConstant);
	}

	@Test
	public void nonAssigningEvaluationNeverKillsAnything()
			throws SemanticException {
		assertEquals(Set.of(), domain.kill(emptyState, yPlusZ(), pp));
	}

	@Test
	public void twoLivenessElementsForTheSameIdentifierAreEqual()
			throws SemanticException {
		Liv first = domain.kill(emptyState, x, yPlusZ(), pp).iterator().next();
		Liv second = domain.kill(emptyState, x, new Constant(intType, 1, pp.getLocation()), pp).iterator().next();
		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode());
	}

	@Test
	public void pushingThenPoppingTheSameScopeRestoresTheOriginalIdentifier()
			throws SemanticException {
		Liv liv = domain.kill(emptyState, x, yPlusZ(), pp).iterator().next();
		ScopeToken token = new ScopeToken(() -> pp.getLocation());

		Liv pushed = liv.pushScope(token, pp);
		assertTrue(!pushed.getInvolvedIdentifiers().contains(x));

		Liv popped = pushed.popScope(token, pp);
		assertEquals(liv, popped);
	}

	@Test
	public void backwardPropagationThroughAnAssignmentTransfersLivenessToItsOperands()
			throws SemanticException {
		// simulates, going backward: ...; x = y; <use of x here>
		// starting right after the assignment, x is live (it is about to be
		// read); once we step backward across "x = y", x should no longer be
		// live (this statement is what defines it) while y should become
		// live instead (its value is needed to compute x)
		Set<Liv> useOfX = domain.gen(emptyState, x, pp);
		PossibleSet<Liv> afterUse = emptyState.update(domain.kill(emptyState, x, pp), useOfX);
		assertEquals(1, afterUse.getDataflowElements().size());

		Set<Liv> kill = domain.kill(afterUse, x, y, pp);
		Set<Liv> gen = domain.gen(afterUse, x, y, pp);
		PossibleSet<Liv> beforeAssign = afterUse.update(kill, gen);

		assertTrue(beforeAssign.getDataflowElements().stream().noneMatch(l -> l.getInvolvedIdentifiers().contains(x)));
		assertTrue(beforeAssign.getDataflowElements().stream().anyMatch(l -> l.getInvolvedIdentifiers().contains(y)));
	}

}
