package it.unive.lisa.analysis.value;

import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.SemanticComponent;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A {@link SemanticComponent} that can be modified by a substitution of
 * identifiers that is caused by a modification of the abstraction provided in
 * the {@link HeapDomain} of the analysis. A substitution is composed by a list
 * of {@link HeapReplacement} instances, that <b>must be applied in order</b>.
 * See <a href=
 * "https://www.sciencedirect.com/science/article/pii/S0304397516300299">this
 * paper</a> for more details.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete {@link DomainLattice} instance that this domain
 *                produces
 * @param <E> the type of {@link SymbolicExpression} that this domain can
 *                process
 */
public interface DomainWithReplacement<L extends DomainLattice<L, L>,
		E extends SymbolicExpression>
		extends
		SemanticComponent<L, L, E, Identifier> {

	/**
	 * Applies a substitution of identifiers that is caused by a modification of
	 * the abstraction provided in the {@link HeapDomain} of the analysis. A
	 * substitution is composed by a list of {@link HeapReplacement} instances,
	 * that <b>must be applied in order</b>.
	 * 
	 * @param state  the current state, that will be modified by this operation
	 * @param r      the replacement to apply
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the state modified by the substitution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L applyReplacement(
			L state,
			HeapReplacement r,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop() || state.isBottom() || r.getSources().isEmpty())
			return state;

		if (r.getTargets().isEmpty())
			// if there are no targets, we just forget the identifiers
			return state.forgetIdentifiers(r.getSources(), pp);

		L lub = state.bottom();
		for (Identifier source : r.getSources()) {
			L partial = state;
			for (Identifier target : r.getTargets())
				partial = assign(partial, target, (E) source, pp, oracle);
			lub = lub.lub(partial);
		}

		return lub.forgetIdentifiers(r.getIdsToForget(), pp);
	}

}
