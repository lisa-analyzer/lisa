package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.MultiCall;
import it.unive.lisa.program.cfg.statement.call.NativeCall;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.TruncatedParamsCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.VoidType;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The definition of interprocedural analyses.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 */
public interface InterproceduralAnalysis<A extends AbstractState<A>> {

	/**
	 * Initializes the interprocedural analysis of the given program. A call to
	 * this method should effectively re-initialize the interprocedural analysis
	 * as if it is yet to be used. This is useful when the same instance is used
	 * in multiple analyses.
	 *
	 * @param callgraph the callgraph used to resolve method calls
	 * @param app       the application to analyze
	 * @param policy    the {@link OpenCallPolicy} to be used for computing the
	 *                      result of {@link OpenCall}s
	 *
	 * @throws InterproceduralAnalysisException if an exception happens while
	 *                                              performing the
	 *                                              interprocedural analysis
	 */
	void init(Application app, CallGraph callgraph, OpenCallPolicy policy) throws InterproceduralAnalysisException;

	/**
	 * Computes a fixpoint over the whole control flow graph, producing a
	 * {@link AnalyzedCFG} for each {@link CFG} contained in this analysis. Each
	 * result is computed with
	 * {@link CFG#fixpoint(AnalysisState, InterproceduralAnalysis, WorkingSet, FixpointConfiguration, ScopeId)}
	 * or one of its overloads. Results of individual cfgs are then available
	 * through {@link #getAnalysisResultsOf(CFG)}.
	 * 
	 * @param entryState         the entry state for the {@link CFG}s that are
	 *                               the entrypoints of the computation
	 * @param fixpointWorkingSet the concrete class of {@link WorkingSet} to be
	 *                               used in fixpoints.
	 * @param conf               the {@link FixpointConfiguration} containing
	 *                               the parameters tuning fixpoint behavior
	 * 
	 * @throws FixpointException if something goes wrong while evaluating the
	 *                               fixpoint
	 */
	void fixpoint(AnalysisState<A> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			FixpointConfiguration conf)
			throws FixpointException;

	/**
	 * Yields the results of the given analysis, identified by its class, on the
	 * given {@link CFG}. Results are provided as {@link AnalyzedCFG}.
	 * 
	 * @param cfg the cfg whose fixpoint results needs to be retrieved
	 *
	 * @return the result of the fixpoint computation of {@code valueDomain}
	 *             over {@code cfg}
	 */
	Collection<AnalyzedCFG<A>> getAnalysisResultsOf(CFG cfg);

	/**
	 * Computes an analysis state that abstracts the execution of the possible
	 * targets considering that they were given {@code parameters} as actual
	 * parameters, and the state when the call is executed is
	 * {@code entryState}.<br>
	 * <br>
	 * Note that the interprocedural analysis is also responsible for
	 * registering the call to the {@link CallGraph}, if needed.
	 *
	 * @param call        the call to evaluate
	 * @param entryState  the abstract analysis state when the call is reached
	 * @param parameters  the expressions representing the actual parameters of
	 *                        the call
	 * @param expressions the cache where analysis states of intermediate
	 *                        expressions must be stored
	 *
	 * @return an abstract analysis state representing the abstract result of
	 *             the cfg call. The
	 *             {@link AnalysisState#getComputedExpressions()} will contain
	 *             an {@link Identifier} pointing to the meta variable
	 *             containing the abstraction of the returned value, if any
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A> expressions)
			throws SemanticException;

	/**
	 * Computes an analysis state that abstracts the execution of an unknown
	 * target considering that they were given {@code parameters} as actual
	 * parameters, and the state when the call is executed is
	 * {@code entryState}.
	 *
	 * @param call        the call to evaluate
	 * @param entryState  the abstract analysis state when the call is reached
	 * @param parameters  the expressions representing the actual parameters of
	 *                        the call
	 * @param expressions the cache where analysis states of intermediate
	 *                        expressions must be stored
	 *
	 * @return an abstract analysis state representing the abstract result of
	 *             the open call. The
	 *             {@link AnalysisState#getComputedExpressions()} will contain
	 *             an {@link Identifier} pointing to the meta variable
	 *             containing the abstraction of the returned value, if any
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	AnalysisState<A> getAbstractResultOf(
			OpenCall call,
			AnalysisState<A> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A> expressions)
			throws SemanticException;

	/**
	 * Yields a {@link Call} implementation that corresponds to the resolution
	 * of the given {@link UnresolvedCall}. This method will forward the call to
	 * {@link CallGraph#resolve(UnresolvedCall, Set[], SymbolAliasing)} if
	 * needed.
	 *
	 * @param call     the call to resolve
	 * @param types    the runtime types of the parameters of the call
	 * @param aliasing the symbol aliasing information
	 * 
	 * @return a collection of all the possible runtime targets
	 *
	 * @throws CallResolutionException if this analysis is unable to resolve the
	 *                                     given call
	 */
	Call resolve(UnresolvedCall call, Set<Type>[] types, SymbolAliasing aliasing) throws CallResolutionException;

