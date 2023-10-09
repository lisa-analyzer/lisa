package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashSet;
import java.util.Set;

/**
 * A base implementation of the {@link HeapDomain} interface, handling base
 * cases of
 * {@link #smallStepSemantics(SymbolicExpression, ProgramPoint, SemanticOracle)}.
 * All implementers of {@link HeapDomain} should inherit from this class for
 * ensuring a consistent behavior on the base cases, unless explicitly needed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <H> the concrete {@link BaseHeapDomain} instance
 */
public interface BaseHeapDomain<H extends BaseHeapDomain<H>> extends BaseLattice<H>, HeapDomain<H> {

	@Override
	@SuppressWarnings("unchecked")
	default H smallStepSemantics(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof HeapExpression)
			return semanticsOf((HeapExpression) expression, pp, oracle);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			return smallStepSemantics(unary.getExpression(), pp, oracle);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			H sem = smallStepSemantics(binary.getLeft(), pp, oracle);
			if (sem.isBottom())
				return sem;
			return sem.smallStepSemantics(binary.getRight(), pp, oracle);
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;
			H sem1 = smallStepSemantics(ternary.getLeft(), pp, oracle);
			if (sem1.isBottom())
				return sem1;
			H sem2 = sem1.smallStepSemantics(ternary.getMiddle(), pp, oracle);
			if (sem2.isBottom())
				return sem2;
			return sem2.smallStepSemantics(ternary.getRight(), pp, oracle);
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
	public abstract H mk(
			H reference);

	@Override
	@SuppressWarnings("unchecked")
	default H pushScope(
			ScopeToken scope)
			throws SemanticException {
		return (H) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	default H popScope(
			ScopeToken scope)
			throws SemanticException {
		return (H) this;
	}

	/**
	 * Yields a new instance of this domain, built by evaluating the semantics
	 * of the given heap expression.
	 * 
	 * @param expression the expression to evaluate
	 * @param pp         the program point that where this expression is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return a new instance of this domain
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public abstract H semanticsOf(
			HeapExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * An {@link ExpressionVisitor} that rewrites {@link SymbolicExpression}s to
	 * {@link ValueExpression}s. The visiting of {@link HeapExpression}s is left
	 * unimplemented for concrete instances to provide their logic.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public abstract static class Rewriter implements ExpressionVisitor<ExpressionSet> {

		@Override
		public ExpressionSet visit(
				UnaryExpression expression,
				ExpressionSet arg,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();
			for (SymbolicExpression expr : arg) {
				UnaryExpression e = new UnaryExpression(expression.getStaticType(), expr, expression.getOperator(),
						expression.getCodeLocation());
				result.add(e);
			}
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				BinaryExpression expression,
				ExpressionSet left,
				ExpressionSet right,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();
			for (SymbolicExpression l : left)
				for (SymbolicExpression r : right) {
					BinaryExpression e = new BinaryExpression(expression.getStaticType(), l, r,
							expression.getOperator(),
							expression.getCodeLocation());
					result.add(e);
				}
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				TernaryExpression expression,
				ExpressionSet left,
				ExpressionSet middle,
				ExpressionSet right,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();
			for (SymbolicExpression l : left)
				for (SymbolicExpression m : middle)
					for (SymbolicExpression r : right) {
						TernaryExpression e = new TernaryExpression(expression.getStaticType(), l, m, r,
								expression.getOperator(),
								expression.getCodeLocation());
						result.add(e);
					}
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				Skip expression,
				Object... params)
				throws SemanticException {
			return new ExpressionSet(expression);
		}

		@Override
		public ExpressionSet visit(
				PushAny expression,
				Object... params)
				throws SemanticException {
			return new ExpressionSet(expression);
		}

		@Override
		public ExpressionSet visit(
				PushInv expression,
				Object... params)
				throws SemanticException {
			return new ExpressionSet(expression);
		}

		@Override
		public ExpressionSet visit(
				Constant expression,
				Object... params)
				throws SemanticException {
			return new ExpressionSet(expression);
		}

		@Override
		public ExpressionSet visit(
				Identifier expression,
				Object... params)
				throws SemanticException {
			return new ExpressionSet(expression);
		}
	}
}
