package it.unive.lisa.outputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class VisualGraphTest {

	@Test
	public void nodeNamePrefixesTheCodeWithNode() {
		assertEquals("node42", VisualGraph.nodeName(42));
	}

	@Test
	public void edgeNameEncodesSourceAndDestination() {
		assertEquals("edge-1-2", VisualGraph.edgeName(1, 2));
		assertNotEquals(VisualGraph.edgeName(1, 2), VisualGraph.edgeName(2, 1));
	}

}
