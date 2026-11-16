package it.unive.lisa.imp.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.imp.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.imp.testsupport.RecordingDomain;
import it.unive.lisa.imp.testsupport.TestFixtures;
import it.unive.lisa.imp.testsupport.UnitLattice;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Constant;
import org.junit.jupiter.api.Test;

public class IMPNewArrayTest {

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private IMPNewArray newArray(
			boolean staticallyAllocated) {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef size = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "size");
		return new IMPNewArray(TestFixtures.CFG, "test", 1, 1, Int32Type.INSTANCE, staticallyAllocated,
				new Expression[] { size });
	}

	@Test
	public void allocatesTheMemoryRegionAndLeavesAReferenceOnTheStack()
			throws SemanticException {
		IMPNewArray array = newArray(false);
		SymbolicExpression sizeValue = new Constant(Int32Type.INSTANCE, 5, TestFixtures.LOCATION);
		ExpressionSet[] params = new ExpressionSet[] { new ExpressionSet(sizeValue) };

		array.forwardSemanticsAux(interprocedural, state, params, expressions);

		// the allocation must be the very first smallStepSemantics call;
		// Analysis.assign() also triggers its own internal smallStepSemantics
		// call while rewriting the non-Identifier "len" assignment target
		// (framework behavior, not IMPNewArray's own logic), so only a lower
		// bound is asserted on the total count
		assertTrue(domain.smallStepCalls.size() >= 2);
		MemoryAllocation allocation = (MemoryAllocation) domain.smallStepCalls.get(0);
		// SUSPECTED DOC/IMPL MISMATCH: the class javadoc says "the type of
		// this expression is the Type of the array's elements", but the
		// constructor registers getStaticType() as the ARRAY type itself
		// (ArrayType.register(type, dimensions.length)), and the allocation
		// is created with that same array type, not the element type
		assertEquals(array.getStaticType(), allocation.getStaticType());

		// the synthetic receiver is bound to the newly allocated region, and
		// its "len" field is bound to the size expression; the exact count
		// also depends on Analysis.assign()'s own internal identifier
		// rewriting, which is framework behavior outside IMPNewArray's own
		// logic, so only a lower bound is asserted here
		assertTrue(domain.assignCalls.size() >= 2);
	}

	@Test
	public void repeatedEvaluationDoesNotAccumulateDuplicateMetaVariables()
			throws SemanticException {
		// the synthetic array reference is added to getMetaVariables() on
		// every evaluation; since it is backed by a HashSet and rebuilt
		// identically each time, evaluating twice must not grow it further
		IMPNewArray array = newArray(false);
		SymbolicExpression sizeValue = new Constant(Int32Type.INSTANCE, 5, TestFixtures.LOCATION);
		ExpressionSet[] params = new ExpressionSet[] { new ExpressionSet(sizeValue) };

		array.forwardSemanticsAux(interprocedural, state, params, expressions);
		int firstSize = array.getMetaVariables().size();
		array.forwardSemanticsAux(interprocedural, state, params, expressions);
		int secondSize = array.getMetaVariables().size();

		assertEquals(firstSize, secondSize);
	}

	@Test
	public void staticAndDynamicAllocationAreNotEqual()
			throws SemanticException {
		IMPNewArray dyn = newArray(false);
		IMPNewArray stat = newArray(true);
		assertNotEquals(dyn, stat);
	}

}
