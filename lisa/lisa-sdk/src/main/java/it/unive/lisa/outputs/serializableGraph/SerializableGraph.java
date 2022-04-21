package it.unive.lisa.outputs.serializableGraph;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import it.unive.lisa.outputs.DotGraph;
import it.unive.lisa.outputs.GraphmlGraph;

public class SerializableGraph {

	private final String name;

	private final String description;

	private final SortedSet<SerializableNode> nodes;

	private final SortedSet<SerializableEdge> edges;

	private final SortedSet<SerializableNodeDescription> descriptions;

	public SerializableGraph() {
		this(null, null, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
	}

	public SerializableGraph(
			String name,
			String description,
			SortedSet<SerializableNode> nodes,
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

	public String getDescription() {
		return description;
	}

	public SortedSet<SerializableNode> getNodes() {
		return nodes;
	}

	public SortedSet<SerializableEdge> getEdges() {
		return edges;
	}

	public SortedSet<SerializableNodeDescription> getDescriptions() {
		return descriptions;
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
		return "graph [name=" + name + ", description=" + description + "]";
	}

	public void dump(Writer writer) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, System.getProperty("lisa.json.indent") != null);
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

	public DotGraph toDot() {
		DotGraph graph = new DotGraph(name);

		Set<Integer> hasFollows = new HashSet<>();
		Set<Integer> hasPreds = new HashSet<>();
		Set<Integer> inners = new HashSet<>();
		Map<Integer, String> labels = new HashMap<>();

		edges.forEach(e -> {
			hasFollows.add(e.getSourceId());
			hasPreds.add(e.getDestId());
		});

		descriptions.forEach(d -> labels.put(d.getNodeId(), d.getDescription().toString()));
		nodes.forEach(n -> inners.addAll(n.getSubNodes()));

		for (SerializableNode n : nodes)
			if (!inners.contains(n.getId()))
				graph.addNode(n, !hasPreds.contains(n.getId()), !hasFollows.contains(n.getId()), labels.get(n.getId()));

		for (SerializableEdge e : edges)
			graph.addEdge(e);

		return graph;
	}

	public GraphmlGraph toGraphml() {
		GraphmlGraph graph = new GraphmlGraph(name);

		Set<Integer> hasFollows = new HashSet<>();
		Set<Integer> hasPreds = new HashSet<>();
		Set<Integer> rootnodes = new HashSet<>();
		Map<SerializableNode, SerializableNode> containers = new HashMap<>();
		Map<Integer, SerializableNode> nodemap = new HashMap<>();
		Map<Integer, String> labels = new HashMap<>();

		edges.forEach(e -> {
			hasFollows.add(e.getSourceId());
			hasPreds.add(e.getDestId());
		});

		descriptions.forEach(d -> labels.put(d.getNodeId(), d.getDescription().toString()));
		nodes.forEach(n -> nodemap.put(n.getId(), n));
		rootnodes.addAll(nodemap.keySet());
		nodes.forEach(n -> {
			n.getSubNodes().forEach(sub -> containers.put(nodemap.get(sub), n));
			rootnodes.removeAll(n.getSubNodes());
		});

		for (SerializableNode n : nodes)
			graph.addNode(n,
					!hasPreds.contains(n.getId()) && rootnodes.contains(n.getId()),
					!hasFollows.contains(n.getId()) && rootnodes.contains(n.getId()),
					labels.get(n.getId()));

		while (!containers.isEmpty()) {
			Set<Entry<SerializableNode, SerializableNode>> leaves = containers.entrySet().stream()
					.filter(entry -> !containers.containsValue(entry.getKey())).collect(Collectors.toSet());
			leaves.forEach(entry -> {
				graph.markSubNode(entry.getValue(), entry.getKey());
				containers.remove(entry.getKey());
			});
		}

		for (SerializableEdge e : edges)
			graph.addEdge(e);

		return graph;
	}

	public static SerializableGraph readGraph(Reader reader) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(reader, SerializableGraph.class);
	}

	public boolean sameStructure(SerializableGraph other) {
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
}
