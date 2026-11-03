package it.unive.lisa.triage.bmc;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.triage.encoding.CellKeySort;
import it.unive.lisa.triage.encoding.ExpressionEncoder;
import it.unive.lisa.triage.encoding.SymbolTable;
import it.unive.lisa.triage.encoding.ValueSort;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.BadCase;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.FreshValue;
import it.unive.lisa.triage.model.HeapModel;
import it.unive.lisa.triage.model.HeapType;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.ModelLocation;
import it.unive.lisa.triage.model.SummaryValue;
import it.unive.lisa.triage.model.SymbolicCase;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The encoding of the executions of an {@link AlarmModel} as SMT formulas over
 * <i>steps</i>. At each step {@code t}, the state of an execution is given by a
 * program counter ({@code pc@t}, an integer identifying the current location)
 * and by the values of the identifiers of the model ({@code id@t}). The
 * encoding provides:
 * <ul>
 * <li>the initial condition ({@link #initial()});</li>
 * <li>the invariants that hold at each step ({@link #invariants(int)}), from
 * the results of the analysis;</li>
 * <li>the transition relation between steps {@code t} and {@code t+1}
 * ({@link #transition(int)});</li>
 * <li>the bad condition at step {@code t} ({@link #bad(int)}).</li>
 * </ul>
 * Fresh values get a new constant at each step, since each execution of a
 * transition produces new ones. The encoding also records whether each
 * transition is <i>exact</i>, that is, whether it does not rely on values that
 * are unconstrained because of imprecision (summary values, or operations that
 * the encoder cannot encode).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Unrolling {

	private final AlarmModel model;

	private final SmtSolver solver;

	private final Map<ModelLocation, Integer> index;

	private final List<ModelLocation> locations;

	private final Map<String, Term> constants;

	private final Map<String, String> functions = new HashMap<>();

	private final Map<String, Boolean> exactness;

	private final Map<String, List<Term>> guards = new HashMap<>();

	/**
	 * The reasons why the encoding with each key is not exact, regardless of
	 * the model (see {@link #getImprecisions(String, SmtSolver)})
	 */
	private final Map<String, Set<String>> reasons = new HashMap<>();

	/**
	 * The reasons why the encoding with each key is not exact in models where
	 * a condition holds and a guard does not (see
	 * {@link #getImprecisions(String, SmtSolver)})
	 */
	private final Map<String, List<GuardedReason>> guardedReasons = new HashMap<>();

	/**
	 * A reason of imprecision that holds when {@code when} holds and
	 * {@code guard} does not.
	 */
	private static final class GuardedReason {

		private final Term when;

		private final Term guard;

		private final String reason;

		private GuardedReason(
				Term when,
				Term guard,
				String reason) {
			this.when = when;
			this.guard = guard;
			this.reason = reason;
		}
	}

	private final boolean strict;

	private final Map<String, Set<FreshValue>> inputs;

	private final Map<EdgeTransition, Integer> edgeIds;

	private final Map<EdgeTransition, List<SymbolicCase>> cases = new IdentityHashMap<>();

	private int fresh;

	/**
	 * Builds the encoding, declaring the needed datatypes on the given solver.
	 *
	 * @param model  the model to encode
	 * @param solver the solver where constants are declared
	 */
	public Unrolling(
			AlarmModel model,
			SmtSolver solver) {
		this(model, solver, false);
	}

	/**
	 * Builds the unrolling, declaring the {@link ValueSort} datatype in the
	 * given solver. In strict mode, transitions, bad cases, and conditions also
	 * require the values they use to have the sorts that make their encoding
	 * precise (e.g., operands of arithmetic operations must be integers): the
	 * encoding then under-approximates the executions of the model, and every
	 * execution it admits is encoded precisely (except for summaries, see
	 * {@link #isExact(String)}).
	 *
	 * @param model  the model to encode
	 * @param solver the solver where constants are declared
	 * @param strict whether or not to encode in strict mode
	 */
	public Unrolling(
			AlarmModel model,
			SmtSolver solver,
			boolean strict) {
		this.strict = strict;
		this.model = model;
		this.solver = solver;
		this.index = new HashMap<>();
		this.locations = new ArrayList<>(model.getLocations());
		for (int i = 0; i < locations.size(); i++)
			index.put(locations.get(i), i);
		this.constants = new HashMap<>();
		this.exactness = new HashMap<>();
		this.inputs = new HashMap<>();
		this.edgeIds = new IdentityHashMap<>();
		solver.declareDatatype(ValueSort.DATATYPE);
		solver.declareDatatype(CellKeySort.DATATYPE);
	}

	/**
	 * Yields the model being encoded.
	 *
	 * @return the model
	 */
	public AlarmModel getModel() {
		return model;
	}

	/**
	 * Yields the index of the given location, used as value of the program
	 * counter.
	 *
	 * @param location the location
	 *
	 * @return the index
	 */
	public int indexOf(
			ModelLocation location) {
		return index.get(location);
	}

	/**
	 * Yields the location with the given index.
	 *
	 * @param index the index
	 *
	 * @return the location
	 */
	public ModelLocation locationAt(
			int index) {
		return locations.get(index);
	}

	private Term constant(
			String name,
			Sort sort) {
		return constants.computeIfAbsent(name, n -> solver.declare(n, sort));
	}

	/**
	 * Yields the symbol of the uninterpreted function with the given name and
	 * signature, declaring it in the solver the first time.
	 */
	private String function(
			String name,
			List<Sort> arguments,
			Sort result) {
		String key = name + arguments + result;
		return functions.computeIfAbsent(key,
				k -> solver.declareFunction(name + "#" + functions.size(), arguments, result));
	}

	/**
	 * Yields the program counter at the given step.
	 *
	 * @param step the step
	 *
	 * @return the program counter
	 */
	public Term pc(
			int step) {
		return constant("pc@" + step, Sort.INT);
	}

	/**
	 * Yields the value of the given identifier at the given step.
	 *
	 * @param id   the identifier
	 * @param step the step
	 *
	 * @return the value
	 */
	public Term var(
			Identifier id,
			int step) {
		return constant(id.getName() + "@" + step, model.getSorts().sortOf(id));
	}

	/**
	 * Yields the condition stating that the program counter is at the given
	 * location at the given step.
	 *
	 * @param step     the step
	 * @param location the location
	 *
	 * @return the condition
	 */
	public Term at(
			int step,
			ModelLocation location) {
		return Term.eq(pc(step), Term.integer(index.get(location)));
	}

	/**
	 * A symbol table naming identifiers at a given step, and fresh values with
	 * a given tag (distinguishing different transitions at the same step).
	 * Fresh constants created by the encoder make the encoding inexact.
	 */
	private class StepSymbols
			implements
			SymbolTable {

		private final int step;

		private final String tag;

		private boolean exact = true;

		private final Set<FreshValue> used = new LinkedHashSet<>();

		private StepSymbols(
				int step,
				String tag) {
			this.step = step;
			this.tag = tag;
		}

		@Override
		public Term constant(
				Identifier id,
				Sort sort) {
			if (id instanceof FreshValue) {
				used.add((FreshValue) id);
				if (id instanceof SummaryValue) {
					exact = false;
					SummaryValue summary = (SummaryValue) id;
					reasons.add(summary.getKind().getDescription() + ": " + summary.getReason());
				}
				return Unrolling.this.constant(id.getName() + "@" + step + tag, sort);
			}
			return var(id, step);
		}

		private final List<Term> guards = new ArrayList<>();

		/**
		 * The reasons of the imprecision signaled by each guard, aligned with
		 * {@link #guards}.
		 */
		private final List<String> guardReasons = new ArrayList<>();

		/**
		 * The reasons why the encoding is not exact, regardless of the model.
		 */
		private final Set<String> reasons = new LinkedHashSet<>();

		private void guard(
				Term guard,
				String reason) {
			guards.add(guard);
			guardReasons.add(reason);
		}

		@Override
		public Term fresh(
				Sort sort,
				SymbolicExpression origin) {
			exact = false;
			reasons.add(UNSUPPORTED_OPERATIONS + ": " + describe(origin));
			return Unrolling.this.constant("$havoc#" + (Unrolling.this.fresh++), sort);
		}

		@Override
		public Term fresh(
				Sort sort,
				SymbolicExpression origin,
				Term guard) {
			guard(guard, OTHER_KINDS + ": " + describe(origin));
			return Unrolling.this.constant("$havoc#" + (Unrolling.this.fresh++), sort);
		}

		@Override
		public void exactOnlyIf(
				Term guard) {
			exactOnlyIf(guard, null);
		}

		@Override
		public void exactOnlyIf(
				Term guard,
				SymbolicExpression origin) {
			guard(guard, OTHER_KINDS + ": " + describe(origin));
		}

		@Override
		public void exactOnlyIfWritten(
				Term written,
				SymbolicExpression origin) {
			// the location might hold a value that the model does not know
			// (e.g., one existing in the initial states, or written by code
			// that is not modeled)
			SymbolicExpression contents = origin instanceof TernaryExpression
					? ((TernaryExpression) origin).getLeft()
					: origin;
			guard(written, SummaryValue.Kind.HEAP.getDescription() + ": contents of " + contents
					+ " not written by the model");
		}

		@Override
		public Term apply(
				String function,
				Sort result,
				List<Term> arguments,
				SymbolicExpression origin,
				Term guard) {
			guard(guard, reasonOf(function, origin));
			List<Sort> sorts = new ArrayList<>();
			for (Term arg : arguments)
				sorts.add(arg.getSort());
			return Term.app(Unrolling.this.function(function, sorts, result), result,
					arguments.toArray(Term[]::new));
		}

		private final List<Term> requirements = new ArrayList<>();

		@Override
		public void require(
				Term condition) {
			requirements.add(condition);
		}

	}

	/**
	 * Yields the constant used for the given fresh value in the transition (or
	 * bad case) with the given key.
	 *
	 * @param value the fresh value
	 * @param key   the key of the transition, as returned by
	 *                  {@link #edgeKey(int, EdgeTransition)} or
	 *                  {@link #badKey(int)}
	 *
	 * @return the constant
	 */
	public Term freshConstant(
			FreshValue value,
			String key) {
		String step = key.substring(key.indexOf('@') + 1, key.indexOf('#'));
		String tag = key.substring(key.indexOf('#'));
		return constant(value.getName() + "@" + step + tag, model.getSorts().sortOf(value));
	}

	/**
	 * Yields the key identifying the encoding of the given transition at the
	 * given step.
	 *
	 * @param step the step
	 * @param edge the transition
	 *
	 * @return the key
	 */
	public String edgeKey(
			int step,
			EdgeTransition edge) {
		int id = edgeIds.computeIfAbsent(edge, e -> edgeIds.size());
		return "edge@" + step + "#e" + id;
	}

	/**
	 * Yields the key identifying the encoding of the bad condition at the given
	 * step.
	 *
	 * @param step the step
	 *
	 * @return the key
	 */
	public String badKey(
			int step) {
		return "bad@" + step + "#bad";
	}

	/**
	 * Yields {@code true} if the encoding with the given key is exact.
	 *
	 * @param key the key
	 *
	 * @return whether or not the encoding is exact
	 */
	public boolean isExact(
			String key) {
		return exactness.getOrDefault(key, false);
	}

	/**
	 * Yields {@code true} if the encoding with the given key is exact in the
	 * current model of the given solver, that must have just answered
	 * {@link it.unive.lisa.triage.smt.SmtResult#SAT}: the encoding must be
	 * exact (see {@link #isExact(String)}), except for values that are
	 * imprecise only under some condition (e.g., arithmetic over values that
	 * are not integers), provided that none of these conditions holds in the
	 * model.
	 *
	 * @param key    the key
	 * @param solver the solver
	 *
	 * @return whether or not the encoding is exact in the model
	 */
	public boolean isExact(
			String key,
			SmtSolver solver) {
		if (!isExact(key))
			return false;
		List<Term> conditions = guards.getOrDefault(key, List.of());
		if (conditions.isEmpty())
			return true;
		for (String value : solver.getValues(conditions).values())
			if (!value.equals("true"))
				return false;
		return true;
	}

	/**
	 * The kind of imprecision of values whose meaning the encoder does not
	 * model (e.g., the results of unsupported operations).
	 */
	public static final String UNSUPPORTED_OPERATIONS = "unsupported operations";

	/**
	 * The kind of imprecision of operations applied to values of kinds they
	 * are not modeled for (e.g., arithmetic over floating point numbers).
	 */
	public static final String OTHER_KINDS = "operations on values of other kinds";

	/**
	 * The kind of imprecision of comparisons and equalities between values
	 * that might not be integers (e.g., floating point numbers).
	 */
	public static final String NON_INTEGER_COMPARISONS = "comparisons of non-integers";

	/**
	 * The kind of imprecision of type tests that the encoder cannot decide.
	 */
	public static final String TYPE_TESTS = "type tests";

	/**
	 * Yields the reasons why the encoding with the given key is not exact in
	 * the current model of the given solver, that must have just answered
	 * {@link it.unive.lisa.triage.smt.SmtResult#SAT}. Each reason has the form
	 * {@code kind: detail}, where the kind is the one of a
	 * {@link SummaryValue} ({@link SummaryValue.Kind#getDescription()}) or one
	 * of the constants of this class, and the detail tells which value or
	 * operation is not encoded precisely. The result is empty if and only if
	 * the encoding is exact in the model ({@link #isExact(String, SmtSolver)}).
	 *
	 * @param key    the key
	 * @param solver the solver
	 *
	 * @return the reasons
	 */
	public Set<String> getImprecisions(
			String key,
			SmtSolver solver) {
		Set<String> result = new LinkedHashSet<>();
		if (!isExact(key))
			result.addAll(reasons.getOrDefault(key, Set.of()));
		List<GuardedReason> guarded = guardedReasons.getOrDefault(key, List.of());
		if (!guarded.isEmpty()) {
			List<Term> terms = new ArrayList<>();
			for (GuardedReason g : guarded) {
				terms.add(g.when);
				terms.add(g.guard);
			}
			Map<Term, String> values = solver.getValues(terms);
			for (GuardedReason g : guarded)
				if ("true".equals(values.get(g.when)) && !"true".equals(values.get(g.guard)))
					result.add(g.reason);
		}
		if (result.isEmpty() && !isExact(key, solver))
			result.add("unknown: imprecision of " + key);
		return result;
	}

	/**
	 * Yields {@code true} if some of the given reasons of imprecision (in the
	 * form yielded by {@link #getImprecisions(String, SmtSolver)}) might be
	 * caused by summarized calls: results of calls (see
	 * {@link SummaryValue.Kind#CALL_RESULT}), or contents of the heap after
	 * writes that are not modeled (see {@link SummaryValue.Kind#HEAP}), such as
	 * the ones of summarized callees.
	 *
	 * @param imprecisions the reasons
	 *
	 * @return whether or not calls might cause imprecision
	 */
	public static boolean dueToCalls(
			Collection<String> imprecisions) {
		String calls = SummaryValue.Kind.CALL_RESULT.getDescription() + ":";
		String heap = SummaryValue.Kind.HEAP.getDescription() + ":";
		return imprecisions.stream().anyMatch(r -> r.startsWith(calls) || r.startsWith(heap));
	}

	/**
	 * The number of details listed by {@link #explain(Collection)}.
	 */
	private static final int MAX_DETAILS = 5;

	/**
	 * Explains that a counterexample relies on values that are not computed
	 * precisely, for the given reasons (in the form yielded by
	 * {@link #getImprecisions(String, SmtSolver)}): the distinct kinds of the
	 * reasons are listed between parentheses, separated by commas, followed by
	 * (some of) their details, separated by semicolons.
	 *
	 * @param imprecisions the reasons
	 *
	 * @return the explanation
	 */
	public static String explain(
			Collection<String> imprecisions) {
		Set<String> kinds = new LinkedHashSet<>();
		List<String> details = new ArrayList<>();
		for (String reason : imprecisions) {
			int sep = reason.indexOf(": ");
			kinds.add(sep < 0 ? reason : reason.substring(0, sep));
			if (sep >= 0 && details.size() < MAX_DETAILS)
				details.add(reason.substring(sep + 2));
		}
		String res = "relying on values that are not computed precisely (" + String.join(", ", kinds) + ")";
		return details.isEmpty() ? res : res + ": " + String.join("; ", details);
	}

	private void record(
			String key,
			StepSymbols symbols,
			List<GuardedReason> guarded) {
		reasons.put(key, new LinkedHashSet<>(symbols.reasons));
		guardedReasons.put(key, guarded);
	}

	private static List<GuardedReason> guardedReasons(
			Term when,
			StepSymbols symbols,
			int from) {
		List<GuardedReason> result = new ArrayList<>();
		for (int i = from; i < symbols.guards.size(); i++)
			result.add(new GuardedReason(when, symbols.guards.get(i), symbols.guardReasons.get(i)));
		return result;
	}

	private static String reasonOf(
			String function,
			SymbolicExpression origin) {
		if (function.startsWith("$is "))
			return TYPE_TESTS + ": " + function.substring(4);
		switch (function) {
		case "$==":
		case "$<":
		case "$<=":
		case "$>":
		case "$>=":
			return NON_INTEGER_COMPARISONS + ": " + describe(origin);
		default:
			return OTHER_KINDS + ": " + describe(origin);
		}
	}

	/**
	 * Yields a short description of the given expression, naming its
	 * operator, if any.
	 */
	private static String describe(
			SymbolicExpression origin) {
		if (origin == null)
			return "unknown";
		if (origin instanceof UnaryExpression)
			return ((UnaryExpression) origin).getOperator().getClass().getSimpleName();
		if (origin instanceof BinaryExpression)
			return ((BinaryExpression) origin).getOperator().getClass().getSimpleName();
		if (origin instanceof TernaryExpression)
			return ((TernaryExpression) origin).getOperator().getClass().getSimpleName();
		return origin.getClass().getSimpleName();
	}

	/**
	 * Yields the constraints stating that the references existing in the
	 * initial states (held by identifiers, fields of objects, or cells of
	 * arrays) have been allocated before the ones that the model allocates,
	 * that is, their identifiers are smaller than the initial value of the
	 * counter of allocations (see {@link HeapModel}). These constraints hold
	 * in every concrete state, for a large enough counter, and they ensure that
	 * new references never alias existing ones.
	 */
	private List<Term> existingReferences() {
		if (!model.getVocabulary().contains(HeapModel.ALLOC))
			return List.of();
		Term counter = var(HeapModel.ALLOC, 0);
		List<Term> result = new ArrayList<>();
		Term r = Term.boundVariable("$r", Sort.INT);
		Term k = Term.boundVariable("$k", CellKeySort.SORT);
		for (Identifier id : model.getVocabulary()) {
			Term value = var(id, 0);
			if (id.getStaticType() == HeapType.FIELD)
				result.add(Term.forall(List.of(r), allocated(Term.app("select", ValueSort.SORT, value, r), counter)));
			else if (id.getStaticType() == HeapType.CELLS)
				result.add(Term.forall(List.of(k), allocated(Term.app("select", ValueSort.SORT, value, k), counter)));
			else if (value.getSort().equals(ValueSort.SORT))
				result.add(allocated(value, counter));
		}
		return result;
	}

	/**
	 * Yields the constraints stating that no location of the heap has been
	 * written by the model in the initial states (see {@link HeapModel}).
	 */
	private List<Term> nothingWritten() {
		List<Term> result = new ArrayList<>();
		for (Identifier id : model.getVocabulary())
			if (isWrittenMap(id))
				result.add(Term.eq(var(id, 0), nothingWritten(id)));
		return result;
	}

	private static boolean isWrittenMap(
			Identifier id) {
		return id.getStaticType() == HeapType.WRITTEN_FIELD || id.getStaticType() == HeapType.WRITTEN_CELLS;
	}

	/**
	 * Yields the map recording that no location has been written, for the
	 * given identifier of the heap model recording written locations.
	 */
	private static Term nothingWritten(
			Identifier id) {
		Sort sort = ((HeapType) id.getStaticType()).getSort();
		return Term.app("(as const " + sort.toSmtLib() + ")", sort, Term.FALSE);
	}

	private static Term allocated(
			Term value,
			Term counter) {
		return Term.implies(ValueSort.isReference(value),
				Term.app("<", Sort.BOOL, ValueSort.referenceId(value), counter));
	}

	/**
	 * Yields the input values used by the encoding with the given key.
	 *
	 * @param key the key
	 *
	 * @return the input values
	 */
	public Set<FreshValue> getFreshValues(
			String key) {
		return inputs.getOrDefault(key, Set.of());
	}

	/**
	 * Yields the initial condition: the program counter is at an initial
	 * location, and the initial constraints hold.
	 *
	 * @return the initial condition
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public Term initial()
			throws SemanticException {
		List<Term> locs = new ArrayList<>();
		for (ModelLocation init : model.getInitialLocations())
			locs.add(at(0, init));
		List<Term> conj = new ArrayList<>();
		conj.add(Term.or(locs.toArray(Term[]::new)));
		ExpressionEncoder encoder = new ExpressionEncoder(model.getSorts(), new StepSymbols(0, "#init"));
		for (SymbolicExpression c : model.getInitialConstraints())
			conj.add(encoder.encodeCondition(c));
		conj.addAll(existingReferences());
		conj.addAll(nothingWritten());
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * Yields the invariants computed by the analysis, as they hold at the given
	 * step: the program counter is at a reachable location, and the invariant
	 * of the current location holds.
	 *
	 * @param step the step
	 *
	 * @return the invariants
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public Term invariants(
			int step)
			throws SemanticException {
		List<Term> locs = new ArrayList<>();
		List<Term> conj = new ArrayList<>();
		ExpressionEncoder encoder = new ExpressionEncoder(model.getSorts(), new StepSymbols(step, "#inv"));
		for (ModelLocation loc : locations) {
			if (model.getSeeds(loc) == null)
				// unreachable location
				continue;
			locs.add(at(step, loc));
			conj.add(Term.implies(at(step, loc), seeds(loc, step, encoder)));
		}
		conj.add(Term.or(locs.toArray(Term[]::new)));
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * Yields the invariant computed by the analysis for the given location, as
	 * it holds at the given step (regardless of the program counter).
	 *
	 * @param location the location
	 * @param step     the step
	 *
	 * @return the invariant, that is {@code false} if the location is
	 *             unreachable
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public Term seeds(
			ModelLocation location,
			int step)
			throws SemanticException {
		return seeds(location, step,
				new ExpressionEncoder(model.getSorts(), new StepSymbols(step, "#inv")));
	}

	private Term seeds(
			ModelLocation location,
			int step,
			ExpressionEncoder encoder)
			throws SemanticException {
		Set<SymbolicExpression> seeds = model.getSeeds(location);
		if (seeds == null)
			return Term.FALSE;
		List<Term> inv = new ArrayList<>();
		for (SymbolicExpression c : seeds)
			inv.add(encoder.encodeCondition(c));
		return Term.and(inv.toArray(Term[]::new));
	}

	/**
	 * Yields the transition relation from step {@code step} to step
	 * {@code step + 1}.
	 *
	 * @param step the step
	 *
	 * @return the transition relation
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public Term transition(
			int step)
			throws SemanticException {
		List<Term> edges = new ArrayList<>();
		for (ModelLocation loc : locations)
			for (EdgeTransition edge : model.getTransitions(loc))
				edges.add(transition(step, edge));
		return Term.or(edges.toArray(Term[]::new));
	}

	/**
	 * Yields the relation of the given transition from step {@code step} to
	 * step {@code step + 1}: the program counter moves from the source to the
	 * destination of the edge, and one of the cases of the transition holds.
	 * The exactness and the fresh values of the encoding are recorded under
	 * {@link #edgeKey(int, EdgeTransition)}.
	 *
	 * @param step the step
	 * @param edge the transition
	 *
	 * @return the transition relation
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public Term transition(
			int step,
			EdgeTransition edge)
			throws SemanticException {
		String key = edgeKey(step, edge);
		StepSymbols symbols = new StepSymbols(step, key.substring(key.indexOf('#')));
		ExpressionEncoder encoder = new ExpressionEncoder(model.getSorts(), symbols);
		List<Term> cases = new ArrayList<>();
		List<Term> precise = new ArrayList<>();
		List<GuardedReason> guarded = new ArrayList<>();
		for (SymbolicCase c : edge.getStore().getCases()) {
			int from = symbols.guards.size();
			int req = symbols.requirements.size();
			Term ct = required(encodeCase(c, step, encoder), symbols, req);
			cases.add(strictly(ct, symbols, from));
			precise.add(preciseCase(ct, symbols, from));
			guarded.addAll(guardedReasons(ct, symbols, from));
		}
		exactness.put(key, symbols.exact);
		guards.put(key, List.of(Term.or(precise.toArray(Term[]::new))));
		record(key, symbols, guarded);
		inputs.put(key, symbols.used);
		return Term.and(
				at(step, edge.getSource()),
				Term.eq(pc(step + 1), Term.integer(indexOf(edge.getDestination()))),
				Term.or(cases.toArray(Term[]::new)));
	}

	private Term encodeCase(
			SymbolicCase c,
			int step,
			ExpressionEncoder encoder)
			throws SemanticException {
		List<Term> conj = new ArrayList<>();
		for (SymbolicExpression cond : c.getPathCondition())
			conj.add(encoder.encodeCondition(cond));
		for (Identifier id : model.getVocabulary()) {
			SymbolicExpression value = c.getUpdates().get(id);
			Term post = var(id, step + 1);
			if (value != null)
				conj.add(Term.eq(post, encoder.encodeAs(value, post.getSort())));
			else if (c.isUnconstrained(id) && isWrittenMap(id))
				// the heap is unconstrained after writes that are not
				// modeled: no location counts as written by the model, so that
				// reading the unconstrained contents is not precise (as done
				// for the initial states)
				conj.add(Term.eq(post, nothingWritten(id)));
			else if (!c.isUnconstrained(id)) {
				// the identifier might have been renamed by scoping
				Identifier pre = c.preIdentifier(id);
				conj.add(Term.eq(post, pre.equals(id) ? var(id, step) : encoder.encodeAs(pre, post.getSort())));
			}
		}
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * Conjoins to the given term the conditions required by the given symbols
	 * (from the given index) for the encoded expressions to be evaluated (see
	 * {@link SymbolTable#require(Term)}).
	 */
	private static Term required(
			Term term,
			StepSymbols symbols,
			int from) {
		if (symbols.requirements.size() == from)
			return term;
		List<Term> conj = new ArrayList<>();
		conj.add(term);
		conj.addAll(symbols.requirements.subList(from, symbols.requirements.size()));
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * In strict mode, conjoins to the given term the conditions recorded by the
	 * given symbols (from the given index) under which its encoding is precise.
	 */
	private Term strictly(
			Term term,
			StepSymbols symbols,
			int from) {
		if (!strict || symbols.guards.size() == from)
			return term;
		List<Term> conj = new ArrayList<>();
		conj.add(term);
		conj.addAll(symbols.guards.subList(from, symbols.guards.size()));
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * Yields the condition under which the given case holds and is encoded
	 * precisely, that is, the case and the conditions recorded by the given
	 * symbols (from the given index) hold. Used for encodings that are
	 * disjunctions of cases: they are precise in a model if one of the cases
	 * that hold is precise.
	 */
	private static Term preciseCase(
			Term term,
			StepSymbols symbols,
			int from) {
		List<Term> conj = new ArrayList<>();
		conj.add(term);
		conj.addAll(symbols.guards.subList(from, symbols.guards.size()));
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * Yields the cases of the given transition, in a fixed order (the one used
	 * by {@link #transition(int, EdgeTransition, int)}).
	 *
	 * @param edge the transition
	 *
	 * @return the cases
	 */
	public List<SymbolicCase> cases(
			EdgeTransition edge) {
		return cases.computeIfAbsent(edge, e -> new ArrayList<>(e.getStore().getCases()));
	}

	/**
	 * Yields the key identifying the encoding of a single case of the given
	 * transition at the given step.
	 *
	 * @param step  the step
	 * @param edge  the transition
	 * @param index the index of the case in {@link #cases(EdgeTransition)}
	 *
	 * @return the key
	 */
	public String caseKey(
			int step,
			EdgeTransition edge,
			int index) {
		return edgeKey(step, edge) + "c" + index;
	}

	/**
	 * Yields the relation of a single case of the given transition from step
	 * {@code step} to step {@code step + 1}: the program counter moves from the
	 * source to the destination of the edge, and the case holds. The exactness
	 * and the fresh values of the encoding are recorded under
	 * {@link #caseKey(int, EdgeTransition, int)}.
	 *
	 * @param step  the step
	 * @param edge  the transition
	 * @param index the index of the case in {@link #cases(EdgeTransition)}
	 *
	 * @return the relation
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public Term transition(
			int step,
			EdgeTransition edge,
			int index)
			throws SemanticException {
		String key = caseKey(step, edge, index);
		StepSymbols symbols = new StepSymbols(step, key.substring(key.indexOf('#')));
		ExpressionEncoder encoder = new ExpressionEncoder(model.getSorts(), symbols);
		Term c = strictly(required(encodeCase(cases(edge).get(index), step, encoder), symbols, 0), symbols, 0);
		exactness.put(key, symbols.exact);
		guards.put(key, symbols.guards);
		record(key, symbols, guardedReasons(Term.TRUE, symbols, 0));
		inputs.put(key, symbols.used);
		return Term.and(
				at(step, edge.getSource()),
				Term.eq(pc(step + 1), Term.integer(indexOf(edge.getDestination()))),
				c);
	}

	/**
	 * Encodes the given boolean expression over the values of identifiers at
	 * the given step. Fresh values are encoded as constants named after the
	 * given tag, so that the same fresh value is encoded by the same constant
	 * for the same step and tag, and can be read through
	 * {@link #freshConstant(FreshValue, String)} with key
	 * {@code "cond@" + step + tag}, under which the exactness and the fresh
	 * values of the encoding are also recorded.
	 *
	 * @param condition the expression
	 * @param step      the step
	 * @param tag       the tag, starting with {@code #}
	 *
	 * @return the encoded condition
	 *
	 * @throws SemanticException if the expression cannot be encoded
	 */
	public Term condition(
			SymbolicExpression condition,
			int step,
			String tag)
			throws SemanticException {
		StepSymbols symbols = new StepSymbols(step, tag);
		Term result = strictly(
				required(new ExpressionEncoder(model.getSorts(), symbols).encodeCondition(condition), symbols, 0),
				symbols, 0);
		String key = "cond@" + step + tag;
		exactness.put(key, symbols.exact);
		guards.put(key, symbols.guards);
		record(key, symbols, guardedReasons(Term.TRUE, symbols, 0));
		inputs.put(key, symbols.used);
		return result;
	}

	/**
	 * Yields the bad condition at the given step: the program counter is at the
	 * bad location, and one of the bad cases holds.
	 *
	 * @param step the step
	 *
	 * @return the bad condition
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public Term bad(
			int step)
			throws SemanticException {
		String key = badKey(step);
		StepSymbols symbols = new StepSymbols(step, key.substring(key.indexOf('#')));
		ExpressionEncoder encoder = new ExpressionEncoder(model.getSorts(), symbols);
		List<Term> cases = new ArrayList<>();
		List<Term> precise = new ArrayList<>();
		List<GuardedReason> guarded = new ArrayList<>();
		for (BadCase c : model.getBadCases()) {
			int from = symbols.guards.size();
			int req = symbols.requirements.size();
			List<Term> conj = new ArrayList<>();
			for (SymbolicExpression cond : c.getPathCondition())
				conj.add(encoder.encodeCondition(cond));
			if (c.getViolation() != null)
				conj.add(encoder.encodeCondition(c.getViolation()));
			Term ct = required(Term.and(conj.toArray(Term[]::new)), symbols, req);
			cases.add(strictly(ct, symbols, from));
			precise.add(preciseCase(ct, symbols, from));
			guarded.addAll(guardedReasons(ct, symbols, from));
		}
		exactness.put(key, symbols.exact);
		guards.put(key, List.of(Term.or(precise.toArray(Term[]::new))));
		record(key, symbols, guarded);
		inputs.put(key, symbols.used);
		return Term.and(at(step, model.getBadLocation()), Term.or(cases.toArray(Term[]::new)));
	}

	/**
	 * Yields the input values of the given set, in order.
	 *
	 * @param values the fresh values
	 *
	 * @return the input values
	 */
	public static List<InputValue> inputsOf(
			Set<FreshValue> values) {
		List<InputValue> result = new ArrayList<>();
		for (FreshValue v : values)
			if (v instanceof InputValue)
				result.add((InputValue) v);
		return result;
	}
}
