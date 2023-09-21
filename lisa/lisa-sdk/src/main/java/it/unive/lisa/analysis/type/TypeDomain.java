package it.unive.lisa.analysis.type;

import it.unive.lisa.analysis.value.ValueDomain;

/**
 * An domain that is able to determine the runtime types of an expression given
 * the runtime types of its operands.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the {@link TypeDomain}
 */
public interface TypeDomain<T extends TypeDomain<T>> extends TypeOracle, ValueDomain<T> {
}
