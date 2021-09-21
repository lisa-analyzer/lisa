package it.unive.lisa.analysis.value;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import java.util.List;

/**
 * A semantic domain that can evaluate the semantic of statements that operate
 * on values, and not on memory locations. A value domain can handle instances
 * of {@link ValueExpression}s, and manage identifiers that are
 * {@link Variable}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <D> the concrete type of the {@link ValueDomain}
 */
public interface ValueDomain<D extends ValueDomain<D>>
		extends SemanticDomain<D, ValueExpression, Identifier>, Lattice<D> {

	/**
	 * Applies a substitution of identifiers that is caused by a modification of
	 * the abstraction provided in the {@link HeapDomain} of the analysis. A
	 * substitution is composed by a list of {@link HeapReplacement} instances,
	 * that <b>must be applied in order</b>.
	 * 
	 * @param substitution the substitution to apply
	 * @param pp           the program point that where this operation is being
	 *                         evaluated
	 * 
	 * @return the value domain instance modified by the substitution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default D applySubstitution(List<HeapReplacement> substitution, ProgramPoint pp) throws SemanticException {
		if (isTop() || isBottom() || substitution == null || substitution.isEmpty())
			return (D) this;

		D result = (D) this;
		for (HeapReplacement r : substitution) {
			if (r.getSources().isEmpty())
				continue;
			D lub = bottom();
			for (Identifier source : r.getSources()) {
				D partial = result;
				for (Identifier target : r.getTargets())
					partial = partial.assign(target, source, pp);
				lub = lub.lub(partial);
			}
			result = lub.forgetIdentifiers(r.getIdsToForget());
		}

		return result;
	}
}
