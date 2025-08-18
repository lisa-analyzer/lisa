package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * An edge between two {@link CallGraphNode}s in a {@link BaseCallGraph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CallGraphEdge implements Edge<CallGraph, CallGraphNode, CallGraphEdge> {

	private final CallGraphNode source;

	private final CallGraphNode destination;

	/**
	 * Build the edge.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 */
	public CallGraphEdge(
			CallGraphNode source,
			CallGraphNode destination) {
		this.source = source;
		this.destination = destination;
	}

	@Override
	public CallGraphNode getSource() {
		return source;
	}

	@Override
	public CallGraphNode getDestination() {
		return destination;
	}

	@Override
	public <V> boolean accept(
			GraphVisitor<CallGraph, CallGraphNode, CallGraphEdge, V> visitor,
			V tool) {
		return visitor.visit(tool, source.getGraph(), this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
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
		CallGraphEdge other = (CallGraphEdge) obj;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return source + " --> " + destination;
	}

}
