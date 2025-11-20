package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A {@link NonRelationalDomain} that focuses on the values of program
 * variables. Instances of this class use {@link ValueEnvironment}s as lattice
 * elements, and are able to process any {@link ValueExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the lattice used as values in the environments
 */
public interface NonRelationalValueDomain<L extends Lattice<L>>
		extends
		ValueDomain<ValueEnvironment<L>>,
		NonRelationalDomain<L, ValueEnvironment<L>, ValueEnvironment<L>, ValueExpression> {
}
