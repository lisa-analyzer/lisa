package it.unive.lisa.events;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.program.cfg.ProgramPoint;

/**
 * A marker interface for events that are related to the semantic evaluation of
 * statements and symbolic expressions during the analysis, that have a
 * pre-state and a post-state.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <I> the type of {@link Lattice} used as pre-state
 * @param <O> the type of {@link Lattice} used as post-state
 */
public interface EvaluationEvent<I extends Lattice<I>, O extends Lattice<O>> {

	/**
	 * Yields the state before the computation.
	 * 
	 * @return the state
	 */
	I getPreState();

	/**
	 * Yields the state after the computation.
	 * 
	 * @return the state
	 */
	O getPostState();

	/**
	 * Yields the program point where the evaluation took place.
	 * 
	 * @return the program point
	 */
	ProgramPoint getProgramPoint();
}
