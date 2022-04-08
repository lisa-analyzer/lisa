package it.unive.lisa.outputs.serializableGraph;

import java.io.IOException;
import java.io.Writer;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SerializableGraph {

	private String name;

	private String description;

	private SortedSet<SerializableNode> nodes = new TreeSet<>();

	private SortedSet<SerializableEdge> edges = new TreeSet<>();

	private SortedSet<SerializableNodeDescription> descriptions = new TreeSet<>();

	public SerializableGraph() {
	}

	public SerializableGraph(String name, String description, SortedSet<SerializableNode> nodes,
			SortedSet<SerializableEdge> edges,
			SortedSet<SerializableNodeDescription> descriptions) {
		this.name = name;
		this.description = description;
		this.nodes = nodes;
		this.edges = edges;
		this.descriptions = descriptions;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SortedSet<SerializableNode> getNodes() {
		return nodes;
	}

	public void setNodes(SortedSet<SerializableNode> nodes) {
		this.nodes = nodes;
	}

	public SortedSet<SerializableEdge> getEdges() {
		return edges;
	}

	public void setEdges(SortedSet<SerializableEdge> edges) {
		this.edges = edges;
	}

	public SortedSet<SerializableNodeDescription> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(SortedSet<SerializableNodeDescription> descriptions) {
		this.descriptions = descriptions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((descriptions == null) ? 0 : descriptions.hashCode());
		result = prime * result + ((edges == null) ? 0 : edges.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
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
		SerializableGraph other = (SerializableGraph) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (descriptions == null) {
			if (other.descriptions != null)
				return false;
		} else if (!descriptions.equals(other.descriptions))
			return false;
		if (edges == null) {
			if (other.edges != null)
				return false;
		} else if (!edges.equals(other.edges))
			return false;
		if (nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (!nodes.equals(other.nodes))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JsonGraph [name=" + name + ", description=" + description + "]";
	}

	public void dump(Writer writer) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		mapper.writeValue(writer, this);
	}

	public void addNode(SerializableNode node) {
		nodes.add(node);
	}

	public void addNodeDescription(SerializableNodeDescription desc) {
		descriptions.add(desc);
	}

	public void addEdge(SerializableEdge edge) {
		edges.add(edge);
	}
}
