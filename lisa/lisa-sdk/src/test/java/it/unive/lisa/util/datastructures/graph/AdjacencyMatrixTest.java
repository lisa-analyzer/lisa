package it.unive.lisa.util.datastructures.graph;

import static org.apache.commons.collections4.CollectionUtils.isEqualCollection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.util.datastructures.graph.TestGraph.TestEdge;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.SetUtils;
import org.junit.Test;

public class AdjacencyMatrixTest {

	private static final Random rand = new Random();

	private <T> String msg(
			String objs,
			String extra,
			Collection<T> exp,
			Collection<T> act) {
		Set<T> ex = exp instanceof Set ? (Set<T>) exp : new HashSet<>(exp);
		Set<T> ac = act instanceof Set ? (Set<T>) act : new HashSet<>(act);
		return "Set of "
			+ objs
			+ " is different "
			+ extra
			+ "\nonly expected: "
			+ SetUtils.difference(ex, ac)
			+ "\nonly actual: "
			+ SetUtils.difference(ac, ex);
	}

	private void verify(
			Map<TestNode, Collection<TestNode>> adj,
			Collection<TestNode> nodes,
			Collection<TestEdge> edges,
			AdjacencyMatrix<TestGraph, TestNode, TestEdge> matrix,
			Collection<TestNode> entries,
			Collection<TestNode> exits) {
		verify(adj, nodes, edges, matrix, entries, exits, "");
	}

	private void verify(
			Map<TestNode, Collection<TestNode>> adj,
			Collection<TestNode> nodes,
			Collection<TestEdge> edges,
			AdjacencyMatrix<TestGraph, TestNode, TestEdge> matrix,
			Collection<TestNode> entries,
			Collection<TestNode> exits,
			String extra) {
		TestNode externalNode = new TestNode(-1);
		TestEdge externalEdge = new TestEdge(externalNode, externalNode);

		// we use isEqualCollection instead of equals since we are ok if the
		// concrete collection used is different, we just want the same elements
		assertTrue(msg("nodes", extra, nodes, matrix.getNodes()), isEqualCollection(nodes, matrix.getNodes()));
		assertTrue(msg("edges", extra, edges, matrix.getEdges()), isEqualCollection(edges, matrix.getEdges()));
		assertTrue(
			msg("entries", extra, entries, matrix.getEntries()),
			isEqualCollection(entries, matrix.getEntries()));
		assertTrue(msg("exits", extra, exits, matrix.getExits()), isEqualCollection(exits, matrix.getExits()));

		for (TestNode node : nodes) {
			assertTrue("matrix does not contain " + node, matrix.containsNode(node));
			Collection<TestEdge> ins = new HashSet<>();
			Collection<TestEdge> outs = new HashSet<>();

			if (adj.containsKey(node))
				for (TestNode dest : adj.get(node))
					outs.add(new TestEdge(node, dest));
			for (Entry<TestNode, Collection<TestNode>> entry : adj.entrySet())
				if (entry.getValue().contains(node))
					ins.add(new TestEdge(entry.getKey(), node));

			assertTrue(
				msg("ingoing edges", extra, ins, matrix.getIngoingEdges(node)),
				isEqualCollection(ins, matrix.getIngoingEdges(node)));
			assertTrue(
				msg("outgoing edges", extra, outs, matrix.getOutgoingEdges(node)),
				isEqualCollection(outs, matrix.getOutgoingEdges(node)));

			Set<TestNode> follows = outs.stream().map(Edge::getDestination).collect(Collectors.toSet());
			Set<TestNode> preds = ins.stream().map(Edge::getSource).collect(Collectors.toSet());
			assertTrue(
				msg("followers", extra, follows, matrix.followersOf(node)),
				isEqualCollection(follows, matrix.followersOf(node)));
			assertTrue(
				msg("predecessors", extra, preds, matrix.predecessorsOf(node)),
				isEqualCollection(preds, matrix.predecessorsOf(node)));

			// we check that re-adding the node does not clear the edges
			matrix.addNode(node);
			assertTrue(
				msg("ingoing edges", extra, ins, matrix.getIngoingEdges(node)),
				isEqualCollection(ins, matrix.getIngoingEdges(node)));
			assertTrue(
				msg("outgoing edges", extra, outs, matrix.getOutgoingEdges(node)),
				isEqualCollection(outs, matrix.getOutgoingEdges(node)));
			boolean failed = false;
			try {
				matrix.addEdge(new TestEdge(externalNode, node));
			} catch (UnsupportedOperationException e) {
				failed = true;
			}
			assertTrue("Adding edge with external endpoint succeded", failed);
			failed = false;
			try {
				matrix.addEdge(new TestEdge(node, externalNode));
			} catch (UnsupportedOperationException e) {
				failed = true;
			}
			assertTrue("Adding edge with external endpoint succeded", failed);
		}

		for (TestEdge edge : edges) {
			assertTrue("matrix does not contain " + edge, matrix.containsEdge(edge));
			assertSame(
				edge + " is not connecting its endpoints",
				edge,
				matrix.getEdgeConnecting(edge.getSource(), edge.getDestination()));
		}

		assertEquals("matrix cloning failed", matrix, new AdjacencyMatrix<>(matrix));

		assertFalse("matrix contains an external node", matrix.containsNode(externalNode));
		assertFalse("matrix contains an external edge", matrix.containsEdge(externalEdge));
		assertNull("matrix contains an external edge", matrix.getEdgeConnecting(externalNode, nodes.iterator().next()));
		assertNull("matrix contains an external edge", matrix.getEdgeConnecting(nodes.iterator().next(), externalNode));
		boolean failed = false;
		try {
			matrix.followersOf(externalNode);
		} catch (IllegalArgumentException e) {
			failed = true;
		}
		assertTrue("Asking for the follower of an external node succeded", failed);
		failed = false;
		try {
			matrix.predecessorsOf(externalNode);
		} catch (IllegalArgumentException e) {
			failed = true;
		}
		assertTrue("Asking for the predecessor of an external node succeded", failed);

		// just to ensure it does not throw exceptions
		matrix.toString();
	}

