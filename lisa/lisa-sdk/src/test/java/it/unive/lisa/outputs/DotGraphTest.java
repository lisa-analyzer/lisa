package it.unive.lisa.outputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.outputs.serializableGraph.SerializableEdge;
import it.unive.lisa.outputs.serializableGraph.SerializableNode;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class DotGraphTest {

	@Test
	public void getTitleReturnsTheConstructorArgument() {
		assertEquals("mygraph", new DotGraph("mygraph").getTitle());
	}

	@Test
	public void dumpProducesValidDotSyntaxWithNodesAndTitle() throws IOException {
		DotGraph g = new DotGraph("mygraph");
		g.addNode(new SerializableNode(1, Collections.emptyList(), "n1"), false, false, null);

		StringWriter writer = new StringWriter();
		g.dump(writer);
		String dot = writer.toString();

		assertTrue(dot.contains("digraph"), "dot output should declare a digraph");
		assertTrue(dot.contains("node1"), "the node's generated name should appear in the dump");
		assertTrue(dot.contains("mygraph"), "the graph title should appear in the (non-stripped) dump");
	}

	@Test
	public void dumpStrippedOmitsTheLegendAndTitleLabel() throws IOException {
		DotGraph g = new DotGraph("mygraph");
		g.addNode(new SerializableNode(1, Collections.emptyList(), "n1"), false, false, null);

		StringWriter full = new StringWriter();
		g.dump(full);
		StringWriter stripped = new StringWriter();
		g.dumpStripped(stripped);

		// the legend cluster is only added to the non-stripped dump
		assertTrue(full.toString().contains("legend"));
		assertFalse(stripped.toString().contains("legend"));
	}

	@Test
	public void addEdgeConnectsTheGeneratedNodeNames() throws IOException {
		DotGraph g = new DotGraph("mygraph");
		g.addNode(new SerializableNode(1, Collections.emptyList(), "n1"), false, false, null);
		g.addNode(new SerializableNode(2, Collections.emptyList(), "n2"), false, false, null);
		g.addEdge(new SerializableEdge(1, 2, "SequentialEdge", null));

		StringWriter writer = new StringWriter();
		g.dumpStripped(writer);
		String dot = writer.toString();
		assertTrue(dot.contains("node1"));
		assertTrue(dot.contains("node2"));
		assertTrue(dot.contains("->"), "a directed edge arrow should appear between the two nodes");
	}

	@Test
	public void errorEdgeCarriesItsLabelInTheDump() throws IOException {
		DotGraph g = new DotGraph("mygraph");
		g.addNode(new SerializableNode(1, Collections.emptyList(), "n1"), false, false, null);
		g.addNode(new SerializableNode(2, Collections.emptyList(), "n2"), false, false, null);
		g.addEdge(new SerializableEdge(1, 2, "ErrorEdge", "MyException"));

		StringWriter writer = new StringWriter();
		g.dumpStripped(writer);
		assertTrue(writer.toString().contains("MyException"), "the error edge's label should appear in the dump");
	}

}
