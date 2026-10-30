package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.edge.Edge;

/**
 * The symbolic transition from the beginning of a node of a cfg to the
 * beginning of one of its successors: the semantics of the node, followed by
 * the traversal of the edge connecting them (e.g., assuming the condition of a
 * conditional edge, or catching an error).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class EdgeTransition {

	private final Edge edge;

	private final SymbolicStore store;

	/**
	 * Builds the transition.
	 *
	 * @param edge  the traversed edge
	 * @param store the symbolic transition
	 */
	public EdgeTransition(
			Edge edge,
			SymbolicStore store) {
		this.edge = edge;
		this.store = store;
	}

	/**
	 * Yields the traversed edge.
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

	@Override
	public String toString() {
		return edge + ": " + store;
	}
}
