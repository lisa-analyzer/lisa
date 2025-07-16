package it.unive.lisa.symbolic;

import it.unive.lisa.analysis.SemanticException;
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

/**
 * A visitor for {@link SymbolicExpression}s, to be used as parameter to
 * {@link SymbolicExpression#accept(ExpressionVisitor, Object...)}. The
 * expression will invoke the callbacks provided by this interface while
 * traversing its structure.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the return type of the visiting callbacks
 */
public interface ExpressionVisitor<T> {

	/**
	 * Visits a generic {@link HeapExpression}. This callback is invoked after
	 * the inner expressions have been visited, and their produced value is
	 * passed as argument. <br>
	 * <br>
	 * This overload allows visiting frontend-defined expressions. For all
	 * standard expressions defined within LiSA, the corresponding overload will
	 * be invoked instead.
	 * 
	 * @param expression     the expression
	 * @param subExpressions the values produced by visiting the
	 *                           sub-expressions, if any; if there are no
	 *                           sub-expressions, this parameter can be
	 *                           {@code null} or empty
	 * @param params         the additional parameters provided to
	 *                           {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                           if any
	 * 
	 * @return the value produced by visiting the expression
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			HeapExpression expression,
			T[] subExpressions,
			Object... params)
			throws SemanticException;

	/**
	 * Visits an {@link AccessChild}. This callback is invoked after the inner
	 * expressions have been visited, and their produced value is passed as
	 * argument.
	 * 
	 * @param expression the expression
	 * @param receiver   the value produced by visiting the receiver
	 *                       ({@link AccessChild#getContainer()}) of the access
	 * @param child      the value produced by visiting the child
	 *                       ({@link AccessChild#getChild()}) of the access
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the expression
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			AccessChild expression,
			T receiver,
			T child,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link MemoryAllocation}.
	 * 
	 * @param expression the allocation
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the allocation
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			MemoryAllocation expression,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link HeapReference}.
	 * 
	 * @param expression the heap reference
	 * @param arg        the value produced by visiting the argument of the
	 *                       expression
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the heap reference
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			HeapReference expression,
			T arg,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link HeapDereference}.
	 * 
	 * @param expression the heap dereference
	 * @param arg        the value produced by visiting the argument of the
	 *                       expression
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the heap dereference
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			HeapDereference expression,
			T arg,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a generic {@link ValueExpression}. This callback is invoked after
	 * the inner expressions have been visited, and their produced value is
	 * passed as argument. <br>
	 * <br>
	 * This overload allows visiting frontend-defined expressions. For all
	 * standard expressions defined within LiSA, the corresponding overload will
	 * be invoked instead.
	 * 
	 * @param expression     the expression
	 * @param subExpressions the values produced by visiting the
	 *                           sub-expressions, if any; if there are no
	 *                           sub-expressions, this parameter can be
	 *                           {@code null} or empty
	 * @param params         the additional parameters provided to
	 *                           {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                           if any
	 * 
	 * @return the value produced by visiting the expression
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			ValueExpression expression,
			T[] subExpressions,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link UnaryExpression}. This callback is invoked after the
	 * inner expressions have been visited, and their produced value is passed
	 * as argument.
	 * 
	 * @param expression the expression
	 * @param arg        the value produced by visiting the argument of the
	 *                       expression
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the expression
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			UnaryExpression expression,
			T arg,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link BinaryExpression}. This callback is invoked after the
	 * inner expressions have been visited, and their produced value is passed
	 * as argument.
	 * 
	 * @param expression the expression
	 * @param left       the value produced by visiting the left-hand side of
	 *                       the expression
	 * @param right      the value produced by visiting the right-hand side of
	 *                       the expression
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the expression
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			BinaryExpression expression,
			T left,
			T right,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link TernaryExpression}. This callback is invoked after the
	 * inner expressions have been visited, and their produced value is passed
	 * as argument.
	 * 
	 * @param expression the expression
	 * @param left       the value produced by visiting the left-hand side of
	 *                       the expression
	 * @param middle     the value produced by visiting the middle-hand side of
	 *                       the expression
	 * @param right      the value produced by visiting the right-hand side of
	 *                       the expression
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the expression
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			TernaryExpression expression,
			T left,
			T middle,
			T right,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link Skip}.
	 * 
	 * @param expression the skip
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the skip
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			Skip expression,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link PushAny}.
	 * 
	 * @param expression the pushany
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the pushany
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			PushAny expression,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link PushInv}.
	 * 
	 * @param expression the pushinv
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the pushinv
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			PushInv expression,
			Object... params)
			throws SemanticException;

	/**
	 * Visits a {@link Constant}.
	 * 
	 * @param expression the constant
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the constant
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			Constant expression,
			Object... params)
			throws SemanticException;

	/**
	 * Visits an {@link Identifier}.
	 * 
	 * @param expression the identifier
	 * @param params     the additional parameters provided to
	 *                       {@link SymbolicExpression#accept(ExpressionVisitor, Object...)},
	 *                       if any
	 * 
	 * @return the value produced by visiting the identifier
	 * 
	 * @throws SemanticException if an error occurs during the visit operation
	 */
	T visit(
			Identifier expression,
			Object... params)
			throws SemanticException;

}
