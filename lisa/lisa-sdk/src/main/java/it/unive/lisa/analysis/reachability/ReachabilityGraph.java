package it.unive.lisa.analysis.reachability;

import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.BaseGraph;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * The graph used by {@link Reachability} to compute the reachability of the
 * statements of a single analysis result. Its nodes are the statements of the
 * result, the nodes representing the first evaluation of loop guards, and a
 * synthetic entry and exit. Its edges are the ones that the executions might
 * follow.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
class ReachabilityGraph
		extends
		BaseGraph<ReachabilityGraph, ReachabilityGraph.Node, ReachabilityGraph.Edge> {

	private final Node entry;

	private final Node exit;

	/**
	 * Builds the graph, containing only the synthetic entry (that is also the
	 * only entrypoint of the graph) and exit.
	 */
	ReachabilityGraph() {
		entry = new Node(this, null, false);
		exit = new Node(this, null, false);
		addNode(entry, true);
		addNode(exit);
	}

	/**
	 * Yields the synthetic entry of this graph.
	 *
	 * @return the entry
	 */
	Node getEntry() {
		return entry;
	}

	/**
	 * Yields the synthetic exit of this graph.
	 *
	 * @return the exit
	 */
	Node getExit() {
		return exit;
	}

	/**
	 * Creates a new node for the given statement and adds it to this graph.
	 *
	 * @param st    the statement
	 * @param first whether the node represents the first evaluation of a loop
	 *                  guard
	 *
	 * @return the new node
	 */
	Node addNode(
			Statement st,
			boolean first) {
		Node node = new Node(this, st, first);
		addNode(node);
		return node;
	}

	/**
	 * Adds an edge from {@code source} to {@code destination}, if it is not
	 * already present.
	 *
	 * @param source      the source of the edge
	 * @param destination the destination of the edge
	 */
	void connect(
			Node source,
			Node destination) {
		if (!followersOf(source).contains(destination))
			addEdge(new Edge(source, destination));
	}

	/**
	 * A node of a {@link ReachabilityGraph}. Nodes are compared by identity.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	static class Node
			implements
			it.unive.lisa.util.datastructures.graph.Node<ReachabilityGraph, Node, Edge> {

		private final ReachabilityGraph graph;

		private final Statement statement;

		private final boolean first;

		private Node(
				ReachabilityGraph graph,
				Statement statement,
				boolean first) {
			this.graph = graph;
			this.statement = statement;
			this.first = first;
		}

		@Override
		public <V> boolean accept(
				GraphVisitor<ReachabilityGraph, Node, Edge, V> visitor,
				V tool) {
			return visitor.visit(tool, graph, this);
		}

		@Override
		public String toString() {
			if (this == graph.entry)
				return "<entry>";
			if (this == graph.exit)
				return "<exit>";
			return first ? "<first> " + statement : String.valueOf(statement);
		}
	}

	/**
	 * An edge of a {@link ReachabilityGraph}. Edges are compared by identity.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	static class Edge
			implements
			it.unive.lisa.util.datastructures.graph.Edge<ReachabilityGraph, Node, Edge> {

		private final Node source;

		private final Node destination;

		private Edge(
				Node source,
				Node destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		public Node getSource() {
			return source;
		}

		@Override
		public Node getDestination() {
			return destination;
		}

		@Override
		public <V> boolean accept(
				GraphVisitor<ReachabilityGraph, Node, Edge, V> visitor,
				V tool) {
			return visitor.visit(tool, source.graph, this);
		}

		@Override
		public String toString() {
			return source + " -> " + destination;
		}
	}
}
