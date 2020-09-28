package it.unive.lisa.cfg;

import it.unive.lisa.cfg.expression.Statement;

/**
 * An edge of a control flow graph, connecting two statements.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Edge {

	/**
	 * The source node.
	 */
	private final Statement source;

	/**
	 * The destination node.
	 */
	private final Statement destination;

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 */
	protected Edge(Statement source, Statement destination) {
		this.source = source;
		this.destination = destination;
	}

	/**
	 * Yields the statement where this edge originates.
	 * 
	 * @return the source statement
	 */
	public final Statement getSource() {
		return source;
	}

	/**
	 * Yields the statement where this edge ends.
	 * 
	 * @return the destination statement
	 */
	public final Statement getDestination() {
		return destination;
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
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
		return "[ " + source + " ] -> [ " + destination + " ]";
	}
}
