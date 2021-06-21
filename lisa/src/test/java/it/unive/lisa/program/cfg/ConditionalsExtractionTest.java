package it.unive.lisa.program.cfg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.imp.expressions.IMPIntLiteral;
import it.unive.lisa.imp.expressions.IMPNotEqual;
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
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import org.junit.Test;

public class ConditionalsExtractionTest {

	private static final CompilationUnit unit = new CompilationUnit(SourceCodeLocation.UNKNOWN, "Testing",
			false);

	@SafeVarargs
	private static <T> Collection<T> collect(T... objs) {
		ArrayList<T> res = new ArrayList<>(objs.length);
		for (T o : objs)
			res.add(o);
		return res;
	}

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

	@Test
	public void testSimpleIf() {
		CFG cfg = new CFG(new CFGDescriptor(SourceCodeLocation.UNKNOWN, unit, false, "simpleIf"));
		IMPIntLiteral constant = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 5);
		IMPNotEqual condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant);
		Assignment a1 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "l"), constant);
		Assignment a2 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "r"), constant);
		Return ret = new Return(cfg, SourceCodeLocation.UNKNOWN, new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"));
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
		CFG cfg = new CFG(new CFGDescriptor(SourceCodeLocation.UNKNOWN, unit, false, "emptyIf"));
		IMPIntLiteral constant = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 5);
		IMPNotEqual condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant);
		Return ret = new Return(cfg, SourceCodeLocation.UNKNOWN, new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"));
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
		CFG cfg = new CFG(new CFGDescriptor(SourceCodeLocation.UNKNOWN, unit, false, "emptyBranch"));
		IMPIntLiteral constant = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 5);
		IMPNotEqual condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant);
		Assignment a1 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "l"), constant);
		Assignment a2 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "r"), constant);
		Return ret = new Return(cfg, SourceCodeLocation.UNKNOWN, new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"));
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
		CFG cfg = new CFG(new CFGDescriptor(SourceCodeLocation.UNKNOWN, unit, false, "asymmetricIf"));
		IMPIntLiteral constant = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 10);
		IMPNotEqual condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant);
		Assignment a1 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "l"), constant);
		Assignment a2 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "r"), constant);
		Assignment a3 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"), constant);
		Return ret = new Return(cfg, SourceCodeLocation.UNKNOWN, new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"));
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
		CFG cfg = new CFG(new CFGDescriptor(SourceCodeLocation.UNKNOWN, unit, false, "bigAsymmetricIf"));
		IMPIntLiteral constant = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 15);
		IMPNotEqual condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant);
		Assignment a1 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "l"), constant);
		Assignment a2 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "r"), constant);
		Assignment a3 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"), constant);
		Assignment a4 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "y"), constant);
		Assignment a5 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "z"), constant);
		Assignment a6 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "w"), constant);
		Return ret = new Return(cfg, SourceCodeLocation.UNKNOWN, new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"));
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
		CFG cfg = new CFG(new CFGDescriptor(SourceCodeLocation.UNKNOWN, unit, false, "simpleLoop"));
		IMPIntLiteral constant = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 5);
		IMPNotEqual condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant);
		Assignment a1 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "l"), constant);
		Assignment a2 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "r"), constant);
		Return ret = new Return(cfg, SourceCodeLocation.UNKNOWN, new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"));
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
		CFG cfg = new CFG(new CFGDescriptor(SourceCodeLocation.UNKNOWN, unit, false, "emptyLoop"));
		IMPIntLiteral constant = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 5);
		IMPNotEqual condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant);
		Assignment a1 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "l"), constant);
		Assignment a2 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "r"), constant);
		Return ret = new Return(cfg, SourceCodeLocation.UNKNOWN, new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"));
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
		CFG cfg = new CFG(new CFGDescriptor(SourceCodeLocation.UNKNOWN, unit, false, "longLoop"));
		IMPIntLiteral constant = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 15);
		IMPNotEqual condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant);
		Assignment a1 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "l"), constant);
		Assignment a2 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "r"), constant);
		Assignment a3 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"), constant);
		Assignment a4 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "y"), constant);
		Assignment a5 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "z"), constant);
		Assignment a6 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "w"), constant);
		Return ret = new Return(cfg, SourceCodeLocation.UNKNOWN, new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"));
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
		CFG cfg = new CFG(new CFGDescriptor(SourceCodeLocation.UNKNOWN, unit, false, "nested"));
		IMPIntLiteral constant = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 10);
		IMPIntLiteral constant1 = new IMPIntLiteral(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), 100);
		IMPNotEqual loop_condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant);
		Assignment loop_a1 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "loop_a1"),
				constant);
		Assignment loop_a2 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "loop_a2"),
				constant);
		IMPNotEqual if_condition = new IMPNotEqual(cfg, SourceCodeLocation.UNKNOWN.getSourceFile(),
				SourceCodeLocation.UNKNOWN.getLine(), SourceCodeLocation.UNKNOWN.getCol(), constant, constant1);
		Assignment if_a1 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "if_a1"),
				constant);
		Assignment if_a2 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "if_a2"),
				constant);
		Assignment if_a3 = new Assignment(cfg, SourceCodeLocation.UNKNOWN,
				new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "if_a3"),
				constant);
		Return ret = new Return(cfg, SourceCodeLocation.UNKNOWN, new VariableRef(cfg, SourceCodeLocation.UNKNOWN, "x"));

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
