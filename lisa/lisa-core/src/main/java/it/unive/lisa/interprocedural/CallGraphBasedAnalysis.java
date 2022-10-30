package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An interprocedural analysis based on a call graph.
 * 
 * @param <A> The abstract state of the analysis
 * @param <H> The heap domain
 * @param <V> The value domain
 * @param <T> The type domain
 */
public abstract class CallGraphBasedAnalysis<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>> implements InterproceduralAnalysis<A, H, V, T> {

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

	@Override
	public void init(Application app, CallGraph callgraph, OpenCallPolicy policy)
			throws InterproceduralAnalysisException {
		this.callgraph = callgraph;
		this.app = app;
		this.policy = policy;
	}

	@Override
	public Call resolve(UnresolvedCall call, Set<Type>[] types, SymbolAliasing aliasing)
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
	public AnalysisState<A, H, V, T> prepareEntryStateOfEntryPoint(AnalysisState<A, H, V, T> entryState, CFG cfg)
			throws SemanticException {
		AnalysisState<A, H, V, T> prepared = entryState;

		for (Parameter arg : cfg.getDescriptor().getFormals()) {
			Variable id = new Variable(arg.getStaticType(), arg.getName(), arg.getAnnotations(), arg.getLocation());
			prepared = prepared.assign(id, new PushAny(arg.getStaticType(), arg.getLocation()),
					cfg.getGenericProgramPoint());
		}

		// the stack has to be empty
		return new AnalysisState<>(prepared.getState(), new ExpressionSet<>(), new SymbolAliasing());
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			OpenCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		return policy.apply(call, entryState, parameters);
	}
}
