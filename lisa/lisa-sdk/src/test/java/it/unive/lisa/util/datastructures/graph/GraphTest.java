package it.unive.lisa.util.datastructures.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.datastructures.graph.TestGraph.TestEdge;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestNode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class GraphTest {

	private static class RecordingVisitor
			implements
			GraphVisitor<TestGraph, TestNode, TestEdge, Object> {

		final Collection<TestGraph> graphs = new HashSet<>();
		final Collection<TestNode> nodes = new HashSet<>();
		final Collection<TestEdge> edges = new HashSet<>();
		boolean stopOnGraph = false;
		TestNode stopOnNode = null;
		boolean stopOnAllNodes = false;
		boolean stopOnAllEdges = false;

		@Override
		public boolean visit(
				Object tool,
				TestGraph graph) {
			graphs.add(graph);
			return !stopOnGraph;
		}

		@Override
		public boolean visit(
				Object tool,
				TestGraph graph,
				TestNode node) {
			nodes.add(node);
			if (stopOnAllNodes)
				return false;
			return stopOnNode == null || !stopOnNode.equals(node);
		}

		@Override
		public boolean visit(
				Object tool,
				TestGraph graph,
				TestEdge edge) {
			edges.add(edge);
			return !stopOnAllEdges;
		}

	}

	private TestGraph linear() {
		TestGraph graph = new TestGraph();
		TestNode one = new TestNode(1);
		TestNode two = new TestNode(2);
		TestNode three = new TestNode(3);
		graph.addNode(one, true);
		graph.addNode(two);
		graph.addNode(three);
		graph.addEdge(new TestEdge(one, two));
		graph.addEdge(new TestEdge(two, three));
		return graph;
	}

	@Test
	public void acceptVisitsTheGraphThenAllNodesThenAllEdgesWhenNeverStopping() {
		TestGraph graph = linear();
		RecordingVisitor visitor = new RecordingVisitor();

		graph.accept(visitor, "tool");

		assertEquals(Set.of(graph), visitor.graphs);
		assertEquals(new HashSet<>(graph.getNodes()), visitor.nodes);
		assertEquals(new HashSet<>(graph.getEdges()), visitor.edges);
	}

	@Test
	public void acceptStopsImmediatelyWhenTheGraphCallbackReturnsFalse() {
		TestGraph graph = linear();
		RecordingVisitor visitor = new RecordingVisitor();
		visitor.stopOnGraph = true;

		graph.accept(visitor, "tool");

		assertEquals(Set.of(graph), visitor.graphs);
		assertTrue(visitor.nodes.isEmpty(), "no node should have been visited");
		assertTrue(visitor.edges.isEmpty(), "no edge should have been visited");
	}

	@Test
	public void acceptStopsTheNodeLoopWithoutVisitingAnyEdgeWhenANodeCallbackReturnsFalse() {
		TestGraph graph = linear();
		RecordingVisitor visitor = new RecordingVisitor();
		visitor.stopOnAllNodes = true;

		graph.accept(visitor, "tool");

		// exactly one node is visited: the first one, whose callback
		// interrupts the node loop before it can reach any other node
		assertEquals(1, visitor.nodes.size());
		assertTrue(visitor.edges.isEmpty(), "no edge should have been visited once the node loop is interrupted");
	}

	@Test
	public void acceptVisitsAllNodesButStopsTheEdgeLoopWhenAnEdgeCallbackReturnsFalse() {
		TestGraph graph = linear();
		RecordingVisitor visitor = new RecordingVisitor();
		visitor.stopOnAllEdges = true;

		graph.accept(visitor, "tool");

		assertEquals(new HashSet<>(graph.getNodes()), visitor.nodes, "the node loop should complete regardless");
		assertEquals(1, visitor.edges.size(), "only the first edge should have been visited");
	}

	@Test
	public void getCycleEntriesIsEmptyForAnAcyclicGraph() {
		TestGraph graph = new TestGraph();
		TestNode source = new TestNode(1);
		TestNode left = new TestNode(2);
		TestNode right = new TestNode(3);
		TestNode join = new TestNode(4);
		graph.addNode(source, true);
		graph.addNode(left);
		graph.addNode(right);
		graph.addNode(join);
		graph.addEdge(new TestEdge(source, left));
		graph.addEdge(new TestEdge(source, right));
		graph.addEdge(new TestEdge(left, join));
		graph.addEdge(new TestEdge(right, join));

		assertTrue(graph.getCycleEntries().isEmpty());
	}

	@Test
	public void getCycleEntriesFindsTheHeaderOfALoop() {
		TestGraph graph = new TestGraph();
		TestNode source = new TestNode(1);
		TestNode loopHeader = new TestNode(2);
		TestNode body = new TestNode(3);
		TestNode end = new TestNode(4);
		graph.addNode(source, true);
		graph.addNode(loopHeader);
		graph.addNode(body);
		graph.addNode(end);
		graph.addEdge(new TestEdge(source, loopHeader));
		graph.addEdge(new TestEdge(loopHeader, body));
		graph.addEdge(new TestEdge(body, loopHeader));
		graph.addEdge(new TestEdge(loopHeader, end));

		// loopHeader is reached both by a normal predecessor (source) and by
		// a back-edge predecessor (body, which loopHeader itself dominates)
		assertEquals(Set.of(loopHeader), graph.getCycleEntries());
	}

	@Test
	public void isEqualToIsReflexiveAndStructural() {
		TestGraph graph = linear();
		assertTrue(graph.isEqualTo(graph));

		TestGraph clone = new TestGraph();
		clone.addNode(new TestNode(1), true);
		clone.addNode(new TestNode(2));
		clone.addNode(new TestNode(3));
		clone.addEdge(new TestEdge(new TestNode(1), new TestNode(2)));
		clone.addEdge(new TestEdge(new TestNode(2), new TestNode(3)));
		assertTrue(graph.isEqualTo(clone), "two graphs with the same structure should be equal");

		assertFalse(graph.isEqualTo(null));
	}

	@Test
	public void isEqualToDetectsStructuralDifferences() {
		TestGraph graph = linear();

		TestGraph missingEdge = new TestGraph();
		missingEdge.addNode(new TestNode(1), true);
		missingEdge.addNode(new TestNode(2));
		missingEdge.addNode(new TestNode(3));
		missingEdge.addEdge(new TestEdge(new TestNode(1), new TestNode(2)));
		assertFalse(graph.isEqualTo(missingEdge));

		TestGraph differentEntrypoints = new TestGraph();
		differentEntrypoints.addNode(new TestNode(1));
		differentEntrypoints.addNode(new TestNode(2));
		differentEntrypoints.addNode(new TestNode(3));
		differentEntrypoints.addEdge(new TestEdge(new TestNode(1), new TestNode(2)));
		differentEntrypoints.addEdge(new TestEdge(new TestNode(2), new TestNode(3)));
		assertFalse(
				graph.isEqualTo(differentEntrypoints),
				"a graph without the same entrypoints should not be equal");
	}

	@Test
	public void toSerializableGraphIsUnsupportedByDefault() {
		assertThrows(UnsupportedOperationException.class, () -> linear().toSerializableGraph());
	}

}
