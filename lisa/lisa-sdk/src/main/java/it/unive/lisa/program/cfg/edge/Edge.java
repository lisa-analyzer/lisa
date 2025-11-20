package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
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
public abstract class Edge
		implements
		CodeEdge<CFG, Statement, Edge> {

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
	protected Edge(
			Statement source,
			Statement destination) {
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
	public boolean equals(
			Object obj) {
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
	 * Traverses this edge in the forward direction, proceeding from source to
	 * destination, optionally modifying the given {@code sourceState} by
	 * applying semantic assumptions.
	 * 
	 * @param <A>      the kind of {@link AbstractLattice} produced by the
	 *                     domain {@code D}
	 * @param <D>      the kind of {@link AbstractDomain} to run during the
	 *                     analysis
	 * @param state    the {@link AnalysisState} computed at the source of this
	 *                     edge
	 * @param analysis the {@link Analysis} that is being executed
	 * 
	 * @return the {@link AnalysisState} after traversing this edge
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> traverseForward(
			AnalysisState<A> state,
			Analysis<A, D> analysis)
			throws SemanticException;

	/**
	 * Traverses this edge in the backward direction, from destination to
	 * source, optionally modifying the given {@code sourceState} by applying
	 * semantic assumptions.
	 * 
	 * @param <A>      the kind of {@link AbstractLattice} produced by the
	 *                     domain {@code D}
	 * @param <D>      the kind of {@link AbstractDomain} to run during the
	 *                     analysis
	 * @param state    the {@link AnalysisState} computed at the destination of
	 *                     this edge
	 * @param analysis the {@link Analysis} that is being executed
	 * 
	 * @return the {@link AnalysisState} after traversing this edge
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> traverseBackwards(
			AnalysisState<A> state,
			Analysis<A, D> analysis)
			throws SemanticException;

	@Override
	public <V> boolean accept(
			GraphVisitor<CFG, Statement, Edge, V> visitor,
			V tool) {
		return visitor.visit(tool, source.getCFG(), this);
	}

	@Override
	public int compareTo(
			Edge o) {
		int cmp;
		if ((cmp = source.compareTo(o.source)) != 0)
			return cmp;
		if ((cmp = destination.compareTo(o.destination)) != 0)
			return cmp;
		return getClass().getName().compareTo(o.getClass().getName());
	}

	/**
	 * Yields the label of this edge, if any. The label is used mainly as a tool
	 * for discriminating edges with the same kind and endpoints but with
	 * different properties, e.g., different exceptions caught.
	 * 
	 * @return the label of this edge, or {@code null} if this edge does not
	 *             have a label
	 */
	public String getLabel() {
		return null;
	}

}
