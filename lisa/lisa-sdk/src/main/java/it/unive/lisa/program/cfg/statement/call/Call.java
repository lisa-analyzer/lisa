package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.Arrays;
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
	 * The original {@link UnresolvedCall} that has been resolved to this one
	 */
	private UnresolvedCall originating = null;

	/**
	 * Builds a call happening at the given source location.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where the expression is defined within the
	 *                       source file. If unknown, use {@code null}
	 * @param parameters the parameters of this call
	 * @param staticType the static type of this call
	 */
	protected Call(CFG cfg, CodeLocation location, Type staticType, Expression... parameters) {
		super(cfg, location, staticType);
		Objects.requireNonNull(parameters, "The array of parameters of a call cannot be null");
		for (int i = 0; i < parameters.length; i++)
			Objects.requireNonNull(parameters[i], "The " + i + "-th parameter of a call cannot be null");
		this.parameters = parameters;
		for (Expression param : parameters)
			param.setParentStatement(this);
	}

	/**
	 * Yields the parameters of this call.
	 * 
	 * @return the parameters of this call
	 */
	public final Expression[] getParameters() {
		return parameters;
	}

	/**
	 * Yields the call that this call originated from, if any. A call <i>r</i>
	 * originates from a call <i>u</i> if:
	 * <ul>
	 * <li><i>u</i> is an {@link UnresolvedCall}, while <i>r</i> is not,
	 * and</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to <i>r</i>, or</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to a call <i>c</i> (e.g. a
	 * {@link HybridCall}), and its semantics generated the call <i>u</i></li>
	 * </ul>
	 * 
	 * @return the call that this one originated from
	 */
	public final UnresolvedCall getOriginating() {
		return originating;
	}

	/**
	 * Sets the call that this call originated from. A call <i>r</i> originates
	 * from a call <i>u</i> if:
	 * <ul>
	 * <li><i>u</i> is an {@link UnresolvedCall}, while <i>r</i> is not,
	 * and</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to <i>r</i>, or</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to a call <i>c</i> (e.g. a
	 * {@link HybridCall}), and its semantics generated the call <i>u</i></li>
	 * </ul>
	 * 
	 * @param originating the call that this one originated from
	 */
	public final void setOriginating(UnresolvedCall originating) {
		this.originating = originating;
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Call other = (Call) obj;
		if (!Arrays.equals(parameters, other.parameters))
			return false;
		return true;
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
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
					StatementStore<A, H, V> expressions)
					throws SemanticException {
		@SuppressWarnings("unchecked")
		ExpressionSet<SymbolicExpression>[] computed = new ExpressionSet[parameters.length];

		@SuppressWarnings("unchecked")
		AnalysisState<A, H, V>[] paramStates = new AnalysisState[parameters.length];
		AnalysisState<A, H, V> preState = entryState;
		for (int i = 0; i < computed.length; i++) {
			preState = paramStates[i] = parameters[i].semantics(preState, interprocedural, expressions);
			expressions.put(parameters[i], paramStates[i]);
			computed[i] = paramStates[i].getComputedExpressions();
		}

		AnalysisState<A, H, V> result = callSemantics(entryState, interprocedural, paramStates, computed);

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
	 * @param <A>             the type of {@link AbstractState}
	 * @param <H>             the type of the {@link HeapDomain}
	 * @param <V>             the type of the {@link ValueDomain}
	 * @param entryState      the entry state of this call
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param computedStates  the array of states chaining the parameters'
	 *                            semantics evaluation starting from
	 *                            {@code entryState}, namely
	 *                            {@code computedState[i]} corresponds to the
	 *                            state obtained by the evaluation of
	 *                            {@code params[i]} in the state
	 *                            {@code computedState[i-1]} ({@code params[0]}
	 *                            is evaluated in {@code entryState})
	 * @param params          the symbolic expressions representing the computed
	 *                            values of the parameters of this call
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState,
					InterproceduralAnalysis<A, H, V> interprocedural, AnalysisState<A, H, V>[] computedStates,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException;

	@Override
	public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		for (Expression par : parameters)
			if (!par.accept(visitor, tool))
				return false;
		return visitor.visit(tool, getCFG(), this);
	}
}
