package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An interprocedural analysis based on a call graph.
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public abstract class CallGraphBasedAnalysis<A extends AbstractLattice<A>,
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
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			Analysis<A, D> analysis)
			throws InterproceduralAnalysisException {
		this.callgraph = callgraph;
		this.app = app;
		this.policy = policy;
		this.analysis = analysis;
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
		AnalysisState<A> st = entryState.bottom();
		StatementStore<A> store = new StatementStore<>(st);

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

}