	private Map<TestNode, Collection<TestNode>> populate(
			AdjacencyMatrix<TestGraph, TestNode, TestEdge> matrix,
			Collection<TestNode> nodes,
			Collection<TestEdge> edges,
			Collection<TestNode> entries,
			Collection<TestNode> exits) {
		for (int i = 20 + rand.nextInt(100); i >= 0; i--) {
			TestNode node = new TestNode(i);
			nodes.add(node);
			matrix.addNode(node);
		}

		int bound = nodes.size();
		entries.addAll(nodes);
		exits.addAll(nodes);
		Map<TestNode, Collection<TestNode>> adj = new HashMap<>();
		List<TestNode> tmp = new ArrayList<>(nodes);
		for (int i = 20 + rand.nextInt(100); i >= 0; i--) {
			TestNode src = tmp.get(rand.nextInt(bound));
			TestNode dest = tmp.get(rand.nextInt(bound));
			if (adj.computeIfAbsent(src, k -> new HashSet<>()).add(dest)) {
				TestEdge e = new TestEdge(src, dest);
				edges.add(e);
				matrix.addEdge(e);
				entries.remove(dest);
				exits.remove(src);
			}
		}
		return adj;
	}

	@Test
	public void testBasicStructure() {
		Collection<TestNode> nodes = new HashSet<>();
		Collection<TestEdge> edges = new HashSet<>();
		Collection<TestNode> entries = new HashSet<>();
		Collection<TestNode> exits = new HashSet<>();
		AdjacencyMatrix<TestGraph, TestNode, TestEdge> matrix = new AdjacencyMatrix<>();
		Map<TestNode, Collection<TestNode>> adj = populate(matrix, nodes, edges, entries, exits);
		verify(adj, nodes, edges, matrix, entries, exits);
	}

