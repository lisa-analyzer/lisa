package it.unive.lisa.util.datastructures.graph.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.collections4.SetUtils;
import org.junit.Test;

import it.unive.lisa.outputs.DotGraph;
import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.datastructures.graph.Node;
import it.unive.lisa.util.workset.FIFOWorkingSet;

public class FixpointTest {

	private static class TestNode implements Node<TestNode, TestEdge, TestGraph> {

		private int offset = -1;
		
		@Override
		public int setOffset(int offset) {
			return this.offset = offset;
		}

		@Override
		public <V> boolean accept(GraphVisitor<TestGraph, TestNode, TestEdge, V> visitor, V tool) {
			return false;
		}

		@Override
		public String toString() {
			return String.valueOf(offset);
		}
	}

	private static class TestEdge implements Edge<TestNode, TestEdge, TestGraph> {

		private final TestNode source, destination;

		private TestEdge(TestNode source, TestNode destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		public TestNode getSource() {
			return source;
		}

		@Override
		public TestNode getDestination() {
			return destination;
		}

		@Override
		public boolean canBeSimplified() {
			return false;
		}

		@Override
		public TestEdge newInstance(TestNode source, TestNode destination) {
			return new TestEdge(source, destination);
		}

		@Override
		public <V> boolean accept(GraphVisitor<TestGraph, TestNode, TestEdge, V> visitor, V tool) {
			return false;
		}

		@Override
		public String toString() {
			return source + "->" + destination;
		}
	}

	private static class TestGraph extends Graph<TestGraph, TestNode, TestEdge> {

		@Override
		protected DotGraph<TestNode, TestEdge, TestGraph> toDot(Function<TestNode, String> labelGenerator) {
			return null;
		}
	}

	private static Set<TestNode> nodeSemantics(TestNode n, Set<TestNode> entry) {
		Set<TestNode> res = new HashSet<>(entry);
		res.add(n);
		return res;
	}

	private static Set<TestNode> edgeSemantics(TestEdge e, Set<TestNode> entry) {
		return entry;
	}

	private static Set<TestNode> join(TestNode n, Set<TestNode> first, Set<TestNode> second) {
		return SetUtils.union(first, second);
	}

	private static boolean equality(TestNode n, Set<TestNode> first, Set<TestNode> second) {
		return second.containsAll(first);
	}

