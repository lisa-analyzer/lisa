package it.unive.lisa.outputs.serializableGraph;

import it.unive.lisa.util.collections.CollectionUtilities;

public class SerializableEdge implements Comparable<SerializableEdge> {

	private int sourceId;

	private int destId;

	private String kind;

	public SerializableEdge() {
	}

	public SerializableEdge(int sourceId, int destId, String kind) {
		this.sourceId = sourceId;
		this.destId = destId;
		this.kind = kind;
	}

	public int getSourceId() {
		return sourceId;
	}

	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}

	public int getDestId() {
		return destId;
	}

	public void setDestId(int destId) {
		this.destId = destId;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
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
		return "JsonEdge [sourceId=" + sourceId + ", destId=" + destId + ", kind=" + kind + "]";
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
