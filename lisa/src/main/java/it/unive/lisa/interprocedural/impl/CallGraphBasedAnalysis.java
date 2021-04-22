package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An interprocedural analysis based on a call graph.
 * 
 * @param <A> The abstract state of the analysis
 * @param <H> The heap domain
 * @param <V> The value domain
 */
public abstract class CallGraphBasedAnalysis<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> implements InterproceduralAnalysis<A, H, V> {

	/**
	 * The call graph used to resolve method calls.
	 */
	private CallGraph callgraph;

	/**
	 * The program.
	 */
	protected Program program;

	@Override
	public final void build(Program program, CallGraph callgraph) throws InterproceduralAnalysisException {
		this.callgraph = callgraph;
		this.program = program;
	}

	@Override
	public final Call resolve(UnresolvedCall unresolvedCall) throws CallResolutionException {
		return callgraph.resolve(unresolvedCall);
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
	protected final AnalysisState<A, H, V> prepareEntryStateOfEntryPoint(AnalysisState<A, H, V> entryState, CFG cfg)
			throws SemanticException {
		AnalysisState<A, H, V> prepared = entryState;

		for (Parameter arg : cfg.getDescriptor().getArgs()) {
			ExternalSet<Type> all = Caches.types().mkSet(arg.getStaticType().allInstances());
			Variable id = new Variable(all, arg.getName());
			prepared = prepared.assign(id, new PushAny(all), cfg.getGenericProgramPoint());
		}

		return prepared;
	}
}
