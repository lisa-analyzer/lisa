package it.unive.lisa.program.cfg;

import static it.unive.lisa.util.collections.CollectionUtilities.collect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowExtractor;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.BinaryNativeCall;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Literal;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.common.Int32;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import org.junit.Test;

public class ConditionalsExtractionTest {

	private static final CompilationUnit unit = new CompilationUnit(new SourceCodeLocation("unknown", 0, 0), "Testing",
			false);

	private static void checkMatrix(String label, Collection<Statement> nodes,
			Collection<Statement> expected) {

		Collection<Statement> missingNodes = new HashSet<>(expected), extraNodes = new HashSet<>(nodes);
		missingNodes.removeAll(nodes);
		extraNodes.removeAll(expected);

		if (!missingNodes.isEmpty())
			System.err.println("The following nodes are missing in " + label + ": " + missingNodes);
		if (!extraNodes.isEmpty())
			System.err.println("The following nodes are spurious in " + label + ": " + extraNodes);

		assertTrue("Set of nodes does not match the expected results",
				missingNodes.isEmpty() && extraNodes.isEmpty());
	}

	private void assertIf(Statement condition, Statement follower, Collection<Statement> tnodes,
			Collection<Statement> fnodes, IfThenElse ith) {
		assertEquals("Wrong condition: " + ith.getCondition(), condition, ith.getCondition());
		assertEquals("Wrong follower: " + ith.getFirstFollower(), follower, ith.getFirstFollower());
		checkMatrix("true branch", ith.getTrueBranch(), tnodes);
		checkMatrix("false branch", ith.getFalseBranch(), fnodes);
	}

	private void assertLoop(Statement condition, Statement follower, Collection<Statement> nodes, Loop loop) {
		assertEquals("Wrong condition: " + loop.getCondition(), condition, loop.getCondition());
		assertEquals("Wrong follower: " + loop.getFirstFollower(), follower, loop.getFirstFollower());
		checkMatrix("loop body", loop.getBody(), nodes);
	}

	class IMPNotEqual extends BinaryNativeCall {

		protected IMPNotEqual(CFG cfg, CodeLocation location, Expression left, Expression right) {
			super(cfg, location, "!=", left, right);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(AnalysisState<A, H, V> entryState,
						InterproceduralAnalysis<A, H, V> interprocedural, AnalysisState<A, H, V> leftState,
						SymbolicExpression leftExp, AnalysisState<A, H, V> rightState, SymbolicExpression rightExp)
						throws SemanticException {
			return null;
		}
	}

