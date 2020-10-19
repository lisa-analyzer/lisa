package it.unive.lisa.cfg.edge;

import it.unive.lisa.cfg.statement.Statement;

/**
 * A sequential edge connecting two statements. The abstract analysis state
 * gets modified by assuming that the statement where this edge originates does
 * not hold.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FalseEdge extends Edge {

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 */
	public FalseEdge(Statement source, Statement destination) {
		super(source, destination);
	}
	
	@Override
	public String toString() {
		return "[ " + getSource() + " ] -F-> [ " + getDestination() + " ]";
	}
}
