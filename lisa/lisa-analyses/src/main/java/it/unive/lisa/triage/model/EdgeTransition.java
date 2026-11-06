package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * The symbolic transition from a location of an {@link AlarmModel} to one of
 * its successors. When the transition corresponds to an edge of a cfg, this is
 * the semantics of the source node, followed by the traversal of the edge
 * connecting it to its successor (e.g., assuming the condition of a conditional
 * edge, or catching an error).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class EdgeTransition {

	private final ModelLocation source;

	private final ModelLocation destination;

	private final Edge edge;

	private final SymbolicStore store;

	private final Statement thrower;

	/**
	 * Builds the transition.
	 *
	 * @param source      the location where the transition starts
	 * @param destination the location where the transition ends
	 * @param edge        the traversed edge (can be {@code null} if the
	 *                        transition does not correspond to an edge of a
	 *                        cfg)
	 * @param store       the symbolic transition
	 */
	public EdgeTransition(
			ModelLocation source,
			ModelLocation destination,
			Edge edge,
			SymbolicStore store) {
		this(source, destination, edge, store, null);
	}

	/**
	 * Builds the transition.
	 *
	 * @param source      the location where the transition starts
	 * @param destination the location where the transition ends
	 * @param edge        the traversed edge (can be {@code null} if the
	 *                        transition does not correspond to an edge of a
	 *                        cfg)
	 * @param store       the symbolic transition
	 * @param thrower     the statement raising the error that the transition
	 *                        moves to, if it leads to a raised error (can be
	 *                        {@code null})
	 */
	public EdgeTransition(
			ModelLocation source,
			ModelLocation destination,
			Edge edge,
			SymbolicStore store,
			Statement thrower) {
		this.source = source;
		this.destination = destination;
		this.edge = edge;
		this.store = store;
		this.thrower = thrower;
	}

	/**
	 * Yields the location where the transition starts.
	 *
	 * @return the source location
	 */
	public ModelLocation getSource() {
		return source;
	}

	/**
	 * Yields the location where the transition ends.
	 *
	 * @return the destination location
	 */
	public ModelLocation getDestination() {
		return destination;
	}

	/**
	 * Yields the traversed edge, or {@code null} if the transition does not
	 * correspond to an edge of a cfg.
	 *
	 * @return the edge
	 */
	public Edge getEdge() {
		return edge;
	}

	/**
	 * Yields the symbolic transition.
	 *
	 * @return the transition
	 */
	public SymbolicStore getStore() {
		return store;
	}

	/**
	 * Yields the statement raising the error that this transition moves to, if
	 * it leads to a raised error and the statement is known.
	 *
	 * @return the statement, or {@code null}
	 */
	public Statement getThrower() {
		return thrower;
	}

	@Override
	public String toString() {
		return (edge != null ? edge.toString() : source + " -> " + destination) + ": " + store;
	}
}
