package it.unive.lisa.outputs.serializableGraph;

import it.unive.lisa.util.collections.CollectionUtilities;

/**
 * An edge of a {@link SerializableGraph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SerializableEdge implements Comparable<SerializableEdge> {

	private final int sourceId;

	private final int destId;

	private final String kind;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + destId;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + sourceId;
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
		return CollectionUtilities.nullSafeCompare(true, kind, o.kind, String::compareTo);
	}
}