	@Test
	public void testMerge() {
		Collection<TestNode> nodes1 = new HashSet<>();
		Collection<TestEdge> edges1 = new HashSet<>();
		Collection<TestNode> entries1 = new HashSet<>();
		Collection<TestNode> exits1 = new HashSet<>();
		AdjacencyMatrix<TestGraph, TestNode, TestEdge> matrix1 = new AdjacencyMatrix<>();
		Map<TestNode, Collection<TestNode>> adj1 = populate(matrix1, nodes1, edges1, entries1, exits1);
		verify(adj1, nodes1, edges1, matrix1, entries1, exits1);

		Collection<TestNode> nodes2 = new HashSet<>();
		Collection<TestEdge> edges2 = new HashSet<>();
		Collection<TestNode> entries2 = new HashSet<>();
		Collection<TestNode> exits2 = new HashSet<>();
		AdjacencyMatrix<TestGraph, TestNode, TestEdge> matrix2 = new AdjacencyMatrix<>();
		Map<TestNode, Collection<TestNode>> adj2 = populate(matrix2, nodes2, edges2, entries2, exits2);
		verify(adj2, nodes2, edges2, matrix2, entries2, exits2);

		matrix1.mergeWith(matrix2);
		nodes1.addAll(nodes2);
		edges1.addAll(edges2);
		entries1.addAll(entries2);
		exits1.addAll(exits2);
		edges1.forEach(e -> {
			entries1.remove(e.getDestination());
			exits1.remove(e.getSource());
		});
		for (Entry<TestNode, Collection<TestNode>> entry : adj2.entrySet())
			adj1.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
		verify(adj1, nodes1, edges1, matrix1, entries1, exits1);
	}

	@Test
	public void testBaseDistances() {
		Collection<TestNode> nodes = new HashSet<>();
		Collection<TestEdge> edges = new HashSet<>();
		Collection<TestNode> entries = new HashSet<>();
		Collection<TestNode> exits = new HashSet<>();
		AdjacencyMatrix<TestGraph, TestNode, TestEdge> matrix = new AdjacencyMatrix<>();
		Map<TestNode, Collection<TestNode>> adj = populate(matrix, nodes, edges, entries, exits);
		verify(adj, nodes, edges, matrix, entries, exits);

		for (Entry<TestNode, Collection<TestNode>> entry : adj.entrySet()) {
			TestNode node = entry.getKey();
			// self-loops don't count: this is the min distance
			assertEquals(0, matrix.distance(node, node));
			for (TestNode dest : entry.getValue())
				if (node != dest)
					assertEquals(1, matrix.distance(node, dest));
		}
	}

