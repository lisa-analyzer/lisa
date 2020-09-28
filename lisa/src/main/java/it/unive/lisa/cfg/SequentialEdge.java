package it.unive.lisa.cfg;

import it.unive.lisa.cfg.expression.Statement;

/**
 * A sequential edge connecting two statement. The abstract analysis state
 * does not get modified when traversing this edge.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SequentialEdge extends Edge {

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 */
	public SequentialEdge(Statement source, Statement destination) {
		super(source, destination);
	}
}
