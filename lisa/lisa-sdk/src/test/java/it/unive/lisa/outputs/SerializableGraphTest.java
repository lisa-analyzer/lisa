package it.unive.lisa.outputs;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.outputs.serializableGraph.SerializableCFG;
import it.unive.lisa.outputs.serializableGraph.SerializableEdge;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableNode;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class SerializableGraphTest {

	private static final ClassUnit unit = new ClassUnit(SyntheticLocation.INSTANCE,
			new Program(new TestLanguageFeatures(), new TestTypeSystem()), "Testing", false);

	private static void addNode(
			SortedSet<SerializableNode> nodes,
			Statement st,
			int offset,
			int... inner) {
		List<Integer> list = new ArrayList<>(inner.length);
		for (int i = 0; i < inner.length; i++)
			list.add(inner[i]);
		nodes.add(new SerializableNode(offset, list, st.toString()));
	}

	private static void addEdge(
			SortedSet<SerializableEdge> edges,
			Edge e,
			int src,
			int dest) {
		edges.add(new SerializableEdge(src, dest, e.getClass().getSimpleName()));
	}

	@Test
	public void testSimpleIf() {
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "simpleIf"));

		VariableRef c1 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "1");
		VariableRef c2 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "2");
		VariableRef c3 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "3");
		VariableRef c4 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "4");
		VariableRef lvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "l");
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		Assignment condition = new Assignment(cfg, SyntheticLocation.INSTANCE, c1, c2);
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

		SortedSet<SerializableNode> nodes = new TreeSet<>();
		SortedSet<SerializableEdge> edges = new TreeSet<>();

		addNode(nodes, c1, 0);
		addNode(nodes, c2, 1);
		addNode(nodes, condition, 2, 0, 1);
		addNode(nodes, lvar, 3);
		addNode(nodes, c3, 4);
		addNode(nodes, a1, 5, 3, 4);
		addNode(nodes, rvar, 6);
		addNode(nodes, c4, 7);
		addNode(nodes, a2, 8, 6, 7);
		addNode(nodes, xvar, 9);
		addNode(nodes, ret, 10, 9);

		addEdge(edges, e1, 2, 5);
		addEdge(edges, e2, 2, 8);
		addEdge(edges, e3, 5, 10);
		addEdge(edges, e4, 8, 10);

		SerializableGraph expected = new SerializableGraph(
				cfg.getDescriptor().getFullSignatureWithParNames(),
				null,
				nodes,
				edges,
				Collections.emptySortedSet());
		assertEquals(expected, graph);
	}

	@Test
	public void testEmptyIf() {
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "emptyIf"));
		VariableRef c1 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "1");
		VariableRef c2 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "2");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		Assignment condition = new Assignment(cfg, SyntheticLocation.INSTANCE, c1, c2);
		Return ret = new Return(cfg, SyntheticLocation.INSTANCE, xvar);
		cfg.addNode(condition, true);
		cfg.addNode(ret);
		Edge e1 = new TrueEdge(condition, ret);
		Edge e2 = new FalseEdge(condition, ret);
		cfg.addEdge(e1);
		cfg.addEdge(e2);
		SerializableGraph graph = SerializableCFG.fromCFG(cfg);
		SortedSet<SerializableNode> nodes = new TreeSet<>();
		SortedSet<SerializableEdge> edges = new TreeSet<>();
		addNode(nodes, c1, 0);
		addNode(nodes, c2, 1);
		addNode(nodes, condition, 2, 0, 1);
		addNode(nodes, xvar, 3);
		addNode(nodes, ret, 4, 3);
		addEdge(edges, e1, 2, 4);
		addEdge(edges, e2, 2, 4);
		SerializableGraph expected = new SerializableGraph(
				cfg.getDescriptor().getFullSignatureWithParNames(),
				null,
				nodes,
				edges,
				Collections.emptySortedSet());
		assertEquals(expected, graph);
	}

	@Test
	public void testIfWithEmptyBranch() {
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "emptyBranch"));
		VariableRef c1 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "1");
		VariableRef c2 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "2");
		VariableRef c3 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "3");
		VariableRef c4 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "4");
		VariableRef lvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "l");
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		Assignment condition = new Assignment(cfg, SyntheticLocation.INSTANCE, c1, c2);
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
		SortedSet<SerializableNode> nodes = new TreeSet<>();
		SortedSet<SerializableEdge> edges = new TreeSet<>();
		addNode(nodes, c1, 0);
		addNode(nodes, c2, 1);
		addNode(nodes, condition, 2, 0, 1);
		addNode(nodes, lvar, 3);
		addNode(nodes, c3, 4);
		addNode(nodes, a1, 5, 3, 4);
		addNode(nodes, rvar, 6);
		addNode(nodes, c4, 7);
		addNode(nodes, a2, 8, 6, 7);
		addNode(nodes, xvar, 9);
		addNode(nodes, ret, 10, 9);
		addEdge(edges, e1, 2, 5);
		addEdge(edges, e2, 2, 10);
		addEdge(edges, e3, 5, 8);
		addEdge(edges, e4, 8, 10);
		SerializableGraph expected = new SerializableGraph(
				cfg.getDescriptor().getFullSignatureWithParNames(),
				null,
				nodes,
				edges,
				Collections.emptySortedSet());
		assertEquals(expected, graph);
	}

	@Test
	public void testAsymmetricIf() {
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "asymmetricIf"));
		VariableRef c1 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "1");
		VariableRef c2 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "2");
		VariableRef c3 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "3");
		VariableRef c4 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "4");
		VariableRef c5 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "5");
		VariableRef lvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "l");
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		VariableRef yvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "y");
		Assignment condition = new Assignment(cfg, SyntheticLocation.INSTANCE, c1, c2);
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
		SortedSet<SerializableNode> nodes = new TreeSet<>();
		SortedSet<SerializableEdge> edges = new TreeSet<>();
		addNode(nodes, c1, 0);
		addNode(nodes, c2, 1);
		addNode(nodes, condition, 2, 0, 1);
		addNode(nodes, lvar, 3);
		addNode(nodes, c3, 4);
		addNode(nodes, a1, 5, 3, 4);
		addNode(nodes, rvar, 6);
		addNode(nodes, c4, 7);
		addNode(nodes, a2, 8, 6, 7);
		addNode(nodes, xvar, 9);
		addNode(nodes, c5, 10);
		addNode(nodes, a3, 11, 9, 10);
		addNode(nodes, yvar, 12);
		addNode(nodes, ret, 13, 12);
		addEdge(edges, e1, 2, 5);
		addEdge(edges, e2, 2, 8);
		addEdge(edges, e3, 5, 11);
		addEdge(edges, e4, 8, 13);
		addEdge(edges, e5, 11, 13);
		SerializableGraph expected = new SerializableGraph(
				cfg.getDescriptor().getFullSignatureWithParNames(),
				null,
				nodes,
				edges,
				Collections.emptySortedSet());
		assertEquals(expected, graph);
	}

	@Test
	public void testSimpleLoop() {
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "simpleLoop"));
		VariableRef c1 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "1");
		VariableRef c2 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "2");
		VariableRef c3 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "3");
		VariableRef c4 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "4");
		VariableRef lvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "l");
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		Assignment condition = new Assignment(cfg, SyntheticLocation.INSTANCE, c1, c2);
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
		SortedSet<SerializableNode> nodes = new TreeSet<>();
		SortedSet<SerializableEdge> edges = new TreeSet<>();
		addNode(nodes, c1, 0);
		addNode(nodes, c2, 1);
		addNode(nodes, condition, 2, 0, 1);
		addNode(nodes, lvar, 3);
		addNode(nodes, c3, 4);
		addNode(nodes, a1, 5, 3, 4);
		addNode(nodes, rvar, 6);
		addNode(nodes, c4, 7);
		addNode(nodes, a2, 8, 6, 7);
		addNode(nodes, xvar, 9);
		addNode(nodes, ret, 10, 9);
		addEdge(edges, e1, 2, 5);
		addEdge(edges, e2, 2, 8);
		addEdge(edges, e3, 5, 2);
		addEdge(edges, e4, 8, 10);
		SerializableGraph expected = new SerializableGraph(
				cfg.getDescriptor().getFullSignatureWithParNames(),
				null,
				nodes,
				edges,
				Collections.emptySortedSet());
		assertEquals(expected, graph);
	}

	@Test
	public void testEmptyLoop() {
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "emptyLoop"));
		VariableRef c1 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "1");
		VariableRef c2 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "2");
		VariableRef c4 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "4");
		VariableRef rvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "r");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		Assignment condition = new Assignment(cfg, SyntheticLocation.INSTANCE, c1, c2);
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
		SortedSet<SerializableNode> nodes = new TreeSet<>();
		SortedSet<SerializableEdge> edges = new TreeSet<>();
		addNode(nodes, c1, 0);
		addNode(nodes, c2, 1);
		addNode(nodes, condition, 2, 0, 1);
		addNode(nodes, rvar, 3);
		addNode(nodes, c4, 4);
		addNode(nodes, a2, 5, 3, 4);
		addNode(nodes, xvar, 6);
		addNode(nodes, ret, 7, 6);
		addEdge(edges, e1, 2, 2);
		addEdge(edges, e2, 2, 5);
		addEdge(edges, e4, 5, 7);
		SerializableGraph expected = new SerializableGraph(
				cfg.getDescriptor().getFullSignatureWithParNames(),
				null,
				nodes,
				edges,
				Collections.emptySortedSet());
		assertEquals(expected, graph);
	}

	@Test
	public void testNestedConditionals() {
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "nested"));
		VariableRef c1 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "1");
		VariableRef c2 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "2");
		VariableRef c3 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "3");
		VariableRef c4 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "4");
		VariableRef c5 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "5");
		VariableRef c6 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "6");
		VariableRef c7 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "7");
		VariableRef c8 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "8");
		VariableRef c9 = new VariableRef(cfg, SyntheticLocation.INSTANCE, "9");
		VariableRef loop_a1var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "loop_a1");
		VariableRef loop_a2var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "loop_a2");
		VariableRef if_a1var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "if_a1");
		VariableRef if_a2var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "if_a2");
		VariableRef if_a3var = new VariableRef(cfg, SyntheticLocation.INSTANCE, "if_a3");
		VariableRef xvar = new VariableRef(cfg, SyntheticLocation.INSTANCE, "x");
		Assignment loop_condition = new Assignment(cfg, SyntheticLocation.INSTANCE, c1, c2);
		Assignment loop_a1 = new Assignment(cfg, SyntheticLocation.INSTANCE, loop_a1var, c3);
		Assignment loop_a2 = new Assignment(cfg, SyntheticLocation.INSTANCE, loop_a2var, c4);
		Assignment if_condition = new Assignment(cfg, SyntheticLocation.INSTANCE, c5, c6);
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
		SortedSet<SerializableNode> nodes = new TreeSet<>();
		SortedSet<SerializableEdge> edges = new TreeSet<>();
		addNode(nodes, c1, 0);
		addNode(nodes, c2, 1);
		addNode(nodes, loop_condition, 2, 0, 1);
		addNode(nodes, loop_a1var, 3);
		addNode(nodes, c3, 4);
		addNode(nodes, loop_a1, 5, 3, 4);
		addNode(nodes, loop_a2var, 6);
		addNode(nodes, c4, 7);
		addNode(nodes, loop_a2, 8, 6, 7);
		addNode(nodes, c5, 9);
		addNode(nodes, c6, 10);
		addNode(nodes, if_condition, 11, 9, 10);
		addNode(nodes, if_a1var, 12);
		addNode(nodes, c7, 13);
		addNode(nodes, if_a1, 14, 12, 13);
		addNode(nodes, if_a2var, 15);
		addNode(nodes, c8, 16);
		addNode(nodes, if_a2, 17, 15, 16);
		addNode(nodes, if_a3var, 18);
		addNode(nodes, c9, 19);
		addNode(nodes, if_a3, 20, 18, 19);
		addNode(nodes, xvar, 21);
		addNode(nodes, ret, 22, 21);
		addEdge(edges, e1, 2, 5);
		addEdge(edges, e2, 5, 11);
		addEdge(edges, e3, 11, 14);
		addEdge(edges, e4, 14, 20);
		addEdge(edges, e5, 20, 8);
		addEdge(edges, e6, 11, 17);
		addEdge(edges, e7, 17, 8);
		addEdge(edges, e8, 8, 2);
		addEdge(edges, e9, 2, 22);
		SerializableGraph expected = new SerializableGraph(
				cfg.getDescriptor().getFullSignatureWithParNames(),
				null,
				nodes,
				edges,
				Collections.emptySortedSet());
		assertEquals(expected, graph);
	}

}