	@Test
	public void testGraph1() {
		TestGraph graph = new TestGraph();
		TestNode one = new TestNode(1);
		TestNode two = new TestNode(2);
		TestNode three = new TestNode(3);
		TestNode four = new TestNode(4);
		TestNode five = new TestNode(5);
		TestNode six = new TestNode(6);
		graph.addNode(one, true);
		graph.addNode(two);
		graph.addNode(three);
		graph.addNode(four);
		graph.addNode(five);
		graph.addNode(six);
		graph.addEdge(new TestEdge(one, two));
		graph.addEdge(new TestEdge(two, three));
		graph.addEdge(new TestEdge(two, four));
		graph.addEdge(new TestEdge(two, six));
		graph.addEdge(new TestEdge(three, five));
		graph.addEdge(new TestEdge(four, five));
		graph.addEdge(new TestEdge(five, two));

		assertEquals(0, graph.adjacencyMatrix.distance(one, one));
		assertEquals(1, graph.adjacencyMatrix.distance(one, two));
		assertEquals(2, graph.adjacencyMatrix.distance(one, three));
		assertEquals(2, graph.adjacencyMatrix.distance(one, four));
		assertEquals(3, graph.adjacencyMatrix.distance(one, five));
		assertEquals(2, graph.adjacencyMatrix.distance(one, six));
		assertEquals(-1, graph.adjacencyMatrix.distance(two, one));
		assertEquals(0, graph.adjacencyMatrix.distance(two, two));
		assertEquals(1, graph.adjacencyMatrix.distance(two, three));
		assertEquals(1, graph.adjacencyMatrix.distance(two, four));
		assertEquals(2, graph.adjacencyMatrix.distance(two, five));
		assertEquals(1, graph.adjacencyMatrix.distance(two, six));
		assertEquals(-1, graph.adjacencyMatrix.distance(three, one));
		assertEquals(2, graph.adjacencyMatrix.distance(three, two));
		assertEquals(0, graph.adjacencyMatrix.distance(three, three));
		assertEquals(3, graph.adjacencyMatrix.distance(three, four));
		assertEquals(1, graph.adjacencyMatrix.distance(three, five));
		assertEquals(3, graph.adjacencyMatrix.distance(three, six));
		assertEquals(-1, graph.adjacencyMatrix.distance(four, one));
		assertEquals(2, graph.adjacencyMatrix.distance(four, two));
		assertEquals(3, graph.adjacencyMatrix.distance(four, three));
		assertEquals(0, graph.adjacencyMatrix.distance(four, four));
		assertEquals(1, graph.adjacencyMatrix.distance(four, five));
		assertEquals(3, graph.adjacencyMatrix.distance(four, six));
		assertEquals(-1, graph.adjacencyMatrix.distance(five, one));
		assertEquals(1, graph.adjacencyMatrix.distance(five, two));
		assertEquals(2, graph.adjacencyMatrix.distance(five, three));
		assertEquals(2, graph.adjacencyMatrix.distance(five, four));
		assertEquals(0, graph.adjacencyMatrix.distance(five, five));
		assertEquals(2, graph.adjacencyMatrix.distance(five, six));
		assertEquals(-1, graph.adjacencyMatrix.distance(six, one));
		assertEquals(-1, graph.adjacencyMatrix.distance(six, two));
		assertEquals(-1, graph.adjacencyMatrix.distance(six, three));
		assertEquals(-1, graph.adjacencyMatrix.distance(six, four));
		assertEquals(-1, graph.adjacencyMatrix.distance(six, five));
		assertEquals(0, graph.adjacencyMatrix.distance(six, six));
	}

