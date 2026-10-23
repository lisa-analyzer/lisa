package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.program.testsupport.RecordingDomain;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.testsupport.UnitLattice;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.type.NullType;
import org.junit.jupiter.api.Test;

public class NullLiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		NullLiteral lit = new NullLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		assertEquals(NullType.INSTANCE, lit.getStaticType());
		assertEquals(null, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameLocation() {
		NullLiteral a = new NullLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		NullLiteral b = new NullLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		NullLiteral a = new NullLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		NullLiteral b = new NullLiteral(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2));
		assertFalse(a.equals(b));
	}

	@Test
	public void forwardSemanticsPushesANullConstantInsteadOfAPlainConstant()
			throws SemanticException {
		// per its javadoc, NullLiteral deviates from the base Literal class:
		// it must evaluate to a NullConstant (a heap expression), not a
		// generic Constant wrapping a null value
		NullLiteral lit = new NullLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		RecordingDomain domain = new RecordingDomain();
		FakeInterproceduralAnalysis interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		AnalysisState<UnitLattice> state = new AnalysisState<>(programState).withExecution(programState);
		StatementStore<UnitLattice> expressions = new StatementStore<>(state);

		AnalysisState<UnitLattice> result = lit.forwardSemantics(state, interprocedural, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		assertTrue(domain.smallStepCalls.get(0) instanceof NullConstant);
		assertEquals(new NullConstant(lit.getLocation()), domain.smallStepCalls.get(0));
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
	}

}