	@Test
	public void testEmptyGraph() {
		Map<TestNode, Set<TestNode>> res = null;
		try {
			res = new Fixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>>(new TestGraph()).fixpoint(
					Map.of(),
					FIFOWorkingSet.mk(),
					FixpointTest::nodeSemantics,
					FixpointTest::edgeSemantics,
					FixpointTest::join,
					FixpointTest::equality);
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
		
		assertNotNull("Fixpoint failed", res);
		assertTrue("Fixpoint returned wrong result", res.isEmpty());
	}

	@Test
	public void testLinearGraph() {
		TestGraph graph = new TestGraph();
		TestNode source = new TestNode();
		TestNode middle = new TestNode();
		TestNode end = new TestNode();
		graph.addNode(source, true);
		graph.addNode(middle);
		graph.addNode(end);
		graph.addEdge(new TestEdge(source, middle));
		graph.addEdge(new TestEdge(middle, end));
		
		
		Map<TestNode, Set<TestNode>> res = null;
		try {
			res = new Fixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>>(graph).fixpoint(
					Map.of(source, Set.of()),
					FIFOWorkingSet.mk(),
					FixpointTest::nodeSemantics,
					FixpointTest::edgeSemantics,
					FixpointTest::join,
					FixpointTest::equality);
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
		
		assertNotNull("Fixpoint failed", res);
		assertEquals("Fixpoint returned wrong result",
				Map.of(source, Set.of(source), 
						middle, Set.of(source, middle), 
						end, Set.of(source, middle, end))
				, res);
	}

	@Test
	public void testBranchingGraph() {
		TestGraph graph = new TestGraph();
		TestNode source = new TestNode();
		TestNode left = new TestNode();
		TestNode right = new TestNode();
		TestNode join = new TestNode();
		TestNode end = new TestNode();
		graph.addNode(source, true);
		graph.addNode(left);
		graph.addNode(right);
		graph.addNode(join);
		graph.addNode(end);
		graph.addEdge(new TestEdge(source, left));
		graph.addEdge(new TestEdge(source, right));
		graph.addEdge(new TestEdge(left, join));
		graph.addEdge(new TestEdge(right, join));
		graph.addEdge(new TestEdge(join, end));
		
		
		Map<TestNode, Set<TestNode>> res = null;
		try {
			res = new Fixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>>(graph).fixpoint(
					Map.of(source, Set.of()),
					FIFOWorkingSet.mk(),
					FixpointTest::nodeSemantics,
					FixpointTest::edgeSemantics,
					FixpointTest::join,
					FixpointTest::equality);
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
		
		assertNotNull("Fixpoint failed", res);
		assertEquals("Fixpoint returned wrong result",
				Map.of(source, Set.of(source), 
						left, Set.of(source, left), 
						right, Set.of(source, right), 
						join, Set.of(source, left, right, join), 
						end, Set.of(source, left, right, join, end))
				, res);
	}

	@Test
	public void testCyclicGraph() {
		TestGraph graph = new TestGraph();
		TestNode source = new TestNode();
		TestNode first = new TestNode();
		TestNode second = new TestNode();
		TestNode join = new TestNode();
		TestNode end = new TestNode();
		graph.addNode(source, true);
		graph.addNode(first);
		graph.addNode(second);
		graph.addNode(join);
		graph.addNode(end);
		graph.addEdge(new TestEdge(source, join));
		graph.addEdge(new TestEdge(join, first));
		graph.addEdge(new TestEdge(first, second));
		graph.addEdge(new TestEdge(second, join));
		graph.addEdge(new TestEdge(join, end));
		
		
		Map<TestNode, Set<TestNode>> res = null;
		try {
			res = new Fixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>>(graph).fixpoint(
					Map.of(source, Set.of()),
					FIFOWorkingSet.mk(),
					FixpointTest::nodeSemantics,
					FixpointTest::edgeSemantics,
					FixpointTest::join,
					FixpointTest::equality);
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
		
		assertNotNull("Fixpoint failed", res);
		assertEquals("Fixpoint returned wrong result",
				Map.of(source, Set.of(source), 
						join, Set.of(source, join, first, second), 
						first, Set.of(source, join, first, second), 
						second, Set.of(source, join, first, second), 
						end, Set.of(source, join, first, second, end))
				, res);
	}
	
	private static Set<TestNode> throwingNodeSemantics(TestNode n, Set<TestNode> entry) throws Exception {
		throw new Exception();
	}

	private static Set<TestNode> throwingEdgeSemantics(TestEdge e, Set<TestNode> entry) throws Exception {
		throw new Exception();
	}

	private static Set<TestNode> throwingJoin(TestNode n, Set<TestNode> first, Set<TestNode> second) throws Exception {
		throw new Exception();
	}

	private static boolean throwingEquality(TestNode n, Set<TestNode> first, Set<TestNode> second) throws Exception {
		throw new Exception();
	}

	@Test
	public void testExceptionalImplementations() {
		TestGraph graph = new TestGraph();
		TestNode source = new TestNode();
		TestNode first = new TestNode();
		TestNode second = new TestNode();
		TestNode join = new TestNode();
		TestNode end = new TestNode();
		graph.addNode(source, true);
		graph.addNode(first);
		graph.addNode(second);
		graph.addNode(join);
		graph.addNode(end);
		graph.addEdge(new TestEdge(source, join));
		graph.addEdge(new TestEdge(join, first));
		graph.addEdge(new TestEdge(first, second));
		graph.addEdge(new TestEdge(second, join));
		graph.addEdge(new TestEdge(join, end));
		
		
		Map<TestNode, Set<TestNode>> res = null;
		boolean fail = false;
		try {
			res = new Fixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>>(graph).fixpoint(
					Map.of(source, Set.of()),
					FIFOWorkingSet.mk(),
					FixpointTest::throwingNodeSemantics,
					FixpointTest::edgeSemantics,
					FixpointTest::join,
					FixpointTest::equality);
		} catch (FixpointException e) {
			fail = true;
			assertTrue("Wrong message", e.getMessage().contains("computing semantics"));
		}

		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull("Fixpoint failed", res);
		
		fail = false;
		try {
			res = new Fixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>>(graph).fixpoint(
					Map.of(source, Set.of()),
					FIFOWorkingSet.mk(),
					FixpointTest::nodeSemantics,
					FixpointTest::throwingEdgeSemantics,
					FixpointTest::join,
					FixpointTest::equality);
		} catch (FixpointException e) {
			fail = true;
			assertTrue("Wrong message", e.getMessage().contains("computing edge semantics"));
		}

		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull("Fixpoint failed", res);
		
		fail = false;
		try {
			res = new Fixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>>(graph).fixpoint(
					Map.of(source, Set.of()),
					FIFOWorkingSet.mk(),
					FixpointTest::nodeSemantics,
					FixpointTest::edgeSemantics,
					FixpointTest::throwingJoin,
					FixpointTest::equality);
		} catch (FixpointException e) {
			fail = true;
			assertTrue("Wrong message", e.getMessage().contains("creating entry state") || e.getMessage().contains("joining states"));
		}

		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull("Fixpoint failed", res);
		
		fail = false;
		try {
			res = new Fixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>>(graph).fixpoint(
					Map.of(source, Set.of()),
					FIFOWorkingSet.mk(),
					FixpointTest::nodeSemantics,
					FixpointTest::edgeSemantics,
					FixpointTest::join,
					FixpointTest::throwingEquality);
		} catch (FixpointException e) {
			fail = true;
			assertTrue("Wrong message", e.getMessage().contains("updating result"));
		}

		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull("Fixpoint failed", res);
	}
}
