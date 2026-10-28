package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestInterproceduralAnalysis;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Untyped;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

public class LiteralTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG newCfg() {
		ClassUnit unit = new ClassUnit(
				LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "unit", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	private static class AnalysisBackedInterprocedural
			extends
			TestInterproceduralAnalysis<TestAbstractState, AbstractDomain<TestAbstractState>> {

		private final Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> analysis = new Analysis<>(
				new TestAbstractDomain());

		@Override
		public Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> getAnalysis() {
			return analysis;
		}
	}

	@Test
	public void getValueYieldsTheConstructorValue() {
		IntLiteral lit = new IntLiteral(newCfg(), LOC, 42, Untyped.INSTANCE);
		assertEquals(42, lit.getValue());
	}

	@Test
	public void toStringIsTheStringOfTheValue() {
		IntLiteral lit = new IntLiteral(newCfg(), LOC, 42, Untyped.INSTANCE);
		assertEquals("42", lit.toString());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnValueAndSuper() {
		IntLiteral a = new IntLiteral(newCfg(), LOC, 42, Untyped.INSTANCE);
		IntLiteral b = new IntLiteral(newCfg(), LOC, 42, Untyped.INSTANCE);
		IntLiteral c = new IntLiteral(newCfg(), LOC, 7, Untyped.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}

	@Test
	public void compareSameClassFallsBackToStringComparisonOfTheValue() {
		IntLiteral small = new IntLiteral(newCfg(), LOC, 2, Untyped.INSTANCE);
		IntLiteral big = new IntLiteral(newCfg(), LOC, 10, Untyped.INSTANCE);
		// lexicographic ("10" < "2"), not numeric, since
		// Literal#compareSameClass
		// is explicitly documented as comparing toString() representations
		assertTrue(small.compareTo(big) > 0);
		assertFalse(small.equals(big));
	}

	@Test
	public void forwardSemanticsWrapsTheValueInAConstantAndDelegatesToTheAnalysis() throws SemanticException {
		IntLiteral lit = new IntLiteral(newCfg(), LOC, 42, Untyped.INSTANCE);
		AnalysisState<TestAbstractState> entry = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		AnalysisBackedInterprocedural interprocedural = new AnalysisBackedInterprocedural();

		AnalysisState<TestAbstractState> result = lit.forwardSemantics(
				entry, interprocedural, new StatementStore<>(entry));

		ExpressionSet computed = result.getExecutionExpressions();
		assertEquals(1, computed.elements().size());
		Iterator<SymbolicExpression> it = computed.iterator();
		SymbolicExpression e = it.next();
		assertTrue(e instanceof Constant);
		Constant constant = (Constant) e;
		assertEquals(42, constant.getValue());
		assertEquals(Untyped.INSTANCE, constant.getStaticType());
	}

}
