package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.*;

import java.util.*;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A base implementation of the {@link HeapDomain} interface, handling base
 * cases of
 * {@link #smallStepSemantics(HeapLattice, SymbolicExpression, ProgramPoint, SemanticOracle)}
 * and providing a base expression rewriting strategy as an
 * {@link ExpressionVisitor}. All implementers of {@link HeapDomain} should
 * inherit from this class for ensuring a consistent behavior on the base cases,
 * unless explicitly needed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link HeapLattice} produced by this domain
 */
public interface BaseHeapDomain<L extends HeapLattice<L>>
		extends
		HeapDomain<L> {

	@Override
	default Pair<L, List<HeapReplacement>> smallStepSemantics(
			L state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof HeapExpression)
			return semanticsOf(state, (HeapExpression) expression, pp, oracle);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			return smallStepSemantics(state, unary.getExpression(), pp, oracle);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			Pair<L, List<HeapReplacement>> sem1 = smallStepSemantics(state, binary.getLeft(), pp, oracle);
			if (sem1.getLeft().isBottom())
				return sem1;
			Pair<L, List<HeapReplacement>> sem2 = smallStepSemantics(sem1.getLeft(), binary.getRight(), pp, oracle);
			return Pair.of(sem2.getLeft(), ListUtils.union(sem1.getRight(), sem2.getRight()));
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;
			Pair<L, List<HeapReplacement>> sem1 = smallStepSemantics(state, ternary.getLeft(), pp, oracle);
			if (sem1.getLeft().isBottom())
				return sem1;
			Pair<L, List<HeapReplacement>> sem2 = smallStepSemantics(sem1.getLeft(), ternary.getMiddle(), pp, oracle);
			if (sem2.getLeft().isBottom())
				return Pair.of(sem2.getLeft(), ListUtils.union(sem1.getRight(), sem2.getRight()));
			Pair<L, List<HeapReplacement>> sem3 = smallStepSemantics(sem2.getLeft(), ternary.getRight(), pp, oracle);
			return Pair.of(
					sem3.getLeft(),
					ListUtils.union(sem1.getRight(), ListUtils.union(sem2.getRight(), sem3.getRight())));
		}

		if (expression instanceof ValueExpression)
			return Pair.of(state, List.of());

		return Pair.of(state.top(), List.of());
	}

	/**
	 * Yields a new instance of this domain, built by evaluating the semantics
	 * of the given heap expression.
	 * 
	 * @param state      the current state of this domain
	 * @param expression the expression to evaluate
	 * @param pp         the program point that where this expression is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return a new instance of this domain
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public abstract Pair<L, List<HeapReplacement>> semanticsOf(
			L state,
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
	public abstract static class Rewriter
			implements
			ExpressionVisitor<ExpressionSet> {

		@Override
		public ExpressionSet visit(
				UnaryExpression expression,
				ExpressionSet arg,
				Object... params)
				throws SemanticException {
			Set<SymbolicExpression> result = new HashSet<>();
			for (SymbolicExpression expr : arg) {
				UnaryExpression e = new UnaryExpression(
						expression.getStaticType(),
						expr,
						expression.getOperator(),
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
					BinaryExpression e = new BinaryExpression(
							expression.getStaticType(),
							l,
							r,
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
						TernaryExpression e = new TernaryExpression(
								expression.getStaticType(),
								l,
								m,
								r,
								expression.getOperator(),
								expression.getCodeLocation());
						result.add(e);
					}
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet visit(
				VariadicExpression expression,
				ExpressionSet[] values,
				Object... params) throws SemanticException {

				Set<SymbolicExpression> result = new HashSet<>();
				for (SymbolicExpression[] e : new VariadicExpression.CartesianProduct(values)) {
					VariadicExpression ve = new VariadicExpression(
							expression.getStaticType(),
							e,
							expression.getVarargsIndex(),
							expression.getOperator(),
							expression.getCodeLocation());
					result.add(ve);
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

		@Override
		public ExpressionSet visit(
				HeapExpression expression,
				ExpressionSet[] subExpressions,
				Object... params)
				throws SemanticException {
			throw new SemanticException(
					"No rewriting rule for heap expression of type " + expression.getClass().getName());
		}

		@Override
		public ExpressionSet visit(
				ValueExpression expression,
				ExpressionSet[] subExpressions,
				Object... params)
				throws SemanticException {
			throw new SemanticException(
					"No rewriting rule for value expression of type " + expression.getClass().getName());
		}

	}

}
