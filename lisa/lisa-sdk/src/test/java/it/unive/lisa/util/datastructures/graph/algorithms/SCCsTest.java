package it.unive.lisa.util.datastructures.graph.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.datastructures.graph.TestGraph;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestEdge;
import it.unive.lisa.util.datastructures.graph.TestGraph.TestNode;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SCCsTest {

	@Test
	public void aSingleNodeWithNoSelfLoopIsOnlyATrivialScc() {
		TestGraph graph = new TestGraph();
		TestNode a = new TestNode(1);
		graph.addNode(a, true);

		SCCs<TestGraph, TestNode, TestEdge> sccs = new SCCs<>();
		Collection<Collection<TestNode>> all = sccs.build(graph);
		assertEquals(Set.of(Set.of(a)), toSetOfSets(all));

		Collection<Collection<TestNode>> nonTrivial = sccs.buildNonTrivial(graph);
		assertTrue(nonTrivial.isEmpty(), "a single node without a self loop is a trivial scc");
	}

	@Test
	public void aSingleNodeWithASelfLoopIsANonTrivialScc() {
		TestGraph graph = new TestGraph();
		TestNode a = new TestNode(1);
		graph.addNode(a, true);
		graph.addEdge(new TestEdge(a, a));

		SCCs<TestGraph, TestNode, TestEdge> sccs = new SCCs<>();
		Collection<Collection<TestNode>> nonTrivial = sccs.buildNonTrivial(graph);
		assertEquals(Set.of(Set.of(a)), toSetOfSets(nonTrivial));
	}

	@Test
	public void aCycleFormsASingleScc() {
		TestGraph graph = new TestGraph();
		TestNode a = new TestNode(1);
		TestNode b = new TestNode(2);
		TestNode c = new TestNode(3);
		graph.addNode(a, true);
		graph.addNode(b);
		graph.addNode(c);
		graph.addEdge(new TestEdge(a, b));
		graph.addEdge(new TestEdge(b, c));
		graph.addEdge(new TestEdge(c, a));

		SCCs<TestGraph, TestNode, TestEdge> sccs = new SCCs<>();
		Collection<Collection<TestNode>> all = sccs.build(graph);
		assertEquals(Set.of(Set.of(a, b, c)), toSetOfSets(all));

		Collection<Collection<TestNode>> nonTrivial = sccs.buildNonTrivial(graph);
		assertEquals(Set.of(Set.of(a, b, c)), toSetOfSets(nonTrivial));
	}

	@Test
	public void aLinearGraphHasOnlyTrivialSccs() {
		TestGraph graph = new TestGraph();
		TestNode a = new TestNode(1);
		TestNode b = new TestNode(2);
		TestNode c = new TestNode(3);
		graph.addNode(a, true);
		graph.addNode(b);
		graph.addNode(c);
		graph.addEdge(new TestEdge(a, b));
		graph.addEdge(new TestEdge(b, c));

		SCCs<TestGraph, TestNode, TestEdge> sccs = new SCCs<>();
		Collection<Collection<TestNode>> all = sccs.build(graph);
		assertEquals(Set.of(Set.of(a), Set.of(b), Set.of(c)), toSetOfSets(all));

		assertTrue(sccs.buildNonTrivial(graph).isEmpty());
	}

	@Test
	public void disjointComponentsAreKeptSeparate() {
		TestGraph graph = new TestGraph();
		TestNode x = new TestNode(1);
		TestNode y = new TestNode(2);
		TestNode z = new TestNode(3);
		graph.addNode(x, true);
		graph.addNode(y);
		graph.addNode(z);
		graph.addEdge(new TestEdge(x, y));
		graph.addEdge(new TestEdge(y, x));

		SCCs<TestGraph, TestNode, TestEdge> sccs = new SCCs<>();
		Collection<Collection<TestNode>> nonTrivial = sccs.buildNonTrivial(graph);
		// z is disconnected and has no self loop, so it must not appear
		assertEquals(Set.of(Set.of(x, y)), toSetOfSets(nonTrivial));
	}

	@Test
	public void getSCCsReflectsTheLastBuild() {
		TestGraph graph = new TestGraph();
		TestNode a = new TestNode(1);
		graph.addNode(a, true);
		graph.addEdge(new TestEdge(a, a));

		SCCs<TestGraph, TestNode, TestEdge> sccs = new SCCs<>();
		Collection<Collection<TestNode>> built = sccs.build(graph);
		assertEquals(toSetOfSets(built), toSetOfSets(sccs.getSCCs()));
	}

	private Set<Set<TestNode>> toSetOfSets(
			Collection<Collection<TestNode>> sccs) {
		return sccs.stream().map(Set::copyOf).collect(java.util.stream.Collectors.toSet());
	}

}
