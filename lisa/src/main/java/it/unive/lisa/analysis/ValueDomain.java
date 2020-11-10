package it.unive.lisa.analysis;

import java.util.List;

import it.unive.lisa.analysis.HeapDomain.Replacement;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.ValueIdentifier;

/**
 * A semantic domain that can evaluate the semantic of statements that operate
 * on values, and not on memory locations. A value domain can handle instances
 * of {@link ValueExpression}s, and manage identifiers that are
 * {@link ValueIdentifier}s.
 * 
 * @param <D> the concrete type of the {@link ValueDomain}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ValueDomain<D extends ValueDomain<D>>
		extends SemanticDomain<D, ValueExpression, ValueIdentifier>, Lattice<D> {

	/**
	 * Applies a substitution of identifiers that is caused by a modification of the
	 * abstraction provided in the {@link HeapDomain} of the analysis. A
	 * substitution is composed by a list of {@link Replacement} instances, that
	 * <b>must be applied in order</b>.
	 * 
	 * @param substitution the substitution to apply
	 */
	void applySubstitution(List<Replacement> substitution);
}
