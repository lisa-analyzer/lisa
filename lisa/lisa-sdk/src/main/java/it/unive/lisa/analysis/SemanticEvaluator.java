package it.unive.lisa.analysis;

import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An entity that can perform semantic evaluations that is not a
 * {@link SemanticDomain}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SemanticEvaluator {

	/**
	 * Yields {@code true} if the domain can process {@code expression},
	 * {@code false} otherwise.
	 * 
	 * @param expression the expression
	 * @param pp         TODO
	 * 
	 * @return {@code true} if the domain can process {@code expression},
	 *             {@code false} otherwise.
	 */
	boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle);
}
