package it.unive.lisa.analysis.value;

import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;

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
		extends
		ValueOracle,
		ReplacementTarget<D, ValueExpression> {
}