	@Test
	public void testGraph2() {
		TestGraph graph = new TestGraph();
		TestNode r = new TestNode(0);
		TestNode a = new TestNode(1);
		TestNode b = new TestNode(2);
		TestNode c = new TestNode(3);
		TestNode d = new TestNode(4);
		TestNode e = new TestNode(5);
		TestNode f = new TestNode(6);
		TestNode g = new TestNode(7);
		TestNode h = new TestNode(8);
		TestNode i = new TestNode(9);
		TestNode j = new TestNode(10);
		TestNode k = new TestNode(11);
		TestNode l = new TestNode(12);
		graph.addNode(r, true);
		graph.addNode(a);
		graph.addNode(b);
		graph.addNode(c);
		graph.addNode(d);
		graph.addNode(e);
		graph.addNode(f);
		graph.addNode(g);
		graph.addNode(h);
		graph.addNode(i);
		graph.addNode(j);
		graph.addNode(k);
		graph.addNode(l);
		graph.addEdge(new TestEdge(r, a));
		graph.addEdge(new TestEdge(r, b));
		graph.addEdge(new TestEdge(r, c));
		graph.addEdge(new TestEdge(a, d));
		graph.addEdge(new TestEdge(b, a));
		graph.addEdge(new TestEdge(b, d));
		graph.addEdge(new TestEdge(b, e));
		graph.addEdge(new TestEdge(d, l));
		graph.addEdge(new TestEdge(e, h));
		graph.addEdge(new TestEdge(l, h));
		graph.addEdge(new TestEdge(h, e));
		graph.addEdge(new TestEdge(h, k));
		graph.addEdge(new TestEdge(c, f));
		graph.addEdge(new TestEdge(c, g));
		graph.addEdge(new TestEdge(f, i));
		graph.addEdge(new TestEdge(g, i));
		graph.addEdge(new TestEdge(g, j));
		graph.addEdge(new TestEdge(j, i));
		graph.addEdge(new TestEdge(i, k));
		graph.addEdge(new TestEdge(k, i));
		graph.addEdge(new TestEdge(k, r));

		assertEquals(0, graph.adjacencyMatrix.distance(r, r));
		assertEquals(1, graph.adjacencyMatrix.distance(r, a));
		assertEquals(1, graph.adjacencyMatrix.distance(r, b));
		assertEquals(1, graph.adjacencyMatrix.distance(r, c));
		assertEquals(2, graph.adjacencyMatrix.distance(r, d));
		assertEquals(2, graph.adjacencyMatrix.distance(r, e));
		assertEquals(2, graph.adjacencyMatrix.distance(r, f));
		assertEquals(2, graph.adjacencyMatrix.distance(r, g));
		assertEquals(3, graph.adjacencyMatrix.distance(r, h));
		assertEquals(3, graph.adjacencyMatrix.distance(r, i));
		assertEquals(3, graph.adjacencyMatrix.distance(r, j));
		assertEquals(4, graph.adjacencyMatrix.distance(r, k));
		assertEquals(3, graph.adjacencyMatrix.distance(r, l));

		assertEquals(5, graph.adjacencyMatrix.distance(a, r));
		assertEquals(0, graph.adjacencyMatrix.distance(a, a));
		assertEquals(6, graph.adjacencyMatrix.distance(a, b));
		assertEquals(6, graph.adjacencyMatrix.distance(a, c));
		assertEquals(1, graph.adjacencyMatrix.distance(a, d));
		assertEquals(4, graph.adjacencyMatrix.distance(a, e));
		assertEquals(7, graph.adjacencyMatrix.distance(a, f));
		assertEquals(7, graph.adjacencyMatrix.distance(a, g));
		assertEquals(3, graph.adjacencyMatrix.distance(a, h));
		assertEquals(5, graph.adjacencyMatrix.distance(a, i));
		assertEquals(8, graph.adjacencyMatrix.distance(a, j));
		assertEquals(4, graph.adjacencyMatrix.distance(a, k));
		assertEquals(2, graph.adjacencyMatrix.distance(a, l));

		assertEquals(4, graph.adjacencyMatrix.distance(b, r));
		assertEquals(1, graph.adjacencyMatrix.distance(b, a));
		assertEquals(0, graph.adjacencyMatrix.distance(b, b));
		assertEquals(5, graph.adjacencyMatrix.distance(b, c));
		assertEquals(1, graph.adjacencyMatrix.distance(b, d));
		assertEquals(1, graph.adjacencyMatrix.distance(b, e));
		assertEquals(6, graph.adjacencyMatrix.distance(b, f));
		assertEquals(6, graph.adjacencyMatrix.distance(b, g));
		assertEquals(2, graph.adjacencyMatrix.distance(b, h));
		assertEquals(4, graph.adjacencyMatrix.distance(b, i));
		assertEquals(7, graph.adjacencyMatrix.distance(b, j));
		assertEquals(3, graph.adjacencyMatrix.distance(b, k));
		assertEquals(2, graph.adjacencyMatrix.distance(b, l));

		assertEquals(4, graph.adjacencyMatrix.distance(c, r));
		assertEquals(5, graph.adjacencyMatrix.distance(c, a));
		assertEquals(5, graph.adjacencyMatrix.distance(c, b));
		assertEquals(0, graph.adjacencyMatrix.distance(c, c));
		assertEquals(6, graph.adjacencyMatrix.distance(c, d));
		assertEquals(6, graph.adjacencyMatrix.distance(c, e));
		assertEquals(1, graph.adjacencyMatrix.distance(c, f));
		assertEquals(1, graph.adjacencyMatrix.distance(c, g));
		assertEquals(7, graph.adjacencyMatrix.distance(c, h));
		assertEquals(2, graph.adjacencyMatrix.distance(c, i));
		assertEquals(2, graph.adjacencyMatrix.distance(c, j));
		assertEquals(3, graph.adjacencyMatrix.distance(c, k));
		assertEquals(7, graph.adjacencyMatrix.distance(c, l));

		assertEquals(4, graph.adjacencyMatrix.distance(d, r));
		assertEquals(5, graph.adjacencyMatrix.distance(d, a));
		assertEquals(5, graph.adjacencyMatrix.distance(d, b));
		assertEquals(5, graph.adjacencyMatrix.distance(d, c));
		assertEquals(0, graph.adjacencyMatrix.distance(d, d));
		assertEquals(3, graph.adjacencyMatrix.distance(d, e));
		assertEquals(6, graph.adjacencyMatrix.distance(d, f));
		assertEquals(6, graph.adjacencyMatrix.distance(d, g));
		assertEquals(2, graph.adjacencyMatrix.distance(d, h));
		assertEquals(4, graph.adjacencyMatrix.distance(d, i));
		assertEquals(7, graph.adjacencyMatrix.distance(d, j));
		assertEquals(3, graph.adjacencyMatrix.distance(d, k));
		assertEquals(1, graph.adjacencyMatrix.distance(d, l));

		assertEquals(3, graph.adjacencyMatrix.distance(e, r));
		assertEquals(4, graph.adjacencyMatrix.distance(e, a));
		assertEquals(4, graph.adjacencyMatrix.distance(e, b));
		assertEquals(4, graph.adjacencyMatrix.distance(e, c));
		assertEquals(5, graph.adjacencyMatrix.distance(e, d));
		assertEquals(0, graph.adjacencyMatrix.distance(e, e));
		assertEquals(5, graph.adjacencyMatrix.distance(e, f));
		assertEquals(5, graph.adjacencyMatrix.distance(e, g));
		assertEquals(1, graph.adjacencyMatrix.distance(e, h));
		assertEquals(3, graph.adjacencyMatrix.distance(e, i));
		assertEquals(6, graph.adjacencyMatrix.distance(e, j));
		assertEquals(2, graph.adjacencyMatrix.distance(e, k));
		assertEquals(6, graph.adjacencyMatrix.distance(e, l));

		assertEquals(3, graph.adjacencyMatrix.distance(f, r));
		assertEquals(4, graph.adjacencyMatrix.distance(f, a));
		assertEquals(4, graph.adjacencyMatrix.distance(f, b));
		assertEquals(4, graph.adjacencyMatrix.distance(f, c));
		assertEquals(5, graph.adjacencyMatrix.distance(f, d));
		assertEquals(5, graph.adjacencyMatrix.distance(f, e));
		assertEquals(0, graph.adjacencyMatrix.distance(f, f));
		assertEquals(5, graph.adjacencyMatrix.distance(f, g));
		assertEquals(6, graph.adjacencyMatrix.distance(f, h));
		assertEquals(1, graph.adjacencyMatrix.distance(f, i));
		assertEquals(6, graph.adjacencyMatrix.distance(f, j));
		assertEquals(2, graph.adjacencyMatrix.distance(f, k));
		assertEquals(6, graph.adjacencyMatrix.distance(f, l));

		assertEquals(3, graph.adjacencyMatrix.distance(g, r));
		assertEquals(4, graph.adjacencyMatrix.distance(g, a));
		assertEquals(4, graph.adjacencyMatrix.distance(g, b));
		assertEquals(4, graph.adjacencyMatrix.distance(g, c));
		assertEquals(5, graph.adjacencyMatrix.distance(g, d));
		assertEquals(5, graph.adjacencyMatrix.distance(g, e));
		assertEquals(5, graph.adjacencyMatrix.distance(g, f));
		assertEquals(0, graph.adjacencyMatrix.distance(g, g));
		assertEquals(6, graph.adjacencyMatrix.distance(g, h));
		assertEquals(1, graph.adjacencyMatrix.distance(g, i));
		assertEquals(1, graph.adjacencyMatrix.distance(g, j));
		assertEquals(2, graph.adjacencyMatrix.distance(g, k));
		assertEquals(6, graph.adjacencyMatrix.distance(g, l));

		assertEquals(2, graph.adjacencyMatrix.distance(h, r));
		assertEquals(3, graph.adjacencyMatrix.distance(h, a));
		assertEquals(3, graph.adjacencyMatrix.distance(h, b));
		assertEquals(3, graph.adjacencyMatrix.distance(h, c));
		assertEquals(4, graph.adjacencyMatrix.distance(h, d));
		assertEquals(1, graph.adjacencyMatrix.distance(h, e));
		assertEquals(4, graph.adjacencyMatrix.distance(h, f));
		assertEquals(4, graph.adjacencyMatrix.distance(h, g));
		assertEquals(0, graph.adjacencyMatrix.distance(h, h));
		assertEquals(2, graph.adjacencyMatrix.distance(h, i));
		assertEquals(5, graph.adjacencyMatrix.distance(h, j));
		assertEquals(1, graph.adjacencyMatrix.distance(h, k));
		assertEquals(5, graph.adjacencyMatrix.distance(h, l));

		assertEquals(2, graph.adjacencyMatrix.distance(i, r));
		assertEquals(3, graph.adjacencyMatrix.distance(i, a));
		assertEquals(3, graph.adjacencyMatrix.distance(i, b));
		assertEquals(3, graph.adjacencyMatrix.distance(i, c));
		assertEquals(4, graph.adjacencyMatrix.distance(i, d));
		assertEquals(4, graph.adjacencyMatrix.distance(i, e));
		assertEquals(4, graph.adjacencyMatrix.distance(i, f));
		assertEquals(4, graph.adjacencyMatrix.distance(i, g));
		assertEquals(5, graph.adjacencyMatrix.distance(i, h));
		assertEquals(0, graph.adjacencyMatrix.distance(i, i));
		assertEquals(5, graph.adjacencyMatrix.distance(i, j));
		assertEquals(1, graph.adjacencyMatrix.distance(i, k));
		assertEquals(5, graph.adjacencyMatrix.distance(i, l));

		assertEquals(3, graph.adjacencyMatrix.distance(j, r));
		assertEquals(4, graph.adjacencyMatrix.distance(j, a));
		assertEquals(4, graph.adjacencyMatrix.distance(j, b));
		assertEquals(4, graph.adjacencyMatrix.distance(j, c));
		assertEquals(5, graph.adjacencyMatrix.distance(j, d));
		assertEquals(5, graph.adjacencyMatrix.distance(j, e));
		assertEquals(5, graph.adjacencyMatrix.distance(j, f));
		assertEquals(5, graph.adjacencyMatrix.distance(j, g));
		assertEquals(6, graph.adjacencyMatrix.distance(j, h));
		assertEquals(1, graph.adjacencyMatrix.distance(j, i));
		assertEquals(0, graph.adjacencyMatrix.distance(j, j));
		assertEquals(2, graph.adjacencyMatrix.distance(j, k));
		assertEquals(6, graph.adjacencyMatrix.distance(j, l));

		assertEquals(1, graph.adjacencyMatrix.distance(k, r));
		assertEquals(2, graph.adjacencyMatrix.distance(k, a));
		assertEquals(2, graph.adjacencyMatrix.distance(k, b));
		assertEquals(2, graph.adjacencyMatrix.distance(k, c));
		assertEquals(3, graph.adjacencyMatrix.distance(k, d));
		assertEquals(3, graph.adjacencyMatrix.distance(k, e));
		assertEquals(3, graph.adjacencyMatrix.distance(k, f));
		assertEquals(3, graph.adjacencyMatrix.distance(k, g));
		assertEquals(4, graph.adjacencyMatrix.distance(k, h));
		assertEquals(1, graph.adjacencyMatrix.distance(k, i));
		assertEquals(4, graph.adjacencyMatrix.distance(k, j));
		assertEquals(0, graph.adjacencyMatrix.distance(k, k));
		assertEquals(4, graph.adjacencyMatrix.distance(k, l));

		assertEquals(3, graph.adjacencyMatrix.distance(l, r));
		assertEquals(4, graph.adjacencyMatrix.distance(l, a));
		assertEquals(4, graph.adjacencyMatrix.distance(l, b));
		assertEquals(4, graph.adjacencyMatrix.distance(l, c));
		assertEquals(5, graph.adjacencyMatrix.distance(l, d));
		assertEquals(2, graph.adjacencyMatrix.distance(l, e));
		assertEquals(5, graph.adjacencyMatrix.distance(l, f));
		assertEquals(5, graph.adjacencyMatrix.distance(l, g));
		assertEquals(1, graph.adjacencyMatrix.distance(l, h));
		assertEquals(3, graph.adjacencyMatrix.distance(l, i));
		assertEquals(6, graph.adjacencyMatrix.distance(l, j));
		assertEquals(2, graph.adjacencyMatrix.distance(l, k));
		assertEquals(0, graph.adjacencyMatrix.distance(l, l));
	}

