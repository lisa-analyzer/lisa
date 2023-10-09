package it.unive.lisa.outputs.serializableGraph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unive.lisa.outputs.DotGraph;
import it.unive.lisa.outputs.GraphmlGraph;
import it.unive.lisa.outputs.HtmlGraph;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A graph that can be serialized. This graph contains {@link SerializableNode}s
 * as nodes, identified through a numeric id. This graph is compound, meaning
 * that each node can contain subnodes, recursively. Nodes are linked by
 * {@link SerializableEdge}s, identifying their bounds through the numeric ids,
 * while also carrying a textual kind. Each node can be enriched with a
 * {@link SerializableNodeDescription}, providing some extra information on each
 * node.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SerializableGraph {

	private final String name;

	private final String description;

	private final SortedSet<SerializableNode> nodes;

	private final SortedSet<SerializableEdge> edges;

	private final SortedSet<SerializableNodeDescription> descriptions;

	/**
	 * Builds an empty graph.
	 */
	public SerializableGraph() {
		this(null, null, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
	}

	/**
	 * Builds a graph.
	 * 
	 * @param name         the name of the graph
	 * @param description  a description of the graph
	 * @param nodes        the set of nodes
	 * @param edges        the set of edges
	 * @param descriptions the descriptions of the nodes
	 */
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

	/**
	 * Yields the name of the graph.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Yields the description of the graph.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Yields the set of nodes of this graph.
	 * 
	 * @return the nodes
	 */
	public SortedSet<SerializableNode> getNodes() {
		return nodes;
	}

	/**
	 * Yields the set of edges of this graph.
	 * 
	 * @return the edges
	 */
	public SortedSet<SerializableEdge> getEdges() {
		return edges;
	}

	/**
	 * Yields the set of descriptions of the nodes of this graph.
	 * 
	 * @return the descriptions
	 */
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
	public boolean equals(
			Object obj) {
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

	/**
	 * Dumps this graph, in JSON format through the given {@link Writer}. If the
	 * system property {@code lisa.json.indent} is set to any value, the json
	 * will be formatted.
	 * 
	 * @param writer the writer to use for dumping the graph
	 * 
	 * @throws IOException if an I/O error occurs while writing
	 */
	public void dump(
			Writer writer)
			throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, System.getProperty("lisa.json.indent") != null);
		mapper.writeValue(writer, this);
	}

	/**
	 * Adds the given node to the graph.
	 * 
	 * @param node the node to add
	 * 
	 * @throws IllegalArgumentException if a node with the same id already
	 *                                      exists in the graph
	 */
	public void addNode(
			SerializableNode node) {
		if (nodes.stream().map(n -> n.getId()).anyMatch(i -> i == node.getId()))
			throw new IllegalArgumentException("A node with the same id " + node.getId() + " is already in the graph");
		nodes.add(node);
	}

	/**
	 * Adds the given node description to the graph.
	 * 
	 * @param desc the description to add
	 * 
	 * @throws IllegalArgumentException if a description for the same node
	 *                                      already exists in the graph
	 */
	public void addNodeDescription(
			SerializableNodeDescription desc) {
		if (descriptions.stream().map(d -> d.getNodeId()).anyMatch(i -> i == desc.getNodeId()))
			throw new IllegalArgumentException(
					"A description for node " + desc.getNodeId() + " is already in the graph");
		descriptions.add(desc);
	}

	/**
	 * Adds the given edge to the graph.
	 * 
	 * @param edge the edge to add
	 */
	public void addEdge(
			SerializableEdge edge) {
		edges.add(edge);
	}

	/**
	 * Converts this graph to a {@link DotGraph}.
	 * 
	 * @return the converted graph
	 */
	public DotGraph toDot() {
		DotGraph graph = new DotGraph(name);

		Set<Integer> hasFollows = new HashSet<>();
		Set<Integer> hasPreds = new HashSet<>();
		Set<Integer> inners = new HashSet<>();
		Map<Integer, SerializableValue> labels = new HashMap<>();

		edges.forEach(e -> {
			hasFollows.add(e.getSourceId());
			hasPreds.add(e.getDestId());
		});

		descriptions.forEach(d -> labels.put(d.getNodeId(), d.getDescription()));
		nodes.forEach(n -> inners.addAll(n.getSubNodes()));

		for (SerializableNode n : nodes)
			if (!inners.contains(n.getId()))
				graph.addNode(n, !hasPreds.contains(n.getId()), !hasFollows.contains(n.getId()), labels.get(n.getId()));

		for (SerializableEdge e : edges)
			graph.addEdge(e);

		return graph;
	}

	/**
	 * Converts this graph to a {@link GraphmlGraph}.
	 * 
	 * @param includeSubnodes whether or not sub-nodes should be part of the
	 *                            graph
	 * 
	 * @return the converted graph
	 */
	public GraphmlGraph toGraphml(
			boolean includeSubnodes) {
		GraphmlGraph graph = new GraphmlGraph(name);

		Set<Integer> hasFollows = new HashSet<>();
		Set<Integer> hasPreds = new HashSet<>();
		Set<Integer> rootnodes = new HashSet<>();
		Map<SerializableNode, SerializableNode> containers = new HashMap<>();
		Map<Integer, SerializableNode> nodemap = new HashMap<>();
		Map<Integer, SerializableValue> labels = new HashMap<>();

		edges.forEach(e -> {
			hasFollows.add(e.getSourceId());
			hasPreds.add(e.getDestId());
		});

		descriptions.forEach(d -> labels.put(d.getNodeId(), d.getDescription()));
		nodes.forEach(n -> nodemap.put(n.getId(), n));
		rootnodes.addAll(nodemap.keySet());
		nodes.forEach(n -> {
			n.getSubNodes().forEach(sub -> containers.put(nodemap.get(sub), n));
			rootnodes.removeAll(n.getSubNodes());
		});

		for (SerializableNode n : nodes)
			if (includeSubnodes || rootnodes.contains(n.getId()))
				graph.addNode(n,
						!hasPreds.contains(n.getId()) && rootnodes.contains(n.getId()),
						!hasFollows.contains(n.getId()) && rootnodes.contains(n.getId()),
						labels.get(n.getId()));

		if (includeSubnodes)
			while (!containers.isEmpty()) {
				Set<Entry<SerializableNode, SerializableNode>> leaves = containers.entrySet().stream()
						.filter(entry -> !containers.containsValue(entry.getKey())).collect(Collectors.toSet());
				leaves.forEach(entry -> {
					graph.markSubNode(entry.getValue(), entry.getKey());
					containers.remove(entry.getKey());
				});
			}

		for (SerializableEdge e : edges)
			if (includeSubnodes || (rootnodes.contains(e.getSourceId()) && rootnodes.contains(e.getDestId())))
				graph.addEdge(e);

		return graph;
	}

	/**
	 * Converts this graph to an {@link HtmlGraph}.
	 * 
	 * @param includeSubnodes  whether or not sub-nodes should be part of the
	 *                             graph
	 * @param descriptionLabel the display name of the descriptions, used as
	 *                             label in the collapse/expand toggles
	 * 
	 * @return the converted graph
	 */
	public HtmlGraph toHtml(
			boolean includeSubnodes,
			String descriptionLabel) {
		SerializableGraph g = new SerializableGraph(name, description, nodes, edges, Collections.emptySortedSet());
		GraphmlGraph graphml = g.toGraphml(includeSubnodes);

		SortedMap<Integer, Pair<String, SerializableNodeDescription>> map = new TreeMap<>();
		for (SerializableNodeDescription d : descriptions)
			for (SerializableNode n : nodes)
				if (d.getNodeId() == n.getId()) {
					map.put(n.getId(), Pair.of(n.getText(), d));
					break;
				}
		return new HtmlGraph(graphml, includeSubnodes, map, description, descriptionLabel);
	}

	/**
	 * Reads a graph through the given {@link Reader}, deserializing it as a
	 * JSON file.
	 * 
	 * @param reader the reader to use for reading the graph
	 * 
	 * @return the deserialized graph
	 * 
	 * @throws IOException if an I/O error occurs while reading
	 */
	public static SerializableGraph readGraph(
			Reader reader)
			throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(reader, SerializableGraph.class);
	}

	/**
	 * Checks if this graph and the given one share the same structure, that is,
	 * if they are equal up to the descriptions of their nodes. This is
	 * effectively the same as {@link #equals(Object)} but ignoring the
	 * descriptions.
	 * 
	 * @param other the other graph
	 * 
	 * @return {@code true} if the given graph has the same structure as this
	 *             one
	 */
	public boolean sameStructure(
			SerializableGraph other) {
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
