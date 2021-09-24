package it.unive.lisa.util.datastructures.graph.algorithms;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.util.datastructures.graph.TestGraph;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestEdge;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class DominatorsTest {

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
		graph.addEdge(new TestEdge(three, five));
		graph.addEdge(new TestEdge(four, five));
		graph.addEdge(new TestEdge(five, two));
		graph.addEdge(new TestEdge(two, six));

		Map<TestNode, Set<TestNode>> res = new Dominators<TestGraph, TestNode, TestEdge>().build(graph);
		assertEquals("Fixpoint returned wrong result",
				Map.of(one, Set.of(one),
						two, Set.of(one, two),
						three, Set.of(one, two, three),
						four, Set.of(one, two, four),
						five, Set.of(one, two, five),
						six, Set.of(one, two, six)),
				res);
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
		graph.addEdge(new TestEdge(b, e));

		Map<TestNode, Set<TestNode>> res = new Dominators<TestGraph, TestNode, TestEdge>().build(graph);
		Map<TestNode, Set<TestNode>> exp = new HashMap<>();
		exp.put(r, Set.of(r));
		exp.put(a, Set.of(r, a));
		exp.put(b, Set.of(r, b));
		exp.put(c, Set.of(r, c));
		exp.put(d, Set.of(r, d));
		exp.put(e, Set.of(r, e));
		exp.put(f, Set.of(r, c, f));
		exp.put(g, Set.of(r, c, g));
		exp.put(h, Set.of(r, h));
		exp.put(i, Set.of(r, i));
		exp.put(j, Set.of(r, c, g, j));
		exp.put(k, Set.of(r, k));
		exp.put(l, Set.of(r, d, l));
		assertEquals("Fixpoint returned wrong result", exp, res);
	}

}
