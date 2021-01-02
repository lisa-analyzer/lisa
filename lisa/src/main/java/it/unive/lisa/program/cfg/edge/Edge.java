package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.datastructures.graph.SemanticEdge;
import java.util.Objects;

/**
 * An edge of a control flow graph, connecting two statements.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Edge implements SemanticEdge<Statement, Edge, CFG> {

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
		Objects.requireNonNull(source, "The source of an edge cannot be null");
		Objects.requireNonNull(destination, "The destination of an edge cannot be null");
		this.source = source;
		this.destination = destination;
	}

	@Override
	public final Statement getSource() {
		return source;
	}

	@Override
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

	@Override
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

	@Override
	public abstract <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> traverse(
					AnalysisState<A, H, V> sourceState) throws SemanticException;

	@Override
	public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		return visitor.visit(tool, source.getCFG(), this);
	}
}
