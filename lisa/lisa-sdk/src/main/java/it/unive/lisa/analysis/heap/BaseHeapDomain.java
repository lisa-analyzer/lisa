package it.unive.lisa.analysis.heap;

import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
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
		return representation().toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public H smallStepSemantics(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		if (expression instanceof HeapExpression)
			return semanticsOf((HeapExpression) expression, pp);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			return smallStepSemantics(unary.getExpression(), pp);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			H sem = smallStepSemantics(binary.getLeft(), pp);
			if (sem.isBottom())
				return sem;
			return sem.smallStepSemantics(binary.getRight(), pp);
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;
			H sem1 = smallStepSemantics(ternary.getLeft(), pp);
			if (sem1.isBottom())
				return sem1;
			H sem2 = sem1.smallStepSemantics(ternary.getMiddle(), pp);
			if (sem2.isBottom())
				return sem2;
			return sem2.smallStepSemantics(ternary.getRight(), pp);
		}

		if (expression instanceof ValueExpression)
			return mk((H) this);

		return top();
	}

	/**
	 * Creates a new instance of this domain containing the same abstract
	 * information of reference. The returned object is effectively a new
	 * instance, meaning that all substitutions should be cleared. If this
	 * domain does not apply substitutions, it is fine to return {@code this}.
	 * 
	 * @param reference the domain whose abstract information needs to be copied
	 * 
	 * @return a new instance of this domain
	 */
	protected abstract H mk(H reference);

	@Override
	@SuppressWarnings("unchecked")
	public H pushScope(ScopeToken scope) throws SemanticException {
		return (H) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public H popScope(ScopeToken scope) throws SemanticException {
		return (H) this;
	}

	/**
	 * Yields a new instance of this domain, built by evaluating the semantics
	 * of the given heap expression.
	 * 
	 * @param expression the expression to evaluate
	 * @param pp         the program point that where this expression is being
	 *                       evaluated
	 * 
	 * @return a new instance of this domain
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected abstract H semanticsOf(HeapExpression expression, ProgramPoint pp) throws SemanticException;

	/**
	 * An {@link ExpressionVisitor} that rewrites {@link SymbolicExpression}s to
	 * {@link ValueExpression}s. The visiting of {@link HeapExpression}s is left
	 * unimplemented for concrete instances to provide their logic.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	protected abstract static class Rewriter implements ExpressionVisitor<ExpressionSet<ValueExpression>> {

		@Override
		public ExpressionSet<ValueExpression> visit(UnaryExpression expression,
				ExpressionSet<ValueExpression> arg,
				Object... params) throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();
			for (ValueExpression expr : arg) {
				UnaryExpression e = new UnaryExpression(expression.getStaticType(), expr, expression.getOperator(),
						expression.getCodeLocation());
				e.setRuntimeTypes(expr.getRuntimeTypes());
				result.add(e);
			}
			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(BinaryExpression expression,
				ExpressionSet<ValueExpression> left,
				ExpressionSet<ValueExpression> right, Object... params) throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();
			for (ValueExpression l : left)
				for (ValueExpression r : right) {
					BinaryExpression e = new BinaryExpression(expression.getStaticType(), l, r,
							expression.getOperator(),
							expression.getCodeLocation());
					e.setRuntimeTypes(expression.getRuntimeTypes());
					result.add(e);
				}
			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(TernaryExpression expression,
				ExpressionSet<ValueExpression> left,
				ExpressionSet<ValueExpression> middle, ExpressionSet<ValueExpression> right, Object... params)
				throws SemanticException {
			Set<ValueExpression> result = new HashSet<>();
			for (ValueExpression l : left)
				for (ValueExpression m : middle)
					for (ValueExpression r : right) {
						TernaryExpression e = new TernaryExpression(expression.getStaticType(), l, m, r,
								expression.getOperator(),
								expression.getCodeLocation());
						e.setRuntimeTypes(expression.getRuntimeTypes());
						result.add(e);
					}
			return new ExpressionSet<>(result);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(Skip expression, Object... params) throws SemanticException {
			return new ExpressionSet<>(expression);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(PushAny expression, Object... params)
				throws SemanticException {
			return new ExpressionSet<>(expression);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(Constant expression, Object... params)
				throws SemanticException {
			return new ExpressionSet<>(expression);
		}

		@Override
		public ExpressionSet<ValueExpression> visit(Identifier expression, Object... params)
				throws SemanticException {
			return new ExpressionSet<>(expression);
		}
	}
}
