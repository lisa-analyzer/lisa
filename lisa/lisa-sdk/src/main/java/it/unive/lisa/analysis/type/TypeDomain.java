package it.unive.lisa.analysis.type;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * An domain that is able to determine the runtime types of an expression given
 * the runtime types of its operands.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the {@link TypeDomain}
 */
public interface TypeDomain<T extends TypeDomain<T>> extends TypeOracle, ValueDomain<T> {

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
	 * @return the type domain instance modified by the substitution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default T applyReplacement(
			HeapReplacement r,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (isTop() || isBottom() || r.getSources().isEmpty())
			return (T) this;

		T result = (T) this;
		T lub = bottom();
		for (Identifier source : r.getSources()) {
			T partial = result;
			for (Identifier target : r.getTargets())
				partial = partial.assign(target, source, pp, oracle);
			lub = lub.lub(partial);
		}
		return lub.forgetIdentifiers(r.getIdsToForget());

	}
}
