package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A base implementation of the {@link HeapDomain} interface, handling base
 * cases of {@link #smallStepSemantics(SymbolicExpression)}. All implementers of
 * {@link HeapDomain} should inherit from this class for ensuring a consistent
 * behavior on the base cases, unless explicitly needed.
 * 
 * @param <H> the concrete {@link BaseHeapDomain} instance
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class BaseHeapDomain<H extends BaseHeapDomain<H>> extends BaseLattice<H> implements HeapDomain<H> {

	@Override
	@SuppressWarnings("unchecked")
	public final H smallStepSemantics(SymbolicExpression expression) throws SemanticException {
		if (expression instanceof HeapExpression)
			return semanticsOf((HeapExpression) expression);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			H sem = smallStepSemantics(unary.getExpression());
			return mk(sem,
					new UnaryExpression(expression.getType(), sem.getRewrittenExpression(), unary.getOperator()));
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			H sem1 = smallStepSemantics(binary.getLeft());
			H sem2 = sem1.smallStepSemantics(binary.getRight());
			return mk(sem2, new BinaryExpression(expression.getType(), sem1.getRewrittenExpression(),
					sem2.getRewrittenExpression(), binary.getOperator()));
		}

		if (expression instanceof ValueExpression)
			return mk((H) this, (ValueExpression) expression);

		return top();
	}

	/**
	 * Creates a new instance of this domain containing the same abstract
	 * information of reference, but setting as rewritten expression the given one.
	 * 
	 * @param reference  the domain whose abstract information needs to be copied
	 * @param expression the expression to set as the rewritten one
	 * @return a new instance of this domain
	 */
	protected abstract H mk(H reference, ValueExpression expression);

	/**
	 * Yields a new instance of this domain, built by evaluating the semantics of
	 * the given heap expression.
	 * 
	 * @param expression the expression to evaluate
	 * @return a new instance of this domain
	 */
	protected abstract H semanticsOf(HeapExpression expression);
}
