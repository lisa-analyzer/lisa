package it.unive.lisa.analysis.nonrelational.type;

import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A {@link NonRelationalDomain} that focuses on the types of program variables.
 * Instances of this class use {@link TypeEnvironment}s as lattice elements, and
 * are able to process any {@link ValueExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the lattice used as values in the environments
 */
public interface NonRelationalTypeDomain<L extends TypeValue<L>>
		extends
		TypeDomain<TypeEnvironment<L>>,
		NonRelationalDomain<L, TypeEnvironment<L>, TypeEnvironment<L>, ValueExpression> {
}
