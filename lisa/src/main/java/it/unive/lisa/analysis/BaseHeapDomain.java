package it.unive.lisa.analysis;

import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A base implementation of the {@link HeapDomain} interface, handling base
 * cases of {@link #smallStepSemantics(SymbolicExpression, ProgramPoint)}. All
 * implementers of {@link HeapDomain} should inherit from this class for
 * ensuring a consistent behavior on the base cases, unless explicitly needed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <H> the concrete {@link BaseHeapDomain} instance
 */
public abstract class BaseHeapDomain<H extends BaseHeapDomain<H>> extends BaseLattice<H> implements HeapDomain<H> {

	@Override
	public final String toString() {
		return representation();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final H smallStepSemantics(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof HeapExpression)
			return semanticsOf((HeapExpression) expression);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			H sem = smallStepSemantics(unary.getExpression(), pp);
			H result = bottom();
			for (ValueExpression expr : sem.getRewrittenExpressions())
				result = result.lub(mk(sem, new UnaryExpression(expression.getTypes(), expr, unary.getOperator())));
			return result;
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			H sem1 = smallStepSemantics(binary.getLeft(), pp);
			H sem2 = sem1.smallStepSemantics(binary.getRight(), pp);
			H result = bottom();
			for (ValueExpression expr1 : sem1.getRewrittenExpressions())
				for (ValueExpression expr2 : sem2.getRewrittenExpressions())
					result = result.lub(
							mk(sem2, new BinaryExpression(expression.getTypes(), expr1, expr2, binary.getOperator())));
			return result;
		}

		if (expression instanceof ValueExpression)
			return mk((H) this, (ValueExpression) expression);

		return top();
	}

	/**
	 * Creates a new instance of this domain containing the same abstract
	 * information of reference, but setting as rewritten expression the given
	 * one.
	 * 
	 * @param reference  the domain whose abstract information needs to be
	 *                       copied
	 * @param expression the expression to set as the rewritten one
	 * 
	 * @return a new instance of this domain
	 */
	protected abstract H mk(H reference, ValueExpression expression);

	/**
	 * Yields a new instance of this domain, built by evaluating the semantics
	 * of the given heap expression.
	 * 
	 * @param expression the expression to evaluate
	 * 
	 * @return a new instance of this domain
	 */
	protected abstract H semanticsOf(HeapExpression expression);
}
