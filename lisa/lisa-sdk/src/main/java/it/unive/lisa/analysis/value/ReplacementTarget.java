package it.unive.lisa.analysis.value;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A {@link SemanticDomain} that can be modified by a substitution of
 * identifiers that is caused by a modification of the abstraction provided in
 * the {@link HeapDomain} of the analysis. A substitution is composed by a list
 * of {@link HeapReplacement} instances, that <b>must be applied in order</b>.
 * See <a href=
 * "https://www.sciencedirect.com/science/article/pii/S0304397516300299">this
 * paper</a> for more details.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <D> the concrete {@link ReplacementTarget} instance
 * @param <E> the type of {@link SymbolicExpression} that this domain can
 *                process
 */
public interface ReplacementTarget<
		D extends ReplacementTarget<D, E>,
		E extends SymbolicExpression>
		extends
		Lattice<D>,
		SemanticDomain<D, E, Identifier> {

	/**
	 * Applies a substitution of identifiers that is caused by a modification of
	 * the abstraction provided in the {@link HeapDomain} of the analysis. A
	 * substitution is composed by a list of {@link HeapReplacement} instances,
	 * that <b>must be applied in order</b>.
	 * 
	 * @param r      the replacement to apply
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the domain instance modified by the substitution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default D applyReplacement(
			HeapReplacement r,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (isTop() || isBottom() || r.getSources().isEmpty())
			return (D) this;

		if (r.getTargets().isEmpty())
			// if there are no targets, we just forget the identifiers
			return (D) forgetIdentifiers(r.getSources(), pp);

		D result = (D) this;
		D lub = bottom();

		for (Identifier source : r.getSources()) {
			D partial = result;
			for (Identifier target : r.getTargets())
				partial = partial.assign(target, (E) source, pp, oracle);
			lub = lub.lub(partial);
		}

		return lub.forgetIdentifiers(r.getIdsToForget(), pp);
	}
}
