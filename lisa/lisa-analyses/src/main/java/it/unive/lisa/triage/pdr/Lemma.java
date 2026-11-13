package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.smt.Term;

/**
 * A lemma of property-directed reachability: a formula over the identifiers of
 * an {@link it.unive.lisa.triage.model.AlarmModel}, that does not depend on the
 * location it is attached to (the location is tracked by {@link Frames}).
 * Lemmas can be instantiated at any step of an {@link Unrolling}, so that the
 * same lemma can be used both on the pre-state and on the post-state of a
 * transition. Implementations must define {@link Object#equals(Object)} and
 * {@link Object#hashCode()}, since lemmas are stored in sets.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface Lemma {

	/**
	 * Yields the formula of this lemma, over the values of identifiers at the
	 * given step of the given unrolling.
	 *
	 * @param unrolling the unrolling
	 * @param step      the step
	 *
	 * @return the formula
	 *
	 * @throws SemanticException if the lemma cannot be encoded
	 */
	Term at(
			Unrolling unrolling,
			int step)
			throws SemanticException;
}
