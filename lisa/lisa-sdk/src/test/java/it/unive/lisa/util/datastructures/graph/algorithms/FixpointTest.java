package it.unive.lisa.util.datastructures.graph.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.TestGraph;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestEdge;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.junit.Test;

public class FixpointTest {

	private static class FixpointTester
			extends
			ForwardFixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>> {

		public FixpointTester(
				TestGraph graph,
				boolean forceFullEvaluation) {
			super(graph, forceFullEvaluation);
		}

		@Override
		public Set<TestNode> semantics(
				TestNode node,
				Set<TestNode> entrystate)
				throws Exception {
			Set<TestNode> res = new HashSet<>(entrystate);
			res.add(node);
			return res;
		}

		@Override
		public Set<TestNode> traverse(
				TestEdge edge,
				Set<TestNode> entrystate)
				throws Exception {
			return entrystate;
		}

		@Override
		public Set<TestNode> union(
				TestNode node,
				Set<TestNode> left,
				Set<TestNode> right)
				throws Exception {
			return SetUtils.union(left, right);
		}

		@Override
		public Set<TestNode> join(
				TestNode node,
				Set<TestNode> approx,
				Set<TestNode> old)
				throws Exception {
			return SetUtils.union(approx, old);
		}

		@Override
		public boolean leq(
				TestNode node,
				Set<TestNode> approx,
				Set<TestNode> old)
				throws Exception {
			return old.containsAll(approx);
		}

	}

	@Test
	public void testEmptyGraph() {
		Map<TestNode, Set<TestNode>> res = null;
		try {
			res = new FixpointTester(new TestGraph(), false)
					.fixpoint(Map.of(), new FIFOWorkingSet<>());
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
		TestNode source = new TestNode(1);
		TestNode middle = new TestNode(2);
		TestNode end = new TestNode(3);
		graph.addNode(source, true);
		graph.addNode(middle);
		graph.addNode(end);
		graph.addEdge(new TestEdge(source, middle));
		graph.addEdge(new TestEdge(middle, end));

		Map<TestNode, Set<TestNode>> res = null;
		try {
			res = new FixpointTester(graph, false)
					.fixpoint(Map.of(source, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull("Fixpoint failed", res);
		assertEquals(
				"Fixpoint returned wrong result",
				Map.of(source, Set.of(source), middle, Set.of(source, middle), end, Set.of(source, middle, end)),
				res);
	}

	@Test
	public void testBranchingGraph() {
		TestGraph graph = new TestGraph();
		TestNode source = new TestNode(1);
		TestNode left = new TestNode(2);
		TestNode right = new TestNode(3);
		TestNode join = new TestNode(4);
		TestNode end = new TestNode(5);
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
			res = new FixpointTester(graph, false)
					.fixpoint(Map.of(source, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull("Fixpoint failed", res);
		assertEquals(
				"Fixpoint returned wrong result",
				Map.of(
						source,
						Set.of(source),
						left,
						Set.of(source, left),
						right,
						Set.of(source, right),
						join,
						Set.of(source, left, right, join),
						end,
						Set.of(source, left, right, join, end)),
				res);
	}

	@Test
	public void testCyclicGraph() {
		TestGraph graph = new TestGraph();
		TestNode source = new TestNode(1);
		TestNode first = new TestNode(2);
		TestNode second = new TestNode(3);
		TestNode join = new TestNode(4);
		TestNode end = new TestNode(5);
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
			res = new FixpointTester(graph, false)
					.fixpoint(Map.of(source, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull("Fixpoint failed", res);
		assertEquals(
				"Fixpoint returned wrong result",
				Map.of(
						source,
						Set.of(source),
						join,
						Set.of(source, join, first, second),
						first,
						Set.of(source, join, first, second),
						second,
						Set.of(source, join, first, second),
						end,
						Set.of(source, join, first, second, end)),
				res);
	}

	private static class ExceptionalTester
			extends
			ForwardFixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>> {

		private final int type;

		private ExceptionalTester(
				TestGraph graph,
				boolean forceFullEvaluation,
				int type) {
			super(graph, forceFullEvaluation);
			this.type = type;
		}

		@Override
		public Set<TestNode> semantics(
				TestNode node,
				Set<TestNode> entrystate)
				throws Exception {
			if (type == 0)
				throw new Exception();
			return Collections.emptySet();
		}

		@Override
		public Set<TestNode> traverse(
				TestEdge edge,
				Set<TestNode> entrystate)
				throws Exception {
			if (type == 1)
				throw new Exception();
			return Collections.emptySet();
		}

		@Override
		public Set<TestNode> union(
				TestNode node,
				Set<TestNode> left,
				Set<TestNode> right)
				throws Exception {
			if (type == 2)
				throw new Exception();
			return Collections.emptySet();
		}

		@Override
		public Set<TestNode> join(
				TestNode node,
				Set<TestNode> approx,
				Set<TestNode> old)
				throws Exception {
			if (type == 3)
				throw new Exception();
			return Collections.emptySet();
		}

		@Override
		public boolean leq(
				TestNode node,
				Set<TestNode> approx,
				Set<TestNode> old)
				throws Exception {
			if (type == 4)
				throw new Exception();
			return true;
		}

	}

	@Test
	public void testExceptionalImplementations() {
		TestGraph graph = new TestGraph();
		TestNode source = new TestNode(1);
		TestNode first = new TestNode(2);
		TestNode second = new TestNode(3);
		TestNode join = new TestNode(4);
		TestNode end = new TestNode(5);
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
			res = new ExceptionalTester(graph, false, 0)
					.fixpoint(Map.of(source, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("computing semantics"));
		}

		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull("Fixpoint failed", res);

		fail = false;
		try {
			res = new ExceptionalTester(graph, false, 1)
					.fixpoint(Map.of(source, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("computing edge semantics"));
		}

		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull("Fixpoint failed", res);

		fail = false;
		try {
			res = new ExceptionalTester(graph, false, 2)
					.fixpoint(Map.of(source, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("creating entry state"));
		}

		if (!fail)

			fail("The fixpoint computation hasn't thrown an exception");
		assertNull("Fixpoint failed", res);

		fail = false;
		try {
			res = new ExceptionalTester(graph, false, 3)
					.fixpoint(Map.of(source, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("joining states"));
		}

		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull("Fixpoint failed", res);

		fail = false;
		try {
			res = new ExceptionalTester(graph, false, 4)
					.fixpoint(Map.of(source, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("updating result"));
		}

		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull("Fixpoint failed", res);
	}

}
