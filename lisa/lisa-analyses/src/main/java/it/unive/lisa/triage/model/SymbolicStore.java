package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.NaryStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.triage.reachability.Reachability;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A symbolic transition, made of a disjunction of {@link SymbolicCase}s. The
 * number of cases is bounded: when it exceeds the bound, all cases are merged
 * into a single one that over-approximates them (see
 * {@link SymbolicCase#merge(SymbolicCase, SymbolicCase, FreshValueFactory, it.unive.lisa.program.cfg.CodeLocation)}).
 * Instances of this class are immutable.<br>
 * <br>
 * The cases of a store are joined from the executions of different branches,
 * that might compute different expressions (e.g., the alternatives of a
 * dispatch on the types of a value): the store records the expressions that
 * each case computed (see {@link #computing(SymbolicExpression)}), so that the
 * operations over an expression computed by some cases only apply to them (see
 * {@link #restrictTo(SymbolicExpression, ProgramPoint)}), instead of pairing every case with
 * every expression computed by any of them.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class SymbolicStore {

	/**
	 * The default maximum number of cases of a store.
	 */
	public static final int DEFAULT_MAX_CASES = 8;

	private final Set<SymbolicCase> cases;

	private final FreshValueFactory factory;

	private final int maxCases;

	/**
	 * The expressions computed by each case, if known: for each statement (or
	 * program point), the expressions it computed in the case (a case that is
	 * not a key computed unknown expressions, and a statement that is not a key
	 * of a case might have computed anything in it).
	 */
	private final Map<SymbolicCase, Map<ProgramPoint, List<SymbolicExpression>>> computed;

	private SymbolicStore(
			Set<SymbolicCase> cases,
			FreshValueFactory factory,
			int maxCases) {
		this(cases, factory, maxCases, Collections.emptyMap());
	}

	private SymbolicStore(
			Set<SymbolicCase> cases,
			FreshValueFactory factory,
			int maxCases,
			Map<SymbolicCase, Map<ProgramPoint, List<SymbolicExpression>>> computed) {
		this.cases = cases;
		this.factory = factory;
		this.maxCases = maxCases;
		this.computed = computed;
	}

	/**
	 * Yields the store where every case computed the given expression at the
	 * given program point (that is, the expression computed by the executions
	 * of this store at that point, e.g., by the statement evaluated last): the
	 * expressions computed before at other points are kept, as they might still
	 * be used (e.g., the operands of an expression whose other operands are
	 * computed afterwards). The cases whose computed expressions are unknown
	 * stay so, unless all of them are (e.g., after a change of scope, or after
	 * all values are lost): then, the expressions are tracked again from here,
	 * as from the identity (see {@link #identity(FreshValueFactory, int)}),
	 * so that the operations following them are still restricted to the cases
	 * computing their operands (see
	 * {@link #restrictTo(ProgramPoint, SymbolicExpression)}). The cases that
	 * are unknown among known ones stay unknown, as the known ones might have
	 * computed, at the same point, expressions that the others computed too.
	 *
	 * @param expression the expression
	 * @param point      the program point computing it
	 *
	 * @return the resulting store
	 */
	public SymbolicStore computing(
			SymbolicExpression expression,
			ProgramPoint point) {
		if (point == null)
			return this;
		ProgramPoint pp = canonical(point);
		Map<SymbolicCase, Map<ProgramPoint, List<SymbolicExpression>>> tags = new LinkedHashMap<>();
		// if all cases computed unknown expressions, they are tracked again
		boolean restart = computed.isEmpty();
		boolean changed = false;
		for (SymbolicCase c : cases) {
			Map<ProgramPoint, List<SymbolicExpression>> known = computed.get(c);
			if (known == null && !restart)
				continue;
			List<SymbolicExpression> previous = known == null ? null : known.get(pp);
			if (previous != null && previous.stream().anyMatch(e -> e == expression)) {
				// already recorded
				tags.put(c, known);
				continue;
			}
			changed = true;
			Map<ProgramPoint, List<SymbolicExpression>> exprs = known == null ? new LinkedHashMap<>()
					: new LinkedHashMap<>(known);
			List<SymbolicExpression> there = previous == null ? new ArrayList<>() : new ArrayList<>(previous);
			there.add(expression);
			exprs.put(pp, Collections.unmodifiableList(there));
			tags.put(c, Collections.unmodifiableMap(exprs));
		}
		if (!changed)
			return this;
		return new SymbolicStore(cases, factory, maxCases, Collections.unmodifiableMap(tags));
	}

	/**
	 * Yields the store made of the cases of this one where the given program
	 * point might have computed the given expression: if some case computed the
	 * very expression (by identity) at that point, the cases where the point
	 * computed only other expressions (not equal to it) are dropped, while the
	 * others (including the ones where the point computed nothing known, or
	 * whose computed expressions are unknown) are kept. Statements evaluating
	 * their semantics over each combination of the expressions computed by
	 * their operands use this with the operands, so that the semantics is
	 * evaluated in the executions computing the combination.
	 *
	 * @param point      the program point
	 * @param expression the expression
	 *
	 * @return the resulting store
	 */
	public SymbolicStore restrictTo(
			ProgramPoint point,
			SymbolicExpression expression) {
		if (computed.isEmpty() || point == null || cases.size() < 2)
			// with a single case, nothing is dropped: if the point computed
			// the expression in some case, it computed it in that one
			return this;
		ProgramPoint pp = canonical(point);
		boolean computedThere = false;
		for (Map<ProgramPoint, List<SymbolicExpression>> exprs : computed.values())
			if (exprs.getOrDefault(pp, Collections.emptyList()).stream().anyMatch(e -> e == expression)) {
				computedThere = true;
				break;
			}
		if (!computedThere)
			return this;
		Set<SymbolicCase> result = new LinkedHashSet<>();
		Map<SymbolicCase, Map<ProgramPoint, List<SymbolicExpression>>> kept = new LinkedHashMap<>();
		for (SymbolicCase c : cases) {
			Map<ProgramPoint, List<SymbolicExpression>> exprs = computed.get(c);
			// a case where the point computed other expressions is dropped (a
			// case where it computed nothing known is kept, as the expressions
			// of some executions might be computed without the domain)
			// (whether to restrict is decided by identity above, while the
			// cases are kept by equality: an equal expression computed at
			// the point stands for the same value)
			if (exprs == null || !exprs.containsKey(pp) || exprs.get(pp).contains(expression)) {
				result.add(c);
				if (exprs != null)
					kept.put(c, exprs);
			}
		}
		if (result.size() == cases.size())
			return this;
		return new SymbolicStore(Collections.unmodifiableSet(result), factory, maxCases,
				Collections.unmodifiableMap(kept));
	}

	/**
	 * Yields whether {@link #restrictTo(ProgramPoint, SymbolicExpression)}
	 * yields exactly the cases where the given program point computed the
	 * given expression: it does if, in each case it keeps, the point computed
	 * only expressions equal to it (while it keeps also the cases where the
	 * point computed nothing known, or several expressions).
	 *
	 * @param point      the program point
	 * @param expression the expression
	 *
	 * @return whether the restriction is exact
	 */
	public boolean restrictsExactly(
			ProgramPoint point,
			SymbolicExpression expression) {
		SymbolicStore restricted = restrictTo(point, expression);
		if (restricted.cases.isEmpty())
			return true;
		if (point == null)
			return false;
		ProgramPoint pp = canonical(point);
		for (SymbolicCase c : restricted.cases) {
			Map<ProgramPoint, List<SymbolicExpression>> exprs = restricted.computed.get(c);
			List<SymbolicExpression> there = exprs == null ? null : exprs.get(pp);
			if (there == null || there.stream().anyMatch(e -> !e.equals(expression)))
				return false;
		}
		return true;
	}

	/**
	 * Yields the store made of the cases of this one that might have computed
	 * the given expression, evaluated at the given program point, for
	 * operations over the expression whose operands are not known to come from
	 * specific program points (e.g., assumptions and assignments): for each
	 * sub-expression of the given one that is an expression computed at a
	 * single program point, the store is restricted to the cases where that
	 * point might have computed it (see
	 * {@link #restrictTo(ProgramPoint, SymbolicExpression)}). A sub-expression
	 * is recognized by identity, or, if it is a compound expression (that the
	 * semantics might build again), by equality with an expression computed by
	 * the given point or by one of its (transitive) operands: an equal
	 * expression computed elsewhere is another computation (e.g., the two
	 * {@code a + 1} of {@code (a if c else b) + 1) * (a + 1)}), and restricting
	 * the store to the cases where that one computed it would drop executions
	 * where it computed something else. Expressions computed at several points
	 * (e.g., a value read and then yielded by another expression) are
	 * ambiguous, and do not restrict the cases.
	 *
	 * @param expression the expression
	 * @param pp         the program point evaluating the expression, or
	 *                       {@code null} if not known (no expression is then
	 *                       recognized by equality)
	 *
	 * @return the resulting store
	 */
	public SymbolicStore restrictTo(
			SymbolicExpression expression,
			ProgramPoint pp) {
		if (computed.isEmpty() || cases.size() < 2)
			// with a single case, nothing is dropped (see
			// restrictTo(ProgramPoint, SymbolicExpression))
			return this;
		return restrictTo(expression, operandsOf(pp));
	}

	/**
	 * As {@link #restrictTo(SymbolicExpression, ProgramPoint)}, recognizing by
	 * equality the expressions computed at the given (canonical) points.
	 */
	SymbolicStore restrictTo(
			SymbolicExpression expression,
			Set<ProgramPoint> operands) {
		if (computed.isEmpty() || cases.size() < 2)
			return this;
		// the points where each computed expression (by identity) is
		// computed, and the expressions computed by the operands by equality
		Map<SymbolicExpression, Set<ProgramPoint>> points = new IdentityHashMap<>();
		Map<SymbolicExpression, SymbolicExpression> equal = new HashMap<>();
		for (Map<ProgramPoint, List<SymbolicExpression>> exprs : computed.values())
			for (Map.Entry<ProgramPoint, List<SymbolicExpression>> e : exprs.entrySet())
				for (SymbolicExpression x : e.getValue()) {
					points.computeIfAbsent(x, k -> new HashSet<>()).add(e.getKey());
					if (operands.contains(e.getKey()))
						equal.putIfAbsent(x, x);
				}
		List<SymbolicExpression> relevant = new ArrayList<>();
		collect(expression, points.keySet(), equal, relevant);
		SymbolicStore result = this;
		for (SymbolicExpression r : relevant) {
			Set<ProgramPoint> where = points.get(r);
			if (where.size() == 1)
				result = result.restrictTo(where.iterator().next(), r);
		}
		return result;
	}

	/**
	 * Yields the canonical points (see {@link #canonical(ProgramPoint)}) of the
	 * given program point and of its (transitive) operands, that is, the
	 * points whose computations the given one might use.
	 */
	private static Set<ProgramPoint> operandsOf(
			ProgramPoint pp) {
		Set<ProgramPoint> result = Collections.newSetFromMap(new IdentityHashMap<>());
		if (pp == null)
			return result;
		List<ProgramPoint> work = new ArrayList<>(List.of(pp, canonical(pp)));
		while (!work.isEmpty()) {
			ProgramPoint current = work.remove(work.size() - 1);
			if (!result.add(current))
				continue;
			Expression[] subs = current instanceof NaryExpression ? ((NaryExpression) current).getSubExpressions()
					: current instanceof NaryStatement ? ((NaryStatement) current).getSubExpressions() : null;
			if (subs != null)
				for (Expression sub : subs) {
					work.add(sub);
					work.add(canonical(sub));
				}
		}
		return result;
	}

	/**
	 * Yields the statement of the program on whose behalf the given program
	 * point computes expressions: the point itself (or the call of the program
	 * it has been resolved from, see {@link Reachability#sourceOf(Statement)}),
	 * or, for expressions that the semantics of other expressions evaluate
	 * (e.g., calls resolved by a frontend, whose parent is the expression
	 * performing them, but that are not among its operands), the closest
	 * expression containing it among whose operands it is.
	 */
	private static ProgramPoint canonical(
			ProgramPoint pp) {
		ProgramPoint current = pp;
		while (current instanceof Expression) {
			// calls resolved from calls of the program stand for them
			if (current instanceof Statement)
				current = Reachability.sourceOf((Statement) current);
			if (!(current instanceof Expression))
				break;
			Statement parent = ((Expression) current).getParentStatement();
			if (!(parent instanceof NaryExpression))
				break;
			ProgramPoint child = current;
			if (Arrays.stream(((NaryExpression) parent).getSubExpressions()).anyMatch(sub -> sub == child))
				break;
			current = parent;
		}
		return current;
	}

	/**
	 * Adds to the given list the sub-expressions of the given expression that
	 * are among the given ones: by identity, or, for compound expressions, by
	 * equality with the ones in {@code equal} (the equal expression is
	 * added).
	 */
	private static void collect(
			SymbolicExpression e,
			Set<SymbolicExpression> tags,
			Map<SymbolicExpression, SymbolicExpression> equal,
			List<SymbolicExpression> relevant) {
		if (e == null)
			return;
		if (tags.contains(e)) {
			relevant.add(e);
			return;
		}
		// constants and identifiers might be built again independently of
		// their computation (e.g., a literal), and unknown values and
		// allocations are equal to any other of the same type: they are
		// recognized only by identity
		if (!(e instanceof Constant) && !(e instanceof Identifier) && !(e instanceof PushAny)
				&& !(e instanceof PushInv) && !(e instanceof MemoryAllocation)) {
			SymbolicExpression t = equal.get(e);
			if (t != null) {
				relevant.add(t);
				return;
			}
		}
		if (e instanceof UnaryExpression)
			collect(((UnaryExpression) e).getExpression(), tags, equal, relevant);
		else if (e instanceof BinaryExpression) {
			collect(((BinaryExpression) e).getLeft(), tags, equal, relevant);
			collect(((BinaryExpression) e).getRight(), tags, equal, relevant);
		} else if (e instanceof TernaryExpression) {
			collect(((TernaryExpression) e).getLeft(), tags, equal, relevant);
			collect(((TernaryExpression) e).getMiddle(), tags, equal, relevant);
			collect(((TernaryExpression) e).getRight(), tags, equal, relevant);
		} else if (e instanceof AccessChild) {
			collect(((AccessChild) e).getContainer(), tags, equal, relevant);
			collect(((AccessChild) e).getChild(), tags, equal, relevant);
		} else if (e instanceof HeapReference)
			collect(((HeapReference) e).getExpression(), tags, equal, relevant);
		else if (e instanceof HeapDereference)
			collect(((HeapDereference) e).getExpression(), tags, equal, relevant);
	}

	/**
	 * Builds the store representing the identity transition.
	 *
	 * @param factory  the factory of fresh values used by the store
	 * @param maxCases the maximum number of cases of the store
	 *
	 * @return the store
	 */
	public static SymbolicStore identity(
			FreshValueFactory factory,
			int maxCases) {
		// the identity computed no expression yet
		return new SymbolicStore(Collections.singleton(SymbolicCase.IDENTITY), factory, maxCases,
				Collections.singletonMap(SymbolicCase.IDENTITY, Collections.emptyMap()));
	}

	/**
	 * Yields the cases of this store.
	 *
	 * @return the cases
	 */
	public Set<SymbolicCase> getCases() {
		return cases;
	}

	/**
	 * Yields the factory of fresh values used by this store.
	 *
	 * @return the factory
	 */
	public FreshValueFactory getFactory() {
		return factory;
	}

	/**
	 * Yields the store representing the identity transition, with the same
	 * factory and bound of this one.
	 *
	 * @return the store
	 */
	public SymbolicStore identity() {
		return identity(factory, maxCases);
	}

	/**
	 * Yields the sequential composition of this store with the given one: each
	 * case of this store composed with each case of the given one (see
	 * {@link SymbolicCase#then(SymbolicCase, FreshValueFactory, long)}). Yields
	 * {@code null} if the composition has more cases than the bound of this
	 * store, or if some composition of cases is too large.
	 *
	 * @param next  the store following this one
	 * @param bound the maximum size of the conditions and values of the
	 *                  composed cases
	 *
	 * @return the composition, or {@code null}
	 */
	public SymbolicStore then(
			SymbolicStore next,
			long bound) {
		if (cases.size() * next.cases.size() > maxCases)
			return null;
		Set<SymbolicCase> result = new LinkedHashSet<>();
		for (SymbolicCase first : cases)
			for (SymbolicCase second : next.cases) {
				SymbolicCase c = first.then(second, factory, bound);
				if (c == null)
					return null;
				result.add(c);
			}
		return new SymbolicStore(Collections.unmodifiableSet(result), factory, maxCases);
	}

	/**
	 * Yields the store where all values are lost.
	 *
	 * @return the store
	 */
	public SymbolicStore top() {
		return new SymbolicStore(Collections.singleton(SymbolicCase.IDENTITY.withHavocAll()), factory, maxCases);
	}

	/**
	 * Yields the store where all values are lost, as {@link #top()}, and so are
	 * the conditions under which they are: the traces relying on it are
	 * imprecise (see
	 * {@link SymbolicCase#withLostConditions(FreshValueFactory)}). The store is
	 * still a top one (see {@link #isTop()}).
	 *
	 * @return the store
	 */
	public SymbolicStore lost() {
		return new SymbolicStore(
				Collections.singleton(SymbolicCase.IDENTITY.withHavocAll().withLostConditions(factory)), factory,
				maxCases);
	}

	/**
	 * Yields {@code true} if this store loses all values, under no condition.
	 *
	 * @return whether or not this store is the top one
	 */
	public boolean isTop() {
		return cases.stream().anyMatch(SymbolicStore::isTop);
	}

	private static boolean isTop(
			SymbolicCase c) {
		return c.isHavocAll() && c.hasLostConditions() && c.getUpdates().isEmpty();
	}

	private SymbolicStore map(
			Function<SymbolicCase, SymbolicCase> f) {
		Set<SymbolicCase> result = new LinkedHashSet<>();
		// the expressions computed by each case are the ones of the cases it
		// comes from (unknown if any of them is)
		Map<SymbolicCase, Map<ProgramPoint, List<SymbolicExpression>>> tags = new LinkedHashMap<>();
		Set<SymbolicCase> unknown = new HashSet<>();
		for (SymbolicCase c : cases) {
			SymbolicCase mapped = f.apply(c);
			result.add(mapped);
			tag(tags, unknown, mapped, computed.get(c));
		}
		tags.keySet().removeAll(unknown);
		return new SymbolicStore(Collections.unmodifiableSet(result), factory, maxCases,
				Collections.unmodifiableMap(tags));
	}

	private static void tag(
			Map<SymbolicCase, Map<ProgramPoint, List<SymbolicExpression>>> tags,
			Set<SymbolicCase> unknown,
			SymbolicCase c,
			Map<ProgramPoint, List<SymbolicExpression>> exprs) {
		if (exprs == null) {
			unknown.add(c);
			return;
		}
		Map<ProgramPoint, List<SymbolicExpression>> previous = tags.get(c);
		if (previous == null)
			tags.put(c, exprs);
		else {
			// the same case coming from different executions: each point
			// computed the expressions of any of them
			Map<ProgramPoint, List<SymbolicExpression>> union = new LinkedHashMap<>(previous);
			for (Map.Entry<ProgramPoint, List<SymbolicExpression>> e : exprs.entrySet()) {
				List<SymbolicExpression> both = new ArrayList<>(union.getOrDefault(e.getKey(),
						Collections.emptyList()));
				for (SymbolicExpression x : e.getValue())
					if (both.stream().noneMatch(y -> y == x))
						both.add(x);
				union.put(e.getKey(), Collections.unmodifiableList(both));
			}
			tags.put(c, Collections.unmodifiableMap(union));
		}
	}

	/**
	 * Assigns the given value to the given identifier in every case.
	 *
	 * @param id    the identifier
	 * @param value the value
	 * @param pp    the program point of the assignment (can be {@code null})
	 *
	 * @return the resulting store
	 */
	public SymbolicStore assign(
			Identifier id,
			SymbolicExpression value,
			ProgramPoint pp) {
		return map(c -> c.assign(id, value, factory, pp));
	}

	/**
	 * Records, in every case, that the given unknown value is introduced (e.g.,
	 * as the result of a call to code that is not part of the program): the
	 * input it stands for is consumed by the executions of the cases (see
	 * {@link SymbolicCase#withProduced(java.util.Collection)}). Nothing changes
	 * if the value does not stand for an input.
	 *
	 * @param value the unknown value
	 * @param pp    the program point where the value is introduced (can be
	 *                  {@code null})
	 *
	 * @return the resulting store
	 */
	public SymbolicStore introduce(
			PushAny value,
			ProgramPoint pp) {
		FreshValue fresh = factory.fresh(value, pp);
		if (!(fresh instanceof InputValue))
			return this;
		Set<InputValue> input = Collections.singleton((InputValue) fresh);
		return map(c -> c.withProduced(input));
	}

	/**
	 * Assumes the given condition in every case.
	 *
	 * @param condition the condition
	 * @param pp        the program point where the condition is assumed (can be
	 *                      {@code null})
	 *
	 * @return the resulting store
	 */
	public SymbolicStore assume(
			SymbolicExpression condition,
			ProgramPoint pp) {
		return map(c -> c.assume(condition, factory, pp));
	}

	/**
	 * Forgets the given identifiers in every case.
	 *
	 * @param ids the identifiers
	 *
	 * @return the resulting store
	 */
	public SymbolicStore forget(
			Iterable<Identifier> ids) {
		return map(c -> c.forget(ids));
	}

	/**
	 * Forgets the identifiers satisfying the given predicate in every case.
	 *
	 * @param test the predicate
	 *
	 * @return the resulting store
	 */
	public SymbolicStore forgetIf(
			Predicate<Identifier> test) {
		return map(c -> c.forgetIf(test));
	}

	/**
	 * Makes the heap unconstrained in every case, except for the contents of
	 * immutable fields (see {@link HeapModel#immutableContents()}).
	 *
	 * @return the resulting store
	 */
	public SymbolicStore heapHavoc() {
		// the values of final fields of existing objects cannot change
		Set<Identifier> immutable = factory.getHeapModel().immutableContents();
		return map(c -> c.withHeapHavoc(immutable));
	}

	/**
	 * Makes the heap unconstrained in every case, except for the given heap
	 * contents (see
	 * {@link SymbolicCase#withHeapHavoc(Set, FreshValueFactory, CodeLocation)}).
	 *
	 * @param preserved the heap contents that are preserved
	 * @param location  the location of the transition
	 *
	 * @return the resulting store
	 */
	public SymbolicStore heapHavoc(
			Set<Identifier> preserved,
			CodeLocation location) {
		return map(c -> c.withHeapHavoc(preserved, factory, location));
	}

	/**
	 * Writes the given value in the given heap location in every case.
	 *
	 * @param target the written location
	 * @param value  the written value
	 * @param pp     the program point of the write (can be {@code null})
	 *
	 * @return the resulting store
	 */
	public SymbolicStore heapWrite(
			AccessChild target,
			SymbolicExpression value,
			ProgramPoint pp) {
		return map(c -> c.heapWrite(target, value, factory, pp));
	}

	/**
	 * Performs the given allocation in every case.
	 *
	 * @param allocation the allocation
	 * @param pp         the program point of the allocation (can be
	 *                       {@code null})
	 *
	 * @return the resulting store
	 */
	public SymbolicStore allocate(
			MemoryAllocation allocation,
			ProgramPoint pp) {
		return map(c -> c.allocate(allocation, factory, pp));
	}

	/**
	 * Loses all values in every case.
	 *
	 * @return the resulting store
	 */
	public SymbolicStore havocAll() {
		return map(SymbolicCase::withHavocAll);
	}

	/**
	 * Pushes (if {@code push} holds) or pops the scope identified by the given
	 * token in every case (see
	 * {@link SymbolicCase#scope(ScopeToken, boolean)}).
	 *
	 * @param token the token of the scope
	 * @param push  whether the scope is pushed or popped
	 *
	 * @return the resulting store
	 */
	public SymbolicStore scope(
			ScopeToken token,
			boolean push) {
		// the computed expressions refer to the identifiers of the scope
		SymbolicStore scoped = map(c -> c.scope(token, push));
		return new SymbolicStore(scoped.cases, factory, maxCases);
	}

	/**
	 * Yields the store containing the cases of both this store and the given
	 * one, merging them if their number exceeds the bound.
	 *
	 * @param other the other store
	 *
	 * @return the resulting store
	 */
	public SymbolicStore lub(
			SymbolicStore other) {
		Set<SymbolicCase> result = new LinkedHashSet<>(cases);
		result.addAll(other.cases);
		Map<SymbolicCase, Map<ProgramPoint, List<SymbolicExpression>>> tags = new LinkedHashMap<>();
		Set<SymbolicCase> unknown = new HashSet<>();
		for (SymbolicCase c : cases)
			tag(tags, unknown, c, computed.get(c));
		for (SymbolicCase c : other.cases)
			tag(tags, unknown, c, other.computed.get(c));
		if (result.size() > maxCases) {
			Iterator<SymbolicCase> it = result.iterator();
			SymbolicCase merged = it.next();
			while (it.hasNext())
				merged = SymbolicCase.merge(merged, it.next(), factory, SyntheticLocation.INSTANCE);
			result = Collections.singleton(merged);
			// the merged case computed any of the expressions of the cases
			// (unknown if some are)
			Map<SymbolicCase, Map<ProgramPoint, List<SymbolicExpression>>> all = new LinkedHashMap<>();
			Set<SymbolicCase> none = new HashSet<>();
			if (!unknown.isEmpty())
				none.add(merged);
			for (Map<ProgramPoint, List<SymbolicExpression>> exprs : tags.values())
				tag(all, none, merged, exprs);
			all.keySet().removeAll(none);
			tags = all;
			unknown = none;
		}
		tags.keySet().removeAll(unknown);
		return new SymbolicStore(Collections.unmodifiableSet(result), factory, maxCases,
				Collections.unmodifiableMap(tags));
	}

	/**
	 * Yields {@code true} if every case of this store is also a case of the
	 * given one, or if the given store loses all values.
	 *
	 * @param other the other store
	 *
	 * @return whether or not this store is included in the given one
	 */
	public boolean lessOrEqual(
			SymbolicStore other) {
		return other.isTop() || other.cases.containsAll(cases);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cases);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SymbolicStore other = (SymbolicStore) obj;
		return Objects.equals(cases, other.cases);
	}

	@Override
	public String toString() {
		return cases.toString();
	}
}
