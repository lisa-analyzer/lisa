package it.unive.lisa.triage.bmc;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.encoding.ExpressionEncoder;
import it.unive.lisa.triage.encoding.SymbolTable;
import it.unive.lisa.triage.encoding.ValueSort;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.BadCase;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.FreshValue;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.SummaryValue;
import it.unive.lisa.triage.model.SymbolicCase;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The encoding of the executions of an {@link AlarmModel} as SMT formulas over
 * <i>steps</i>. At each step {@code t}, the state of an execution is given by
 * a program counter ({@code pc@t}, an integer identifying the current
 * location) and by the values of the identifiers of the model
 * ({@code id@t}). The encoding provides:
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
 * are unconstrained because of imprecision (summary values, or operations
 * that the encoder cannot encode).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Unrolling {

	private final AlarmModel model;

	private final SmtSolver solver;

	private final Map<Statement, Integer> index;

	private final List<Statement> locations;

	private final Map<String, Term> constants;

	private final Map<String, Boolean> exactness;

	private final Map<String, Set<FreshValue>> inputs;

	private final Map<EdgeTransition, Integer> edgeIds;

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
			Statement location) {
		return index.get(location);
	}

	/**
	 * Yields the location with the given index.
	 *
	 * @param index the index
	 *
	 * @return the location
	 */
	public Statement locationAt(
			int index) {
		return locations.get(index);
	}

	private Term constant(
			String name,
			Sort sort) {
		return constants.computeIfAbsent(name, n -> solver.declare(n, sort));
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
			Statement location) {
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
				if (id instanceof SummaryValue)
					exact = false;
				return Unrolling.this.constant(id.getName() + "@" + step + tag, sort);
			}
			return var(id, step);
		}

		@Override
		public Term fresh(
				Sort sort,
				SymbolicExpression origin) {
			exact = false;
			return Unrolling.this.constant("$havoc#" + (Unrolling.this.fresh++), sort);
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
	 * Yields the key identifying the encoding of the bad condition at the
	 * given step.
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
		for (Statement init : model.getInitialLocations())
			locs.add(at(0, init));
		List<Term> conj = new ArrayList<>();
		conj.add(Term.or(locs.toArray(Term[]::new)));
		ExpressionEncoder encoder = new ExpressionEncoder(model.getSorts(), new StepSymbols(0, "#init"));
		for (SymbolicExpression c : model.getInitialConstraints())
			conj.add(encoder.encodeCondition(c));
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * Yields the invariants computed by the analysis, as they hold at the
	 * given step: the program counter is at a reachable location, and the
	 * invariant of the current location holds.
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
		for (Statement loc : locations) {
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
			Statement location,
			int step)
			throws SemanticException {
		return seeds(location, step,
				new ExpressionEncoder(model.getSorts(), new StepSymbols(step, "#inv")));
	}

	private Term seeds(
			Statement location,
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
		for (Statement loc : locations)
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
		for (SymbolicCase c : edge.getStore().getCases())
			cases.add(encodeCase(c, step, encoder));
		exactness.put(key, symbols.exact);
		inputs.put(key, symbols.used);
		return Term.and(
				at(step, edge.getEdge().getSource()),
				Term.eq(pc(step + 1), Term.integer(indexOf(edge.getEdge().getDestination()))),
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
			else if (!c.isUnconstrained(id))
				conj.add(Term.eq(post, var(id, step)));
		}
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * Yields the bad condition at the given step: the program counter is at
	 * the bad location, and one of the bad cases holds.
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
		for (BadCase c : model.getBadCases()) {
			List<Term> conj = new ArrayList<>();
			for (SymbolicExpression cond : c.getPathCondition())
				conj.add(encoder.encodeCondition(cond));
			if (c.getViolation() != null)
				conj.add(encoder.encodeCondition(c.getViolation()));
			cases.add(Term.and(conj.toArray(Term[]::new)));
		}
		exactness.put(key, symbols.exact);
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
	static List<InputValue> inputsOf(
			Set<FreshValue> values) {
		List<InputValue> result = new ArrayList<>();
		for (FreshValue v : values)
			if (v instanceof InputValue)
				result.add((InputValue) v);
		return result;
	}
}
