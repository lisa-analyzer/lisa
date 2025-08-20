package it.unive.lisa.analysis.value;

import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A {@link DomainLattice} that can be modified by a substitution of identifiers
 * that is caused by a modification of the abstraction provided in the
 * {@link HeapDomain} of the analysis. A substitution is composed by a list of
 * {@link HeapReplacement} instances, that <b>must be applied in order</b>. See
 * <a href=
 * "https://www.sciencedirect.com/science/article/pii/S0304397516300299">this
 * paper</a> for more details.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete {@link LatticeWithReplacement} type
 */
public interface LatticeWithReplacement<L extends LatticeWithReplacement<L>>
		extends
		DomainLattice<L, L> {

	/**
	 * Applies a substitution of identifiers that is caused by a modification of
	 * the abstraction provided in the {@link HeapDomain} of the analysis. A
	 * substitution is composed by a list of {@link HeapReplacement} instances,
	 * that <b>must be applied in order</b>.
	 * 
	 * @param r  the replacement to apply
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return the lattice modified by the substitution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L applyReplacement(
			HeapReplacement r,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || r.getSources().isEmpty())
			return (L) this;

		if (r.getTargets().isEmpty())
			// if there are no targets, we just forget the identifiers
			return forgetIdentifiers(r.getSources(), pp);

		L lub = bottom();
		for (Identifier source : r.getSources()) {
			L partial = (L) this;
			for (Identifier target : r.getTargets())
				partial = partial.store(target, source);
			lub = lub.lub(partial);
		}

		return lub.forgetIdentifiers(r.getIdsToForget(), pp);
	}

	/**
	 * Stores the approximation of the value of {@code source} as the
	 * approximation of the value of {@code id} in the current state. The
	 * assignment originates from a substitution, and is thus a mean to replace
	 * the abstraction of a variable with the abstraction of another variable to
	 * materialize/summarize new memory locations.
	 * 
	 * @param target the identifier to which the value is assigned
	 * @param source the identifier whose value is assigned to {@code id}
	 * 
	 * @return the new state of the domain after the assignment
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L store(
			Identifier target,
			Identifier source)
			throws SemanticException;

}
