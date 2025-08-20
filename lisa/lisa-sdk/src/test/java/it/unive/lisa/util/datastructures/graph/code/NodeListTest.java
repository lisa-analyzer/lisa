package it.unive.lisa.util.datastructures.graph.code;

import static org.apache.commons.collections4.CollectionUtils.isEqualCollection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.code.TestCodeGraph.TestCodeEdge;
import it.unive.lisa.util.datastructures.graph.code.TestCodeGraph.TestCodeNode;
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

public class NodeListTest {

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
			Map<TestCodeNode, Collection<TestCodeNode>> adj,
			Collection<TestCodeNode> nodes,
			Collection<TestCodeEdge> edges,
			NodeList<TestCodeGraph, TestCodeNode, TestCodeEdge> matrix,
			Collection<TestCodeNode> entries,
			Collection<TestCodeNode> exits) {
		verify(adj, nodes, edges, matrix, entries, exits, "");
	}

	private void verify(
			Map<TestCodeNode, Collection<TestCodeNode>> adj,
			Collection<TestCodeNode> nodes,
			Collection<TestCodeEdge> edges,
			NodeList<TestCodeGraph, TestCodeNode, TestCodeEdge> matrix,
			Collection<TestCodeNode> entries,
			Collection<TestCodeNode> exits,
			String extra) {
		TestCodeNode externalNode = new TestCodeNode(-1);
		TestCodeEdge externalEdge = new TestCodeEdge(externalNode, externalNode);

		// we use isEqualCollection instead of equals since we are ok if the
		// concrete collection used is different, we just want the same elements
		assertTrue(msg("nodes", extra, nodes, matrix.getNodes()), isEqualCollection(nodes, matrix.getNodes()));
		assertTrue(msg("edges", extra, edges, matrix.getEdges()), isEqualCollection(edges, matrix.getEdges()));
		assertTrue(
				msg("entries", extra, entries, matrix.getEntries()),
				isEqualCollection(entries, matrix.getEntries()));
		assertTrue(msg("exits", extra, exits, matrix.getExits()), isEqualCollection(exits, matrix.getExits()));

		for (TestCodeNode node : nodes) {
			assertTrue("matrix does not contain " + node, matrix.containsNode(node));
			Collection<TestCodeEdge> ins = new HashSet<>();
			Collection<TestCodeEdge> outs = new HashSet<>();

			if (adj.containsKey(node))
				for (TestCodeNode dest : adj.get(node))
					outs.add(new TestCodeEdge(node, dest));
			for (Entry<TestCodeNode, Collection<TestCodeNode>> entry : adj.entrySet())
				if (entry.getValue().contains(node))
					ins.add(new TestCodeEdge(entry.getKey(), node));

			assertTrue(
					msg("ingoing edges", extra, ins, matrix.getIngoingEdges(node)),
					isEqualCollection(ins, matrix.getIngoingEdges(node)));
			assertTrue(
					msg("outgoing edges", extra, outs, matrix.getOutgoingEdges(node)),
					isEqualCollection(outs, matrix.getOutgoingEdges(node)));

			Set<TestCodeNode> follows = outs.stream().map(Edge::getDestination).collect(Collectors.toSet());
			Set<TestCodeNode> preds = ins.stream().map(Edge::getSource).collect(Collectors.toSet());
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
				matrix.addEdge(new TestCodeEdge(externalNode, node));
			} catch (UnsupportedOperationException e) {
				failed = true;
			}
			assertTrue("Adding edge with external endpoint succeded", failed);
			failed = false;
			try {
				matrix.addEdge(new TestCodeEdge(node, externalNode));
			} catch (UnsupportedOperationException e) {
				failed = true;
			}
			assertTrue("Adding edge with external endpoint succeded", failed);
		}

		for (TestCodeEdge edge : edges) {
			assertTrue("matrix does not contain " + edge, matrix.containsEdge(edge));
			// equals instead of same since sequential edges are encoded in the
			// list,
			// end are freshly created when queried
			assertEquals(
					edge + " is not connecting its endpoints",
					edge,
					matrix.getEdgeConnecting(edge.getSource(), edge.getDestination()));
		}

		assertEquals("matrix cloning failed", matrix, new NodeList<>(matrix));

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

	private Map<TestCodeNode, Collection<TestCodeNode>> populate(
			NodeList<TestCodeGraph, TestCodeNode, TestCodeEdge> matrix,
			Collection<TestCodeNode> nodes,
			Collection<TestCodeEdge> edges,
			Collection<TestCodeNode> entries,
			Collection<TestCodeNode> exits) {
		for (int i = 20 + rand.nextInt(100); i >= 0; i--) {
			TestCodeNode node = new TestCodeNode(i);
			nodes.add(node);
			matrix.addNode(node);
		}

		int bound = nodes.size();
		entries.addAll(nodes);
		exits.addAll(nodes);
		Map<TestCodeNode, Collection<TestCodeNode>> adj = new HashMap<>();
		List<TestCodeNode> tmp = new ArrayList<>(nodes);
		for (int i = 20 + rand.nextInt(100); i >= 0; i--) {
			TestCodeNode src = tmp.get(rand.nextInt(bound));
			TestCodeNode dest = tmp.get(rand.nextInt(bound));
			if (adj.computeIfAbsent(src, k -> new HashSet<>()).add(dest)) {
				TestCodeEdge e = new TestCodeEdge(src, dest);
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
		Collection<TestCodeNode> nodes = new HashSet<>();
		Collection<TestCodeEdge> edges = new HashSet<>();
		Collection<TestCodeNode> entries = new HashSet<>();
		Collection<TestCodeNode> exits = new HashSet<>();
		NodeList<TestCodeGraph, TestCodeNode, TestCodeEdge> matrix = new NodeList<>(new TestCodeEdge(null, null));
		Map<TestCodeNode, Collection<TestCodeNode>> adj = populate(matrix, nodes, edges, entries, exits);
		verify(adj, nodes, edges, matrix, entries, exits);
	}

	@Test
	public void testMerge() {
		Collection<TestCodeNode> nodes1 = new HashSet<>();
		Collection<TestCodeEdge> edges1 = new HashSet<>();
		Collection<TestCodeNode> entries1 = new HashSet<>();
		Collection<TestCodeNode> exits1 = new HashSet<>();
		NodeList<TestCodeGraph, TestCodeNode, TestCodeEdge> matrix1 = new NodeList<>(new TestCodeEdge(null, null));
		Map<TestCodeNode, Collection<TestCodeNode>> adj1 = populate(matrix1, nodes1, edges1, entries1, exits1);
		verify(adj1, nodes1, edges1, matrix1, entries1, exits1);

		Collection<TestCodeNode> nodes2 = new HashSet<>();
		Collection<TestCodeEdge> edges2 = new HashSet<>();
		Collection<TestCodeNode> entries2 = new HashSet<>();
		Collection<TestCodeNode> exits2 = new HashSet<>();
		NodeList<TestCodeGraph, TestCodeNode, TestCodeEdge> matrix2 = new NodeList<>(new TestCodeEdge(null, null));
		Map<TestCodeNode, Collection<TestCodeNode>> adj2 = populate(matrix2, nodes2, edges2, entries2, exits2);
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
		for (Entry<TestCodeNode, Collection<TestCodeNode>> entry : adj2.entrySet())
			adj1.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
		verify(adj1, nodes1, edges1, matrix1, entries1, exits1);
	}

	@Test
	public void testBaseDistances() {
		Collection<TestCodeNode> nodes = new HashSet<>();
		Collection<TestCodeEdge> edges = new HashSet<>();
		Collection<TestCodeNode> entries = new HashSet<>();
		Collection<TestCodeNode> exits = new HashSet<>();
		NodeList<TestCodeGraph, TestCodeNode, TestCodeEdge> matrix = new NodeList<>(new TestCodeEdge(null, null));
		Map<TestCodeNode, Collection<TestCodeNode>> adj = populate(matrix, nodes, edges, entries, exits);
		verify(adj, nodes, edges, matrix, entries, exits);

		for (Entry<TestCodeNode, Collection<TestCodeNode>> entry : adj.entrySet()) {
			TestCodeNode node = entry.getKey();
			// self-loops don't count: this is the min distance
			assertEquals(0, matrix.distance(node, node));
			for (TestCodeNode dest : entry.getValue())
				if (node != dest)
					assertEquals(1, matrix.distance(node, dest));
		}
	}

	@Test
	public void testCodeGraph1() {
		TestCodeGraph graph = new TestCodeGraph();
		TestCodeNode one = new TestCodeNode(1);
		TestCodeNode two = new TestCodeNode(2);
		TestCodeNode three = new TestCodeNode(3);
		TestCodeNode four = new TestCodeNode(4);
		TestCodeNode five = new TestCodeNode(5);
		TestCodeNode six = new TestCodeNode(6);
		graph.addNode(one, true);
		graph.addNode(two);
		graph.addNode(three);
		graph.addNode(four);
		graph.addNode(five);
		graph.addNode(six);
		graph.addEdge(new TestCodeEdge(one, two));
		graph.addEdge(new TestCodeEdge(two, three));
		graph.addEdge(new TestCodeEdge(two, four));
		graph.addEdge(new TestCodeEdge(two, six));
		graph.addEdge(new TestCodeEdge(three, five));
		graph.addEdge(new TestCodeEdge(four, five));
		graph.addEdge(new TestCodeEdge(five, two));

		assertEquals(0, graph.getNodeList().distance(one, one));
		assertEquals(1, graph.getNodeList().distance(one, two));
		assertEquals(2, graph.getNodeList().distance(one, three));
		assertEquals(2, graph.getNodeList().distance(one, four));
		assertEquals(3, graph.getNodeList().distance(one, five));
		assertEquals(2, graph.getNodeList().distance(one, six));
		assertEquals(-1, graph.getNodeList().distance(two, one));
		assertEquals(0, graph.getNodeList().distance(two, two));
		assertEquals(1, graph.getNodeList().distance(two, three));
		assertEquals(1, graph.getNodeList().distance(two, four));
		assertEquals(2, graph.getNodeList().distance(two, five));
		assertEquals(1, graph.getNodeList().distance(two, six));
		assertEquals(-1, graph.getNodeList().distance(three, one));
		assertEquals(2, graph.getNodeList().distance(three, two));
		assertEquals(0, graph.getNodeList().distance(three, three));
		assertEquals(3, graph.getNodeList().distance(three, four));
		assertEquals(1, graph.getNodeList().distance(three, five));
		assertEquals(3, graph.getNodeList().distance(three, six));
		assertEquals(-1, graph.getNodeList().distance(four, one));
		assertEquals(2, graph.getNodeList().distance(four, two));
		assertEquals(3, graph.getNodeList().distance(four, three));
		assertEquals(0, graph.getNodeList().distance(four, four));
		assertEquals(1, graph.getNodeList().distance(four, five));
		assertEquals(3, graph.getNodeList().distance(four, six));
		assertEquals(-1, graph.getNodeList().distance(five, one));
		assertEquals(1, graph.getNodeList().distance(five, two));
		assertEquals(2, graph.getNodeList().distance(five, three));
		assertEquals(2, graph.getNodeList().distance(five, four));
		assertEquals(0, graph.getNodeList().distance(five, five));
		assertEquals(2, graph.getNodeList().distance(five, six));
		assertEquals(-1, graph.getNodeList().distance(six, one));
		assertEquals(-1, graph.getNodeList().distance(six, two));
		assertEquals(-1, graph.getNodeList().distance(six, three));
		assertEquals(-1, graph.getNodeList().distance(six, four));
		assertEquals(-1, graph.getNodeList().distance(six, five));
		assertEquals(0, graph.getNodeList().distance(six, six));
	}

	@Test
	public void testCodeGraph2() {
		TestCodeGraph graph = new TestCodeGraph();
		TestCodeNode r = new TestCodeNode(0);
		TestCodeNode a = new TestCodeNode(1);
		TestCodeNode b = new TestCodeNode(2);
		TestCodeNode c = new TestCodeNode(3);
		TestCodeNode d = new TestCodeNode(4);
		TestCodeNode e = new TestCodeNode(5);
		TestCodeNode f = new TestCodeNode(6);
		TestCodeNode g = new TestCodeNode(7);
		TestCodeNode h = new TestCodeNode(8);
		TestCodeNode i = new TestCodeNode(9);
		TestCodeNode j = new TestCodeNode(10);
		TestCodeNode k = new TestCodeNode(11);
		TestCodeNode l = new TestCodeNode(12);
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
		graph.addEdge(new TestCodeEdge(r, a));
		graph.addEdge(new TestCodeEdge(r, b));
		graph.addEdge(new TestCodeEdge(r, c));
		graph.addEdge(new TestCodeEdge(a, d));
		graph.addEdge(new TestCodeEdge(b, a));
		graph.addEdge(new TestCodeEdge(b, d));
		graph.addEdge(new TestCodeEdge(b, e));
		graph.addEdge(new TestCodeEdge(d, l));
		graph.addEdge(new TestCodeEdge(e, h));
		graph.addEdge(new TestCodeEdge(l, h));
		graph.addEdge(new TestCodeEdge(h, e));
		graph.addEdge(new TestCodeEdge(h, k));
		graph.addEdge(new TestCodeEdge(c, f));
		graph.addEdge(new TestCodeEdge(c, g));
		graph.addEdge(new TestCodeEdge(f, i));
		graph.addEdge(new TestCodeEdge(g, i));
		graph.addEdge(new TestCodeEdge(g, j));
		graph.addEdge(new TestCodeEdge(j, i));
		graph.addEdge(new TestCodeEdge(i, k));
		graph.addEdge(new TestCodeEdge(k, i));
		graph.addEdge(new TestCodeEdge(k, r));

		assertEquals(0, graph.getNodeList().distance(r, r));
		assertEquals(1, graph.getNodeList().distance(r, a));
		assertEquals(1, graph.getNodeList().distance(r, b));
		assertEquals(1, graph.getNodeList().distance(r, c));
		assertEquals(2, graph.getNodeList().distance(r, d));
		assertEquals(2, graph.getNodeList().distance(r, e));
		assertEquals(2, graph.getNodeList().distance(r, f));
		assertEquals(2, graph.getNodeList().distance(r, g));
		assertEquals(3, graph.getNodeList().distance(r, h));
		assertEquals(3, graph.getNodeList().distance(r, i));
		assertEquals(3, graph.getNodeList().distance(r, j));
		assertEquals(4, graph.getNodeList().distance(r, k));
		assertEquals(3, graph.getNodeList().distance(r, l));

		assertEquals(5, graph.getNodeList().distance(a, r));
		assertEquals(0, graph.getNodeList().distance(a, a));
		assertEquals(6, graph.getNodeList().distance(a, b));
		assertEquals(6, graph.getNodeList().distance(a, c));
		assertEquals(1, graph.getNodeList().distance(a, d));
		assertEquals(4, graph.getNodeList().distance(a, e));
		assertEquals(7, graph.getNodeList().distance(a, f));
		assertEquals(7, graph.getNodeList().distance(a, g));
		assertEquals(3, graph.getNodeList().distance(a, h));
		assertEquals(5, graph.getNodeList().distance(a, i));
		assertEquals(8, graph.getNodeList().distance(a, j));
		assertEquals(4, graph.getNodeList().distance(a, k));
		assertEquals(2, graph.getNodeList().distance(a, l));

		assertEquals(4, graph.getNodeList().distance(b, r));
		assertEquals(1, graph.getNodeList().distance(b, a));
		assertEquals(0, graph.getNodeList().distance(b, b));
		assertEquals(5, graph.getNodeList().distance(b, c));
		assertEquals(1, graph.getNodeList().distance(b, d));
		assertEquals(1, graph.getNodeList().distance(b, e));
		assertEquals(6, graph.getNodeList().distance(b, f));
		assertEquals(6, graph.getNodeList().distance(b, g));
		assertEquals(2, graph.getNodeList().distance(b, h));
		assertEquals(4, graph.getNodeList().distance(b, i));
		assertEquals(7, graph.getNodeList().distance(b, j));
		assertEquals(3, graph.getNodeList().distance(b, k));
		assertEquals(2, graph.getNodeList().distance(b, l));

		assertEquals(4, graph.getNodeList().distance(c, r));
		assertEquals(5, graph.getNodeList().distance(c, a));
		assertEquals(5, graph.getNodeList().distance(c, b));
		assertEquals(0, graph.getNodeList().distance(c, c));
		assertEquals(6, graph.getNodeList().distance(c, d));
		assertEquals(6, graph.getNodeList().distance(c, e));
		assertEquals(1, graph.getNodeList().distance(c, f));
		assertEquals(1, graph.getNodeList().distance(c, g));
		assertEquals(7, graph.getNodeList().distance(c, h));
		assertEquals(2, graph.getNodeList().distance(c, i));
		assertEquals(2, graph.getNodeList().distance(c, j));
		assertEquals(3, graph.getNodeList().distance(c, k));
		assertEquals(7, graph.getNodeList().distance(c, l));

		assertEquals(4, graph.getNodeList().distance(d, r));
		assertEquals(5, graph.getNodeList().distance(d, a));
		assertEquals(5, graph.getNodeList().distance(d, b));
		assertEquals(5, graph.getNodeList().distance(d, c));
		assertEquals(0, graph.getNodeList().distance(d, d));
		assertEquals(3, graph.getNodeList().distance(d, e));
		assertEquals(6, graph.getNodeList().distance(d, f));
		assertEquals(6, graph.getNodeList().distance(d, g));
		assertEquals(2, graph.getNodeList().distance(d, h));
		assertEquals(4, graph.getNodeList().distance(d, i));
		assertEquals(7, graph.getNodeList().distance(d, j));
		assertEquals(3, graph.getNodeList().distance(d, k));
		assertEquals(1, graph.getNodeList().distance(d, l));

		assertEquals(3, graph.getNodeList().distance(e, r));
		assertEquals(4, graph.getNodeList().distance(e, a));
		assertEquals(4, graph.getNodeList().distance(e, b));
		assertEquals(4, graph.getNodeList().distance(e, c));
		assertEquals(5, graph.getNodeList().distance(e, d));
		assertEquals(0, graph.getNodeList().distance(e, e));
		assertEquals(5, graph.getNodeList().distance(e, f));
		assertEquals(5, graph.getNodeList().distance(e, g));
		assertEquals(1, graph.getNodeList().distance(e, h));
		assertEquals(3, graph.getNodeList().distance(e, i));
		assertEquals(6, graph.getNodeList().distance(e, j));
		assertEquals(2, graph.getNodeList().distance(e, k));
		assertEquals(6, graph.getNodeList().distance(e, l));

		assertEquals(3, graph.getNodeList().distance(f, r));
		assertEquals(4, graph.getNodeList().distance(f, a));
		assertEquals(4, graph.getNodeList().distance(f, b));
		assertEquals(4, graph.getNodeList().distance(f, c));
		assertEquals(5, graph.getNodeList().distance(f, d));
		assertEquals(5, graph.getNodeList().distance(f, e));
		assertEquals(0, graph.getNodeList().distance(f, f));
		assertEquals(5, graph.getNodeList().distance(f, g));
		assertEquals(6, graph.getNodeList().distance(f, h));
		assertEquals(1, graph.getNodeList().distance(f, i));
		assertEquals(6, graph.getNodeList().distance(f, j));
		assertEquals(2, graph.getNodeList().distance(f, k));
		assertEquals(6, graph.getNodeList().distance(f, l));

		assertEquals(3, graph.getNodeList().distance(g, r));
		assertEquals(4, graph.getNodeList().distance(g, a));
		assertEquals(4, graph.getNodeList().distance(g, b));
		assertEquals(4, graph.getNodeList().distance(g, c));
		assertEquals(5, graph.getNodeList().distance(g, d));
		assertEquals(5, graph.getNodeList().distance(g, e));
		assertEquals(5, graph.getNodeList().distance(g, f));
		assertEquals(0, graph.getNodeList().distance(g, g));
		assertEquals(6, graph.getNodeList().distance(g, h));
		assertEquals(1, graph.getNodeList().distance(g, i));
		assertEquals(1, graph.getNodeList().distance(g, j));
		assertEquals(2, graph.getNodeList().distance(g, k));
		assertEquals(6, graph.getNodeList().distance(g, l));

		assertEquals(2, graph.getNodeList().distance(h, r));
		assertEquals(3, graph.getNodeList().distance(h, a));
		assertEquals(3, graph.getNodeList().distance(h, b));
		assertEquals(3, graph.getNodeList().distance(h, c));
		assertEquals(4, graph.getNodeList().distance(h, d));
		assertEquals(1, graph.getNodeList().distance(h, e));
		assertEquals(4, graph.getNodeList().distance(h, f));
		assertEquals(4, graph.getNodeList().distance(h, g));
		assertEquals(0, graph.getNodeList().distance(h, h));
		assertEquals(2, graph.getNodeList().distance(h, i));
		assertEquals(5, graph.getNodeList().distance(h, j));
		assertEquals(1, graph.getNodeList().distance(h, k));
		assertEquals(5, graph.getNodeList().distance(h, l));

		assertEquals(2, graph.getNodeList().distance(i, r));
		assertEquals(3, graph.getNodeList().distance(i, a));
		assertEquals(3, graph.getNodeList().distance(i, b));
		assertEquals(3, graph.getNodeList().distance(i, c));
		assertEquals(4, graph.getNodeList().distance(i, d));
		assertEquals(4, graph.getNodeList().distance(i, e));
		assertEquals(4, graph.getNodeList().distance(i, f));
		assertEquals(4, graph.getNodeList().distance(i, g));
		assertEquals(5, graph.getNodeList().distance(i, h));
		assertEquals(0, graph.getNodeList().distance(i, i));
		assertEquals(5, graph.getNodeList().distance(i, j));
		assertEquals(1, graph.getNodeList().distance(i, k));
		assertEquals(5, graph.getNodeList().distance(i, l));

		assertEquals(3, graph.getNodeList().distance(j, r));
		assertEquals(4, graph.getNodeList().distance(j, a));
		assertEquals(4, graph.getNodeList().distance(j, b));
		assertEquals(4, graph.getNodeList().distance(j, c));
		assertEquals(5, graph.getNodeList().distance(j, d));
		assertEquals(5, graph.getNodeList().distance(j, e));
		assertEquals(5, graph.getNodeList().distance(j, f));
		assertEquals(5, graph.getNodeList().distance(j, g));
		assertEquals(6, graph.getNodeList().distance(j, h));
		assertEquals(1, graph.getNodeList().distance(j, i));
		assertEquals(0, graph.getNodeList().distance(j, j));
		assertEquals(2, graph.getNodeList().distance(j, k));
		assertEquals(6, graph.getNodeList().distance(j, l));

		assertEquals(1, graph.getNodeList().distance(k, r));
		assertEquals(2, graph.getNodeList().distance(k, a));
		assertEquals(2, graph.getNodeList().distance(k, b));
		assertEquals(2, graph.getNodeList().distance(k, c));
		assertEquals(3, graph.getNodeList().distance(k, d));
		assertEquals(3, graph.getNodeList().distance(k, e));
		assertEquals(3, graph.getNodeList().distance(k, f));
		assertEquals(3, graph.getNodeList().distance(k, g));
		assertEquals(4, graph.getNodeList().distance(k, h));
		assertEquals(1, graph.getNodeList().distance(k, i));
		assertEquals(4, graph.getNodeList().distance(k, j));
		assertEquals(0, graph.getNodeList().distance(k, k));
		assertEquals(4, graph.getNodeList().distance(k, l));

		assertEquals(3, graph.getNodeList().distance(l, r));
		assertEquals(4, graph.getNodeList().distance(l, a));
		assertEquals(4, graph.getNodeList().distance(l, b));
		assertEquals(4, graph.getNodeList().distance(l, c));
		assertEquals(5, graph.getNodeList().distance(l, d));
		assertEquals(2, graph.getNodeList().distance(l, e));
		assertEquals(5, graph.getNodeList().distance(l, f));
		assertEquals(5, graph.getNodeList().distance(l, g));
		assertEquals(1, graph.getNodeList().distance(l, h));
		assertEquals(3, graph.getNodeList().distance(l, i));
		assertEquals(6, graph.getNodeList().distance(l, j));
		assertEquals(2, graph.getNodeList().distance(l, k));
		assertEquals(0, graph.getNodeList().distance(l, l));
	}

	@Test
	public void testCodeNodeRemoval() {
		Collection<TestCodeNode> nodes = new HashSet<>();
		Collection<TestCodeEdge> edges = new HashSet<>();
		Collection<TestCodeNode> entries = new HashSet<>();
		Collection<TestCodeNode> exits = new HashSet<>();
		NodeList<TestCodeGraph, TestCodeNode, TestCodeEdge> matrix = new NodeList<>(new TestCodeEdge(null, null));
		Map<TestCodeNode, Collection<TestCodeNode>> adj = populate(matrix, nodes, edges, entries, exits);
		verify(adj, nodes, edges, matrix, entries, exits);

		Collection<TestCodeNode> removed = new HashSet<>();
		int limit = nodes.size() / 4;
		for (int i = 0; i < limit; i++) {
			TestCodeNode n = random(nodes);
			removed.add(n);
			nodes.remove(n);
			entries.remove(n);
			exits.remove(n);
			edges.removeIf(e -> e.getSource() == n || e.getDestination() == n);
			adj.remove(n);
			adj.forEach(
					(
							nn,
							follows) -> follows.remove(n));
			matrix.removeNode(n);
		}

		entries = new HashSet<>(nodes);
		exits = new HashSet<>(nodes);
		for (TestCodeEdge e : edges) {
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
	public void testCodeEdgeRemoval() {
		Collection<TestCodeNode> nodes = new HashSet<>();
		Collection<TestCodeEdge> edges = new HashSet<>();
		Collection<TestCodeNode> entries = new HashSet<>();
		Collection<TestCodeNode> exits = new HashSet<>();
		NodeList<TestCodeGraph, TestCodeNode, TestCodeEdge> matrix = new NodeList<>(new TestCodeEdge(null, null));
		Map<TestCodeNode, Collection<TestCodeNode>> adj = populate(matrix, nodes, edges, entries, exits);
		verify(adj, nodes, edges, matrix, entries, exits);

		Collection<TestCodeEdge> removed = new HashSet<>();
		int limit = edges.size() / 4;
		for (int i = 0; i < limit; i++) {
			TestCodeEdge e = random(edges);
			removed.add(e);
			edges.remove(e);
			adj.get(e.getSource()).remove(e.getDestination());
			matrix.removeEdge(e);
		}

		entries = new HashSet<>(nodes);
		exits = new HashSet<>(nodes);
		for (TestCodeEdge e : edges) {
			entries.remove(e.getDestination());
			exits.remove(e.getSource());
		}

		verify(adj, nodes, edges, matrix, entries, exits, "after removing " + removed.toString());
	}

}
