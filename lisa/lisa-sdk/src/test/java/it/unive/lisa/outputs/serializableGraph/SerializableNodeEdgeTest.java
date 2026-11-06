package it.unive.lisa.outputs.serializableGraph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SerializableNodeEdgeTest {

	@Test
	public void compareToOrdersPrimarilyById() {
		SerializableNode a = new SerializableNode(1, Collections.emptyList(), "z");
		SerializableNode b = new SerializableNode(2, Collections.emptyList(), "a");
		assertTrue(a.compareTo(b) < 0);
		assertTrue(b.compareTo(a) > 0);
	}

	@Test
	public void equalsUpToIdsMatchesStructurallyEqualNodesWithDifferentIds() {
		// left graph: parent(id=1) -> subnode(id=2, text "leaf")
		// right graph: parent(id=10) -> subnode(id=20, text "leaf")
		SerializableNode leftLeaf = new SerializableNode(2, Collections.emptyList(), "leaf");
		SerializableNode leftParent = new SerializableNode(1, List.of(2), "parent");
		SerializableNode rightLeaf = new SerializableNode(20, Collections.emptyList(), "leaf");
		SerializableNode rightParent = new SerializableNode(10, List.of(20), "parent");

		Set<SerializableNode> leftNodes = Set.of(leftLeaf, leftParent);
		Set<SerializableNode> rightNodes = Set.of(rightLeaf, rightParent);

		assertTrue(leftParent.equalsUpToIds(rightParent, leftNodes, rightNodes));
	}

	@Test
	public void equalsUpToIdsRejectsDifferentText() {
		SerializableNode a = new SerializableNode(1, Collections.emptyList(), "a");
		SerializableNode b = new SerializableNode(2, Collections.emptyList(), "b");
		assertFalse(a.equalsUpToIds(b, Set.of(a), Set.of(b)));
	}

	@Test
	public void equalsUpToIdsRejectsWhenASubNodeCannotBeResolved() {
		SerializableNode leftLeaf = new SerializableNode(2, Collections.emptyList(), "leaf");
		SerializableNode parent = new SerializableNode(1, List.of(2), "parent");
		SerializableNode otherParent = new SerializableNode(10, List.of(99), "parent");
		// the left side resolves subnode id 2 just fine, but otherParent
		// references subnode id 99, which does not exist in the right set
		assertFalse(
				parent.equalsUpToIds(otherParent, Set.of(parent, leftLeaf), Set.of(otherParent)));
	}

	@Test
	public void edgeCompareToOrdersBySourceThenDest() {
		SerializableEdge a = new SerializableEdge(1, 5, "k", null);
		SerializableEdge b = new SerializableEdge(1, 3, "k", null);
		SerializableEdge c = new SerializableEdge(2, 0, "k", null);
		assertTrue(a.compareTo(b) > 0);
		assertTrue(b.compareTo(c) < 0);
	}

	@Test
	public void edgeToStringOmitsTheLabelWhenAbsent() {
		SerializableEdge withoutLabel = new SerializableEdge(1, 2, "seq", null);
		SerializableEdge withLabel = new SerializableEdge(1, 2, "seq", "onTrue");
		assertTrue(withoutLabel.toString().equals("1-seq->2"));
		assertTrue(withLabel.toString().equals("1-seq-onTrue->2"));
	}

	@Test
	public void edgeEqualsUpToIdsMatchesEdgesBetweenStructurallyEqualEndpoints() {
		SerializableNode leftSrc = new SerializableNode(1, Collections.emptyList(), "src");
		SerializableNode leftDst = new SerializableNode(2, Collections.emptyList(), "dst");
		SerializableNode rightSrc = new SerializableNode(10, Collections.emptyList(), "src");
		SerializableNode rightDst = new SerializableNode(20, Collections.emptyList(), "dst");

		SerializableEdge left = new SerializableEdge(1, 2, "seq", null);
		SerializableEdge right = new SerializableEdge(10, 20, "seq", null);

		Set<SerializableNode> leftNodes = Set.of(leftSrc, leftDst);
		Set<SerializableNode> rightNodes = Set.of(rightSrc, rightDst);

		assertTrue(left.equalsUpToIds(right, leftNodes, rightNodes));
	}

	@Test
	public void edgeEqualsUpToIdsRejectsDifferentKind() {
		SerializableNode leftSrc = new SerializableNode(1, Collections.emptyList(), "src");
		SerializableNode leftDst = new SerializableNode(2, Collections.emptyList(), "dst");
		SerializableEdge left = new SerializableEdge(1, 2, "true", null);
		SerializableEdge right = new SerializableEdge(1, 2, "false", null);
		Set<SerializableNode> nodes = Set.of(leftSrc, leftDst);
		assertFalse(left.equalsUpToIds(right, nodes, nodes));
	}

}
