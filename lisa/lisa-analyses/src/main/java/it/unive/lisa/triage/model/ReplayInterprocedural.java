package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * The {@link InterproceduralAnalysis} used to replay the semantics of the
 * statements of one analysis result (an {@link AnalyzedCFG}, i.e., a cfg in a
 * given context) through a {@link ReplayDomain}. No fixpoint is ever computed:
 * <ul>
 * <li>calls to cfgs are <b>summarized</b>: their result is an unknown value (a
 * {@link SummaryMarker}), and the heap is unconstrained after them; errors
 * raised by the callees, and executions halting inside them, are taken from the
 * results of the analysis, with all values unconstrained;</li>
 * <li>open calls are evaluated through the {@link OpenCallPolicy} of the
 * analysis, so that the unknown values they return become inputs of the
 * program;</li>
 * <li>calls are resolved by the interprocedural analysis that produced the
 * results, so that resolution matches the one of the analysis;</li>
 * <li>the current context is the one of the replayed result.</li>
 * </ul>
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of the abstract states of the analysis
 * @param <D> the type of the domain of the analysis
 */
public class ReplayInterprocedural<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		InterproceduralAnalysis<ReplayState<A>, ReplayDomain<A, D>> {

	private final AnalyzedCFG<A> result;

	private final Analysis<ReplayState<A>, ReplayDomain<A, D>> analysis;

	private final InterproceduralAnalysis<A, D> original;

	private final OpenCallPolicy policy;

	/**
	 * Builds the analysis.
	 *
	 * @param tool   the tool containing the results of the analysis
	 * @param result the result whose statements are replayed
	 * @param domain the domain used for the replay
	 */
	@SuppressWarnings("unchecked")
	public ReplayInterprocedural(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			ReplayDomain<A, D> domain) {
		this.result = result;
		this.analysis = new Analysis<>(domain, tool.getConfiguration().shouldSmashError);
		this.original = (InterproceduralAnalysis<A, D>) tool.getConfiguration().interproceduralAnalysis;
		this.policy = tool.getConfiguration().openCallPolicy;
	}

	/**
	 * Yields the result whose statements are replayed.
	 *
	 * @return the result
	 */
	public AnalyzedCFG<A> getResult() {
		return result;
	}

	@Override
	public boolean needsCallGraph() {
		return false;
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			EventQueue events,
			Analysis<ReplayState<A>, ReplayDomain<A, D>> analysis) {
		throw new UnsupportedOperationException("Replays do not need initialization");
	}

	@Override
	public Analysis<ReplayState<A>, ReplayDomain<A, D>> getAnalysis() {
		return analysis;
	}

	@Override
	public EventQueue getEventQueue() {
		// replays are not part of the analysis
		return null;
	}

	@Override
	public void fixpoint(
			AnalysisState<ReplayState<A>> entryState,
			FixpointConfiguration<ReplayState<A>, ReplayDomain<A, D>> conf)
			throws FixpointException {
		throw new UnsupportedOperationException("Replays do not compute fixpoints");
	}

	@Override
	public Collection<AnalyzedCFG<ReplayState<A>>> getAnalysisResultsOf(
			CFG cfg) {
		throw new UnsupportedOperationException("Replays do not produce results");
	}

	@Override
	public AnalysisState<ReplayState<A>> getAbstractResultOf(
			CFGCall call,
			AnalysisState<ReplayState<A>> entryState,
			ExpressionSet[] parameters,
			StatementStore<ReplayState<A>> expressions)
			throws SemanticException {
		ScopeId<ReplayState<A>> context = getCurrentContext();
		String reason = "result of call to " + call.getFullTargetName();
		AnalysisState<ReplayState<A>> callres;
		if (call.getStaticType().isVoidType())
			callres = analysis.smallStepSemantics(entryState, new Skip(call.getLocation()), context, call);
		else
			// as done by open call policies: the returned value is assigned
			// to the meta variable of the call
			callres = analysis.assign(
					entryState,
					call.getMetaVariable(),
					new SummaryMarker(call.getStaticType(), reason, call.getLocation()),
					context,
					call);
		// the callees might have written the heap
		callres = callres.withExecution(mapState(callres.getExecution(), s -> s.withStore(s.getStore().heapHavoc())));

		// errors raised and halts reached inside the callees, as computed by
		// the analysis, with all values unconstrained
		AnalysisState<A> post = result.getAnalysisStateAfter(call.getSource() == null ? call : call.getSource());
		SymbolicStore lost = entryState.getExecutionState().getStore().havocAll();
		// errors raised by calls are attributed to the call in the program
		Call thrower = call;
		while (thrower.getSource() != null)
			thrower = thrower.getSource();
		GenericMapLattice<Error, ProgramState<A>> errors = post.getErrors();
		if (!errors.isBottom() && !errors.isTop())
			for (Error error : errors.getKeys())
				if (error.getThrower().equals(thrower) && !errors.getState(error).isBottom())
					callres = callres.addError(error, lift(errors.getState(error), lost));
		GenericMapLattice<Type, GenericSetLattice<Statement>> smashed = post.getSmashedErrors();
		if (!smashed.isBottom() && !smashed.isTop() && !post.getSmashedErrorsState().isBottom())
			for (Type type : smashed.getKeys())
				if (smashed.getState(type).contains(thrower))
					callres = callres.addError(new Error(type, thrower), lift(post.getSmashedErrorsState(), lost));
		if (!post.getHalt().isBottom())
			callres = callres.withHalt(callres.getHalt().lub(lift(post.getHalt(), lost)));
		return callres;
	}

	private ProgramState<ReplayState<A>> lift(
			ProgramState<A> state,
			SymbolicStore store) {
		return new ProgramState<>(
				new ReplayState<>(state.getState(), store),
				state.getComputedExpressions(),
				state.getFixpointInformation());
	}

	private static <A extends AbstractLattice<A>> ProgramState<ReplayState<A>> mapState(
			ProgramState<ReplayState<A>> state,
			UnaryOperator<ReplayState<A>> f) {
		if (state.isBottom())
			return state;
		return new ProgramState<>(f.apply(state.getState()), state.getComputedExpressions(),
				state.getFixpointInformation());
	}

	@Override
	public AnalysisState<ReplayState<A>> getAbstractResultOf(
			OpenCall call,
			AnalysisState<ReplayState<A>> entryState,
			ExpressionSet[] parameters,
			StatementStore<ReplayState<A>> expressions)
			throws SemanticException {
		return policy.apply(call, entryState, analysis, getCurrentContext(), parameters);
	}

	@Override
	public Call resolve(
			UnresolvedCall call,
			Set<Type>[] types,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		return original.resolve(call, types, aliasing);
	}

	@Override
	public FixpointResults<ReplayState<A>> getFixpointResults() {
		throw new UnsupportedOperationException("Replays do not produce results");
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ScopeId<ReplayState<A>> getCurrentContext() {
		// the context of the replayed result is used as-is, so that the
		// abstract states see the same context they saw during the analysis
		return (ScopeId) result.getId();
	}
}
