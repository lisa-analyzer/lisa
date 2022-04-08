package it.unive.lisa.outputs.serializableGraph;

import java.util.List;

public class SerializableNode implements Comparable<SerializableNode> {

	private int id;

	private List<Integer> subNodes;

	private String text;

	public SerializableNode() {
	}

	public SerializableNode(int id, List<Integer> subNodes, String text) {
		this.id = id;
		this.subNodes = subNodes;
		this.text = text;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Integer> getSubNodes() {
		return subNodes;
	}

	public void setSubNodes(List<Integer> subNodes) {
		this.subNodes = subNodes;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public int compareTo(SerializableNode o) {
		return Integer.compare(id, o.id);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((subNodes == null) ? 0 : subNodes.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
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
		return true;
	}

	@Override
	public String toString() {
		return "JsonNode [id=" + id + ", subNodes=" + subNodes + ", text=" + text + "]";
	}
}
