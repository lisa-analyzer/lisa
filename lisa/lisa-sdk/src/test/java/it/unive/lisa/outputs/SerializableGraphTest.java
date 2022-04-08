package it.unive.lisa.outputs;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import it.unive.lisa.outputs.serializableGraph.SerializableCFG;
import it.unive.lisa.outputs.serializableGraph.SerializableEdge;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableNode;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.comparison.NotEqual;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;

public class SerializableGraphTest {

	private static final CompilationUnit unit = new CompilationUnit(SyntheticLocation.INSTANCE, "Testing",
			false);

	private static void addNode(SerializableGraph graph, Statement st, Statement... inner) {
		List<Integer> list = new ArrayList<>(inner.length);
		for (int i = 0; i < inner.length; i++)
			list.add(inner[i].getOffset());
		graph.addNode(new SerializableNode(st.getOffset(), list, st.toString()));
	}

	private static void addEdge(SerializableGraph graph, Edge e) {
		graph.addEdge(new SerializableEdge(e.getSource().getOffset(), e.getDestination().getOffset(),
				e.getClass().getSimpleName()));
	}

	@Test
	public void testSimpleIf() {
		CFG cfg = new CFG(new CFGDescriptor(SyntheticLocation.INSTANCE, unit, false, "simpleIf"));

		Int32Literal c1 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 1);
		Int32Literal c2 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 2);
		Int32Literal c3 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 3);
		Int32Literal c4 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 4);
		VariableRef lvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "l");
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		NotEqual condition = new NotEqual(cfg, SyntheticLocation.INSTANCE, c1, c2);
		Assignment a1 = new Assignment(cfg, SyntheticLocation.INSTANCE, lvar, c3);
		Assignment a2 = new Assignment(cfg, SyntheticLocation.INSTANCE, rvar, c4);
		Return ret = new Return(cfg, SyntheticLocation.INSTANCE, xvar);

		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(ret);

		Edge e1 = new TrueEdge(condition, a1);
		Edge e2 = new FalseEdge(condition, a2);
		Edge e3 = new SequentialEdge(a1, ret);
		Edge e4 = new SequentialEdge(a2, ret);
		cfg.addEdge(e1);
		cfg.addEdge(e2);
		cfg.addEdge(e3);
		cfg.addEdge(e4);

		SerializableGraph graph = SerializableCFG.fromCFG(cfg);

		SerializableGraph expected = new SerializableGraph();
		expected.setName(cfg.getDescriptor().getFullSignatureWithParNames());
		
		addNode(expected, c1);
		addNode(expected, c2);
		addNode(expected, c3);
		addNode(expected, c4);
		addNode(expected, lvar);
		addNode(expected, rvar);
		addNode(expected, xvar);
		addNode(expected, condition, c1, c2);
		addNode(expected, a1, lvar, c3);
		addNode(expected, a2, rvar, c4);
		addNode(expected, ret, xvar);
		
		addEdge(expected, e1);
		addEdge(expected, e2);
		addEdge(expected, e3);
		addEdge(expected, e4);

		assertEquals(expected, graph);
	}

	@Test
	public void testEmptyIf() {
		CFG cfg = new CFG(new CFGDescriptor(SyntheticLocation.INSTANCE, unit, false, "emptyIf"));

		Int32Literal c1 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 1);
		Int32Literal c2 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 2);
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		NotEqual condition = new NotEqual(cfg, SyntheticLocation.INSTANCE, c1, c2);
		Return ret = new Return(cfg, SyntheticLocation.INSTANCE, xvar);

		cfg.addNode(condition, true);
		cfg.addNode(ret);

		Edge e1 = new TrueEdge(condition, ret);
		Edge e2 = new FalseEdge(condition, ret);
		cfg.addEdge(e1);
		cfg.addEdge(e2);

		SerializableGraph graph = SerializableCFG.fromCFG(cfg);

		SerializableGraph expected = new SerializableGraph();
		expected.setName(cfg.getDescriptor().getFullSignatureWithParNames());
		
		addNode(expected, c1);
		addNode(expected, c2);
		addNode(expected, xvar);
		addNode(expected, condition, c1, c2);
		addNode(expected, ret, xvar);
		
		addEdge(expected, e1);
		addEdge(expected, e2);

		assertEquals(expected, graph);
	}

	@Test
	public void testIfWithEmptyBranch() {
		CFG cfg = new CFG(new CFGDescriptor(SyntheticLocation.INSTANCE, unit, false, "emptyBranch"));
		
		Int32Literal c1 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 1);
		Int32Literal c2 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 2);
		Int32Literal c3 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 3);
		Int32Literal c4 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 4);
		VariableRef lvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "l");
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		NotEqual condition = new NotEqual(cfg, SyntheticLocation.INSTANCE, c1, c2);
		Assignment a1 = new Assignment(cfg, SyntheticLocation.INSTANCE, lvar, c3);
		Assignment a2 = new Assignment(cfg, SyntheticLocation.INSTANCE, rvar, c4);
		Return ret = new Return(cfg, SyntheticLocation.INSTANCE, xvar);

		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(ret);

		Edge e1 = new TrueEdge(condition, a1);
		Edge e2 = new FalseEdge(condition, ret);
		Edge e3 = new SequentialEdge(a1, a2);
		Edge e4 = new SequentialEdge(a2, ret);
		cfg.addEdge(e1);
		cfg.addEdge(e2);
		cfg.addEdge(e3);
		cfg.addEdge(e4);

		SerializableGraph graph = SerializableCFG.fromCFG(cfg);

		SerializableGraph expected = new SerializableGraph();
		expected.setName(cfg.getDescriptor().getFullSignatureWithParNames());
		
		addNode(expected, c1);
		addNode(expected, c2);
		addNode(expected, c3);
		addNode(expected, c4);
		addNode(expected, lvar);
		addNode(expected, rvar);
		addNode(expected, xvar);
		addNode(expected, condition, c1, c2);
		addNode(expected, a1, lvar, c3);
		addNode(expected, a2, rvar, c4);
		addNode(expected, ret, xvar);
		
		addEdge(expected, e1);
		addEdge(expected, e2);
		addEdge(expected, e3);
		addEdge(expected, e4);

		assertEquals(expected, graph);
	}

	@Test
	public void testAsymmetricIf() {
		CFG cfg = new CFG(new CFGDescriptor(SyntheticLocation.INSTANCE, unit, false, "asymmetricIf"));
		
		Int32Literal c1 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 1);
		Int32Literal c2 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 2);
		Int32Literal c3 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 3);
		Int32Literal c4 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 4);
		Int32Literal c5 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 5);
		VariableRef lvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "l");
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		VariableRef yvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "y");
		NotEqual condition = new NotEqual(cfg, SyntheticLocation.INSTANCE, c1, c2);
		Assignment a1 = new Assignment(cfg, SyntheticLocation.INSTANCE, lvar, c3);
		Assignment a2 = new Assignment(cfg, SyntheticLocation.INSTANCE, rvar, c4);
		Assignment a3 = new Assignment(cfg, SyntheticLocation.INSTANCE, xvar, c5);
		Return ret = new Return(cfg, SyntheticLocation.INSTANCE, yvar);

		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(a3);
		cfg.addNode(ret);

		Edge e1 = new TrueEdge(condition, a1);
		Edge e2 = new FalseEdge(condition, a2);
		Edge e3 = new SequentialEdge(a1, a3);
		Edge e4 = new SequentialEdge(a2, ret);
		Edge e5 = new SequentialEdge(a3, ret);
		cfg.addEdge(e1);
		cfg.addEdge(e2);
		cfg.addEdge(e3);
		cfg.addEdge(e4);
		cfg.addEdge(e5);

		SerializableGraph graph = SerializableCFG.fromCFG(cfg);

		SerializableGraph expected = new SerializableGraph();
		expected.setName(cfg.getDescriptor().getFullSignatureWithParNames());
		
		addNode(expected, c1);
		addNode(expected, c2);
		addNode(expected, c3);
		addNode(expected, c4);
		addNode(expected, c5);
		addNode(expected, lvar);
		addNode(expected, rvar);
		addNode(expected, xvar);
		addNode(expected, yvar);
		addNode(expected, condition, c1, c2);
		addNode(expected, a1, lvar, c3);
		addNode(expected, a2, rvar, c4);
		addNode(expected, a3, xvar, c5);
		addNode(expected, ret, yvar);
		
		addEdge(expected, e1);
		addEdge(expected, e2);
		addEdge(expected, e3);
		addEdge(expected, e4);
		addEdge(expected, e5);

		assertEquals(expected, graph);
	}

	@Test
	public void testSimpleLoop() {
		CFG cfg = new CFG(new CFGDescriptor(SyntheticLocation.INSTANCE, unit, false, "simpleLoop"));
		
		Int32Literal c1 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 1);
		Int32Literal c2 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 2);
		Int32Literal c3 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 3);
		Int32Literal c4 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 4);
		VariableRef lvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "l");
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		NotEqual condition = new NotEqual(cfg, SyntheticLocation.INSTANCE, c1, c2);
		Assignment a1 = new Assignment(cfg, SyntheticLocation.INSTANCE, lvar, c3);
		Assignment a2 = new Assignment(cfg, SyntheticLocation.INSTANCE, rvar, c4);
		Return ret = new Return(cfg, SyntheticLocation.INSTANCE, xvar);

		cfg.addNode(condition, true);
		cfg.addNode(a1);
		cfg.addNode(a2);
		cfg.addNode(ret);

		Edge e1 = new TrueEdge(condition, a1);
		Edge e2 = new FalseEdge(condition, a2);
		Edge e3 = new SequentialEdge(a1, condition);
		Edge e4 = new SequentialEdge(a2, ret);
		cfg.addEdge(e1);
		cfg.addEdge(e2);
		cfg.addEdge(e3);
		cfg.addEdge(e4);

		SerializableGraph graph = SerializableCFG.fromCFG(cfg);

		SerializableGraph expected = new SerializableGraph();
		expected.setName(cfg.getDescriptor().getFullSignatureWithParNames());
		
		addNode(expected, c1);
		addNode(expected, c2);
		addNode(expected, c3);
		addNode(expected, c4);
		addNode(expected, lvar);
		addNode(expected, rvar);
		addNode(expected, xvar);
		addNode(expected, condition, c1, c2);
		addNode(expected, a1, lvar, c3);
		addNode(expected, a2, rvar, c4);
		addNode(expected, ret, xvar);
		
		addEdge(expected, e1);
		addEdge(expected, e2);
		addEdge(expected, e3);
		addEdge(expected, e4);

		assertEquals(expected, graph);
	}

	@Test
	public void testEmptyLoop() {
		CFG cfg = new CFG(new CFGDescriptor(SyntheticLocation.INSTANCE, unit, false, "emptyLoop"));
		
		Int32Literal c1 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 1);
		Int32Literal c2 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 2);
		Int32Literal c4 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 4);
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		NotEqual condition = new NotEqual(cfg, SyntheticLocation.INSTANCE, c1, c2);
		Assignment a2 = new Assignment(cfg, SyntheticLocation.INSTANCE, rvar, c4);
		Return ret = new Return(cfg, SyntheticLocation.INSTANCE, xvar);

		cfg.addNode(condition, true);
		cfg.addNode(a2);
		cfg.addNode(ret);

		Edge e1 = new TrueEdge(condition, condition);
		Edge e2 = new FalseEdge(condition, a2);
		Edge e4 = new SequentialEdge(a2, ret);
		cfg.addEdge(e1);
		cfg.addEdge(e2);
		cfg.addEdge(e4);

		SerializableGraph graph = SerializableCFG.fromCFG(cfg);

		SerializableGraph expected = new SerializableGraph();
		expected.setName(cfg.getDescriptor().getFullSignatureWithParNames());
		
		addNode(expected, c1);
		addNode(expected, c2);
		addNode(expected, c4);
		addNode(expected, rvar);
		addNode(expected, xvar);
		addNode(expected, condition, c1, c2);
		addNode(expected, a2, rvar, c4);
		addNode(expected, ret, xvar);
		
		addEdge(expected, e1);
		addEdge(expected, e2);
		addEdge(expected, e4);

		assertEquals(expected, graph);
	}

	@Test
	public void testNestedConditionals() {
		CFG cfg = new CFG(new CFGDescriptor(SyntheticLocation.INSTANCE, unit, false, "nested"));
		
		Int32Literal c1 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 1);
		Int32Literal c2 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 2);
		Int32Literal c3 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 3);
		Int32Literal c4 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 4);
		Int32Literal c5 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 5);
		Int32Literal c6 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 6);
		Int32Literal c7 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 7);
		Int32Literal c8 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 8);
		Int32Literal c9 = new Int32Literal(cfg, SyntheticLocation.INSTANCE, 9);
		VariableRef loop_a1var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "loop_a1");
		VariableRef loop_a2var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "loop_a2");
		VariableRef if_a1var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "if_a1");
		VariableRef if_a2var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "if_a2");
		VariableRef if_a3var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "if_a3");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		NotEqual loop_condition = new NotEqual(cfg, SyntheticLocation.INSTANCE, c1, c2);
		Assignment loop_a1 = new Assignment(cfg, SyntheticLocation.INSTANCE, loop_a1var, c3);
		Assignment loop_a2 = new Assignment(cfg, SyntheticLocation.INSTANCE, loop_a2var, c4);
		NotEqual if_condition = new NotEqual(cfg, SyntheticLocation.INSTANCE, c5, c6);
		Assignment if_a1 = new Assignment(cfg, SyntheticLocation.INSTANCE, if_a1var, c7);
		Assignment if_a2 = new Assignment(cfg, SyntheticLocation.INSTANCE, if_a2var, c8);
		Assignment if_a3 = new Assignment(cfg, SyntheticLocation.INSTANCE, if_a3var, c9);
		Return ret = new Return(cfg, SyntheticLocation.INSTANCE, xvar);

		cfg.addNode(loop_condition, true);
		cfg.addNode(loop_a1);
		cfg.addNode(loop_a2);
		cfg.addNode(if_condition);
		cfg.addNode(if_a1);
		cfg.addNode(if_a2);
		cfg.addNode(if_a3);
		cfg.addNode(ret);



		Edge e1 = new TrueEdge(loop_condition, loop_a1);
		Edge e2 = new SequentialEdge(loop_a1, if_condition);
		Edge e3 = new TrueEdge(if_condition, if_a1);
		Edge e4 = new SequentialEdge(if_a1, if_a3);
		Edge e5 = new SequentialEdge(if_a3, loop_a2);
		Edge e6 = new FalseEdge(if_condition, if_a2);
		Edge e7 = new SequentialEdge(if_a2, loop_a2);
		Edge e8 = new SequentialEdge(loop_a2, loop_condition);
		Edge e9 = new FalseEdge(loop_condition, ret);
		cfg.addEdge(e1);
		cfg.addEdge(e2);
		cfg.addEdge(e3);
		cfg.addEdge(e4);
		cfg.addEdge(e5);
		cfg.addEdge(e6);
		cfg.addEdge(e7);
		cfg.addEdge(e8);
		cfg.addEdge(e9);

		SerializableGraph graph = SerializableCFG.fromCFG(cfg);

		SerializableGraph expected = new SerializableGraph();
		expected.setName(cfg.getDescriptor().getFullSignatureWithParNames());
		
		addNode(expected, c1);
		addNode(expected, c2);
		addNode(expected, c3);
		addNode(expected, c4);
		addNode(expected, c5);
		addNode(expected, c6);
		addNode(expected, c7);
		addNode(expected, c8);
		addNode(expected, c9);
		addNode(expected, loop_a1var);
		addNode(expected, loop_a2var);
		addNode(expected, if_a1var);
		addNode(expected, if_a2var);
		addNode(expected, if_a3var);
		addNode(expected, xvar);
		addNode(expected, loop_condition, c1, c2);
		addNode(expected, loop_a1, loop_a1var, c3);
		addNode(expected, loop_a2, loop_a2var, c4);
		addNode(expected, if_condition, c5, c6);
		addNode(expected, if_a1, if_a1var, c7);
		addNode(expected, if_a2, if_a2var, c8);
		addNode(expected, if_a3, if_a3var, c9);
		addNode(expected, ret, xvar);
		
		addEdge(expected, e1);
		addEdge(expected, e2);
		addEdge(expected, e3);
		addEdge(expected, e4);
		addEdge(expected, e5);
		addEdge(expected, e6);
		addEdge(expected, e7);
		addEdge(expected, e8);
		addEdge(expected, e9);

		assertEquals(expected, graph);
	}
}
