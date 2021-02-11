package it.unive.lisa.callgraph.impl.intraproc;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.callgraph.CallGraphConstructionException;
import it.unive.lisa.callgraph.CallResolutionException;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.OpenCall;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueIdentifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
	private final Map<CFG, Optional<CFGWithAnalysisResults<?, ?, ?>>> results;

	private Program program;

	/**
	 * Builds the call graph.
	 */
	public IntraproceduralCallGraph() {
		this.results = new ConcurrentHashMap<>();
	}

	@Override
	public void build(Program program) throws CallGraphConstructionException {
		this.program = program;
	}

	@Override
	public void clear() {
		results.clear();
	}

	@Override
	public Call resolve(UnresolvedCall call) throws CallResolutionException {
		Collection<CodeMember> targets = new ArrayList<>();

		if (call.isInstanceCall()) {
			if (call.getParameters().length == 0)
				throw new CallResolutionException(
						"An instance call should have at least one parameter to be used as the receiver of the call");
			Expression receiver = call.getParameters()[0];
			for (Type recType : receiver.getRuntimeTypes()) {
				if (!recType.isUnitType())
					continue;

				CompilationUnit unit = recType.asUnitType().getUnit();
				Collection<CodeMember> candidates = unit.getInstanceCodeMembersByName(call.getTargetName(), true);
				for (CodeMember candidate : candidates)
					if (call.getStrategy().matches(candidate.getDescriptor().getArgs(), call.getParameters()))
						targets.add(candidate);
			}
		} else {
			for (CodeMember cm : program.getAllCodeMembers())
				if (cm.getDescriptor().isInstance() && cm.getDescriptor().getName().equals(call.getTargetName())
						&& call.getStrategy().matches(cm.getDescriptor().getArgs(), call.getParameters()))
					targets.add(cm);
		}

		Call resolved;
		if (targets.isEmpty())
			resolved = new OpenCall(call.getCFG(), call.getSourceFile(), call.getLine(), call.getCol(),
					call.getTargetName(), call.getStaticType(), call.getParameters());
		else if (targets.size() == 1 && targets.iterator().next() instanceof NativeCFG)
			resolved = ((NativeCFG) targets.iterator().next()).rewrite(call, call.getParameters());
		else {
			if (targets.stream().anyMatch(t -> t instanceof NativeCFG))
				throw new CallResolutionException(
						"Hybrid resolution is not supported: when more than one target is present, they must all be CFGs and not NativeCFGs");

			resolved = new CFGCall(call.getCFG(), call.getSourceFile(), call.getLine(), call.getCol(),
					call.getTargetName(), targets.stream().map(t -> (CFG) t).collect(Collectors.toList()),
					call.getParameters());
		}

		resolved.setOffset(call.getOffset());
		return resolved;
	}

	@Override
	public <A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>> void fixpoint(
			AnalysisState<A, H, V> entryState)
			throws FixpointException {
		for (CFG cfg : IterationLogger.iterate(log, program.getAllCFGs(), "Computing fixpoint over the whole program",
				"cfgs"))
			try {
				results.put(cfg, Optional.of(cfg.fixpoint(prepare(entryState, cfg), this)));
			} catch (SemanticException e) {
				throw new FixpointException("Error while creating the entrystate for " + cfg, e);
			}
	}

	private <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> prepare(AnalysisState<A, H, V> entryState, CFG cfg)
					throws SemanticException {
		AnalysisState<A, H, V> prepared = entryState;
		for (Parameter arg : cfg.getDescriptor().getArgs())
			if (arg.getStaticType().isPointerType()) {
				prepared = prepared.smallStepSemantics(
						new HeapReference(Caches.types().mkSingletonSet(arg.getStaticType()), arg.getName()),
						cfg.getGenericProgramPoint());
				for (SymbolicExpression expr : prepared.getComputedExpressions())
					prepared = prepared.assign((HeapIdentifier) expr,
							new PushAny(Caches.types().mkSingletonSet(arg.getStaticType())),
							cfg.getGenericProgramPoint());
			} else {
				ValueIdentifier id = new ValueIdentifier(Caches.types().mkSingletonSet(arg.getStaticType()),
						arg.getName());
				prepared = prepared.assign(id, new PushAny(Caches.types().mkSingletonSet(arg.getStaticType())),
						cfg.getGenericProgramPoint());
			}
		return prepared;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> getAnalysisResultsOf(
					CFG cfg) {
		return (CFGWithAnalysisResults<A, H, V>) results.get(cfg).orElse(null);
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> getAbstractResultOf(CFGCall call,
					AnalysisState<A, H, V> entryState, Collection<SymbolicExpression>[] parameters)
					throws SemanticException {
		if (call.getStaticType().isVoidType())
			return entryState.top();

		return entryState.top().smallStepSemantics(new ValueIdentifier(call.getRuntimeTypes(), "ret_value"), call);
	}

}
