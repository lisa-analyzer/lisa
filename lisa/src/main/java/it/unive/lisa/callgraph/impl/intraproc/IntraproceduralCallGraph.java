package it.unive.lisa.callgraph.impl.intraproc;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFG.SemanticFunction;
import it.unive.lisa.cfg.Parameter;
import it.unive.lisa.cfg.statement.CFGCall;
import it.unive.lisa.cfg.statement.Call;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.OpenCall;
import it.unive.lisa.cfg.statement.UnresolvedCall;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueIdentifier;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An instance of {@link CallGraph} that does not handle interprocedurality. In
 * particular:
 * <ul>
 * <li>resolves {@link UnresolvedCall} to all the {@link CFG}s that match the
 * target's signature</li>
 * <li>returns top when asked for the abstract result of a {@link CFGCall}</li>
 * </ul>
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IntraproceduralCallGraph implements CallGraph {

	private static final Logger log = LogManager.getLogger(IntraproceduralCallGraph.class);

	/**
	 * The cash of the fixpoints' results. {@link Map#keySet()} will contain all
	 * the cfgs that have been added. If a key's values's
	 * {@link Optional#isEmpty()} yields true, then the fixpoint for that key
	 * has not be computed yet.
	 */
	private final Map<CFG, Optional<CFGWithAnalysisResults<?, ?>>> results;

	/**
	 * Builds the call graph.
	 */
	public IntraproceduralCallGraph() {
		this.results = new ConcurrentHashMap<>();
	}

	@Override
	public void addCFG(CFG cfg) {
		results.put(cfg, Optional.empty());
	}

	@Override
	public void clear() {
		for (CFG cfg : results.keySet())
			results.put(cfg, Optional.empty());
	}

	@Override
	public Call resolve(UnresolvedCall call) {
		Collection<CFG> targets = new ArrayList<>();
		for (CFG cfg : results.keySet())
			if (cfg.getDescriptor().getFullName().equals(call.getQualifiedName())
					&& cfg.getDescriptor().getReturnType().canBeAssignedTo(call.getStaticType())
					&& matchParametersTypes(cfg.getDescriptor().getArgs(), call.getParameters()))
				targets.add(cfg);

		Call resolved;
		if (targets.isEmpty())
			resolved = new OpenCall(call.getCFG(), call.getSourceFile(), call.getLine(), call.getCol(),
					call.getQualifiedName(), call.getStaticType(), call.getParameters());
		else
			resolved = new CFGCall(call.getCFG(), call.getSourceFile(), call.getLine(), call.getCol(),
					call.getQualifiedName(), targets, call.getParameters());

		resolved.setOffset(call.getOffset());
		return resolved;
	}

	private boolean matchParametersTypes(Parameter[] formals, Expression[] actuals) {
		if (formals.length != actuals.length)
			return false;

		for (int i = 0; i < formals.length; i++)
			if (!formals[i].getStaticType().canBeAssignedTo(actuals[i].getStaticType()))
				return false;

		return true;
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> void fixpoint(AnalysisState<H, V> entryState,
			SemanticFunction<H, V> semantics)
			throws FixpointException {
		for (CFG cfg : IterationLogger.iterate(log, results.keySet(), "Computing fixpoint over the whole program",
				"cfgs"))
			results.put(cfg, Optional.of(cfg.fixpoint(entryState, this, semantics)));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> getAnalysisResultsOf(
			CFG cfg) {
		return (CFGWithAnalysisResults<H, V>) results.get(cfg).orElse(null);
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> getAbstractResultOf(CFGCall call,
			AnalysisState<H, V> entryState, Collection<SymbolicExpression>[] parameters) throws SemanticException {
		if (call.getStaticType().isVoidType())
			return entryState.top();

		return entryState.top().smallStepSemantics(new ValueIdentifier(call.getRuntimeTypes(), "ret_value"));
	}

}
