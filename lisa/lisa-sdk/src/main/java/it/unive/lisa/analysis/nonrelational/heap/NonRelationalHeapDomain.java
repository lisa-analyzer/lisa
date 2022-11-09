package it.unive.lisa.analysis.nonrelational.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapSemanticOperation;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashSet;
import java.util.Set;

/**
 * A non-relational heap domain, that is able to compute the value of a
 * {@link SymbolicExpression} by knowing the values of all program variables.
 * Instances of this class can be wrapped inside a {@link HeapEnvironment} to
 * represent abstract values of individual {@link Identifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the domain
 */
public interface NonRelationalHeapDomain<T extends NonRelationalHeapDomain<T>>
		extends NonRelationalDomain<T, SymbolicExpression, HeapEnvironment<T>>, HeapSemanticOperation {

	/**
	 * Rewrites a {@link SymbolicExpression}, getting rid of the parts that
	 * access heap structures, substituting them with synthetic
	 * {@link HeapLocation}s representing the accessed locations. The
	 * expression(s) returned by this method should not contain
	 * {@link HeapExpression}s.<br>
	 * <br>
	 * Note that a single expression might be rewritten to more than one
	 * expression, depending on the individual reasoning of the domain.<br>
	 * <br>
	 * If no rewriting is necessary, the input expression can be returned
	 * instead.<br>
	 * 
	 * @param expression  the expression to rewrite
	 * @param environment the environment containing information about the
	 *                        program variables
	 * @param pp          the program point that where this expression is being
	 *                        rewritten
	 * 
	 * @return the rewritten expressions, or the original one
	 * 
	 * @throws SemanticException if something goes wrong during the rewriting
	 */
	ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, HeapEnvironment<T> environment,
			ProgramPoint pp)
			throws SemanticException;

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
	 * expressions, invoking
	 * {@link #rewrite(SymbolicExpression, HeapEnvironment, ProgramPoint)} on
	 * all of them.<br>
	 * <br>
	 * The collection returned by this method usually contains one expression,
	 * but instances created through lattice operations (e.g., lub) might
	 * contain more.
	 * 
	 * @param expressions the expressions to rewrite
	 * @param environment the environment containing information about the
	 *                        program variables
	 * @param pp          the program point that where this expressions are
	 *                        being rewritten
	 * 
	 * @return the rewritten expressions, or the original ones
	 * 
	 * @throws SemanticException if something goes wrong during the rewriting
	 */
	default ExpressionSet<ValueExpression> rewriteAll(ExpressionSet<SymbolicExpression> expressions,
			HeapEnvironment<T> environment, ProgramPoint pp) throws SemanticException {
		Set<ValueExpression> result = new HashSet<>();
		for (SymbolicExpression expr : expressions)
			result.addAll(rewrite(expr, environment, pp).elements());
		return new ExpressionSet<>(result);
	}

}