	/**
	 * Yields the results of the fixpoint computation over the whole
	 * application.
	 * 
	 * @return the results of the fixpoint
	 */
	FixpointResults<A> getFixpointResults();

	/**
	 * Converts the pre-state of {@code call} to a valid entry state for one of
	 * its targets. Specifically, the state returned by this method corresponds
	 * to the given one modified by (i) pushing the scope that is introduced
	 * when the call happens (and that can be popped with
	 * {@link #unscope(CFGCall, ScopeToken, AnalysisState)}), and (ii)
	 * generating the expressions for the formal parameters by pushing the same
	 * scope to the ones of the actual parameters.
	 * 
	 * @param scope   the scope corresponding to the call
	 * @param state   the exit state of the call's target
	 * @param actuals the expressions representing the actual parameters at the
	 *                    program point of the call
	 * 
	 * @return a pair containing the computed call state and the expressions
	 *             corresponding to the formal parameters
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	@SuppressWarnings("unchecked")
	default Pair<AnalysisState<A>, ExpressionSet<SymbolicExpression>[]> scope(
			AnalysisState<A> state,
			ScopeToken scope,
			ExpressionSet<SymbolicExpression>[] actuals)
			throws SemanticException {
		ExpressionSet<SymbolicExpression>[] locals = new ExpressionSet[actuals.length];
		AnalysisState<A> callState = state.pushScope(scope);
		for (int i = 0; i < actuals.length; i++)
			locals[i] = actuals[i].pushScope(scope);
		return Pair.of(callState, locals);
	}

	/**
	 * Converts the exit state of a cfg that was invoked by {@code call} to a
	 * valid post-state of {@code call}. Specifically, the state returned by
	 * this method corresponds to the given one modified by (i) popping the
	 * scope introduced before the call happened (see
	 * {@link #scope(AnalysisState, ScopeToken, ExpressionSet[])}), and (ii)
	 * storing the returned value on the meta-variable left on the stack, if
	 * any.
	 * 
	 * @param call  the call that caused the computation of the given state
	 *                  through a fixpoint computation
	 * @param scope the scope corresponding to the call
	 * @param state the exit state of the call's target
	 * 
	 * @return the state that can be returned by the call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	default AnalysisState<A> unscope(
			CFGCall call,
			ScopeToken scope,
			AnalysisState<A> state)
			throws SemanticException {
		if (returnsVoid(call, state))
			return state.popScope(scope);

		AnalysisState<A> tmp = state.bottom();
		Identifier meta = (Identifier) call.getMetaVariable().pushScope(scope);
		for (SymbolicExpression ret : state.getComputedExpressions())
			tmp = tmp.lub(state.assign(meta, ret, call));

		return tmp.popScope(scope);
	}

	/**
	 * Yields whether or if this call returned no value or its return type is
	 * {@link VoidType}. If this method returns {@code true}, then no value
	 * should be assigned to the call's meta variable.
	 * 
	 * @param call     the call
	 * @param returned the post-state of the call
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean returnsVoid(Call call, AnalysisState<A> returned) {
		if (call.getStaticType().isVoidType())
			return true;

		if (!call.getStaticType().isUntyped())
			return false;

		if (call instanceof CFGCall) {
			CFGCall cfgcall = (CFGCall) call;
			Collection<CFG> targets = cfgcall.getTargetedCFGs();
			if (!targets.isEmpty())
				return !targets.iterator()
						.next()
						.getNormalExitpoints()
						.stream()
						// returned values will be stored in meta variables
						.anyMatch(st -> st instanceof MetaVariableCreator);
		}

		if (call instanceof NativeCall) {
			NativeCall nativecall = (NativeCall) call;
			Collection<NativeCFG> targets = nativecall.getTargetedConstructs();
			if (!targets.isEmpty())
				// native cfgs will always rewrite to expressions and return a
				// value
				return false;
		}

		if (call instanceof TruncatedParamsCall)
			return returnsVoid(((TruncatedParamsCall) call).getInnerCall(), returned);

		if (call instanceof MultiCall) {
			MultiCall multicall = (MultiCall) call;
			Collection<Call> targets = multicall.getCalls();
			if (!targets.isEmpty())
				// we get the return type from one of its targets
				return returnsVoid(targets.iterator().next(), returned);
		}

		if (returned != null)
			if (returned.getComputedExpressions().isEmpty())
				return true;
			else if (returned.getComputedExpressions().size() == 1
					&& returned.getComputedExpressions().iterator().next() instanceof Skip)
				return true;

		return false;
	}
}
