package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.ConditionViolation;
import it.unive.lisa.checks.verification.ErrorViolation;
import it.unive.lisa.checks.verification.ReachViolation;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.triage.encoding.SortPolicy;
import it.unive.lisa.triage.inputs.InputSource;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import it.unive.lisa.triage.reachability.Reachability;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.tuple.Triple;

/**
 * Builds the {@link AlarmModel} of a candidate violation, given the results of
 * an analysis. The locations of the model are the nodes of the cfg that can
 * reach the bad location (its <i>cone</i>): the node of the statement on which
 * the warning would be issued, or, for {@link ConditionViolation}s, the node
 * containing the statement after which the condition is evaluated.<br>
 * <br>
 * Calls to cfgs can be <i>expanded</i>, given the frames of their callees (see
 * {@link Frame#enter(it.unive.lisa.program.cfg.statement.call.CFGCall, AnalyzedCFG)}
 * and {@link ReplayInterprocedural}): the model then also contains the
 * locations of the frames of the expanded callees (the nodes of their cfgs that
 * can reach an exit point, recursively expanding their own calls), and the
 * locations where the callers resume after the callees return, connected by the
 * transitions entering and returning from the callees. Since the number of
 * locations can grow quickly, expansion stops when the model has more locations
 * than a given budget (the remaining calls are summarized). The model is
 * finally restricted to the locations that can reach the bad location.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class AlarmModelBuilder {

	/**
	 * The default maximum number of locations of a model, beyond which calls
	 * are no longer expanded.
	 */
	public static final int DEFAULT_MAX_LOCATIONS = 2000;

	private AlarmModelBuilder() {
	}

	/**
	 * Builds the model of the given violation, summarizing all calls.
	 *
	 * @param <A>       the type of the abstract states of the analysis
	 * @param <D>       the type of the domain of the analysis
	 * @param tool      the tool containing the results of the analysis
	 * @param result    the result containing the statement
	 * @param st        the statement on which the warning would be issued
	 * @param violation the violation
	 * @param maxCases  the maximum number of cases of each transition
	 *
	 * @return the model
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               model
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AlarmModel build(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation,
			int maxCases)
			throws SemanticException {
		return build(tool, result, st, violation, maxCases, Collections.emptySet(), DEFAULT_MAX_LOCATIONS);
	}

	/**
	 * Builds the model of the given violation, expanding the calls whose callee
	 * frames are all in the given set, with {@link #DEFAULT_MAX_LOCATIONS} as
	 * budget of locations.
	 *
	 * @param <A>       the type of the abstract states of the analysis
	 * @param <D>       the type of the domain of the analysis
	 * @param tool      the tool containing the results of the analysis
	 * @param result    the result containing the statement
	 * @param st        the statement on which the warning would be issued
	 * @param violation the violation
	 * @param maxCases  the maximum number of cases of each transition
	 * @param expanded  the frames of the callees to expand
	 *
	 * @return the model
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               model
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AlarmModel build(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation,
			int maxCases,
			Set<Frame> expanded)
			throws SemanticException {
		return build(tool, result, st, violation, maxCases, expanded, DEFAULT_MAX_LOCATIONS);
	}

	/**
	 * Builds the model of the given violation, expanding the calls whose callee
	 * frames are all in the given set, as long as the model has at most
	 * {@code maxLocations} locations.
	 *
	 * @param <A>          the type of the abstract states of the analysis
	 * @param <D>          the type of the domain of the analysis
	 * @param tool         the tool containing the results of the analysis
	 * @param result       the result containing the statement
	 * @param st           the statement on which the warning would be issued
	 * @param violation    the violation
	 * @param maxCases     the maximum number of cases of each transition
	 * @param expanded     the frames of the callees to expand
	 * @param maxLocations the maximum number of locations of the model, beyond
	 *                         which calls are no longer expanded
	 *
	 * @return the model
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               model
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AlarmModel build(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation,
			int maxCases,
			Set<Frame> expanded,
			int maxLocations)
			throws SemanticException {
		return build(tool, result, st, violation, maxCases, expanded, maxLocations, Collections.emptyList());
	}

	/**
	 * Builds the model of the given violation starting from a caller of the
	 * result containing the statement, reached through the given chain of
	 * calls: the root frame of the model is the one of the first caller, and
	 * the calls of the chain are expanded, so that the bad location is in the
	 * frame of the result. Since the model only considers the executions going
	 * through the chain, it can confirm the violation, but not refute it. With
	 * an empty chain, the model starts from the result itself.
	 *
	 * @param <A>          the type of the abstract states of the analysis
	 * @param <D>          the type of the domain of the analysis
	 * @param tool         the tool containing the results of the analysis
	 * @param result       the result containing the statement
	 * @param st           the statement on which the warning would be issued
	 * @param violation    the violation
	 * @param maxCases     the maximum number of cases of each transition
	 * @param expanded     the frames of the callees to expand
	 * @param maxLocations the maximum number of locations of the model, beyond
	 *                         which calls are no longer expanded
	 * @param chain        the chain of calls, from the first caller: for each
	 *                         call, the result of the caller, its node
	 *                         containing the call, and the call (the callee of
	 *                         the last call is the result)
	 *
	 * @return the model
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               model
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AlarmModel build(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation,
			int maxCases,
			Set<Frame> expanded,
			int maxLocations,
			List<Triple<AnalyzedCFG<A>, Statement, CFGCall>> chain)
			throws SemanticException {
		return build(tool, result, st, violation, maxCases, expanded, maxLocations, chain, Long.MAX_VALUE);
	}

	/**
	 * Builds the model of the given violation as
	 * {@link #build(SemanticTool, AnalyzedCFG, Statement, Violation, int, Set, int, List)}
	 * does, expanding calls only until the given time, and abandoning the
	 * construction of the model after it.
	 *
	 * @param <A>          the type of the abstract states of the analysis
	 * @param <D>          the type of the domain of the analysis
	 * @param tool         the tool containing the results of the analysis
	 * @param result       the result containing the statement
	 * @param st           the statement on which the warning would be issued
	 * @param violation    the violation
	 * @param maxCases     the maximum number of cases of each transition
	 * @param expanded     the frames of the callees to expand
	 * @param maxLocations the maximum number of locations of the model, beyond
	 *                         which calls are no longer expanded
	 * @param chain        the chain of calls, from the first caller (see
	 *                         {@link #build(SemanticTool, AnalyzedCFG, Statement, Violation, int, Set, int, List)})
	 * @param deadline     the time (as given by
	 *                         {@link System#currentTimeMillis()}) after which
	 *                         calls are no longer expanded, and the
	 *                         construction of the model is abandoned
	 *
	 * @return the model, or {@code null} if the deadline passed before the
	 *             model could be built
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               model
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AlarmModel build(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation,
			int maxCases,
			Set<Frame> expanded,
			int maxLocations,
			List<Triple<AnalyzedCFG<A>, Statement, CFGCall>> chain,
			long deadline)
			throws SemanticException {
		return build(tool, result, st, violation, maxCases, expanded, maxLocations, chain, deadline,
				new ResultCache<>());
	}

	/**
	 * Builds the model of the given violation as
	 * {@link #build(SemanticTool, AnalyzedCFG, Statement, Violation, int, Set, int, List, long)}
	 * does, using the given cache of the information computed from the results
	 * of the analysis, that can be shared by all the models built over the same
	 * results. If the given time passes before all the transitions of the model
	 * have been extracted, no model is built.
	 *
	 * @param <A>          the type of the abstract states of the analysis
	 * @param <D>          the type of the domain of the analysis
	 * @param tool         the tool containing the results of the analysis
	 * @param result       the result containing the statement
	 * @param st           the statement on which the warning would be issued
	 * @param violation    the violation
	 * @param maxCases     the maximum number of cases of each transition
	 * @param expanded     the frames of the callees to expand
	 * @param maxLocations the maximum number of locations of the model, beyond
	 *                         which calls are no longer expanded
	 * @param chain        the chain of calls, from the first caller (see
	 *                         {@link #build(SemanticTool, AnalyzedCFG, Statement, Violation, int, Set, int, List)})
	 * @param deadline     the time (as given by
	 *                         {@link System#currentTimeMillis()}) after which
	 *                         calls are no longer expanded, and the
	 *                         construction of the model is abandoned
	 * @param cache        the cache
	 *
	 * @return the model, or {@code null} if the deadline passed before the
	 *             model could be built
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               model
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AlarmModel build(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation,
			int maxCases,
			Set<Frame> expanded,
			int maxLocations,
			List<Triple<AnalyzedCFG<A>, Statement, CFGCall>> chain,
			long deadline,
			ResultCache<A> cache)
			throws SemanticException {
		return build(tool, result, st, violation, maxCases, expanded, maxLocations, chain, deadline, cache, null);
	}

	/**
	 * The transitions extracted while building a model, that can be reused by
	 * the next models built for the same violation and chain of calls, which
	 * only expand more calls (see
	 * {@link AlarmModelBuilder#build(SemanticTool, AnalyzedCFG, Statement, Violation, int, Set, int, List, long, ResultCache, Reuse)}):
	 * the transitions of a location are reused unless the location reaches
	 * calls whose callees are expanded by the next model and not by the
	 * previous one. The fresh values used by the models are produced by the
	 * same factory, so that they are never confused.
	 */
	public static final class Reuse {

		private FreshValueFactory factory;

		private final Map<ModelLocation, NodeTransitions> transitions = new LinkedHashMap<>();

		/**
		 * Discards the transitions of the locations reaching calls to the given
		 * frames, that are going to be expanded.
		 *
		 * @param frames the frames
		 */
		public void expand(
				Collection<Frame> frames) {
			transitions.values().removeIf(tr -> {
				for (ReplayedCall call : tr.getCalls())
					for (Frame f : call.getCalleeFrames())
						if (frames.contains(f))
							return true;
				return false;
			});
		}
	}

	/**
	 * Builds the model of the given violation as
	 * {@link #build(SemanticTool, AnalyzedCFG, Statement, Violation, int, Set, int, List, long, ResultCache)}
	 * does, reusing the transitions extracted by a previous model built for the
	 * same violation and chain of calls, but expanding fewer calls.
	 *
	 * @param <A>          the type of the abstract states of the analysis
	 * @param <D>          the type of the domain of the analysis
	 * @param tool         the tool containing the results of the analysis
	 * @param result       the result containing the statement
	 * @param st           the statement on which the warning would be issued
	 * @param violation    the violation
	 * @param maxCases     the maximum number of cases of each transition
	 * @param expanded     the frames of the callees to expand
	 * @param maxLocations the maximum number of locations of the model, beyond
	 *                         which calls are no longer expanded
	 * @param chain        the chain of calls, from the first caller (see
	 *                         {@link #build(SemanticTool, AnalyzedCFG, Statement, Violation, int, Set, int, List)})
	 * @param deadline     the time (as given by
	 *                         {@link System#currentTimeMillis()}) after which
	 *                         calls are no longer expanded, and the
	 *                         construction of the model is abandoned
	 * @param cache        the cache
	 * @param reuse        the transitions of the previous models, updated with
	 *                         the ones of this model ({@code null} means that
	 *                         nothing is reused)
	 *
	 * @return the model, or {@code null} if the deadline passed before the
	 *             model could be built
	 *
	 * @throws SemanticException if something goes wrong while building the
	 *                               model
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AlarmModel build(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation,
			int maxCases,
			Set<Frame> expanded,
			int maxLocations,
			List<Triple<AnalyzedCFG<A>, Statement, CFGCall>> chain,
			long deadline,
			ResultCache<A> cache,
			Reuse reuse)
			throws SemanticException {
		Statement bad = violation instanceof ConditionViolation
				? rootOf(((ConditionViolation) violation).getWhere())
				: rootOf(st);

		AnalyzedCFG<A> root = chain.isEmpty() ? result : chain.get(0).getLeft();
		Frame frame = Frame.root(root);
		// the calls of the chain are always expanded
		Frame badFrame = frame;
		Set<Frame> forced = new LinkedHashSet<>(expanded);
		for (int i = 0; i < chain.size(); i++) {
			AnalyzedCFG<A> callee = i + 1 < chain.size() ? chain.get(i + 1).getLeft() : result;
			badFrame = badFrame.enter(chain.get(i).getRight(), callee);
			forced.add(badFrame);
		}
		Frame target = badFrame;
		ModelLocation badLocation = new ModelLocation(badFrame, bad);
		Set<ModelLocation> cone = new LinkedHashSet<>();
		for (Statement node : cone(root, chain.isEmpty() ? bad : chain.get(0).getMiddle()))
			cone.add(new ModelLocation(frame, node));

		InputSourceResolver resolver = new InputSourceResolver(tool);
		FreshValueFactory factory = reuse != null && reuse.factory != null ? reuse.factory
				: new FreshValueFactory(resolver, new HeapModel(root.getProgram()));
		if (reuse != null)
			reuse.factory = factory;
		// the locations are extracted once, in the order they are discovered:
		// the ones of the cone first, then the ones of the expanded callees
		// and the ones where callers resume
		Set<ModelLocation> locations = new LinkedHashSet<>(cone);
		LinkedList<ModelLocation> ws = new LinkedList<>(cone);
		boolean[] truncated = { false };
		BooleanSupplier budget = () -> {
			if (locations.size() < maxLocations && System.currentTimeMillis() < deadline)
				return true;
			truncated[0] = true;
			return false;
		};
		Map<Frame, TransitionExtractor<A, D>> extractors = new LinkedHashMap<>();
		extractors.put(frame, new TransitionExtractor<>(tool, frame, factory, maxCases, forced, budget, null, null,
				cache));
		Map<ModelLocation, NodeTransitions> transitions = new LinkedHashMap<>();
		Map<ModelLocation, AnalysisState<A>> preStates = new LinkedHashMap<>();
		boolean expansion = false;
		while (!ws.isEmpty()) {
			if (System.currentTimeMillis() >= deadline)
				return null;
			ModelLocation loc = ws.removeFirst();
			TransitionExtractor<A, D> extractor = extractors.get(loc.getFrame());
			// calls in the bad node are not expanded, so that the property is
			// checked at a single location, unless the violation is about the
			// errors they raise
			NodeTransitions tr = reuse == null ? null : reuse.transitions.get(loc);
			if (tr == null) {
				tr = extractor.extract(loc,
						!(loc.getFrame().equals(target) && loc.getNode().equals(bad))
								|| violation instanceof ErrorViolation);
				if (reuse != null)
					reuse.transitions.put(loc, tr);
			}
			transitions.put(loc, tr);
			preStates.put(loc, extractor.preStateOf(loc.getNode()));
			for (EdgeTransition e : tr.getOutgoing()) {
				ModelLocation dest = e.getDestination();
				if (e.getEdge() != null || locations.contains(dest))
					continue;
				expansion = true;
				Frame callee = dest.getFrame();
				if (dest.getResumed() != null)
					// the location where the caller resumes
					add(dest, locations, ws);
				else if (!extractors.containsKey(callee)) {
					// a new callee frame, with all its nodes: the ones that
					// cannot return to the caller nor raise errors leaving
					// the callee are pruned later
					extractors.put(callee, new TransitionExtractor<>(tool, callee, factory, maxCases, forced,
							budget, loc.getNode(), extractor, cache));
					for (Statement node : callee.getResult().getNodes())
						add(new ModelLocation(callee, node), locations, ws);
				}
			}
		}

		List<BadCase> badCases = null;
		if (violation instanceof ErrorViolation && expansion && entersCallees(transitions, frame, bad)) {
			// the errors raised by the bad node, either by itself or by the
			// callees it enters, reach a synthetic location that is the bad
			// one
			ErrorViolation err = (ErrorViolation) violation;
			ModelLocation raised = ModelLocation.raised(frame, bad);
			for (ModelLocation loc : new ArrayList<>(locations)) {
				NodeTransitions tr = transitions.get(loc);
				List<EdgeTransition> more = new ArrayList<>();
				if (loc.getFrame().isRoot() && loc.getNode().equals(bad))
					for (Map.Entry<Error, SymbolicStore> e : tr.getErrors().entrySet())
						if (err.matches(e.getKey().getType()))
							more.add(new EdgeTransition(loc, raised, null, e.getValue(), e.getKey().getThrower()));
				for (NodeTransitions.EscapingError e : tr.getEscaping())
					if (e.getNode().equals(bad) && err.matches(e.getError().getType()))
						more.add(new EdgeTransition(loc, raised, null, e.getStore(), e.getOrigin()));
				transitions.put(loc, tr.withOutgoing(more));
			}
			locations.add(raised);
			transitions.put(raised, new NodeTransitions(raised, List.of(), null, Map.of(), Map.of()));
			preStates.put(raised, preStates.get(badLocation));
			badLocation = raised;
			// reaching the location is the violation
			badCases = List.of(new BadCase(List.of(), null));
		}

		if (expansion) {
			// only the locations that can reach the bad one are relevant
			Set<ModelLocation> relevant = backwardReachable(badLocation, locations, transitions);
			locations.retainAll(relevant);
			transitions.keySet().retainAll(relevant);
		}

		Set<ModelLocation> initial = new LinkedHashSet<>();
		for (Statement entry : root.getEntrypoints()) {
			ModelLocation loc = new ModelLocation(frame, entry);
			if (locations.contains(loc))
				initial.add(loc);
		}

		// the transitions of the root frame are also kept by node, since the
		// order in which they are visited determines the order of the
		// vocabulary; the ones of other locations follow, in the order they
		// have been discovered
		Map<Statement, NodeTransitions> byNode = new LinkedHashMap<>();
		List<NodeTransitions> others = new ArrayList<>();
		for (ModelLocation loc : locations)
			if (loc.getFrame().isRoot() && loc.getResumed() == null)
				byNode.put(loc.getNode(), transitions.get(loc));
			else
				others.add(transitions.get(loc));
		List<NodeTransitions> all = new ArrayList<>(byNode.values());
		all.addAll(others);

		if (badCases == null)
			// the bad location might be missing if the calls of the chain
			// could not be expanded
			badCases = transitions.containsKey(badLocation)
					? badCases(transitions.get(badLocation), violation, factory)
					: new ArrayList<>();

		Map<Identifier, InputSource> parameters = new LinkedHashMap<>();
		for (Parameter formal : root.getDescriptor().getFormals()) {
			InputSource source = resolver.resolve(new PushAny(formal.getStaticType(), formal.getLocation()), null);
			parameters.put(formal.toSymbolicVariable(), source);
		}

		Set<Identifier> vocabulary = new LinkedHashSet<>(parameters.keySet());
		for (NodeTransitions tr : all)
			collect(tr, vocabulary);
		for (BadCase c : badCases) {
			for (SymbolicExpression cond : c.getPathCondition())
				collect(cond, vocabulary);
			if (c.getViolation() != null)
				collect(c.getViolation(), vocabulary);
		}
		// identifiers that are written but never read cannot influence any
		// execution of the model: dropping them avoids encoding their values
		// (e.g., the unused results of calls, that are not computed precisely)
		Set<Identifier> read = new LinkedHashSet<>(parameters.keySet());
		for (NodeTransitions tr : all)
			collectRead(tr, read);
		for (BadCase c : badCases) {
			for (SymbolicExpression cond : c.getPathCondition())
				collect(cond, read);
			if (c.getViolation() != null)
				collect(c.getViolation(), read);
		}
		vocabulary.retainAll(read);
		// identifiers renamed by scoping take their values from the
		// identifiers they are renamed from, that are thus read as well;
		// when callees are expanded, only the identifiers that are visible in
		// some frame are relevant (e.g., popping a scope renames any
		// identifier, even the ones that are already out of that scope)
		Set<List<ScopeToken>> scopes = new LinkedHashSet<>();
		for (Frame f : extractors.keySet())
			scopes.addAll(scopesOf(f));
		// only the cases pushing or popping scopes rename identifiers
		List<SymbolicCase> scoping = new ArrayList<>();
		for (NodeTransitions tr : all)
			for (SymbolicStore store : storesOf(tr))
				for (SymbolicCase c : store.getCases())
					if (c.hasScopings())
						scoping.add(c);
		boolean grown = !scoping.isEmpty();
		while (grown) {
			grown = false;
			for (SymbolicCase c : scoping)
				for (Identifier id : new ArrayList<>(vocabulary))
					if (!c.getUpdates().containsKey(id) && !c.isUnconstrained(id)) {
						Identifier pre = c.preIdentifier(id);
						if (!expansion || scopes.contains(scopesOf(pre)))
							grown |= vocabulary.add(pre);
					}
		}

		// each frame exports constraints in its own context
		Map<Frame, ConstraintExporter<A, D>> exporters = new LinkedHashMap<>();
		Map<ModelLocation, Set<SymbolicExpression>> seeds = new LinkedHashMap<>();
		Set<SymbolicExpression> conditions = conditions(all, badCases);
		Map<SymbolicExpression, Set<Identifier>> conditionIds = new LinkedHashMap<>();
		for (SymbolicExpression cond : conditions) {
			Set<Identifier> ids = new LinkedHashSet<>();
			collect(cond, ids);
			conditionIds.put(cond, ids);
		}
		for (ModelLocation loc : locations) {
			Statement node = loc.getNode();
			ConstraintExporter<A, D> exporter = exporters.computeIfAbsent(loc.getFrame(),
					f -> new ConstraintExporter<>(tool.getAnalysis().domain, f.getResult().getId()));
			Set<SymbolicExpression> cs = exporter.constraints(preStates.get(loc), vocabulary, node);
			if (cs == null)
				continue;
			if (loc.getResumed() != null || loc.isRaised()) {
				// the state before the node does not describe the values
				// after the callees returned
				seeds.put(loc, new LinkedHashSet<>());
				continue;
			}
			// conditions of the model whose truth is decided by the analysis
			// are invariants, even if the domain cannot express them as
			// constraints
			AnalysisState<A> pre = preStates.get(loc);
			for (SymbolicExpression cond : cache.decidesConditions() ? conditions
					: Collections.<SymbolicExpression>emptySet()) {
				// conditions over identifiers unknown to the state cannot be
				// decided by the analysis
				if (!knowsAll(pre, conditionIds.get(cond)))
					continue;
				try {
					// an analysis without events, as the queries are not part
					// of the fixpoint computation
					Satisfiability sat = extractors.get(loc.getFrame()).getAnalysis().satisfies(pre, cond,
							loc.getFrame().getResult().getId(), node);
					if (sat == Satisfiability.SATISFIED)
						cs.add(cond);
					else if (sat == Satisfiability.NOT_SATISFIED)
						cs.add(new UnaryExpression(cond.getStaticType(), cond, LogicalNegation.INSTANCE,
								cond.getCodeLocation()));
				} catch (SemanticException e) {
					// the condition cannot be evaluated in this state
				}
			}
			seeds.put(loc, cs);
		}
		Set<SymbolicExpression> init = new LinkedHashSet<>();
		if (!initial.isEmpty()) {
			Set<SymbolicExpression> cs = exporters.get(frame).constraints(root.getEntryState(), vocabulary,
					initial.iterator().next().getNode());
			if (cs != null)
				init.addAll(cs);
		}

		Map<Identifier, Set<Type>> types = new LinkedHashMap<>();
		for (ModelLocation loc : locations) {
			AnalysisState<A> pre = preStates.get(loc);
			if (pre.getExecution().isBottom())
				continue;
			TransitionExtractor<A, D> extractor = extractors.get(loc.getFrame());
			for (Identifier id : vocabulary)
				if (pre.getExecutionState().knowsIdentifier(id))
					types.computeIfAbsent(id, i -> new LinkedHashSet<>())
							.addAll(extractor.getAnalysis().getRuntimeTypesOf(pre, id,
									loc.getFrame().getResult().getId(), loc.getNode()));
		}
		// the identifiers of the callers visible in callees hold the values of
		// the identifiers of the callers (whose types might be unknown where
		// they are out of scope)
		for (Identifier id : vocabulary)
			if (id instanceof OutOfScopeIdentifier) {
				Identifier current = id;
				for (ScopeToken token : scopesOf(id))
					current = current instanceof OutOfScopeIdentifier
							? ((OutOfScopeIdentifier) current).popScope(token, null)
							: current;
				Set<Type> original = current == null ? null : types.get(current);
				if (original != null)
					types.computeIfAbsent(id, i -> new LinkedHashSet<>()).addAll(original);
			}
		for (FreshValue fresh : factory.getValues().values())
			if (!fresh.getStaticType().isUntyped())
				types.put(fresh, Set.of(fresh.getStaticType()));

		boolean confirmable = root.getId().isStartingId()
				&& root.getProgram().getEntryPoints().stream()
						.anyMatch(cfg -> cfg.getDescriptor().equals(root.getDescriptor()));

		Set<Frame> frames = new LinkedHashSet<>();
		for (ModelLocation loc : locations)
			if (!loc.getFrame().isRoot())
				frames.add(loc.getFrame());

		return new AlarmModel(
				frame,
				badLocation,
				locations,
				initial,
				transitions,
				init,
				badCases,
				seeds,
				vocabulary,
				new SortPolicy(types, bad.getProgram().getTypes().getTypes(), factory.getHeapModel().fieldTypes()),
				factory.getValues(),
				parameters,
				confirmable,
				frames,
				truncated[0]);
	}

	private static boolean knowsAll(
			AnalysisState<?> state,
			Set<Identifier> ids) {
		for (Identifier id : ids)
			if (!state.getExecutionState().knowsIdentifier(id))
				return false;
		return true;
	}

	/**
	 * Yields the scopes of the identifiers of the callers that are visible in
	 * the given frame, from the innermost one: for each prefix of the chain of
	 * calls leading to the frame (from the last call), the tokens of the scopes
	 * pushed by the calls.
	 */
	private static Set<List<ScopeToken>> scopesOf(
			Frame frame) {
		Set<List<ScopeToken>> result = new LinkedHashSet<>();
		List<ScopeToken> chain = new ArrayList<>();
		result.add(new ArrayList<>(chain));
		for (Frame f = frame; !f.isRoot(); f = f.getParent()) {
			chain.add(new ScopeToken(f.getCall()));
			result.add(new ArrayList<>(chain));
		}
		return result;
	}

	/**
	 * Yields the tokens of the scopes the given identifier is out of, from the
	 * innermost one.
	 */
	private static List<ScopeToken> scopesOf(
			Identifier id) {
		List<ScopeToken> chain = new ArrayList<>();
		Identifier current = id;
		while (current instanceof OutOfScopeIdentifier) {
			ScopeToken token = ((OutOfScopeIdentifier) current).getScope();
			chain.add(token);
			try {
				current = ((OutOfScopeIdentifier) current).popScope(token, null);
			} catch (SemanticException e) {
				break;
			}
		}
		return chain;
	}

	/**
	 * Yields {@code true} if some location of the given node, in the given
	 * frame, enters the frame of a callee.
	 */
	private static boolean entersCallees(
			Map<ModelLocation, NodeTransitions> transitions,
			Frame frame,
			Statement node) {
		for (Map.Entry<ModelLocation, NodeTransitions> tr : transitions.entrySet())
			if (tr.getKey().getFrame().equals(frame) && tr.getKey().getNode().equals(node))
				for (EdgeTransition e : tr.getValue().getOutgoing())
					if (e.getEdge() == null && !e.getDestination().getFrame().equals(frame))
						return true;
		return false;
	}

	private static void add(
			ModelLocation loc,
			Set<ModelLocation> locations,
			LinkedList<ModelLocation> ws) {
		if (locations.add(loc))
			ws.add(loc);
	}

	/**
	 * The locations that can reach the given one through the transitions of the
	 * given locations, including the location itself.
	 */
	private static Set<ModelLocation> backwardReachable(
			ModelLocation target,
			Set<ModelLocation> locations,
			Map<ModelLocation, NodeTransitions> transitions) {
		Map<ModelLocation, Set<ModelLocation>> predecessors = new LinkedHashMap<>();
		for (ModelLocation loc : locations)
			for (EdgeTransition e : transitions.get(loc).getOutgoing())
				if (locations.contains(e.getDestination()))
					predecessors.computeIfAbsent(e.getDestination(), d -> new LinkedHashSet<>()).add(loc);
		Set<ModelLocation> reached = new LinkedHashSet<>();
		LinkedList<ModelLocation> ws = new LinkedList<>();
		ws.add(target);
		while (!ws.isEmpty()) {
			ModelLocation current = ws.removeFirst();
			if (reached.add(current))
				ws.addAll(predecessors.getOrDefault(current, Collections.emptySet()));
		}
		return reached;
	}

	private static Statement rootOf(
			Statement st) {
		// expressions synthesized by the semantics of other statements might
		// have no root in the cfg
		Statement root = st instanceof Expression ? ((Expression) st).getRootStatement() : st;
		if (st.getCFG() == null || st.getCFG().containsNode(root))
			return root;
		Statement node = Reachability.nodeOf(st);
		return node == null ? root : node;
	}

	/**
	 * The nodes that can reach the given one, including the node itself.
	 */
	private static Set<Statement> cone(
			AnalyzedCFG<?> result,
			Statement node) {
		Set<Statement> cone = new LinkedHashSet<>();
		LinkedList<Statement> ws = new LinkedList<>();
		ws.add(node);
		while (!ws.isEmpty()) {
			Statement current = ws.removeFirst();
			if (cone.add(current))
				ws.addAll(result.predecessorsOf(current));
		}
		return cone;
	}

	private static List<BadCase> badCases(
			NodeTransitions bad,
			Violation violation,
			FreshValueFactory factory) {
		List<BadCase> cases = new ArrayList<>();
		if (violation instanceof ReachViolation)
			// reaching the bad location is the violation
			cases.add(new BadCase(List.of(), null));
		else if (violation instanceof ConditionViolation) {
			ConditionViolation cond = (ConditionViolation) violation;
			SymbolicStore store = bad.getPartial(cond.getWhere());
			if (store != null)
				for (SymbolicCase c : store.getCases())
					cases.add(new BadCase(
							c.getPathCondition(),
							c.substitute(cond.getCondition(), factory, cond.getWhere())));
		} else if (violation instanceof ErrorViolation) {
			ErrorViolation err = (ErrorViolation) violation;
			for (Map.Entry<Error, SymbolicStore> e : bad.getErrors().entrySet())
				if (err.matches(e.getKey().getType()))
					for (SymbolicCase c : e.getValue().getCases())
						cases.add(new BadCase(c.getPathCondition(), null));
		}
		return cases;
	}

	/**
	 * Yields the conjuncts of the path conditions of the transitions and of the
	 * bad cases that do not contain fresh values nor the heap model, that is,
	 * the ones that can be evaluated in the states computed by the analysis.
	 */
	private static Set<SymbolicExpression> conditions(
			Collection<NodeTransitions> transitions,
			List<BadCase> badCases) {
		List<SymbolicExpression> all = new ArrayList<>();
		for (NodeTransitions tr : transitions)
			for (EdgeTransition e : tr.getOutgoing())
				for (SymbolicCase c : e.getStore().getCases())
					all.addAll(c.getPathCondition());
		for (BadCase c : badCases) {
			all.addAll(c.getPathCondition());
			if (c.getViolation() != null)
				all.add(c.getViolation());
		}
		Set<SymbolicExpression> result = new LinkedHashSet<>();
		for (SymbolicExpression e : all)
			conjuncts(e, result);
		return result;
	}

	private static void conjuncts(
			SymbolicExpression e,
			Set<SymbolicExpression> result) {
		if (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalAnd.INSTANCE) {
			conjuncts(((BinaryExpression) e).getLeft(), result);
			conjuncts(((BinaryExpression) e).getRight(), result);
		} else {
			// the analysis evaluates global variables, but not the rest of the
			// heap model
			SymbolicExpression cond = HeapModel.withoutDefinitions(e);
			if (!hasFreshValues(cond) && !HeapModel.mentionsHeapModel(cond))
				result.add(cond);
		}
	}

	private static boolean hasFreshValues(
			SymbolicExpression e) {
		if (e instanceof FreshValue)
			return true;
		if (e instanceof UnaryExpression)
			return hasFreshValues(((UnaryExpression) e).getExpression());
		if (e instanceof BinaryExpression)
			return hasFreshValues(((BinaryExpression) e).getLeft())
					|| hasFreshValues(((BinaryExpression) e).getRight());
		if (e instanceof TernaryExpression)
			return hasFreshValues(((TernaryExpression) e).getLeft())
					|| hasFreshValues(((TernaryExpression) e).getMiddle())
					|| hasFreshValues(((TernaryExpression) e).getRight());
		return false;
	}

	private static void collect(
			NodeTransitions tr,
			Set<Identifier> ids) {
		List<SymbolicStore> stores = new ArrayList<>();
		for (EdgeTransition e : tr.getOutgoing())
			stores.add(e.getStore());
		if (tr.getExit() != null)
			stores.add(tr.getExit());
		stores.addAll(tr.getErrors().values());
		for (SymbolicStore store : stores)
			for (SymbolicCase c : store.getCases()) {
				for (SymbolicExpression cond : c.getPathCondition())
					collect(cond, ids);
				for (Map.Entry<Identifier, SymbolicExpression> u : c.getUpdates().entrySet()) {
					collect(u.getKey(), ids);
					collect(u.getValue(), ids);
				}
			}
	}

	/**
	 * Collects the identifiers read by the given transitions: the ones in their
	 * path conditions and in the values of their updates (not the updated
	 * identifiers).
	 */
	private static void collectRead(
			NodeTransitions tr,
			Set<Identifier> ids) {
		for (SymbolicStore store : storesOf(tr))
			for (SymbolicCase c : store.getCases()) {
				for (SymbolicExpression cond : c.getPathCondition())
					collect(cond, ids);
				for (SymbolicExpression value : c.getUpdates().values())
					collect(value, ids);
			}
	}

	/**
	 * Yields the stores of the given transitions: the ones of the outgoing
	 * edges, of the exit, and of the errors raised.
	 */
	private static List<SymbolicStore> storesOf(
			NodeTransitions tr) {
		List<SymbolicStore> stores = new ArrayList<>();
		for (EdgeTransition e : tr.getOutgoing())
			stores.add(e.getStore());
		if (tr.getExit() != null)
			stores.add(tr.getExit());
		stores.addAll(tr.getErrors().values());
		return stores;
	}

	private static void collect(
			SymbolicExpression e,
			Set<Identifier> ids) {
		if (e instanceof FreshValue)
			return;
		if (e instanceof Identifier)
			ids.add((Identifier) e);
		else if (e instanceof UnaryExpression)
			collect(((UnaryExpression) e).getExpression(), ids);
		else if (e instanceof BinaryExpression) {
			collect(((BinaryExpression) e).getLeft(), ids);
			collect(((BinaryExpression) e).getRight(), ids);
		} else if (e instanceof TernaryExpression) {
			collect(((TernaryExpression) e).getLeft(), ids);
			collect(((TernaryExpression) e).getMiddle(), ids);
			collect(((TernaryExpression) e).getRight(), ids);
		}
	}
}
