package it.unive.lisa.outputs.serializableGraph;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import it.unive.lisa.util.collections.CollectionUtilities;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * An edge of a {@link SerializableGraph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SerializableEdge implements Comparable<SerializableEdge> {

	private final int sourceId;

	private final int destId;

	private final String kind;

	// Capture all other fields that Jackson does not match during
	// deserialization
	private final Map<String, String> unknownFields;

	/**
	 * Builds an empty (invalid) edge.
	 */
	public SerializableEdge() {
		this(-1, -1, null);
	}

	/**
	 * Builds an edge.
	 * 
	 * @param sourceId the id of the source {@link SerializableNode}
	 * @param destId   the id of the destination {@link SerializableNode}
	 * @param kind     the kind of this edge
	 */
	public SerializableEdge(int sourceId, int destId, String kind) {
		this.sourceId = sourceId;
		this.destId = destId;
		this.kind = kind;
		unknownFields = new TreeMap<>();
	}

	/**
	 * Yields the id of the source {@link SerializableNode}.
	 * 
	 * @return the id
	 */
	public int getSourceId() {
		return sourceId;
	}

	/**
	 * Yields the id of the destination {@link SerializableNode}.
	 * 
	 * @return the id
	 */
	public int getDestId() {
		return destId;
	}

	/**
	 * Yields the kind of this edge.
	 * 
	 * @return the kind
	 */
	public String getKind() {
		return kind;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + destId;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + sourceId;
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
		SerializableEdge other = (SerializableEdge) obj;
		if (destId != other.destId)
			return false;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		if (sourceId != other.sourceId)
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
		return sourceId + "-" + kind + "->" + destId;
	}

	@Override
	public int compareTo(SerializableEdge o) {
		int cmp;
		if ((cmp = Integer.compare(sourceId, o.sourceId)) != 0)
			return cmp;
		if ((cmp = Integer.compare(destId, o.destId)) != 0)
			return cmp;
		if ((cmp = Integer.compare(unknownFields.keySet().size(), o.unknownFields.keySet().size())) != 0)
			return cmp;
		if ((cmp = CollectionUtilities.nullSafeCompare(true, kind, o.kind, String::compareTo)) != 0)
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
}
