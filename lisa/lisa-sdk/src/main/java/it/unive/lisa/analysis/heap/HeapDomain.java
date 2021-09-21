package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashSet;
import java.util.Set;

/**
 * A semantic domain that can evaluate the semantic of statements that operate
 * on heap locations, and not on concrete values. A heap domain can handle
 * instances of {@link HeapExpression}s, and manage identifiers that are
 * {@link HeapLocation}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <D> the concrete type of the {@link HeapDomain}
 */
public interface HeapDomain<D extends HeapDomain<D>>
		extends SemanticDomain<D, SymbolicExpression, Identifier>, Lattice<D>, HeapSemanticOperation {

	/**
	 * Rewrites a {@link SymbolicExpression}, getting rid of the parts that
	 * access heap structures, substituting them with synthetic
	 * {@link HeapLocation}s representing the accessed locations. The expression
	 * returned by this method should not contain {@link HeapExpression}s.<br>
	 * <br>
	 * If no rewriting is necessary, the input expression can be returned
	 * instead.<br>
	 * 
	 * @param expression the expression to rewrite
	 * @param pp         the program point that where this expression is being
	 *                       rewritten
	 * 
	 * @return the rewritten expression, or the original one
	 * 
	 * @throws SemanticException if something goes wrong during the rewriting
	 */
	ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp) throws SemanticException;

	/**
	 * Rewrites all {@link SymbolicExpression}s, getting rid of the parts that
	 * access heap structures, substituting them with synthetic
	 * {@link HeapLocation}s representing the accessed locations. The
	 * expressions returned by this method should not contain
	 * {@link HeapExpression}s.<br>
	 * <br>
	 * If no rewriting is necessary, the returned {@link ExpressionSet} will
	 * contain the input expressions.<br>
	 * <br>
	 * The default implementation of this method simply iterates over the input
	 * expressions, invoking {@link #rewrite(SymbolicExpression, ProgramPoint)}
	 * on all of them.<br>
	 * <br>
	 * The collection returned by this method usually contains one expression,
	 * but instances created through lattice operations (e.g., lub) might
	 * contain more.
	 * 
	 * @param expressions the expressions to rewrite
	 * @param pp          the program point that where this expressions are
	 *                        being rewritten
	 * 
	 * @return the rewritten expressions, or the original ones
	 * 
	 * @throws SemanticException if something goes wrong during the rewriting
	 */
	default ExpressionSet<ValueExpression> rewriteAll(ExpressionSet<SymbolicExpression> expressions, ProgramPoint pp)
			throws SemanticException {
		Set<ValueExpression> result = new HashSet<>();
		for (SymbolicExpression expr : expressions)
			result.addAll(rewrite(expr, pp).elements());
		return new ExpressionSet<>(result);
	}
}