	@Test
	public void testSimpleIf() {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "simpleIf"));
		Literal constant = new Literal(cfg, unknown, 5, Int32.INSTANCE);
		IMPNotEqual condition = new IMPNotEqual(cfg, unknown, constant, constant);
		Assignment a1 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "l"), constant);
		Assignment a2 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "r"), constant);
		Return ret = new Return(cfg, unknown, new VariableRef(cfg, unknown, "x"));
		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(condition, a1));
		cfg.addEdge(new FalseEdge(condition, a2));
		cfg.addEdge(new SequentialEdge(a1, ret));
		cfg.addEdge(new SequentialEdge(a2, ret));

		Collection<ControlFlowStructure> extracted = new ControlFlowExtractor(cfg).extract();
		assertEquals("Incorrect number of structures: " + extracted.size(), 1, extracted.size());

		ControlFlowStructure struct = extracted.iterator().next();
		assertTrue(struct + " does not represent an if-then-else", struct instanceof IfThenElse);

		assertIf(condition, ret, collect(a1), collect(a2), (IfThenElse) struct);
	}

	@Test
	public void testEmptyIf() {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "emptyIf"));
		Literal constant = new Literal(cfg, unknown, 5, Int32.INSTANCE);
		IMPNotEqual condition = new IMPNotEqual(cfg, unknown, constant, constant);
		Return ret = new Return(cfg, unknown, new VariableRef(cfg, unknown, "x"));
		cfg.addNode(condition, true);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(condition, ret));
		cfg.addEdge(new FalseEdge(condition, ret));

		Collection<ControlFlowStructure> extracted = new ControlFlowExtractor(cfg).extract();
		assertEquals("Incorrect number of structures: " + extracted.size(), 1, extracted.size());

		ControlFlowStructure struct = extracted.iterator().next();
		assertTrue(struct + " does not represent an if-then-else", struct instanceof IfThenElse);

		assertIf(condition, ret, Collections.emptyList(), Collections.emptyList(), (IfThenElse) struct);
	}

	@Test
	public void testIfWithEmptyBranch() {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "emptyBranch"));
		Literal constant = new Literal(cfg, unknown, 5, Int32.INSTANCE);
		IMPNotEqual condition = new IMPNotEqual(cfg, unknown, constant, constant);
		Assignment a1 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "l"), constant);
		Assignment a2 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "r"), constant);
		Return ret = new Return(cfg, unknown, new VariableRef(cfg, unknown, "x"));
		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(condition, a1));
		cfg.addEdge(new FalseEdge(condition, ret));
		cfg.addEdge(new SequentialEdge(a1, a2));
		cfg.addEdge(new SequentialEdge(a2, ret));

		Collection<ControlFlowStructure> extracted = new ControlFlowExtractor(cfg).extract();
		assertEquals("Incorrect number of structures: " + extracted.size(), 1, extracted.size());

		ControlFlowStructure struct = extracted.iterator().next();
		assertTrue(struct + " does not represent an if-then-else", struct instanceof IfThenElse);

		assertIf(condition, ret, collect(a1, a2), Collections.emptyList(), (IfThenElse) struct);
	}

	@Test
	public void testAsymmetricIf() {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "asymmetricIf"));
		Literal constant = new Literal(cfg, unknown, 10, Int32.INSTANCE);
		IMPNotEqual condition = new IMPNotEqual(cfg, unknown, constant, constant);
		Assignment a1 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "l"), constant);
		Assignment a2 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "r"), constant);
		Assignment a3 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "x"), constant);
		Return ret = new Return(cfg, unknown, new VariableRef(cfg, unknown, "x"));
		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(a3);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(condition, a1));
		cfg.addEdge(new FalseEdge(condition, a2));
		cfg.addEdge(new SequentialEdge(a1, a3));
		cfg.addEdge(new SequentialEdge(a2, ret));
		cfg.addEdge(new SequentialEdge(a3, ret));

		Collection<ControlFlowStructure> extracted = new ControlFlowExtractor(cfg).extract();
		assertEquals("Incorrect number of structures: " + extracted.size(), 1, extracted.size());

		ControlFlowStructure struct = extracted.iterator().next();
		assertTrue(struct + " does not represent an if-then-else", struct instanceof IfThenElse);

		assertIf(condition, ret, collect(a1, a3), collect(a2), (IfThenElse) struct);
	}

	@Test
	public void testBigAsymmetricIf() {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "bigAsymmetricIf"));
		Literal constant = new Literal(cfg, unknown, 15, Int32.INSTANCE);
		IMPNotEqual condition = new IMPNotEqual(cfg, unknown, constant, constant);
		Assignment a1 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "l"), constant);
		Assignment a2 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "r"), constant);
		Assignment a3 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "x"), constant);
		Assignment a4 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "y"), constant);
		Assignment a5 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "z"), constant);
		Assignment a6 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "w"), constant);
		Return ret = new Return(cfg, unknown, new VariableRef(cfg, unknown, "x"));
		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(a3);
		cfg.addNode(a4);
		cfg.addNode(a5);
		cfg.addNode(a6);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(condition, a1));
		cfg.addEdge(new SequentialEdge(a1, a2));
		cfg.addEdge(new SequentialEdge(a2, a3));
		cfg.addEdge(new SequentialEdge(a3, a4));
		cfg.addEdge(new SequentialEdge(a4, a6));
		cfg.addEdge(new FalseEdge(condition, a5));
		cfg.addEdge(new SequentialEdge(a5, a6));
		cfg.addEdge(new SequentialEdge(a6, ret));

		Collection<ControlFlowStructure> extracted = new ControlFlowExtractor(cfg).extract();
		assertEquals("Incorrect number of structures: " + extracted.size(), 1, extracted.size());

		ControlFlowStructure struct = extracted.iterator().next();
		assertTrue(struct + " does not represent an if-then-else", struct instanceof IfThenElse);

		assertIf(condition, a6, collect(a1, a2, a3, a4), collect(a5), (IfThenElse) struct);
	}

	@Test
	public void testSimpleLoop() {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "simpleLoop"));
		Literal constant = new Literal(cfg, unknown, 5, Int32.INSTANCE);
		IMPNotEqual condition = new IMPNotEqual(cfg, unknown, constant, constant);
		Assignment a1 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "l"), constant);
		Assignment a2 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "r"), constant);
		Return ret = new Return(cfg, unknown, new VariableRef(cfg, unknown, "x"));
		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(condition, a1));
		cfg.addEdge(new SequentialEdge(a1, condition));
		cfg.addEdge(new FalseEdge(condition, a2));
		cfg.addEdge(new SequentialEdge(a2, ret));

		Collection<ControlFlowStructure> extracted = new ControlFlowExtractor(cfg).extract();
		assertEquals("Incorrect number of structures: " + extracted.size(), 1, extracted.size());

		ControlFlowStructure struct = extracted.iterator().next();
		assertTrue(struct + " does not represent a loop", struct instanceof Loop);

		assertLoop(condition, a2, collect(a1), (Loop) struct);
	}

	@Test
	public void testEmptyLoop() {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "emptyLoop"));
		Literal constant = new Literal(cfg, unknown, 5, Int32.INSTANCE);
		IMPNotEqual condition = new IMPNotEqual(cfg, unknown, constant, constant);
		Assignment a1 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "l"), constant);
		Assignment a2 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "r"), constant);
		Return ret = new Return(cfg, unknown, new VariableRef(cfg, unknown, "x"));
		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(condition, condition));
		cfg.addEdge(new FalseEdge(condition, a2));
		cfg.addEdge(new SequentialEdge(a2, ret));

		Collection<ControlFlowStructure> extracted = new ControlFlowExtractor(cfg).extract();
		assertEquals("Incorrect number of structures: " + extracted.size(), 1, extracted.size());

		ControlFlowStructure struct = extracted.iterator().next();
		assertTrue(struct + " does not represent a loop", struct instanceof Loop);

		assertLoop(condition, a2, Collections.emptyList(), (Loop) struct);
	}

	@Test
	public void testLongLoop() {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "longLoop"));
		Literal constant = new Literal(cfg, unknown, 15, Int32.INSTANCE);
		IMPNotEqual condition = new IMPNotEqual(cfg, unknown, constant, constant);
		Assignment a1 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "l"), constant);
		Assignment a2 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "r"), constant);
		Assignment a3 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "x"), constant);
		Assignment a4 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "y"), constant);
		Assignment a5 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "z"), constant);
		Assignment a6 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "w"), constant);
		Return ret = new Return(cfg, unknown, new VariableRef(cfg, unknown, "x"));
		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(a3);
		cfg.addNode(a4);
		cfg.addNode(a5);
		cfg.addNode(a6);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(condition, a1));
		cfg.addEdge(new SequentialEdge(a1, a2));
		cfg.addEdge(new SequentialEdge(a2, a3));
		cfg.addEdge(new SequentialEdge(a3, a4));
		cfg.addEdge(new SequentialEdge(a4, a5));
		cfg.addEdge(new SequentialEdge(a5, condition));
		cfg.addEdge(new FalseEdge(condition, a6));
		cfg.addEdge(new SequentialEdge(a6, ret));

		Collection<ControlFlowStructure> extracted = new ControlFlowExtractor(cfg).extract();
		assertEquals("Incorrect number of structures: " + extracted.size(), 1, extracted.size());

		ControlFlowStructure struct = extracted.iterator().next();
		assertTrue(struct + " does not represent a loop", struct instanceof Loop);

		assertLoop(condition, a6, collect(a1, a2, a3, a4, a5), (Loop) struct);
	}

	@Test
	public void testNestedConditionals() {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "nested"));
		Literal constant = new Literal(cfg, unknown, 10, Int32.INSTANCE);
		Literal constant1 = new Literal(cfg, unknown, 100, Int32.INSTANCE);
		IMPNotEqual loop_condition = new IMPNotEqual(cfg, unknown, constant, constant);
		Assignment loop_a1 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "loop_a1"),
				constant);
		Assignment loop_a2 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "loop_a2"),
				constant);
		IMPNotEqual if_condition = new IMPNotEqual(cfg, unknown, constant, constant1);
		Assignment if_a1 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "if_a1"),
				constant);
		Assignment if_a2 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "if_a2"),
				constant);
		Assignment if_a3 = new Assignment(cfg, unknown,
				new VariableRef(cfg, unknown, "if_a3"),
				constant);
		Return ret = new Return(cfg, unknown, new VariableRef(cfg, unknown, "x"));

		cfg.addNode(loop_condition, true);
		cfg.addNode(loop_a1);
		cfg.addNode(loop_a2);
		cfg.addNode(if_condition);
		cfg.addNode(if_a1);
		cfg.addNode(if_a2);
		cfg.addNode(if_a3);
		cfg.addNode(ret);

		cfg.addEdge(new TrueEdge(loop_condition, loop_a1));
		cfg.addEdge(new SequentialEdge(loop_a1, if_condition));
		cfg.addEdge(new TrueEdge(if_condition, if_a1));
		cfg.addEdge(new SequentialEdge(if_a1, if_a3));
		cfg.addEdge(new SequentialEdge(if_a3, loop_a2));
		cfg.addEdge(new FalseEdge(if_condition, if_a2));
		cfg.addEdge(new SequentialEdge(if_a2, loop_a2));
		cfg.addEdge(new SequentialEdge(loop_a2, loop_condition));
		cfg.addEdge(new FalseEdge(loop_condition, ret));

		Collection<ControlFlowStructure> extracted = new ControlFlowExtractor(cfg).extract();
		assertEquals("Incorrect number of structures: " + extracted.size(), 2, extracted.size());

		Iterator<ControlFlowStructure> it = extracted.iterator();
		ControlFlowStructure first = it.next();
		ControlFlowStructure second = it.next();

		Loop loop = null;
		IfThenElse ith = null;
		if (first instanceof IfThenElse && second instanceof Loop) {
			ith = (IfThenElse) first;
			loop = (Loop) second;
		} else if (second instanceof IfThenElse && first instanceof Loop) {
			ith = (IfThenElse) second;
			loop = (Loop) first;
		} else
			fail("Wrong conditional structures: excpected one loop and one if-then-else, but got a "
					+ first.getClass().getSimpleName() + " and a " + second.getClass().getSimpleName());

		assertIf(if_condition, loop_a2, collect(if_a1, if_a3), collect(if_a2), ith);
		assertLoop(loop_condition, ret, collect(loop_a1, if_condition, if_a1, if_a3, if_a2, loop_a2), loop);
	}
}
