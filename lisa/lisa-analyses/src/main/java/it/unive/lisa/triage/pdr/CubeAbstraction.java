package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A strategy proposing, for a cube that has been blocked at a location, a more
 * general cube (that is, one representing more states) shaped by an
 * abstraction. The proposal is only a candidate: {@link PdrEngine} checks that
 * it is still blocked before using it, so implementations do not need to be
 * sound, but their proposals should contain the states of the cube that satisfy
 * the invariants of the location, for the generalization to make progress.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface CubeAbstraction {

	/**
	 * Proposes a generalization of the given cube, blocked at the given
	 * location.
	 *
	 * @param location the location
	 * @param cube     the cube
	 *
	 * @return the proposal, or {@code null} if there is none
	 *
	 * @throws SemanticException if something goes wrong while computing the
	 *                               proposal
	 */
	Cube abstractCube(
			Statement location,
			Cube cube)
			throws SemanticException;
}
