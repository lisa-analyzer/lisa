package it.unive.lisa.symbolic;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;

public interface ExpressionVisitor<T> {

	T visit(AccessChild expression, T receiver, T child, Object... params) throws SemanticException;
	T visit(HeapAllocation expression, Object... params) throws SemanticException;
	
	T visit(UnaryExpression expression, T arg, Object... params) throws SemanticException;
	T visit(BinaryExpression expression, T left, T right, Object... params) throws SemanticException;
	T visit(TernaryExpression expression, T left, T middle, T right, Object... params) throws SemanticException;
	
	T visit(Skip expression, Object... params) throws SemanticException;
	T visit(PushAny expression, Object... params) throws SemanticException;
	T visit(Constant expression, Object... params) throws SemanticException;
	
	T visit(Identifier expression, Object... params) throws SemanticException;
}
