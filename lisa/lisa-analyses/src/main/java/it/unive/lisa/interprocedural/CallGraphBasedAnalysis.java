package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A base class for {@link InterproceduralAnalysis} implementations that resolve
 * calls through a {@link CallGraph} (see
 * {@link #resolve(UnresolvedCall, Set[], SymbolAliasing)}) and analyze
 * {@link CFGCall}s by propagating the caller's state into the callee:
 * subclasses only need to provide the actual fixpoint strategy (e.g.,
 * context-insensitive, k-depth context-sensitive, or inlining-based), while
 * this class provides the common machinery for preparing entry states (scoping
 * visible variables and binding actual to formal parameters, see
 * {@link #prepareEntryState}) and for evaluating {@link OpenCall}s through the
 * configured {@link OpenCallPolicy}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public abstract class CallGraphBasedAnalysis<
		A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		implements
		InterproceduralAnalysis<A, D> {

	/**
	 * The call graph used to resolve method calls.
	 */
	protected CallGraph callgraph;

	/**
	 * The application.
	 */
	protected Application app;

	/**
	 * The policy to evaluate results of open calls.
	 */
	protected OpenCallPolicy policy;

	/**
	 * The analysis that is being run.
	 */
	protected Analysis<A, D> analysis;

	/**
	 * The event queue for the analysis.
	 */
	protected EventQueue events;

	/**
	 * Builds the analysis.
	 */
	protected CallGraphBasedAnalysis() {
	}

	/**
	 * Builds the analysis by copying the given one.
	 * 
	 * @param other the original analysis to copy
	 */
	protected CallGraphBasedAnalysis(
			CallGraphBasedAnalysis<A, D> other) {
		this.callgraph = other.callgraph;
		this.app = other.app;
		this.policy = other.policy;
		this.analysis = other.analysis;
		this.events = other.events;
	}

	@Override
	public boolean needsCallGraph() {
		return true;
	}

	@Override
	public Analysis<A, D> getAnalysis() {
		return analysis;
	}

	@Override
	public EventQueue getEventQueue() {
		return events;
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			EventQueue events,
			Analysis<A, D> analysis)
			throws InterproceduralAnalysisException {
		this.callgraph = callgraph;
		this.app = app;
		this.policy = policy;
		this.analysis = analysis;
		this.events = events;
	}

	@Override
	public Call resolve(
			UnresolvedCall call,
			Set<Type>[] types,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		return callgraph.resolve(call, types, aliasing);
	}

	/**
	 * Prepare and entry state for the analysis of a method by renaming
	 * parameters.
	 * 
	 * @param entryState the initial entry state
	 * @param cfg        the CFG of the method
	 * 
	 * @return the entry state with the right parameter binding
	 * 
	 * @throws SemanticException if the analysis fails
	 */
	public AnalysisState<A> prepareEntryStateOfEntryPoint(
			AnalysisState<A> entryState,
			CFG cfg)
			throws SemanticException {
		AnalysisState<A> prepared = entryState;
		StatementStore<A> store = new StatementStore<>(entryState.bottom());

		for (Parameter arg : cfg.getDescriptor().getFormals()) {
			CodeLocation loc = arg.getLocation();
			Assignment a = new Assignment(
					cfg,
					loc,
					new VariableRef(cfg, loc, arg.getName()),
					arg.getStaticType().unknownValue(cfg, loc));
			prepared = a.forwardSemantics(prepared, this, store);
		}

		// the stack has to be empty
		return prepared.withExecutionExpressions(new ExpressionSet());
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			OpenCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		return policy.apply(call, entryState, analysis, parameters);
	}

	/**
	 * Whether or not this analysis can avoid computing a fixpoint for the given
	 * cfg when it is invoked by a call, and shortcut to the result for the same
	 * token if it exists and if it was produced with a greater entry state.
	 *
	 * @param cfg the cfg under evaluation
	 * 
	 * @return {@code true} if that condition holds (defaults to {@code true})
	 */
	protected boolean canShortcut(
			CFG cfg) {
		return true;
	}

	/**
	 * Whether or not this analysis should look for recursions when evaluating
	 * calls, immediately returning bottom when one is found.
	 * 
	 * @return {@code true} if that condition holds (defaults to {@code true})
	 */
	protected boolean shouldCheckForRecursions() {
		return true;
	}

	/**
	 * Whether or not this analysis should store the results of fixpoint
	 * executions for them to be returned as part of
	 * {@link #getFixpointResults()}.
	 * 
	 * @return {@code true} if that condition holds (defaults to {@code true})
	 */
	protected boolean shouldStoreFixpointResults() {
		return true;
	}

	/**
	 * Prepares the entry state for a call, by scoping the visible variables and
	 * assigning the parameters between the caller and the callee contexts.
	 *
	 * @param call        the call to prepare the entry state for
	 * @param entryState  the entry state of the call
	 * @param parameters  the actual parameters of the call
	 * @param expressions the statement store of the call
	 * @param scope       the scope of the call
	 * @param cfg         the target of the call
	 * 
	 * @return a pair whose left element is the prepared entry state, and whose
	 *             right element is the set of local variables of the callee
	 * 
	 * @throws SemanticException if something goes wrong during the preparation
	 *                               of the entry state
	 */
	protected Pair<AnalysisState<A>, ExpressionSet[]> prepareEntryState(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions,
			ScopeToken scope,
			CFG cfg)
			throws SemanticException {
		Parameter[] formals = cfg.getDescriptor().getFormals();

		// prepare the state for the call: hide the visible variables
		Pair<AnalysisState<A>,
				ExpressionSet[]> scoped = call.getProgram()
						.getFeatures()
						.getScopingStrategy()
						.scope(call, scope, entryState, analysis, parameters);
		AnalysisState<A> callState = scoped.getLeft();
		ExpressionSet[] locals = scoped.getRight();

		// assign parameters between the caller and the callee contexts
		ParameterAssigningStrategy strategy = call.getProgram().getFeatures().getAssigningStrategy();
		Pair<AnalysisState<A>,
				ExpressionSet[]> prepared = strategy.prepare(call, callState, this, expressions, formals, locals);
		return prepared;
	}

}
