package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.outputs.DotGraph;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.HybridCall;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.Graph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An instance of {@link CallGraph} that provides the basic mechanism to resolve
 * {@link UnresolvedCall}s.<br>
 * <br>
 * The graph underlying this call graph is built lazily through each call to
 * resolve: querying for information about the graph before the completion of
 * the analysis might lead to wrong results.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a> and
 *             <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 */
public abstract class BaseCallGraph extends Graph<BaseCallGraph, CallGraphNode, CallGraphEdge> implements CallGraph {

	private Program program;

	private final Map<CodeMember, Collection<Call>> callsites = new HashMap<>();

	private final Map<UnresolvedCall, Call> resolvedCache = new IdentityHashMap<>();

	@Override
	public final void init(Program program) throws CallGraphConstructionException {
		this.program = program;
	}

	@Override
	public void registerCall(CFGCall call) {
		CallGraphNode source = new CallGraphNode(this, call.getCFG());
		if (!adjacencyMatrix.containsNode(source))
			addNode(source, program.getEntryPoints().contains(call.getCFG()));

		for (CFG cfg : call.getTargets()) {
			callsites.computeIfAbsent(cfg, cm -> new HashSet<>()).add(call);

			CallGraphNode t = new CallGraphNode(this, cfg);
			if (!adjacencyMatrix.containsNode(t))
				addNode(t, program.getEntryPoints().contains(call.getCFG()));
			addEdge(new CallGraphEdge(source, t));
		}
	}

	@Override
	public final Call resolve(UnresolvedCall call) throws CallResolutionException {
		Call cached = resolvedCache.get(call);
		if (cached != null)
			return cached;

		Collection<CFG> targets = new ArrayList<>();
		Collection<NativeCFG> nativeTargets = new ArrayList<>();

		if (call.isInstanceCall()) {
			if (call.getParameters().length == 0)
				throw new CallResolutionException(
						"An instance call should have at least one parameter to be used as the receiver of the call");
			Expression receiver = call.getParameters()[0];
			for (Type recType : getPossibleTypesOfReceiver(receiver)) {
				if (!recType.isUnitType())
					continue;

				CompilationUnit unit = recType.asUnitType().getUnit();
				Collection<CodeMember> candidates = unit.getInstanceCodeMembersByName(call.getTargetName(), true);
				for (CodeMember cm : candidates)
					if (cm.getDescriptor().isInstance()
							&& call.getStrategy().matches(cm.getDescriptor().getArgs(), call.getParameters()))
						if (cm instanceof CFG)
							targets.add((CFG) cm);
						else
							nativeTargets.add((NativeCFG) cm);
			}
		} else {
			for (CodeMember cm : program.getAllCodeMembers())
				if (!cm.getDescriptor().isInstance() && cm.getDescriptor().getName().equals(call.getTargetName())
						&& call.getStrategy().matches(cm.getDescriptor().getArgs(), call.getParameters()))
					if (cm instanceof CFG)
						targets.add((CFG) cm);
					else
						nativeTargets.add((NativeCFG) cm);
		}

		Call resolved;
		if (targets.isEmpty() && nativeTargets.isEmpty())
			resolved = new OpenCall(call.getCFG(), call.getLocation(), call.getTargetName(), call.getStaticType(),
					call.getParameters());
		else if (nativeTargets.isEmpty())
			resolved = new CFGCall(call.getCFG(), call.getLocation(), call.getTargetName(), targets,
					call.getParameters());
		else
			resolved = new HybridCall(call.getCFG(), call.getLocation(), call.getTargetName(), targets, nativeTargets,
					call.getParameters());

		resolved.setOffset(call.getOffset());

		CallGraphNode source = new CallGraphNode(this, call.getCFG());
		if (!adjacencyMatrix.containsNode(source))
			addNode(source, program.getEntryPoints().contains(call.getCFG()));

		for (CFG target : targets) {
			CallGraphNode t = new CallGraphNode(this, target);
			if (!adjacencyMatrix.containsNode(t))
				addNode(t, program.getEntryPoints().contains(call.getCFG()));
			addEdge(new CallGraphEdge(source, t));
			callsites.computeIfAbsent(target, cm -> new HashSet<>()).add(call);
		}

		for (NativeCFG target : nativeTargets) {
			CallGraphNode t = new CallGraphNode(this, target);
			if (!adjacencyMatrix.containsNode(t))
				addNode(t, false);
			addEdge(new CallGraphEdge(source, t));
			callsites.computeIfAbsent(target, cm -> new HashSet<>()).add(call);
		}

		resolvedCache.put(call, resolved);

		return resolved;
	}

	/**
	 * Returns all the possible types of the given expression, that is a
	 * receiver of a method call. How we choose this set varies from the call
	 * graph algorithm we decide to adopt (e.g., CHA, RTA, 0-CFA, ...)
	 * 
	 * @param receiver an expression
	 * 
	 * @return the possible types of the given expression
	 */
	protected abstract Collection<Type> getPossibleTypesOfReceiver(Expression receiver);

	@Override
	public Collection<CodeMember> getCallees(CodeMember cm) {
		return followersOf(new CallGraphNode(this, cm)).stream().map(CallGraphNode::getCodeMember)
				.collect(Collectors.toList());
	}

	@Override
	public Collection<CodeMember> getCallers(CodeMember cm) {
		return predecessorsOf(new CallGraphNode(this, cm)).stream().map(CallGraphNode::getCodeMember)
				.collect(Collectors.toList());
	}

	@Override
	public Collection<Call> getCallSites(CodeMember cm) {
		return callsites.get(cm);
	}

	@Override
	protected DotGraph<CallGraphNode, CallGraphEdge, BaseCallGraph> toDot(
			Function<CallGraphNode, String> labelGenerator) {
		throw new UnsupportedOperationException();
	}
}
