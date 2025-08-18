package it.unive.lisa.analysis.value;

import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A semantic domain that can evaluate the semantic of expressions that operate
 * on values, and not on memory locations. A value domain can handle instances
 * of {@link ValueExpression}s, and are associated to {@link ValueLattice}
 * instances.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link ValueLattice} that this domain works with
 */
public interface ValueDomain<L extends ValueLattice<L>> extends DomainWithReplacement<L, ValueExpression> {
}
