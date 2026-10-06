package it.unive.lisa.symbolic;

import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.ArrayList;
import java.util.List;

// a minimal ExpressionVisitor recording, in visitation order, the expression
// each callback was invoked on plus the sub-results it received; every
// callback returns the visited expression itself, so composite callbacks can
// assert on exactly what sub-results they were handed
public class RecordingVisitor
		implements
		ExpressionVisitor<SymbolicExpression> {

	public final List<SymbolicExpression> visited = new ArrayList<>();

	@Override
	public SymbolicExpression visit(
			HeapExpression expression,
			SymbolicExpression[] subExpressions,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			AccessChild expression,
			SymbolicExpression receiver,
			SymbolicExpression child,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			MemoryAllocation expression,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			HeapReference expression,
			SymbolicExpression arg,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			HeapDereference expression,
			SymbolicExpression arg,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			ValueExpression expression,
			SymbolicExpression[] subExpressions,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			UnaryExpression expression,
			SymbolicExpression arg,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			BinaryExpression expression,
			SymbolicExpression left,
			SymbolicExpression right,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			TernaryExpression expression,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			Skip expression,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			PushAny expression,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			PushInv expression,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			Constant expression,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			Identifier expression,
			Object... params) {
		visited.add(expression);
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			NullConstant expression,
			Object... params) {
		visited.add(expression);
		return expression;
	}

}
