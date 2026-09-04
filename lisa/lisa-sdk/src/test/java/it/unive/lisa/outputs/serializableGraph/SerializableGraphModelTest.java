package it.unive.lisa.outputs.serializableGraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class SerializableGraphModelTest {

	private static SerializableString str(
			String value) {
		return new SerializableString(new TreeMap<>(), value);
	}

	private static SerializableNode node(
			int id) {
		return new SerializableNode(id, Collections.emptyList(), "n" + id);
	}

	@Test
	public void emptyGraphHasNoNodesEdgesOrDescriptions() {
		SerializableGraph g = new SerializableGraph();
		assertTrue(g.getNodes().isEmpty());
		assertTrue(g.getEdges().isEmpty());
		assertTrue(g.getDescriptions().isEmpty());
	}

	@Test
	public void addNodeRejectsDuplicateIds() {
		SerializableGraph g = new SerializableGraph(
				"g", null, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
		g.addNode(node(1));
		assertThrows(IllegalArgumentException.class, () -> g.addNode(node(1)));
	}

	@Test
	public void addNodeDescriptionRejectsDuplicateNodeIds() {
		SerializableGraph g = new SerializableGraph(
				"g", null, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
		g.addNodeDescription(new SerializableNodeDescription(1, str("a")));
		assertThrows(
				IllegalArgumentException.class,
				() -> g.addNodeDescription(new SerializableNodeDescription(1, str("b"))));
	}

	@Test
	public void addEdgeDoesNotValidateThatEndpointsExist() {
		// documented behavior: edges are trusted as-is and are not checked
		// against the set of known node ids (e.g. useful when building a
		// graph incrementally, or deserializing one from JSON)
		SerializableGraph g = new SerializableGraph(
				"g", null, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
		g.addEdge(new SerializableEdge(1, 2, "kind", null));
		assertEquals(1, g.getEdges().size());
	}

	@Test
	public void getNodeByIdFindsAnExistingNodeAndThrowsForAMissingOne() {
		SerializableGraph g = new SerializableGraph(
				"g", null, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
		SerializableNode n = node(1);
		g.addNode(n);
		assertEquals(n, g.getNodeById(1));
		assertThrows(IllegalArgumentException.class, () -> g.getNodeById(2));
	}

	@Test
	public void sameStructureIgnoresDescriptionsButNotStructure() {
		TreeSet<SerializableNode> nodes = new TreeSet<>(List.of(node(1)));
		TreeSet<SerializableEdge> edges = new TreeSet<>();
		SerializableGraph a = new SerializableGraph(
				"g", null, nodes, edges,
				new TreeSet<>(List.of(new SerializableNodeDescription(1, str("a")))));
		SerializableGraph b = new SerializableGraph(
				"g", null, nodes, edges,
				new TreeSet<>(List.of(new SerializableNodeDescription(1, str("b")))));
		assertTrue(a.sameStructure(b));
		assertFalse(a.equals(b));

		SerializableGraph differentNodes = new SerializableGraph(
				"g", null, new TreeSet<>(List.of(node(1), node(2))), edges, new TreeSet<>());
		assertFalse(a.sameStructure(differentNodes));
	}

	@Test
	public void equalsAndHashCodeConsiderNameDescriptionNodesEdgesAndDescriptions() {
		TreeSet<SerializableNode> nodes = new TreeSet<>(List.of(node(1)));
		SerializableGraph a = new SerializableGraph("g", "d", nodes, new TreeSet<>(), new TreeSet<>());
		SerializableGraph b = new SerializableGraph("g", "d", nodes, new TreeSet<>(), new TreeSet<>());
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		SerializableGraph differentName = new SerializableGraph("other", "d", nodes, new TreeSet<>(), new TreeSet<>());
		assertFalse(a.equals(differentName));
	}

	@Test
	public void dumpAndReadGraphRoundTripPreservesEquality()
			throws IOException {
		TreeSet<SerializableNode> nodes = new TreeSet<>(List.of(node(1), node(2)));
		TreeSet<SerializableEdge> edges = new TreeSet<>(List.of(new SerializableEdge(1, 2, "seq", null)));
		TreeSet<SerializableNodeDescription> descs = new TreeSet<>(
				List.of(new SerializableNodeDescription(1, str("hello"))));
		SerializableGraph original = new SerializableGraph("g", "desc", nodes, edges, descs);

		StringWriter writer = new StringWriter();
		original.dump(writer);

		SerializableGraph roundTripped = SerializableGraph.readGraph(new StringReader(writer.toString()));
		assertEquals(original, roundTripped);
	}

}
