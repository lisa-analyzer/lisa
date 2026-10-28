package it.unive.lisa.util.datastructures.graph.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.TestGraph;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestEdge;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

// mirrors FixpointTest, but for BackwardFixpoint: propagation follows
// predecessorsOf instead of followersOf, so a node's result is the closure
// of all nodes reachable FROM it (inclusive), rather than the closure of
// all nodes that can reach it
public class BackwardFixpointTest {

	private static class BackwardFixpointTester
			extends
			BackwardFixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>> {

		public BackwardFixpointTester(
				TestGraph graph,
				boolean forceFullEvaluation) {
			super(graph, forceFullEvaluation);
		}

		@Override
		public Pair<Set<TestNode>, TestNode> semantics(
				TestNode node,
				Set<TestNode> exitstate,
				Map<TestNode, Set<TestNode>> result)
				throws Exception {
			Set<TestNode> res = new HashSet<>(exitstate);
			res.add(node);
			return Pair.of(res, node);
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
			res = new BackwardFixpointTester(new TestGraph(), false)
					.fixpoint(Map.of(), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull(res, "Fixpoint failed");
		assertTrue(res.isEmpty(), "Fixpoint returned wrong result");
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
			// starting from the exit node, propagating backward
			res = new BackwardFixpointTester(graph, false)
					.fixpoint(Map.of(end, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull(res, "Fixpoint failed");
		assertEquals(
				Map.of(end, Set.of(end), middle, Set.of(end, middle), source, Set.of(end, middle, source)),
				res,
				"Fixpoint returned wrong result");
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
			res = new BackwardFixpointTester(graph, false)
					.fixpoint(Map.of(end, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull(res, "Fixpoint failed");
		// every node inside (or before) the join/first/second cycle reaches
		// the whole cycle plus the exit node; the exit node only reaches
		// itself
		assertEquals(
				Map.of(
						end,
						Set.of(end),
						join,
						Set.of(join, first, second, end),
						first,
						Set.of(join, first, second, end),
						second,
						Set.of(join, first, second, end),
						source,
						Set.of(source, join, first, second, end)),
				res,
				"Fixpoint returned wrong result");
	}

	private static class ExceptionalTester
			extends
			BackwardFixpoint<TestGraph, TestNode, TestEdge, Set<TestNode>> {

		private final int type;

		private ExceptionalTester(
				TestGraph graph,
				boolean forceFullEvaluation,
				int type) {
			super(graph, forceFullEvaluation);
			this.type = type;
		}

		@Override
		public Pair<Set<TestNode>, TestNode> semantics(
				TestNode node,
				Set<TestNode> exitstate,
				Map<TestNode, Set<TestNode>> result)
				throws Exception {
			if (type == 0)
				throw new Exception();
			return Pair.of(Collections.emptySet(), node);
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
					.fixpoint(Map.of(end, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue(e.getMessage().contains("computing semantics"), "Wrong message: " + e.getMessage());
		}
		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull(res, "Fixpoint failed");

		fail = false;
		try {
			res = new ExceptionalTester(graph, false, 1)
					.fixpoint(Map.of(end, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue(e.getMessage().contains("computing edge semantics"), "Wrong message: " + e.getMessage());
		}
		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull(res, "Fixpoint failed");

		fail = false;
		try {
			res = new ExceptionalTester(graph, false, 2)
					.fixpoint(Map.of(end, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue(e.getMessage().contains("creating entry state"), "Wrong message: " + e.getMessage());
		}
		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull(res, "Fixpoint failed");

		fail = false;
		try {
			res = new ExceptionalTester(graph, false, 3)
					.fixpoint(Map.of(end, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue(e.getMessage().contains("joining states"), "Wrong message: " + e.getMessage());
		}
		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull(res, "Fixpoint failed");

		fail = false;
		try {
			res = new ExceptionalTester(graph, false, 4)
					.fixpoint(Map.of(end, Set.of()), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			fail = true;
			assertTrue(e.getMessage().contains("updating result"), "Wrong message: " + e.getMessage());
		}
		if (!fail)
			fail("The fixpoint computation hasn't thrown an exception");
		assertNull(res, "Fixpoint failed");
	}

}
