package it.unive.lisa.outputs.serializableGraph;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.unive.lisa.util.collections.CollectionUtilities;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A node of a {@link SerializableGraph}, represented by a numeric id.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SerializableNode implements Comparable<SerializableNode> {

	private final int id;

	private final List<Integer> subNodes;

	private final String text;

	// Capture all other fields that Jackson does not match during
	// deserialization
	private final Map<String, String> unknownFields;

	/**
	 * Builds an empty (invalid) node.
	 */
	public SerializableNode() {
		this(-1, Collections.emptyList(), null);
	}

	/**
	 * Builds a node.
	 * 
	 * @param id       the id of the node
	 * @param subNodes the id of the nodes that are subnodes of this one, in the
	 *                     order they appear
	 * @param text     the text of this node
	 */
	public SerializableNode(int id, List<Integer> subNodes, String text) {
		this.id = id;
		this.subNodes = subNodes;
		this.text = text;
		unknownFields = new TreeMap<>();
	}

	/**
	 * Yields the id of this node.
	 * 
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Yields the id of the nodes that are subnodes of this one, in the order
	 * they appear. Note that if node {@code a} contains node {@code b}, and
	 * {@code b} contains node {@code c}, then {@code a.getSubNodes()} returns
	 * only {@code b}, that is, the list is not recursive.
	 * 
	 * @return the ids of the subnodes
	 */
	@JsonInclude(value = Include.NON_EMPTY)
	public List<Integer> getSubNodes() {
		return subNodes;
	}

	/**
	 * Yields the text of this node.
	 * 
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Yields all fields that were unrecognized during deserialization.
	 * 
	 * @return the other fields
	 */
	@JsonAnyGetter
	public Map<String, String> otherFields() {
		return unknownFields;
	}

	/**
	 * Adds a field that was not recognized during deserialization.
	 * 
	 * @param name  he name of the field
	 * @param value the value of the field
	 */
	@JsonAnySetter
	public void setOtherField(String name, String value) {
		unknownFields.put(name, value);
	}

	@Override
	public int compareTo(SerializableNode o) {
		int cmp;
		if ((cmp = Integer.compare(id, o.id)) != 0)
			return cmp;
		if ((cmp = CollectionUtilities.nullSafeCompare(true, text, o.text, String::compareTo)) != 0)
			return cmp;
		if ((cmp = subNodes.size() - o.subNodes.size()) != 0)
			return cmp;
		for (int i = 0; i < subNodes.size(); i++)
			if ((cmp = subNodes.get(i) - o.subNodes.get(i)) != 0)
				return cmp;

		if ((cmp = Integer.compare(unknownFields.keySet().size(), o.unknownFields.keySet().size())) != 0)
			return cmp;

		CollectionsDiffBuilder<
				String> builder = new CollectionsDiffBuilder<>(String.class, unknownFields.keySet(),
						o.unknownFields.keySet());
		builder.compute(String::compareTo);

		if (!builder.sameContent())
			// same size means that both have at least one element that is
			// different
			return builder.getOnlyFirst().iterator().next().compareTo(builder.getOnlySecond().iterator().next());

		// same keys: just iterate over them and apply comparisons
		// since unknownFields is sorted, the order of iteration will be
		// consistent
		for (Entry<String, String> entry : unknownFields.entrySet())
			if ((cmp = entry.getValue().compareTo(o.unknownFields.get(entry.getKey()))) != 0)
				return cmp;

		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((subNodes == null) ? 0 : subNodes.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + ((unknownFields == null) ? 0 : unknownFields.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SerializableNode other = (SerializableNode) obj;
		if (id != other.id)
			return false;
		if (subNodes == null) {
			if (other.subNodes != null)
				return false;
		} else if (!subNodes.equals(other.subNodes))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (unknownFields == null) {
			if (other.unknownFields != null)
				return false;
		} else if (!unknownFields.equals(other.unknownFields))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return id + "(" + subNodes + "):" + text;
	}
}
