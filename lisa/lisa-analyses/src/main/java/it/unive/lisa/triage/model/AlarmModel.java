package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.encoding.SortPolicy;
import it.unive.lisa.triage.inputs.InputSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A model of the executions of a cfg (in a given context) that might lead to
 * the violation of a property, built by {@link AlarmModelBuilder}. The model is
 * a control-flow automaton whose locations (see {@link ModelLocation}) are the
 * beginnings of the nodes of the cfg that can reach the node where the property is checked (the <i>bad
 * location</i>), and whose edges carry the symbolic transitions extracted from
 * the results of the analysis. If some calls are expanded, the locations also
 * include the ones of the frames of their callees, and the ones where the
 * callers resume after the callees return (see {@link AlarmModelBuilder}). The
 * model contains:
 * <ul>
 * <li>the locations ({@link #getLocations()}) and their transitions
 * ({@link #getTransitions(ModelLocation)}, restricted to edges between
 * locations);</li>
 * <li>the initial locations ({@link #getInitialLocations()}), where the
 * executions start in a state satisfying the initial constraints
 * ({@link #getInitialConstraints()});</li>
 * <li>the bad location and the ways of violating the property there
 * ({@link #getBadCases()});</li>
 * <li>the invariants computed by the analysis at each location, as constraints
 * ({@link #getSeeds(ModelLocation)});</li>
 * <li>the identifiers used in the model, with the sorts to encode them, the
 * fresh values used in the transitions, and the sources of the parameters of
 * the cfg.</li>
 * </ul>
 * A model is <i>confirmable</i> if its initial states correspond to the
 * beginning of the program, so that an execution of the model reaching the bad
 * location corresponds to an execution of the program.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AlarmModel {

	private final Frame root;

	private final ModelLocation badLocation;

	private final Set<ModelLocation> locations;

	private final Set<ModelLocation> initial;

	private final Map<ModelLocation, NodeTransitions> transitions;

	private final Set<SymbolicExpression> initialConstraints;

	private final List<BadCase> badCases;

	private final Map<ModelLocation, Set<SymbolicExpression>> seeds;

	private final Set<Identifier> vocabulary;

	private final SortPolicy sorts;

	private final Map<String, FreshValue> freshValues;

	private final Map<Identifier, InputSource> parameters;

	private final boolean confirmable;

	private final Set<Frame> frames;

	private final boolean truncated;

	/**
	 * Builds the model.
	 *
	 * @param root               the root frame of the model, whose result is
	 *                               the one the model is built from
	 * @param badLocation        the bad location
	 * @param locations          the locations
	 * @param initial            the initial locations
	 * @param transitions        the transitions of each location
	 * @param initialConstraints the constraints that hold in the initial states
	 * @param badCases           the ways of violating the property at the bad
	 *                               location
	 * @param seeds              the invariants of each reachable location
	 *                               (unreachable locations are not in the map)
	 * @param vocabulary         the identifiers used in the model
	 * @param sorts              the policy deciding the sorts of identifiers
	 * @param freshValues        the fresh values used in the model, by name
	 * @param parameters         the sources of the parameters of the cfg
	 * @param confirmable        whether or not the model is confirmable
	 */
	public AlarmModel(
			Frame root,
			ModelLocation badLocation,
			Set<ModelLocation> locations,
			Set<ModelLocation> initial,
			Map<ModelLocation, NodeTransitions> transitions,
			Set<SymbolicExpression> initialConstraints,
			List<BadCase> badCases,
			Map<ModelLocation, Set<SymbolicExpression>> seeds,
			Set<Identifier> vocabulary,
			SortPolicy sorts,
			Map<String, FreshValue> freshValues,
			Map<Identifier, InputSource> parameters,
			boolean confirmable) {
		this(root, badLocation, locations, initial, transitions, initialConstraints, badCases, seeds, vocabulary,
				sorts, freshValues, parameters, confirmable, Collections.emptySet(), false);
	}

	/**
	 * Builds the model.
	 *
	 * @param root               the root frame of the model, whose result is
	 *                               the one the model is built from
	 * @param badLocation        the bad location
	 * @param locations          the locations
	 * @param initial            the initial locations
	 * @param transitions        the transitions of each location
	 * @param initialConstraints the constraints that hold in the initial states
	 * @param badCases           the ways of violating the property at the bad
	 *                               location
	 * @param seeds              the invariants of each reachable location
	 *                               (unreachable locations are not in the map)
	 * @param vocabulary         the identifiers used in the model
	 * @param sorts              the policy deciding the sorts of identifiers
	 * @param freshValues        the fresh values used in the model, by name
	 * @param parameters         the sources of the parameters of the cfg
	 * @param confirmable        whether or not the model is confirmable
	 * @param frames             the frames of the expanded callees that have
	 *                               locations in the model
	 * @param truncated          whether or not some calls have not been
	 *                               expanded because the model reached its
	 *                               maximum number of locations
	 */
	public AlarmModel(
			Frame root,
			ModelLocation badLocation,
			Set<ModelLocation> locations,
			Set<ModelLocation> initial,
			Map<ModelLocation, NodeTransitions> transitions,
			Set<SymbolicExpression> initialConstraints,
			List<BadCase> badCases,
			Map<ModelLocation, Set<SymbolicExpression>> seeds,
			Set<Identifier> vocabulary,
			SortPolicy sorts,
			Map<String, FreshValue> freshValues,
			Map<Identifier, InputSource> parameters,
			boolean confirmable,
			Set<Frame> frames,
			boolean truncated) {
		this.root = root;
		this.badLocation = badLocation;
		this.locations = Collections.unmodifiableSet(locations);
		this.initial = Collections.unmodifiableSet(initial);
		this.transitions = Collections.unmodifiableMap(transitions);
		this.initialConstraints = Collections.unmodifiableSet(initialConstraints);
		this.badCases = Collections.unmodifiableList(badCases);
		this.seeds = Collections.unmodifiableMap(seeds);
		this.vocabulary = Collections.unmodifiableSet(vocabulary);
		this.sorts = sorts;
		this.freshValues = Collections.unmodifiableMap(freshValues);
		this.parameters = Collections.unmodifiableMap(parameters);
		this.confirmable = confirmable;
		this.frames = Collections.unmodifiableSet(frames);
		this.truncated = truncated;
	}

	/**
	 * Yields a copy of this model whose identifiers are encoded with the given
	 * policy.
	 *
	 * @param sorts the policy
	 *
	 * @return the copy
	 */
	public AlarmModel withSorts(
			SortPolicy sorts) {
		return new AlarmModel(root, badLocation, locations, initial, transitions, initialConstraints, badCases,
				seeds, vocabulary, sorts, freshValues, parameters, confirmable, frames, truncated);
	}

	/**
	 * Yields the frames of the expanded callees that have locations in this
	 * model (the root frame excluded).
	 *
	 * @return the frames
	 */
	public Set<Frame> getFrames() {
		return frames;
	}

	/**
	 * Yields the frames of the callees of the expandable calls reached at the
	 * given locations of this model, that are not already among the given
	 * ones (see {@link NodeTransitions#getCalls()}).
	 *
	 * @param trace    the locations
	 * @param expanded the frames already expanded
	 *
	 * @return the frames to expand
	 */
	public Set<Frame> getExpandableFrames(
			Collection<ModelLocation> trace,
			Set<Frame> expanded) {
		Set<Frame> more = new LinkedHashSet<>();
		for (ModelLocation loc : trace) {
			NodeTransitions tr = transitions.get(loc);
			if (tr != null)
				for (ReplayedCall call : tr.getCalls())
					if (call.isExpandable())
						more.addAll(call.getCalleeFrames());
		}
		more.removeAll(expanded);
		return more;
	}

	/**
	 * Yields {@code true} if some calls have not been expanded because the
	 * model reached its maximum number of locations.
	 *
	 * @return whether or not the model has been truncated
	 */
	public boolean isTruncated() {
		return truncated;
	}

	/**
	 * Yields the root frame of the model, that is, the one of the cfg where the
	 * model starts.
	 *
	 * @return the root frame
	 */
	public Frame getRootFrame() {
		return root;
	}

	/**
	 * Yields the analysis result the model is built from, that is, the one of
	 * its root frame.
	 *
	 * @return the result
	 */
	public AnalyzedCFG<?> getResult() {
		return root.getResult();
	}

	/**
	 * Yields the bad location, that is, the node where the property is checked.
	 *
	 * @return the bad location
	 */
	public ModelLocation getBadLocation() {
		return badLocation;
	}

	/**
	 * Yields the locations of the model.
	 *
	 * @return the locations
	 */
	public Set<ModelLocation> getLocations() {
		return locations;
	}

	/**
	 * Yields the initial locations of the model.
	 *
	 * @return the initial locations
	 */
	public Set<ModelLocation> getInitialLocations() {
		return initial;
	}

	/**
	 * Yields all the transitions extracted for the given location.
	 *
	 * @param location the location
	 *
	 * @return the transitions, or {@code null} if the given location is not a
	 *             location of the model
	 */
	public NodeTransitions getNodeTransitions(
			ModelLocation location) {
		return transitions.get(location);
	}

	/**
	 * Yields the transitions from the given location to other locations of the
	 * model (including the ones entering and returning from callees).
	 *
	 * @param location the location
	 *
	 * @return the transitions
	 */
	public List<EdgeTransition> getTransitions(
			ModelLocation location) {
		NodeTransitions tr = transitions.get(location);
		List<EdgeTransition> result = new ArrayList<>();
		if (tr != null)
			for (EdgeTransition e : tr.getOutgoing())
				if (locations.contains(e.getDestination()))
					result.add(e);
		return result;
	}

	/**
	 * Yields the constraints that hold in the initial states.
	 *
	 * @return the constraints
	 */
	public Set<SymbolicExpression> getInitialConstraints() {
		return initialConstraints;
	}

	/**
	 * Yields the ways of violating the property at the bad location. An empty
	 * list means that the property cannot be violated.
	 *
	 * @return the bad cases
	 */
	public List<BadCase> getBadCases() {
		return badCases;
	}

	/**
	 * Yields the invariant computed by the analysis at the given location, as a
	 * set of constraints.
	 *
	 * @param location the location
	 *
	 * @return the constraints, or {@code null} if the analysis deemed the
	 *             location unreachable
	 */
	public Set<SymbolicExpression> getSeeds(
			ModelLocation location) {
		return seeds.get(location);
	}

	/**
	 * Yields the identifiers used in the model, excluding fresh values.
	 *
	 * @return the identifiers
	 */
	public Set<Identifier> getVocabulary() {
		return vocabulary;
	}

	/**
	 * Yields the policy deciding the sorts used to encode the identifiers and
	 * the fresh values of the model.
	 *
	 * @return the policy
	 */
	public SortPolicy getSorts() {
		return sorts;
	}

	/**
	 * Yields the fresh values used in the model, indexed by name.
	 *
	 * @return the fresh values
	 */
	public Map<String, FreshValue> getFreshValues() {
		return freshValues;
	}

	/**
	 * Yields the parameters of the cfg, mapped to their sources: their values
	 * in the initial states are inputs of the executions.
	 *
	 * @return the parameters
	 */
	public Map<Identifier, InputSource> getParameters() {
		return parameters;
	}

	/**
	 * Yields {@code true} if the initial states of this model correspond to the
	 * beginning of the program.
	 *
	 * @return whether or not this model is confirmable
	 */
	public boolean isConfirmable() {
		return confirmable;
	}

	/**
	 * Yields a human-readable description of this model, listing its
	 * vocabulary, initial states, locations (with their invariants and
	 * transitions), and bad cases. This is meant for debugging purposes.
	 *
	 * @return the description
	 */
	public String dump() {
		StringBuilder sb = new StringBuilder();
		AnalyzedCFG<?> result = root.getResult();
		sb.append("model of ").append(result.getDescriptor().getFullSignature()).append(" in ")
				.append(result.getId()).append(confirmable ? " (confirmable)" : " (not confirmable)").append("\n");
		sb.append("bad location: ").append(badLocation).append(" at ").append(badLocation.getNode().getLocation())
				.append("\n");

		sb.append("vocabulary:\n");
		for (Identifier id : vocabulary) {
			sb.append("  ").append(id).append(": ").append(sorts.sortOf(id).toSmtLib());
			InputSource source = parameters.get(id);
			if (source != null)
				sb.append(" (").append(source.getDescription()).append(")");
			sb.append("\n");
		}
		if (!freshValues.isEmpty()) {
			sb.append("fresh values:\n");
			for (Map.Entry<String, FreshValue> fresh : freshValues.entrySet())
				sb.append("  ").append(fresh.getKey()).append(": ")
						.append(fresh.getValue() instanceof InputValue
								? ((InputValue) fresh.getValue()).getSource().getDescription()
								: fresh.getValue() instanceof SummaryValue
										? "summary of " + ((SummaryValue) fresh.getValue()).getReason()
										: "allocated references")
						.append("\n");
		}

		sb.append("initial locations: ").append(initial).append("\n");
		sb.append("initial constraints: ").append(initialConstraints).append("\n");

		List<ModelLocation> sorted = new ArrayList<>(locations);
		// locations of different kinds are not comparable with each other
		sorted.sort((
				l,
				r) -> {
			CodeLocation left = l.getNode().getLocation();
			CodeLocation right = r.getNode().getLocation();
			int cmp = left.getClass().getName().compareTo(right.getClass().getName());
			return cmp != 0 ? cmp : left.compareTo(right);
		});
		for (ModelLocation loc : sorted) {
			sb.append("location ").append(loc).append(" at ").append(loc.getNode().getLocation());
			if (loc.equals(badLocation))
				sb.append(" [bad]");
			if (initial.contains(loc))
				sb.append(" [initial]");
			sb.append("\n");
			Set<SymbolicExpression> inv = seeds.get(loc);
			sb.append("  invariant: ").append(inv == null ? "unreachable" : inv).append("\n");
			for (EdgeTransition edge : getTransitions(loc)) {
				sb.append("  -> ").append(edge.getDestination()).append(" (")
						.append(edge.getEdge() == null ? "synthetic" : edge.getEdge().getClass().getSimpleName())
						.append(")\n");
				for (SymbolicCase c : edge.getStore().getCases())
					sb.append("     | ").append(c).append("\n");
			}
		}

		sb.append("bad cases:\n");
		for (BadCase bad : badCases)
			sb.append("  | ").append(bad).append("\n");
		return sb.toString();
	}
}
