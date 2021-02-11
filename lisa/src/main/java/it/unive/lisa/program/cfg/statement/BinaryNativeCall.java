package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;

/**
 * A {@link NativeCall} with a exactly two arguments.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class BinaryNativeCall extends NativeCall {

	/**
	 * Builds the untyped native call. The location where this call happens is
	 * unknown (i.e. no source file/line/column is available). The static type
	 * of this call is {@link Untyped}.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param left          the first parameter of this call
	 * @param right         the second parameter of this call
	 */
	protected BinaryNativeCall(CFG cfg, String constructName, Expression left, Expression right) {
		super(cfg, constructName, left, right);
	}

	/**
	 * Builds the native call. The location where this call happens is unknown
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param staticType    the static type of this call
	 * @param left          the first parameter of this call
	 * @param right         the second parameter of this call
	 */
	protected BinaryNativeCall(CFG cfg, String constructName, Type staticType, Expression left, Expression right) {
		super(cfg, constructName, staticType, left, right);
	}

	/**
	 * Builds the untyped native call, happening at the given location in the
	 * program. The static type of this call is {@link Untyped}.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param sourceFile    the source file where this expression happens. If
	 *                          unknown, use {@code null}
	 * @param line          the line number where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param col           the column where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param left          the first parameter of this call
	 * @param right         the second parameter of this call
	 */
	protected BinaryNativeCall(CFG cfg, String sourceFile, int line, int col, String constructName,
			Expression left, Expression right) {
		super(cfg, sourceFile, line, col, constructName, left, right);
	}

	/**
	 * Builds the native call, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param sourceFile    the source file where this expression happens. If
	 *                          unknown, use {@code null}
	 * @param line          the line number where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param col           the column where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param staticType    the static type of this call
	 * @param left          the first parameter of this call
	 * @param right         the second parameter of this call
	 */
	protected BinaryNativeCall(CFG cfg, String sourceFile, int line, int col, String constructName, Type staticType,
			Expression left, Expression right) {
		super(cfg, sourceFile, line, col, constructName, staticType, left, right);
	}

	@Override
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState,
					CallGraph callGraph, AnalysisState<A, H, V>[] computedStates,
					Collection<SymbolicExpression>[] params)
					throws SemanticException {
		AnalysisState<A, H, V> result = null;

		for (SymbolicExpression left : params[0])
			for (SymbolicExpression right : params[1]) {
				AnalysisState<A, H, V> tmp = binarySemantics(entryState, callGraph, computedStates[0], left,
						computedStates[1], right);
				if (result == null)
					result = tmp;
				else
					result = result.lub(tmp);
			}

		return result;
	}

	/**
	 * Computes the semantics of the call, after the semantics of the parameters
	 * have been computed. Meta variables from the parameters will be forgotten
	 * after this call returns.
	 * 
	 * @param <A>        the type of {@link AbstractState}
	 * @param <H>        the type of the {@link HeapDomain}
	 * @param <V>        the type of the {@link ValueDomain}
	 * @param entryState the entry state of this binary call
	 * @param callGraph  the call graph of the program to analyze
	 * @param leftState  the state obtained by evaluating {@code left} in
	 *                       {@code entryState}
	 * @param leftExp    the symbolic expression representing the computed value
	 *                       of the first parameter of this call
	 * @param rightState the state obtained by evaluating {@code right} in
	 *                       {@code leftState}
	 * @param rightExp   the symbolic expression representing the computed value
	 *                       of the second parameter of this call
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	protected abstract <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
					AnalysisState<A, H, V> entryState,
					CallGraph callGraph,
					AnalysisState<A, H, V> leftState, SymbolicExpression leftExp,
					AnalysisState<A, H, V> rightState, SymbolicExpression rightExp)
					throws SemanticException;
}
