package it.unive.lisa.interprocedural.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

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
	public void init(Program program) throws CallGraphConstructionException {
		this.program = program;
	}

	@Override
	public void registerCall(CFGCall call) {
		if (call.getSource() != null)
			// this call has been generated through the resolution of an
			// UnresolvedCall, and that one has already been registered
			return;

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
	public Call resolve(UnresolvedCall call) throws CallResolutionException {
		Call cached = resolvedCache.get(call);
		if (cached != null)
			return cached;

		Collection<CFG> targets = new ArrayList<>();
		Collection<NativeCFG> nativeTargets = new ArrayList<>();

		if (call.isInstanceCall())
			resolveInstance(call, targets, nativeTargets);
		else
			resolveNonInstance(call, targets, nativeTargets);

		Call resolved;
		if (targets.isEmpty() && nativeTargets.isEmpty())
			resolved = new OpenCall(call);
		else if (nativeTargets.isEmpty())
			resolved = new CFGCall(call, targets);
		else
			resolved = new HybridCall(call, targets, nativeTargets);

		resolved.setOffset(call.getOffset());
		resolved.setSource(call);
		resolvedCache.put(call, resolved);

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

		return resolved;
	}

	/**
	 * Resolves the given call as regular (non-instance) call.
	 * 
	 * @param call    the call to resolve
	 * @param targets the list of targets that, after the execution of this
	 *                    method, will contain the {@link CFG}s targeted by the
	 *                    call
	 * @param natives the list of targets that, after the execution of this
	 *                    method, will contain the {@link NativeCFG}s targeted
	 *                    by the call
	 * 
	 * @throws CallResolutionException if something goes wrong while resolving
	 *                                     the call
	 */
	protected void resolveNonInstance(UnresolvedCall call, Collection<CFG> targets, Collection<NativeCFG> natives)
			throws CallResolutionException {
		for (CodeMember cm : program.getAllCodeMembers())
			if (!cm.getDescriptor().isInstance()
					&& matchCFGName(call, cm)
					&& call.getMatchingStrategy().matches(call, cm.getDescriptor().getFormals(), call.getParameters()))
				if (cm instanceof CFG)
					targets.add((CFG) cm);
				else
					natives.add((NativeCFG) cm);
	}

	/**
	 * Resolves the given call as an instance call.
	 * 
	 * @param call    the call to resolve
	 * @param targets the list of targets that, after the execution of this
	 *                    method, will contain the {@link CFG}s targeted by the
	 *                    call
	 * @param natives the list of targets that, after the execution of this
	 *                    method, will contain the {@link NativeCFG}s targeted
	 *                    by the call
	 * 
	 * @throws CallResolutionException if something goes wrong while resolving
	 *                                     the call
	 */
	protected void resolveInstance(UnresolvedCall call, Collection<CFG> targets, Collection<NativeCFG> natives)
			throws CallResolutionException {
		if (call.getParameters().length == 0)
			throw new CallResolutionException(
					"An instance call should have at least one parameter to be used as the receiver of the call");
		Expression receiver = call.getParameters()[0];
		for (Type recType : getPossibleTypesOfReceiver(receiver)) {
			if (!recType.isUnitType())
				continue;

			CompilationUnit unit = recType.asUnitType().getUnit();
			for (CompilationUnit cu : call.getTraversalStrategy().traverse(call, unit)) {
				// we inspect only the ones of the current unit
				Collection<CodeMember> candidates = cu.getInstanceCodeMembersByName(call.getTargetName(), false);
				for (CodeMember cm : candidates)
					if (cm.getDescriptor().isInstance()
							&& call.getMatchingStrategy().matches(call, cm.getDescriptor().getFormals(),
									call.getParameters()))
						if (cm instanceof CFG)
							targets.add((CFG) cm);
						else
							natives.add((NativeCFG) cm);
			}
		}
	}

	/**
	 * Matches the name (qualifier + target name) of the given call against the
	 * given code member.
	 * 
	 * @param call the call to match
	 * @param cm   the code member being matched
	 * 
	 * @return {@code true} if the name of {@code cm} is compatible with the one
	 *             of the call's target
	 */
	protected boolean matchCFGName(UnresolvedCall call, CodeMember cm) {
		if (!cm.getDescriptor().getName().equals(call.getTargetName()))
			return false;
		if (StringUtils.isBlank(call.getQualifier()))
			return true;
		return cm.getDescriptor().getUnit().getName().equals(call.getQualifier());
	}

	/**
	 * Returns all the possible types of the given expression, that is a
	 * receiver of a method call. How we choose this set varies from the call
	 * graph algorithm we decide to adopt (e.g., CHA, RTA, 0-CFA, ...)
	 * 
	 * @param receiver an expression
	 * 
	 * @return the possible types of the given expression
	 * 
	 * @throws CallResolutionException if the types cannot be computed
	 */
	protected abstract Collection<Type> getPossibleTypesOfReceiver(Expression receiver) throws CallResolutionException;

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
		return callsites.getOrDefault(cm, Collections.emptyList());
	}

	@Override
	protected DotGraph<CallGraphNode, CallGraphEdge, BaseCallGraph> toDot(
			Function<CallGraphNode, String> labelGenerator) {
		throw new UnsupportedOperationException();
	}
}