	@Test
	public void testNodeRemoval() {
		Collection<TestNode> nodes = new HashSet<>();
		Collection<TestEdge> edges = new HashSet<>();
		Collection<TestNode> entries = new HashSet<>();
		Collection<TestNode> exits = new HashSet<>();
		AdjacencyMatrix<TestGraph, TestNode, TestEdge> matrix = new AdjacencyMatrix<>();
		Map<TestNode, Collection<TestNode>> adj = populate(matrix, nodes, edges, entries, exits);
		verify(adj, nodes, edges, matrix, entries, exits);

		Collection<TestNode> removed = new HashSet<>();
		for (int i = 0; i < nodes.size() / 4; i++) {
			TestNode n = random(nodes);
			removed.add(n);
			nodes.remove(n);
			edges.removeIf(e -> e.getSource() == n || e.getDestination() == n);
			adj.remove(n);
			adj.forEach(
				(
						nn,
						follows
				) -> follows.remove(n));
			matrix.removeNode(n);
		}

		entries = new HashSet<>(nodes);
		exits = new HashSet<>(nodes);
		for (TestEdge e : edges) {
			entries.remove(e.getDestination());
			exits.remove(e.getSource());
		}

		verify(adj, nodes, edges, matrix, entries, exits, "after removing " + removed.toString());
	}

