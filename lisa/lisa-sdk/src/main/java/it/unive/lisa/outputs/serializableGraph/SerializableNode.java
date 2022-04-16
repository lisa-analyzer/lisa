package it.unive.lisa.outputs.serializableGraph;

import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.Collections;
import java.util.List;

public class SerializableNode implements Comparable<SerializableNode> {

	private final int id;

	private final List<Integer> subNodes;

	private final String text;

	public SerializableNode() {
		this(-1, Collections.emptyList(), null);
	}

	public SerializableNode(int id, List<Integer> subNodes, String text) {
		this.id = id;
		this.subNodes = subNodes;
		this.text = text;
	}

	public int getId() {
		return id;
	}

	public List<Integer> getSubNodes() {
		return subNodes;
	}

	public String getText() {
		return text;
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
		return 0;
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
		return id + "(" + subNodes + "):" + text;
	}
}
