package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.datastructures.graph.code.CodeEdge;
import java.util.Objects;

/**
 * An edge of a control flow graph, connecting two statements.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Edge implements CodeEdge<CFG, Statement, Edge> {

	/**
	 * The source node.
	 */
	private final Statement source;

	/**
	 * The destination node.
	 */
	private final Statement destination;

	/**
	 * Builds an "empty" edge, meaning that it does not have endpoints.
	 */
	protected Edge() {
		this.source = null;
		this.destination = null;
	}

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
	public abstract String toString();

	/**
	 * Traverses this edge, optionally modifying the given {@code sourceState}
	 * by applying semantic assumptions.
	 * 
	 * @param <A>         the concrete {@link AbstractState} instance
	 * @param <H>         the concrete {@link HeapDomain} instance
	 * @param <V>         the concrete {@link ValueDomain} instance
	 * @param <T>         the concrete {@link TypeDomain} instance
	 * @param sourceState the {@link AnalysisState} computed at the source of
	 *                        this edge
	 * 
	 * @return the {@link AnalysisState} after traversing this edge
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> traverse(
					AnalysisState<A, H, V, T> sourceState)
					throws SemanticException;

	@Override
	public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		return visitor.visit(tool, source.getCFG(), this);
	}

	@Override
	public int compareTo(Edge o) {
		int cmp;
		if ((cmp = source.compareTo(o.source)) != 0)
			return cmp;
		if ((cmp = destination.compareTo(o.destination)) != 0)
			return cmp;
		return getClass().getName().compareTo(o.getClass().getName());
	}
}