	private static <T> T random(
			Collection<T> elements) {
		int idx = rand.nextInt(elements.size());
		for (T e : elements)
			if (--idx < 0)
				return e;
		throw new AssertionError("No more elements");
	}

	@Test
	public void testEdgeRemoval() {
		Collection<TestNode> nodes = new HashSet<>();
		Collection<TestEdge> edges = new HashSet<>();
		Collection<TestNode> entries = new HashSet<>();
		Collection<TestNode> exits = new HashSet<>();
		AdjacencyMatrix<TestGraph, TestNode, TestEdge> matrix = new AdjacencyMatrix<>();
		Map<TestNode, Collection<TestNode>> adj = populate(matrix, nodes, edges, entries, exits);
		verify(adj, nodes, edges, matrix, entries, exits);

		Collection<TestEdge> removed = new HashSet<>();
		for (int i = 0; i < edges.size() / 4; i++) {
			TestEdge e = random(edges);
			removed.add(e);
			edges.remove(e);
			adj.get(e.getSource()).remove(e.getDestination());
			matrix.removeEdge(e);
		}

		entries = new HashSet<>(nodes);
		exits = new HashSet<>(nodes);
		for (TestEdge e : edges) {
			entries.remove(e.getDestination());
			exits.remove(e.getSource());
		}

		verify(adj, nodes, edges, matrix, entries, exits, "after removing " + removed.toString());
	}

}
