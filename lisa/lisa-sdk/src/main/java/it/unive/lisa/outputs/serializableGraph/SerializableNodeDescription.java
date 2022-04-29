package it.unive.lisa.outputs.serializableGraph;

/**
 * A description for a {@link SerializableNode}, pairing it with a
 * {@link SerializableValue}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SerializableNodeDescription implements Comparable<SerializableNodeDescription> {

	private final int nodeId;

	private final SerializableValue description;

	/**
	 * Builds an empty (invalid) description.
	 */
	public SerializableNodeDescription() {
		this(-1, null);
	}

	/**
	 * Builds a description.
	 * 
	 * @param id          the id of the {@link SerializableNode} that this
	 *                        description refers to
	 * @param description the {@link SerializableValue} to be used as
	 *                        description
	 */
	public SerializableNodeDescription(int id, SerializableValue description) {
		this.nodeId = id;
		this.description = description;
	}

	/**
	 * Yields the id of the {@link SerializableNode} that this description
	 * refers to.
	 * 
	 * @return the id
	 */
	public int getNodeId() {
		return nodeId;
	}

	/**
	 * Yields the {@link SerializableValue} to be used as description.
	 * 
	 * @return the description
	 */
	public SerializableValue getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + nodeId;
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
		SerializableNodeDescription other = (SerializableNodeDescription) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (nodeId != other.nodeId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return nodeId + ":" + description;
	}

	@Override
	public int compareTo(SerializableNodeDescription o) {
		int cmp;
		if ((cmp = Integer.compare(nodeId, o.nodeId)) != 0)
			return cmp;
		return description.compareTo(o.description);
	}

}
