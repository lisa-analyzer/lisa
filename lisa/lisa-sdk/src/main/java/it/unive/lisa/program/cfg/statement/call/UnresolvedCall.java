package it.unive.lisa.program.cfg.statement.call;

import java.util.Objects;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.assignment.ParameterAssigningStrategy;
import it.unive.lisa.program.cfg.statement.call.assignment.PythonLikeAssigningStrategy;
import it.unive.lisa.program.cfg.statement.call.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.cfg.statement.call.traversal.HierarcyTraversalStrategy;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * A call that happens inside the program to analyze. At this stage, the
 * target(s) of the call is (are) unknown. During the semantic computation, the
 * {@link CallGraph} used by the analysis will resolve this to a {@link CFGCall}
 * or to an {@link OpenCall}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UnresolvedCall extends Call {

	/**
	 * The {@link ParameterMatchingStrategy} of the parameters of this call
	 */
	private final ParameterMatchingStrategy matchingStrategy;

	/**
	 * The {@link HierarcyTraversalStrategy} of the parameters of this call
	 */
	private final HierarcyTraversalStrategy traversalStrategy;

	/**
	 * Builds the unresolved call, happening at the given location in the
	 * program. The static type of this call is {@link Untyped}. The
	 * {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg               the cfg that this expression belongs to
	 * @param location          the location where the expression is defined
	 *                              within the program
	 * @param assigningStrategy the {@link ParameterAssigningStrategy} of the
	 *                              parameters of this call
	 * @param matchingStrategy  the {@link ParameterMatchingStrategy} of the
	 *                              parameters of this call
	 * @param traversalStrategy the {@link HierarcyTraversalStrategy} of this
	 *                              call
	 * @param instanceCall      whether or not this is a call to an instance
	 *                              method of a unit (that can be overridden) or
	 *                              not
	 * @param qualifier         the optional qualifier of the call (can be null
	 *                              or empty - see {@link #getFullTargetName()}
	 *                              for more info)
	 * @param targetName        the name of the target of this call
	 * @param parameters        the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, CodeLocation location, ParameterAssigningStrategy assigningStrategy,
			ParameterMatchingStrategy matchingStrategy, HierarcyTraversalStrategy traversalStrategy,
			boolean instanceCall, String qualifier, String targetName, Expression... parameters) {
		this(cfg, location, assigningStrategy, matchingStrategy, traversalStrategy, instanceCall, qualifier, targetName,
				Untyped.INSTANCE, parameters);
	}

	/**
	 * Builds the unresolved call, happening at the given location in the
	 * program. The static type of this call is {@link Untyped}. The
	 * {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg               the cfg that this expression belongs to
	 * @param location          the location where the expression is defined
	 *                              within the program
	 * @param matchingStrategy  the {@link ParameterMatchingStrategy} of the
	 *                              parameters of this call
	 * @param traversalStrategy the {@link HierarcyTraversalStrategy} of this
	 *                              call
	 * @param instanceCall      whether or not this is a call to an instance
	 *                              method of a unit (that can be overridden) or
	 *                              not
	 * @param qualifier         the optional qualifier of the call (can be null
	 *                              or empty - see {@link #getFullTargetName()}
	 *                              for more info)
	 * @param targetName        the name of the target of this call
	 * @param parameters        the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, CodeLocation location, ParameterMatchingStrategy matchingStrategy,
			HierarcyTraversalStrategy traversalStrategy, boolean instanceCall, String qualifier, String targetName,
			Expression... parameters) {
		this(cfg, location, PythonLikeAssigningStrategy.INSTANCE, matchingStrategy, traversalStrategy, instanceCall,
				qualifier,
				targetName, Untyped.INSTANCE, parameters);
	}

	/**
	 * Builds the unresolved call, happening at the given location in the
	 * program. The {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg               the cfg that this expression belongs to
	 * @param location          the location where the expression is defined
	 *                              within the program
	 * @param assigningStrategy the {@link ParameterAssigningStrategy} of the
	 *                              parameters of this call
	 * @param matchingStrategy  the {@link ParameterMatchingStrategy} of the
	 *                              parameters of this call
	 * @param traversalStrategy the {@link HierarcyTraversalStrategy} of this
	 *                              call
	 * @param instanceCall      whether or not this is a call to an instance
	 *                              method of a unit (that can be overridden) or
	 *                              not
	 * @param qualifier         the optional qualifier of the call (can be null
	 *                              or empty - see {@link #getFullTargetName()}
	 *                              for more info)
	 * @param targetName        the name of the target of this call
	 * @param staticType        the static type of this call
	 * @param parameters        the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, CodeLocation location, ParameterAssigningStrategy assigningStrategy,
			ParameterMatchingStrategy matchingStrategy, HierarcyTraversalStrategy traversalStrategy,
			boolean instanceCall, String qualifier, String targetName, Type staticType, Expression... parameters) {
		this(cfg, location, assigningStrategy, matchingStrategy, traversalStrategy, instanceCall, qualifier, targetName,
				LeftToRightEvaluation.INSTANCE, staticType, parameters);
	}

	/**
	 * Builds the unresolved call, happening at the given location in the
	 * program. The {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg               the cfg that this expression belongs to
	 * @param location          the location where the expression is defined
	 *                              within the program
	 * @param matchingStrategy  the {@link ParameterMatchingStrategy} of the
	 *                              parameters of this call
	 * @param traversalStrategy the {@link HierarcyTraversalStrategy} of this
	 *                              call
	 * @param instanceCall      whether or not this is a call to an instance
	 *                              method of a unit (that can be overridden) or
	 *                              not
	 * @param qualifier         the optional qualifier of the call (can be null
	 *                              or empty - see {@link #getFullTargetName()}
	 *                              for more info)
	 * @param targetName        the name of the target of this call
	 * @param staticType        the static type of this call
	 * @param parameters        the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, CodeLocation location, ParameterMatchingStrategy matchingStrategy,
			HierarcyTraversalStrategy traversalStrategy, boolean instanceCall, String qualifier, String targetName,
			Type staticType, Expression... parameters) {
		this(cfg, location, PythonLikeAssigningStrategy.INSTANCE, matchingStrategy, traversalStrategy, instanceCall,
				qualifier,
				targetName, LeftToRightEvaluation.INSTANCE, staticType, parameters);
	}

	/**
	 * Builds the unresolved call, happening at the given location in the
	 * program. The static type of this call is {@link Untyped}.
	 * 
	 * @param cfg               the cfg that this expression belongs to
	 * @param location          the location where the expression is defined
	 *                              within the program
	 * @param assigningStrategy the {@link ParameterAssigningStrategy} of the
	 *                              parameters of this call
	 * @param matchingStrategy  the {@link ParameterMatchingStrategy} of the
	 *                              parameters of this call
	 * @param traversalStrategy the {@link HierarcyTraversalStrategy} of this
	 *                              call
	 * @param instanceCall      whether or not this is a call to an instance
	 *                              method of a unit (that can be overridden) or
	 *                              not
	 * @param qualifier         the optional qualifier of the call (can be null
	 *                              or empty - see {@link #getFullTargetName()}
	 *                              for more info)
	 * @param targetName        the name of the target of this call
	 * @param order             the evaluation order of the sub-expressions
	 * @param parameters        the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, CodeLocation location, ParameterAssigningStrategy assigningStrategy,
			ParameterMatchingStrategy matchingStrategy, HierarcyTraversalStrategy traversalStrategy,
			boolean instanceCall, String qualifier, String targetName,
			EvaluationOrder order, Expression... parameters) {
		this(cfg, location, assigningStrategy, matchingStrategy, traversalStrategy, instanceCall, qualifier, targetName,
				order, Untyped.INSTANCE, parameters);
	}

	/**
	 * Builds the unresolved call, happening at the given location in the
	 * program. The static type of this call is {@link Untyped}.
	 * 
	 * @param cfg               the cfg that this expression belongs to
	 * @param location          the location where the expression is defined
	 *                              within the program
	 * @param matchingStrategy  the {@link ParameterMatchingStrategy} of the
	 *                              parameters of this call
	 * @param traversalStrategy the {@link HierarcyTraversalStrategy} of this
	 *                              call
	 * @param instanceCall      whether or not this is a call to an instance
	 *                              method of a unit (that can be overridden) or
	 *                              not
	 * @param qualifier         the optional qualifier of the call (can be null
	 *                              or empty - see {@link #getFullTargetName()}
	 *                              for more info)
	 * @param targetName        the name of the target of this call
	 * @param order             the evaluation order of the sub-expressions
	 * @param parameters        the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, CodeLocation location, ParameterMatchingStrategy matchingStrategy,
			HierarcyTraversalStrategy traversalStrategy, boolean instanceCall, String qualifier, String targetName,
			EvaluationOrder order, Expression... parameters) {
		this(cfg, location, PythonLikeAssigningStrategy.INSTANCE, matchingStrategy, traversalStrategy, instanceCall,
				qualifier,
				targetName, order, Untyped.INSTANCE, parameters);
	}

	/**
	 * Builds the unresolved call, happening at the given location in the
	 * program.
	 * 
	 * @param cfg               the cfg that this expression belongs to
	 * @param location          the location where the expression is defined
	 *                              within the program
	 * @param assigningStrategy the {@link ParameterAssigningStrategy} of the
	 *                              parameters of this call
	 * @param matchingStrategy  the {@link ParameterMatchingStrategy} of the
	 *                              parameters of this call
	 * @param traversalStrategy the {@link HierarcyTraversalStrategy} of this
	 *                              call
	 * @param instanceCall      whether or not this is a call to an instance
	 *                              method of a unit (that can be overridden) or
	 *                              not
	 * @param qualifier         the optional qualifier of the call (can be null
	 *                              or empty - see {@link #getFullTargetName()}
	 *                              for more info)
	 * @param targetName        the name of the target of this call
	 * @param order             the evaluation order of the sub-expressions
	 * @param staticType        the static type of this call
	 * @param parameters        the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, CodeLocation location, ParameterAssigningStrategy assigningStrategy,
			ParameterMatchingStrategy matchingStrategy, HierarcyTraversalStrategy traversalStrategy,
			boolean instanceCall, String qualifier, String targetName, EvaluationOrder order, Type staticType,
			Expression... parameters) {
		super(cfg, location, assigningStrategy, instanceCall, qualifier, targetName, order, staticType, parameters);
		Objects.requireNonNull(matchingStrategy, "The matching strategy of an unresolved call cannot be null");
		Objects.requireNonNull(traversalStrategy, "The traversal strategy of an unresolved call cannot be null");
		this.matchingStrategy = matchingStrategy;
		this.traversalStrategy = traversalStrategy;
	}

	/**
	 * Yields the {@link ParameterMatchingStrategy} of the parameters of this
	 * call.
	 * 
	 * @return the matching strategy
	 */
	public ParameterMatchingStrategy getMatchingStrategy() {
		return matchingStrategy;
	}

	/**
	 * Yields the {@link HierarcyTraversalStrategy} of this call.
	 * 
	 * @return the traversal strategy
	 */
	public HierarcyTraversalStrategy getTraversalStrategy() {
		return traversalStrategy;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((matchingStrategy == null) ? 0 : matchingStrategy.hashCode());
		result = prime * result + ((traversalStrategy == null) ? 0 : traversalStrategy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof UnresolvedCall))
			return false;
		UnresolvedCall other = (UnresolvedCall) obj;
		if (matchingStrategy == null) {
			if (other.matchingStrategy != null)
				return false;
		} else if (!matchingStrategy.equals(other.matchingStrategy))
			return false;
		if (traversalStrategy == null) {
			if (other.traversalStrategy != null)
				return false;
		} else if (!traversalStrategy.equals(other.traversalStrategy))
			return false;
		return true;
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>, T extends TypeDomain<T>> AnalysisState<A, H, V, T> expressionSemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					ExpressionSet<SymbolicExpression>[] params,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		Call resolved;
		try {
			resolved = interprocedural.resolve(this);
		} catch (CallResolutionException e) {
			throw new SemanticException("Unable to resolve call " + this, e);
		}
		AnalysisState<A, H, V, T> result = resolved.expressionSemantics(interprocedural, state, params, expressions);
		getMetaVariables().addAll(resolved.getMetaVariables());
		return result;
	}
}
