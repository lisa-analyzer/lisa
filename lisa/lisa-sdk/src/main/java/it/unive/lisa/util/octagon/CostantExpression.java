package it.unive.lisa.util.octagon;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;

/*
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso </a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 * Shytermeja</a>
 */

public class CostantExpression implements ExpressionVisitor<ValueExpression> {

	@Override
	public ValueExpression visit(
			HeapExpression expression,
			ValueExpression[] subExpressions,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			AccessChild expression,
			ValueExpression receiver,
			ValueExpression child,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			MemoryAllocation expression,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			HeapReference expression,
			ValueExpression arg,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			HeapDereference expression,
			ValueExpression arg,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			ValueExpression expression,
			ValueExpression[] subExpressions,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			UnaryExpression expression,
			ValueExpression arg,
			Object... params)
			throws SemanticException {

		return arg.accept(this, params);
	}

	@Override
	public ValueExpression visit(
			BinaryExpression expression,
			ValueExpression left,
			ValueExpression right,
			Object... params)
			throws SemanticException {

		System.out.println("Espressione finale: " + expression.toString() + " : sinistro: "
				+ expression.getLeft().getStaticType() + " destro" + expression.getRight());

		// if (expression.getStaticType() == Type.DOUBLE) {
		return new Constant(expression.getStaticType(), Double.valueOf(expression.toString()), null);
		// } else {
		// return expression;
		// }
	}

	@Override
	public ValueExpression visit(
			TernaryExpression expression,
			ValueExpression left,
			ValueExpression middle,
			ValueExpression right,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			Skip expression,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			PushAny expression,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			PushInv expression,
			Object... params)
			throws SemanticException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			Constant expression,
			Object... params)
			throws SemanticException {

		// System.out.println("Costant expression: " + expression.toString());

		return expression;
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ValueExpression visit(
			Identifier expression,
			Object... params)
			throws SemanticException {

		System.out.println("Binary expression: " + expression.toString());

		return expression;
		// throw new UnsupportedOperationException("Not supported yet.");
	}

}
