package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.analysis.symbols.Aliases;
import it.unive.lisa.analysis.symbols.NameSymbol;
import it.unive.lisa.analysis.symbols.QualifiedNameSymbol;
import it.unive.lisa.analysis.symbols.QualifierSymbol;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.AbstractCodeMember;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.MultiCall;
import it.unive.lisa.program.cfg.statement.call.NativeCall;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.ResolvedCall;
import it.unive.lisa.program.cfg.statement.call.TruncatedParamsCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.language.hierarchytraversal.HierarchyTraversalStrategy;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public abstract class BaseCallGraph
		extends
		CallGraph {

	private static final Logger LOG = LogManager.getLogger(BaseCallGraph.class);

	private Application app;

	private EventQueue events;

	private final Map<CodeMember, Collection<Call>> callsites = new HashMap<>();

	private final Map<UnresolvedCall, Map<List<Set<Type>>, Call>> resolvedCache = new IdentityHashMap<>();

	@Override
	public void init(
			Application app,
			EventQueue events)
			throws CallGraphConstructionException {
		super.init(app, events);
		this.app = app;
		this.events = events;
		this.callsites.clear();
		this.resolvedCache.clear();
	}

	@Override
	public void registerCall(
			CFGCall call) {
		if (call.getSource() != null)
			// this call has been generated through the resolution of an
			// UnresolvedCall, and that one has already been registered
			return;

		CallGraphNode source = new CallGraphNode(this, call.getCFG());
		if (!adjacencyMatrix.containsNode(source))
			addNode(source, app.getEntryPoints().contains(call.getCFG()));

		for (CFG cfg : call.getTargetedCFGs()) {
			callsites.computeIfAbsent(cfg, cm -> new HashSet<>()).add(call);

			CallGraphNode t = new CallGraphNode(this, cfg);
			if (!adjacencyMatrix.containsNode(t))
				addNode(t, app.getEntryPoints().contains(call.getCFG()));
			addEdge(new CallGraphEdge(source, t));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Call resolve(
			UnresolvedCall call,
			Set<Type>[] types,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		List<Set<Type>> typeList = Arrays.asList(types);
		Call cached = resolvedCache.getOrDefault(call, Map.of()).get(typeList);
		if (cached != null)
			return cached;

		Expression[] params = call.getParameters();
		if (types == null || types.length != params.length)
			// we allow types to be null only for calls that we already resolved
			throw new CallResolutionException("Cannot resolve call without runtime types");

		if (Arrays.stream(types).anyMatch(rts -> rts == null || rts.isEmpty())) {
			// if we do not have runtime types of a parameter, we consider it to
			// be of any possible type compatible with its static one
			types = Arrays.copyOf(types, types.length);
			for (int i = 0; i < types.length; i++)
				if (types[i] == null || types[i].isEmpty())
					types[i] = params[i].getStaticType().allInstances(call.getProgram().getTypes());
		}

		Set<CFG> targets = new HashSet<>();
		Set<NativeCFG> nativeTargets = new HashSet<>();
		Set<CFG> targetsNoRec = new HashSet<>();
		Set<NativeCFG> nativeTargetsNoRec = new HashSet<>();

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
					CallType.INSTANCE,
					call.getQualifier(),
					call.getTargetName(),
					call.getOrder(),
					params);
			resolveInstance(tempCall, types, targets, nativeTargets, aliasing);

			if (!(params[0] instanceof VariableRef)) {
				LOG.debug(
						call
								+ ": solving unknown-type calls as static-type requires the first parameter to be a reference to a variable, skipping");
				break;
			}

			Expression[] truncatedParams = new Expression[params.length - 1];
			Set<Type>[] truncatedTypes = new Set[types.length - 1];
			System.arraycopy(params, 1, truncatedParams, 0, params.length - 1);
			System.arraycopy(types, 1, truncatedTypes, 0, types.length - 1);
			tempCall = new UnresolvedCall(
					call.getCFG(),
					call.getLocation(),
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

		resolved.setSource(call);
		resolvedCache.computeIfAbsent(call, c -> new HashMap<>()).put(typeList, resolved);

		CallGraphNode source = new CallGraphNode(this, call.getCFG());
		if (!adjacencyMatrix.containsNode(source))
			addNode(source, app.getEntryPoints().contains(call.getCFG()));

		for (CFG target : SetUtils.union(targets, targetsNoRec)) {
			CallGraphNode t = new CallGraphNode(this, target);
			if (!adjacencyMatrix.containsNode(t))
				addNode(t, app.getEntryPoints().contains(call.getCFG()));
			addEdge(new CallGraphEdge(source, t));
			callsites.computeIfAbsent(target, cm -> new HashSet<>()).add(call);
		}

		for (NativeCFG target : SetUtils.union(nativeTargets, nativeTargetsNoRec)) {
			CallGraphNode t = new CallGraphNode(this, target);
			if (!adjacencyMatrix.containsNode(t))
				addNode(t, false);
			addEdge(new CallGraphEdge(source, t));
			callsites.computeIfAbsent(target, cm -> new HashSet<>()).add(call);
		}

		LOG.trace(
				call + " [" + call.getLocation() + "] has been resolved to: " + ((ResolvedCall) resolved).getTargets());
		return resolved;
	}

	private boolean onlyNativeCFGTargets(
			Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty() && !nativeTargets.isEmpty() && targetsNoRec.isEmpty() && !nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyCFGTargets(
			Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return !targets.isEmpty() && nativeTargets.isEmpty() && !targetsNoRec.isEmpty() && nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyRewritingTargets(
			Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty() && nativeTargets.isEmpty() && !targetsNoRec.isEmpty() && !nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyRewritingNativeTargets(
			Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty() && nativeTargets.isEmpty() && targetsNoRec.isEmpty() && !nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyRewritingCFGTargets(
			Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty() && nativeTargets.isEmpty() && !targetsNoRec.isEmpty() && nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyNonRewritingTargets(
			Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return !targets.isEmpty() && !nativeTargets.isEmpty() && targetsNoRec.isEmpty() && nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyNonRewritingNativeTargets(
			Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty() && !nativeTargets.isEmpty() && targetsNoRec.isEmpty() && nativeTargetsNoRec.isEmpty();
	}

	private boolean onlyNonRewritingCFGTargets(
			Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return !targets.isEmpty() && nativeTargets.isEmpty() && targetsNoRec.isEmpty() && nativeTargetsNoRec.isEmpty();
	}

	private boolean noTargets(
			Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets,
			Collection<CFG> targetsNoRec,
			Collection<NativeCFG> nativeTargetsNoRec) {
		return targets.isEmpty() && nativeTargets.isEmpty() && targetsNoRec.isEmpty() && nativeTargetsNoRec.isEmpty();
	}

	/**
	 * Resolves a non-instance call to its possible targets.
	 * <p>
	 * The resolution process works as follows:
	 * <ol>
	 * <li>Looks up the {@link Unit}s corresponding to the call's qualifier,
	 * considering also possible aliases.</li>
	 * <li>If one such unit is a {@link CompilationUnit}, its hierarchy is
	 * traversed as well using the program's
	 * {@link HierarchyTraversalStrategy}.</li>
	 * <li>For each candidate {@link CodeMember},
	 * {@link #isATarget(UnresolvedCall, SymbolAliasing, CodeMember, Set[], boolean)}
	 * decides if it is a suitable target for the call.</li>
	 * <li>For each suitable target, its "distance" from a perfect match using
	 * {@link ParameterMatchingStrategy#distanceFromPerfectTarget(UnresolvedCall, Set[], CodeMember, boolean)}.</li>
	 * <li>The best candidate (lowest distance) for each possible unit is
	 * selected as a possible call target. If a distance of 0 (perfect match) is
	 * found, resolution for that unit stops immediately.</li>
	 * <li>If multiple candidates along the same unit have the same best
	 * distance greater than 0, a {@link CallResolutionException} is thrown due
	 * to ambiguity.</li>
	 * <li>The best match of each possible unit is added to the
	 * {@code targets}/{@code natives} collections.</li>
	 * </ol>
	 * </p>
	 *
	 * @param call     the unresolved call to resolve
	 * @param types    the possible runtime types of the call's parameters
	 * @param targets  the collection where resolved {@link CFG} targets will be
	 *                     stored
	 * @param natives  the collection where resolved {@link NativeCFG} targets
	 *                     will be stored
	 * @param aliasing aliasing information that may affect resolution
	 *
	 * @throws CallResolutionException if multiple equally suitable targets are
	 *                                     found
	 */
	public void resolveNonInstance(
			UnresolvedCall call,
			Set<Type>[] types,
			Collection<CFG> targets,
			Collection<NativeCFG> natives,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		HierarchyTraversalStrategy strategy = call.getProgram().getFeatures().getTraversalStrategy();
		ParameterMatchingStrategy matching = call.getProgram().getFeatures().getMatchingStrategy();

		Collection<Unit> possibleUnits = new HashSet<>();
		if (call.getQualifier() == null) {
			possibleUnits.addAll(call.getProgram().getUnits());
			possibleUnits.add(call.getProgram());
		} else {
			Unit cu = call.getProgram().getUnit(call.getQualifier());
			if (cu != null)
				possibleUnits.add(cu);
			else if (call.getQualifier().equals(call.getProgram().getName())) {
				possibleUnits.add(call.getProgram());
			} else {
				Aliases qAlias = aliasing.getState(new QualifierSymbol(call.getQualifier()));
				if (qAlias.isEmpty())
					return;
				for (QualifierSymbol alias : qAlias.castElements(QualifierSymbol.class)) {
					cu = call.getProgram().getUnit(alias.getQualifier());
					if (cu != null)
						possibleUnits.add(cu);
				}
			}
		}

		Set<Unit> seen = new HashSet<>();

		unitLoop: for (Unit targetUnit : possibleUnits) {
			int lowestDistance = Integer.MAX_VALUE;
			Collection<CFG> bestTargets = new LinkedHashSet<>();
			Collection<NativeCFG> bestNatives = new LinkedHashSet<>();

			if (!(targetUnit instanceof CompilationUnit)) {
				if (!seen.add(targetUnit))
					continue;
				for (CodeMember cm : targetUnit.getCodeMembers()) {
					if (!isATarget(call, aliasing, cm, types, false))
						continue;
					int distance = matching.distanceFromPerfectTarget(call, types, cm, false);
					if (distance < 0)
						continue; // incomparable
					if (distance < lowestDistance) {
						lowestDistance = distance;
						bestTargets.clear();
						bestNatives.clear();
						addTarget(cm, bestTargets, bestNatives);
						if (distance == 0) {
							targets.addAll(bestTargets);
							natives.addAll(bestNatives);
							continue unitLoop;
						}
					} else if (distance == lowestDistance)
						throw new CallResolutionException(
								"Multiple call targets for call " + call + " found in " + targetUnit +
										": " + bestTargets + " " + bestNatives);
				}
			} else
				for (CompilationUnit cu : strategy.traverse(call, (CompilationUnit) targetUnit)) {
					if (!seen.add(cu))
						continue;
					for (CodeMember cm : cu.getCodeMembers()) {
						if (!isATarget(call, aliasing, cm, types, false))
							continue;
						int distance = matching.distanceFromPerfectTarget(call, types, cm, false);
						if (distance < 0)
							continue; // incomparable
						if (distance < lowestDistance) {
							lowestDistance = distance;
							bestTargets.clear();
							bestNatives.clear();
							addTarget(cm, bestTargets, bestNatives);
							if (distance == 0) {
								targets.addAll(bestTargets);
								natives.addAll(bestNatives);
								continue unitLoop;
							}
						} else if (distance == lowestDistance)
							throw new CallResolutionException(
									"Multiple call targets for call " + call + " found in " + targetUnit +
											": " + bestTargets + " " + bestNatives);
					}
				}
		}
	}

	/**
	 * Resolves an instance call to its possible targets. The resolution process
	 * works as follows:
	 * <ol>
	 * <li>If the call does not have at least one parameter (the receiver), a
	 * {@link CallResolutionException} is thrown.</li>
	 * <li>The possible runtime {@link Type}s of the receiver expression are
	 * computed, and only the ones corresponding to {@link CompilationUnit}s
	 * (using {@link #getReceiverCompilationUnit(Type)}) are considered.</li>
	 * <li>For each receiver type, the hierarchy of its unit is traversed using
	 * the program's {@link HierarchyTraversalStrategy}.</li>
	 * <li>For each candidate {@link CodeMember},
	 * {@link #isATarget(UnresolvedCall, SymbolAliasing, CodeMember, Set[], boolean)}
	 * decides if it is a suitable target for the call.</li>
	 * <li>For each suitable target, its "distance" from a perfect match using
	 * {@link ParameterMatchingStrategy#distanceFromPerfectTarget(UnresolvedCall, Set[], CodeMember, boolean)}.</li>
	 * <li>The best candidate (lowest distance) in the hierarchy is selected as
	 * a possible call target. If a distance of 0 (perfect match) is found,
	 * resolution for that receiver type stops immediately.</li>
	 * <li>If multiple candidates along the same hierarchy have the same best
	 * distance greater than 0, a {@link CallResolutionException} is thrown due
	 * to ambiguity.</li>
	 * <li>The best match of each possible receiver type is added to the
	 * {@code targets}/{@code natives} collections.</li>
	 * </ol>
	 *
	 * @param call     the unresolved instance call to resolve
	 * @param types    the possible runtime types of the call's parameters,
	 *                     where {@code types[0]} corresponds to the receiver
	 * @param targets  the collection where resolved {@link CFG} targets will be
	 *                     stored
	 * @param natives  the collection where resolved {@link NativeCFG} targets
	 *                     will be stored
	 * @param aliasing aliasing information that may affect resolution
	 *
	 * @throws CallResolutionException if the call has no receiver, or if
	 *                                     multiple equally suitable targets are
	 *                                     found
	 */
	public void resolveInstance(
			UnresolvedCall call,
			Set<Type>[] types,
			Collection<CFG> targets,
			Collection<NativeCFG> natives,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		if (call.getParameters().length == 0)
			throw new CallResolutionException(
					"An instance call should have at least one parameter as receiver");
		Expression receiver = call.getParameters()[0];
		HierarchyTraversalStrategy strategy = call.getProgram().getFeatures().getTraversalStrategy();
		ParameterMatchingStrategy matching = call.getProgram().getFeatures().getMatchingStrategy();

		for (Type recType : getPossibleTypesOfReceiver(receiver, types[0])) {
			CompilationUnit unit = getReceiverCompilationUnit(recType);
			if (unit == null)
				continue;

			Set<CompilationUnit> seen = new HashSet<>();
			Collection<CFG> bestTargets = new LinkedHashSet<>();
			Collection<NativeCFG> bestNatives = new LinkedHashSet<>();
			int lowestDistance = Integer.MAX_VALUE;

			hierarchyLoop: for (CompilationUnit cu : strategy.traverse(call, unit)) {
				if (!seen.add(cu))
					continue;
				for (CodeMember cm : cu.getInstanceCodeMembers(false)) {
					if (!isATarget(call, aliasing, cm, types, true))
						continue;
					int distance = matching.distanceFromPerfectTarget(call, types, cm, true);
					if (distance < 0)
						continue; // incomparable
					if (distance < lowestDistance) {
						lowestDistance = distance;
						bestTargets.clear();
						bestNatives.clear();
						addTarget(cm, bestTargets, bestNatives);
						if (distance == 0)
							break hierarchyLoop;
					} else if (distance == lowestDistance)
						// we allow only one target in each compilation unit
						throw new CallResolutionException(
								"Multiple call targets for call " + call + " found in " + unit +
										": " + bestTargets + " " + bestNatives);
				}
			}

			targets.addAll(bestTargets);
			natives.addAll(bestNatives);
		}
	}

	/**
	 * Returns the {@link CompilationUnit} associated with a receiver type.
	 * <p>
	 * Supports both unit types and pointer-to-unit types.
	 * </p>
	 *
	 * @param receiverType the type of the receiver
	 *
	 * @return the compilation unit of the receiver, or {@code null} if
	 *             unavailable
	 */
	public CompilationUnit getReceiverCompilationUnit(
			Type receiverType) {
		if (receiverType.isUnitType())
			return receiverType.asUnitType().getUnit();
		if (receiverType.isPointerType() && receiverType.asPointerType().getInnerType().isUnitType())
			return receiverType.asPointerType().getInnerType().asUnitType().getUnit();
		return null;
	}

	/**
	 * Checks whether a given code member is a valid target for a call.
	 *
	 * @param call     the unresolved call
	 * @param aliasing aliasing information
	 * @param cm       the candidate code member
	 * @param types    the possible runtime types of the call's parameters
	 * @param instance whether the call is an instance call
	 *
	 * @return {@code true} if the code member is a valid target, {@code false}
	 *             otherwise
	 */
	public boolean isATarget(
			UnresolvedCall call,
			SymbolAliasing aliasing,
			CodeMember cm,
			Set<Type>[] types,
			boolean instance) {
		CodeMemberDescriptor descr = cm.getDescriptor();
		if (instance != descr.isInstance() || cm instanceof AbstractCodeMember)
			return false;

		String qualifier = descr.getUnit().getName();
		String name = descr.getName();

		boolean target = false;
		if (aliasing != null)
			if (matchesAlias(call, aliasing, name, qualifier))
				target = true;

		if (!target)
			target = matchCodeMemberName(call, qualifier, name);

		ParameterMatchingStrategy strategy = call.getProgram().getFeatures().getMatchingStrategy();
		return target && strategy.matches(call, descr.getFormals(), call.getParameters(), types);
	}

	/**
	 * Checks whether the given call matches any known aliases for a member.
	 *
	 * @param call      the unresolved call
	 * @param aliasing  aliasing information
	 * @param name      the original method name
	 * @param qualifier the original qualifier (class or namespace)
	 *
	 * @return {@code true} if the call matches an alias, {@code false}
	 *             otherwise
	 */
	public boolean matchesAlias(
			UnresolvedCall call,
			SymbolAliasing aliasing,
			String name,
			String qualifier) {
		Aliases nAlias = aliasing.getState(new NameSymbol(name));
		Aliases qAlias = aliasing.getState(new QualifierSymbol(qualifier));
		Aliases qnAlias = aliasing.getState(new QualifiedNameSymbol(qualifier, name));

		if (!qnAlias.isEmpty())
			for (QualifiedNameSymbol alias : qnAlias.castElements(QualifiedNameSymbol.class))
				if (matchCodeMemberName(call, alias.getQualifier(), alias.getName()))
					return true;

		if (!qAlias.isEmpty())
			for (QualifierSymbol alias : qAlias.castElements(QualifierSymbol.class))
				if (matchCodeMemberName(call, alias.getQualifier(), name))
					return true;

		if (!nAlias.isEmpty())
			for (NameSymbol alias : nAlias.castElements(NameSymbol.class))
				if (matchCodeMemberName(call, qualifier, alias.getName()))
					return true;

		return false;
	}

	/**
	 * Adds a resolved code member to the appropriate collection based on
	 * whether it is a {@link CFG} or a {@link NativeCFG}.
	 *
	 * @param cm            the code member
	 * @param cfgTargets    the collection for CFG targets
	 * @param nativeTargets the collection for native CFG targets
	 */
	public void addTarget(
			CodeMember cm,
			Collection<CFG> cfgTargets,
			Collection<NativeCFG> nativeTargets) {
		if (cm instanceof CFG)
			cfgTargets.add((CFG) cm);
		else
			nativeTargets.add((NativeCFG) cm);
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
	public boolean matchCodeMemberName(
			UnresolvedCall call,
			String qualifier,
			String name) {
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
	public abstract Collection<Type> getPossibleTypesOfReceiver(
			Expression receiver,
			Set<Type> types)
			throws CallResolutionException;

	@Override
	public Collection<Call> getCallSites(
			CodeMember cm) {
		return callsites.getOrDefault(cm, Collections.emptyList());
	}

}
