package it.unive.lisa.cfg.statement;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ExpressionStore;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * A call to another procedure. This concrete instance of this class determines
 * whether this class represent a true call to another CFG (either in or out of
 * the analysis), or if it represents the invocation of one of the native
 * constructs of the language.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Call extends Expression {

	/**
	 * The parameters of this call
	 */
	private final Expression[] parameters;

	/**
	 * Builds a call happening at the given source location.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this expression happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param parameters the parameters of this call
	 * @param staticType the static type of this call
	 */
	protected Call(CFG cfg, String sourceFile, int line, int col, Type staticType, Expression... parameters) {
		super(cfg, sourceFile, line, col, staticType);
		Objects.requireNonNull(parameters, "The array of parameters of a call cannot be null");
		for (int i = 0; i < parameters.length; i++)
			Objects.requireNonNull(parameters[i], "The " + i + "-th parameter of a call cannot be null");
		this.parameters = parameters;
	}

	/**
	 * Yields the parameters of this call.
	 * 
	 * @return the parameters of this call
	 */
	public final Expression[] getParameters() {
		return parameters;
	}

	@Override
	public final int setOffset(int offset) {
		this.offset = offset;
		int off = offset;
		for (Expression param : parameters)
			off = param.setOffset(off + 1);
		return off;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(parameters);
		return result;
	}

	@Override
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (getClass() != st.getClass())
			return false;
		if (!super.isEqualTo(st))
			return false;
		Call other = (Call) st;
		if (!areEquals(parameters, other.parameters))
			return false;
		return true;
	}

	private static boolean areEquals(Expression[] params, Expression[] otherParams) {
		if (params == otherParams)
			return true;

		if (params == null || otherParams == null)
			return false;

		int length = params.length;
		if (otherParams.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (!isEqualTo(params[i], otherParams[i]))
				return false;

		return true;
	}

	private static boolean isEqualTo(Expression a, Expression b) {
		return (a == b) || (a != null && a.isEqualTo(b));
	}

	/**
	 * Semantics of a call statement is evaluated by computing the semantics of
	 * its parameters, from left to right, using the analysis state from each
	 * parameter's computation as entry state for the next one. Then, the
	 * semantics of the call itself is evaluated. <br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> semantics(
			AnalysisState<H, V> entryState, CallGraph callGraph, ExpressionStore<AnalysisState<H, V>> expressions)
			throws SemanticException {
		@SuppressWarnings("unchecked")
		Collection<SymbolicExpression>[] computed = new Collection[parameters.length];

		AnalysisState<H, V> current = entryState;
		for (int i = 0; i < computed.length; i++) {
			current = parameters[i].semantics(current, callGraph, expressions);
			expressions.put(parameters[i], current);
			computed[i] = current.getComputedExpressions();
		}

		AnalysisState<H, V> result = callSemantics(current, callGraph, computed);
		for (Expression param : parameters)
			if (!param.getMetaVariables().isEmpty())
				result = result.forgetIdentifiers(param.getMetaVariables());
		return result;
	}

	/**
	 * Computes the semantics of the call, after the semantics of all parameters
	 * have been computed. Meta variables from the parameters will be forgotten
	 * after this call returns.
	 * 
	 * @param <H>           the type of the heap analysis
	 * @param <V>           the type of the value analysis
	 * @param computedState the entry state that has been computed by chaining
	 *                          the parameters' semantics evaluation
	 * @param callGraph     the call graph of the program to analyze
	 * @param params        the symbolic expressions representing the computed
	 *                          values of the parameters of this call
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
			throws SemanticException;

	@Override
	public final <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> typeInference(
			AnalysisState<H, TypeEnvironment> entryState, CallGraph callGraph,
			ExpressionStore<AnalysisState<H, TypeEnvironment>> expressions) throws SemanticException {
		@SuppressWarnings("unchecked")
		Collection<SymbolicExpression>[] computed = new Collection[parameters.length];

		AnalysisState<H, TypeEnvironment> current = entryState;
		for (int i = 0; i < computed.length; i++) {
			current = parameters[i].typeInference(current, callGraph, expressions);
			expressions.put(parameters[i], current);
			computed[i] = current.getComputedExpressions();
		}

		AnalysisState<H, TypeEnvironment> result = callSemantics(current, callGraph, computed);
		for (Expression param : parameters)
			if (!param.getMetaVariables().isEmpty())
				result = result.forgetIdentifiers(param.getMetaVariables());
		return result;
	}

	/**
	 * Infer the types of the call, after the types of all parameters have been
	 * inferred. Meta variables from the parameters will be forgotten after this
	 * call returns.
	 * 
	 * @param <H>           the type of the heap analysis
	 * @param computedState the entry state that has been computed by chaining
	 *                          the parameters' type inference
	 * @param callGraph     the call graph of the program to analyze
	 * @param params        the symbolic expressions representing the computed
	 *                          values of the parameters of this call
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> callTypeInference(
			AnalysisState<H, TypeEnvironment> computedState, CallGraph callGraph,
			Collection<SymbolicExpression>[] params)
			throws SemanticException;
}
