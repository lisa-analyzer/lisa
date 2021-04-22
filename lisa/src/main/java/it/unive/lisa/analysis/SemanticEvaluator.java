package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * An entity that can perform semantic evaluations that is not a
 * {@link SemanticDomain}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SemanticEvaluator {

	/**
	 * Yields {@code true} if the domain tracks information on the identifier
	 * {@code id}, {@code false} otherwise.
	 * 
	 * @param id the identifier
	 * 
	 * @return {@code true} if the domain tracks information on the identifier
	 *             {@code id}, {@code false} otherwise
	 */
	public boolean tracksIdentifiers(Identifier id);

	/**
	 * Yields {@code true} if the domain can process {@code expression},
	 * {@code false} otherwise.
	 * 
	 * @param expression the expression
	 * 
	 * @return {@code true} if the domain can process {@code expression},
	 *             {@code false} otherwise.
	 */
	public boolean canProcess(SymbolicExpression expression);
}
