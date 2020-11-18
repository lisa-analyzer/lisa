package it.unive.lisa.cfg.edge;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.cfg.statement.Statement;

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
		result = prime * result + getClass().getName().hashCode();
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

	/**
	 * Checks if this edge is effectively equal to the given one, that is, if they
	 * have the same structure while potentially being different instances. This
	 * translates into comparing source and destination statements with
	 * {@link Statement#isEqualTo(Statement)} instead of using
	 * {@link Statement#equals(Object)}
	 * 
	 * @param other the other edge
	 * @return {@code true} if this edge and the given one are effectively equals
	 */
	public boolean isEqualTo(Edge other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (getClass() != other.getClass())
			return false;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.isEqualTo(other.destination))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.isEqualTo(other.source))
			return false;
		return true;
	}

	@Override
	public abstract String toString();

	/**
	 * Traverses this edge, optionally modifying the given {@code sourceState} by
	 * applying semantic assumptions.
	 * 
	 * @param <H>         the concrete {@link HeapDomain} instance
	 * @param <V>         the concrete {@link ValueDomain} instance
	 * @param sourceState the {@link AnalysisState} computed at the source of this
	 *                    edge
	 * @return the {@link AnalysisState} after traversing this edge
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> traverse(
			AnalysisState<H, V> sourceState) throws SemanticException;
}
