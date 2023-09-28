package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashSet;
import java.util.Set;

/**
 * An oracle that can be queried for information about the static and dynamic
 * memory of the program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface MemoryOracle {

	/**
	 * Rewrites the given expression to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param expression the expression to rewrite
	 * @param pp         the program point where the rewrite happens
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	ExpressionSet rewrite(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Rewrites the given expressions to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param expressions the expressions to rewrite
	 * @param pp          the program point where the rewrite happens
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	default ExpressionSet rewrite(
			ExpressionSet expressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		for (SymbolicExpression expr : expressions)
			if (!expr.mightNeedRewriting())
				result.add(expr);
			else
				result.addAll(rewrite(expr, pp, oracle).elements());
		return new ExpressionSet(result);
	}

}
