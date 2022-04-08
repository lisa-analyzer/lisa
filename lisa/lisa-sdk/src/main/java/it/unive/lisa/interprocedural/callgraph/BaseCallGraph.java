package it.unive.lisa.interprocedural.callgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.analysis.symbols.Aliases;
import it.unive.lisa.analysis.symbols.NameSymbol;
import it.unive.lisa.analysis.symbols.QualifiedNameSymbol;
import it.unive.lisa.analysis.symbols.QualifierSymbol;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.MultiCall;
import it.unive.lisa.program.cfg.statement.call.NativeCall;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.TruncatedParamsCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
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

	private static final Logger LOG = LogManager.getLogger(BaseCallGraph.class);

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
	@SuppressWarnings("unchecked")
	public Call resolve(UnresolvedCall call, ExternalSet<Type>[] types, SymbolAliasing aliasing)
			throws CallResolutionException {
		Call cached = resolvedCache.get(call);
		if (cached != null)
			return cached;

		if (types == null)
			// we allow types to be null only for calls that we already resolved
			throw new CallResolutionException("Cannot resolve call without runtime types");
		if (aliasing == null)
			// we allow aliasing to be null only for calls that we already
			// resolved
			throw new CallResolutionException("Cannot resolve call without symbol aliasing");

		Collection<CFG> targets = new HashSet<>();
		Collection<NativeCFG> nativeTargets = new HashSet<>();
		Collection<CFG> targetsNoRec = new HashSet<>();
		Collection<NativeCFG> nativeTargetsNoRec = new HashSet<>();

		Expression[] params = call.getParameters();
		switch (call.getCallType()) {
		case INSTANCE:
			resolveInstance(call, types, targets, nativeTargets, aliasing);
			break;
		case STATIC:
			resolveNonInstance(call, types, targets, nativeTargets, aliasing);
			break;
		case UNKNOWN:
		default:
			UnresolvedCall tempCall = new UnresolvedCall(
					call.getCFG(),
					call.getLocation(),
					call.getAssigningStrategy(),
					call.getMatchingStrategy(),
					call.getTraversalStrategy(),
					CallType.INSTANCE,
					call.getQualifier(),
					call.getTargetName(),
					call.getOrder(),
					params);
			resolveInstance(tempCall, types, targets, nativeTargets, aliasing);

			if (!(params[0] instanceof VariableRef)) {
				LOG.debug(call
						+ ": solving unknown-type calls as static-type requires the first parameter to be a reference to a variable, skipping");
				break;
			}

			Expression[] truncatedParams = new Expression[params.length - 1];
			ExternalSet<Type>[] truncatedTypes = new ExternalSet[types.length - 1];
			System.arraycopy(params, 1, truncatedParams, 0, params.length - 1);
			System.arraycopy(types, 1, truncatedTypes, 0, types.length - 1);
			tempCall = new UnresolvedCall(
					call.getCFG(),
					call.getLocation(),
					call.getAssigningStrategy(),
					call.getMatchingStrategy(),
					call.getTraversalStrategy(),
					CallType.STATIC,
					((VariableRef) params[0]).getName(),
					call.getTargetName(),
					call.getOrder(),
					truncatedParams);
			resolveNonInstance(tempCall, truncatedTypes, targetsNoRec, nativeTargetsNoRec, aliasing);
			break;
		}

		Call resolved;
		CFGCall cfgcall = new CFGCall(call, targets);
		NativeCall nativecall = new NativeCall(call, nativeTargets);
		TruncatedParamsCall cfgcallnorec = new CFGCall(call, targetsNoRec).removeFirstParameter();
		TruncatedParamsCall nativecallnorec = new NativeCall(call, nativeTargetsNoRec).removeFirstParameter();
		if (noTargets(targets, nativeTargets, targetsNoRec, nativeTargetsNoRec))
			resolved = new OpenCall(call);
		else if (onlyNonRewritingCFGTargets(targets, nativeTargets, targetsNoRec, nativeTargetsNoRec))
			resolved = cfgcall;
		else if (onlyNonRewritingNativeTargets(targets, nativeTargets, targetsNoRec, nativeTargetsNoRec))
			resolved = nativecall;
		else if (onlyNonRewritingTargets(targets, nativeTargets, targetsNoRec, nativeTargetsNoRec))
			resolved = new MultiCall(call, cfgcall, nativecall);
		else if (onlyRewritingCFGTargets(targets, nativeTargets, targetsNoRec, nativeTargetsNoRec))
			resolved = cfgcallnorec;
		else if (onlyRewritingNativeTargets(targets, nativeTargets, targetsNoRec, nativeTargetsNoRec))
			resolved = nativecallnorec;
		else if (onlyRewritingTargets(targets, nativeTargets, targetsNoRec, nativeTargetsNoRec))
			resolved = new MultiCall(call, cfgcallnorec, nativecallnorec);
		else if (onlyCFGTargets(targets, nativeTargets, targetsNoRec, nativeTargetsNoRec))
			resolved = new MultiCall(call, cfgcall, cfgcallnorec);
		else if (onlyNativeCFGTargets(targets, nativeTargets, targetsNoRec, nativeTargetsNoRec))
			resolved = new MultiCall(call, nativecall, nativecallnorec);
		else
			resolved = new MultiCall(call, cfgcall, cfgcallnorec, nativecall, nativecallnorec);

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

	private boolean onlyNativeCFGTargets(Collection<CFG> targets, Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty()
				&& !nativeTargets.isEmpty()
				&& targetsNoRec.isEmpty()
				&& !nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyCFGTargets(Collection<CFG> targets, Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return !targets.isEmpty()
				&& nativeTargets.isEmpty()
				&& !targetsNoRec.isEmpty()
				&& nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyRewritingTargets(Collection<CFG> targets, Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty()
				&& nativeTargets.isEmpty()
				&& !targetsNoRec.isEmpty()
				&& !nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyRewritingNativeTargets(Collection<CFG> targets, Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty()
				&& nativeTargets.isEmpty()
				&& targetsNoRec.isEmpty()
				&& !nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyRewritingCFGTargets(Collection<CFG> targets, Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty()
				&& nativeTargets.isEmpty()
				&& !targetsNoRec.isEmpty()
				&& nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyNonRewritingTargets(Collection<CFG> targets, Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return !targets.isEmpty()
				&& !nativeTargets.isEmpty()
				&& targetsNoRec.isEmpty()
				&& nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyNonRewritingNativeTargets(Collection<CFG> targets, Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty()
				&& !nativeTargets.isEmpty()
				&& targetsNoRec.isEmpty()
				&& nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyNonRewritingCFGTargets(Collection<CFG> targets, Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return !targets.isEmpty()
				&& nativeTargets.isEmpty()
				&& targetsNoRec.isEmpty()
				&& nativeTargetsNoRec.isEmpty();
	}

	private boolean noTargets(Collection<CFG> targets, Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty()
				&& nativeTargets.isEmpty()
				&& targetsNoRec.isEmpty()
				&& nativeTargetsNoRec.isEmpty();
	}

	/**
	 * Resolves the given call as regular (non-instance) call.
	 * 
	 * @param call     the call to resolve
	 * @param types    the runtime types of the parameters of the call
	 * @param targets  the list of targets that, after the execution of this
	 *                     method, will contain the {@link CFG}s targeted by the
	 *                     call
	 * @param natives  the list of targets that, after the execution of this
	 *                     method, will contain the {@link NativeCFG}s targeted
	 *                     by the call
	 * @param aliasing the symbol aliasing information
	 * 
	 * @throws CallResolutionException if something goes wrong while resolving
	 *                                     the call
	 */
	protected void resolveNonInstance(UnresolvedCall call, ExternalSet<Type>[] types, Collection<CFG> targets,
			Collection<NativeCFG> natives, SymbolAliasing aliasing)
			throws CallResolutionException {
		for (CodeMember cm : program.getAllCodeMembers())
			checkMember(call, types, targets, natives, aliasing, cm, false);
	}

	/**
	 * Resolves the given call as an instance call.
	 * 
	 * @param call     the call to resolve
	 * @param types    the runtime types of the parameters of the call
	 * @param targets  the list of targets that, after the execution of this
	 *                     method, will contain the {@link CFG}s targeted by the
	 *                     call
	 * @param natives  the list of targets that, after the execution of this
	 *                     method, will contain the {@link NativeCFG}s targeted
	 *                     by the call
	 * @param aliasing the symbol aliasing information
	 * 
	 * @throws CallResolutionException if something goes wrong while resolving
	 *                                     the call
	 */
	protected void resolveInstance(UnresolvedCall call, ExternalSet<Type>[] types, Collection<CFG> targets,
			Collection<NativeCFG> natives, SymbolAliasing aliasing)
			throws CallResolutionException {
		if (call.getParameters().length == 0)
			throw new CallResolutionException(
					"An instance call should have at least one parameter to be used as the receiver of the call");
		Expression receiver = call.getParameters()[0];
		for (Type recType : getPossibleTypesOfReceiver(receiver, types[0])) {
			Collection<CompilationUnit> units;
			if (recType.isUnitType())
				units = Collections.singleton(recType.asUnitType().getUnit());
			else if (recType.isPointerType() && recType.asPointerType().getInnerTypes().anyMatch(Type::isUnitType))
				units = recType.asPointerType()
						.getInnerTypes()
						.stream()
						.filter(Type::isUnitType)
						.map(Type::asUnitType)
						.map(UnitType::getUnit)
						.collect(Collectors.toSet());
			else
				continue;

			Set<CompilationUnit> seen = new HashSet<>();
			for (CompilationUnit unit : units)
				for (CompilationUnit cu : call.getTraversalStrategy().traverse(call, unit))
					if (seen.add(unit))
						// we inspect only the ones of the current unit
						for (CodeMember cm : cu.getInstanceCodeMembers(false))
							checkMember(call, types, targets, natives, aliasing, cm, true);
		}
	}

	/**
	 * Checks if the given code member {@code cm} is a candidate target for the
	 * given call, and proceeds to add it to the set of targets if it is.
	 * Aliasing information is used here to match code members that have been
	 * aliased and that can be targeted by calls that refer to other names.
	 * 
	 * @param call     the call to match
	 * @param types    the runtime types of the parameters of the call
	 * @param targets  the list of targets that, after the execution of this
	 *                     method, will contain the {@link CFG}s targeted by the
	 *                     call
	 * @param natives  the list of targets that, after the execution of this
	 *                     method, will contain the {@link NativeCFG}s targeted
	 *                     by the call
	 * @param aliasing the symbol aliasing information
	 * @param cm       the code member to match
	 * @param instance whether or not the only instance or non-instance members
	 *                     should be matched
	 */
	protected void checkMember(
			UnresolvedCall call,
			ExternalSet<Type>[] types,
			Collection<CFG> targets,
			Collection<NativeCFG> natives,
			SymbolAliasing aliasing,
			CodeMember cm,
			boolean instance) {
		CFGDescriptor descr = cm.getDescriptor();
		if (instance != descr.isInstance())
			return;

		String qualifier = descr.getUnit().getName();
		String name = descr.getName();

		Aliases nAlias = aliasing.getState(new NameSymbol(name));
		Aliases qAlias = aliasing.getState(new QualifierSymbol(qualifier));
		Aliases qnAlias = aliasing.getState(new QualifiedNameSymbol(qualifier, name));

		boolean add = false;
		// we first check the qualified name, then the qualifier and the
		// name individually
		if (!qnAlias.isEmpty()) {
			for (QualifiedNameSymbol alias : qnAlias.castElements(QualifiedNameSymbol.class))
				if (matchCodeMemberName(call, alias.getQualifier(), alias.getName())) {
					add = true;
					break;
				}
		}

		if (!add && !qAlias.isEmpty()) {
			for (QualifierSymbol alias : qAlias.castElements(QualifierSymbol.class))
				if (matchCodeMemberName(call, alias.getQualifier(), name)) {
					add = true;
					break;
				}
		}

		if (!add && !nAlias.isEmpty()) {
			for (NameSymbol alias : nAlias.castElements(NameSymbol.class))
				if (matchCodeMemberName(call, qualifier, alias.getName())) {
					add = true;
					break;
				}
		}

		if (!add)
			add = matchCodeMemberName(call, qualifier, name);

		if (add && call.getMatchingStrategy().matches(call, descr.getFormals(), call.getParameters(), types))
			add(targets, natives, cm);
	}

	private void add(Collection<CFG> targets, Collection<NativeCFG> natives, CodeMember cm) {
		if (cm instanceof CFG)
			targets.add((CFG) cm);
		else
			natives.add((NativeCFG) cm);
	}

	/**
	 * Matches the name (qualifier + target name) of the given call against the
	 * given code member.
	 * 
	 * @param call      the call to match
	 * @param qualifier the qualifier (name of the defining unit) of the code
	 *                      member
	 * @param name      the name of the code member
	 * 
	 * @return {@code true} if the qualifier and name are compatible with the
	 *             ones of the call's target
	 */
	protected boolean matchCodeMemberName(UnresolvedCall call, String qualifier, String name) {
		if (!name.equals(call.getTargetName()))
			return false;
		if (StringUtils.isBlank(call.getQualifier()))
			return true;
		return qualifier.equals(call.getQualifier());
	}

	/**
	 * Returns all the possible types of the given expression that should be
	 * considered as possible receivers of the call. How we choose this set
	 * varies from the call graph algorithm we decide to adopt (e.g., CHA, RTA,
	 * 0-CFA, ...)
	 * 
	 * @param receiver an expression
	 * @param types    the runtime types of the receiver
	 * 
	 * @return the possible types to use as receivers
	 * 
	 * @throws CallResolutionException if the types cannot be computed
	 */
	protected abstract Collection<Type> getPossibleTypesOfReceiver(Expression receiver, ExternalSet<Type> types)
			throws CallResolutionException;

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
}
