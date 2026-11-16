package it.unive.lisa.triage.model;

import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.ArrayList;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
 * operations over an expression computed by some cases only apply to them
 * (see {@link #restrictTo(SymbolicExpression)}), instead of pairing every case
 * with every expression computed by any of them.
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
	 * The expressions computed by each case, if known: a case that is not a
	 * key computed unknown expressions.
	 */
	private final Map<SymbolicCase, List<SymbolicExpression>> computed;

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
			Map<SymbolicCase, List<SymbolicExpression>> computed) {
		this.cases = cases;
		this.factory = factory;
		this.maxCases = maxCases;
		this.computed = computed;
	}

	/**
	 * Yields the store where every case computed the given expression (that
	 * is, the expression computed by the executions of this store).
	 *
	 * @param expression the expression
	 *
	 * @return the resulting store
	 */
	public SymbolicStore computing(
			SymbolicExpression expression) {
		Map<SymbolicCase, List<SymbolicExpression>> tags = new LinkedHashMap<>();
		for (SymbolicCase c : cases)
			tags.put(c, Collections.singletonList(expression));
		return new SymbolicStore(cases, factory, maxCases, Collections.unmodifiableMap(tags));
	}

	/**
	 * Yields the store made of the cases of this one that might have computed
	 * the given expression, that is, an operation over the expression only
	 * applies to them: if the expression contains (as itself, or as a
	 * sub-expression) the very expression computed by some cases, only the
	 * cases that computed it (or an expression equal to it), and the ones whose
	 * computed expressions are unknown, are kept. Expressions not computed by
	 * any case (e.g., new constants) keep all cases.
	 *
	 * @param expression the expression
	 *
	 * @return the resulting store
	 */
	public SymbolicStore restrictTo(
			SymbolicExpression expression) {
		if (computed.isEmpty())
			return this;
		// the computed expressions (by identity) that the expression contains
		Set<SymbolicExpression> tags = Collections.newSetFromMap(new IdentityHashMap<>());
		for (List<SymbolicExpression> exprs : computed.values())
			tags.addAll(exprs);
		List<SymbolicExpression> relevant = new ArrayList<>();
		collect(expression, tags, relevant);
		if (relevant.isEmpty())
			return this;
		Set<SymbolicCase> result = new LinkedHashSet<>();
		Map<SymbolicCase, List<SymbolicExpression>> kept = new LinkedHashMap<>();
		for (SymbolicCase c : cases) {
			List<SymbolicExpression> exprs = computed.get(c);
			if (exprs == null)
				result.add(c);
			else if (exprs.stream().anyMatch(relevant::contains)) {
				result.add(c);
				kept.put(c, exprs);
			}
		}
		if (result.size() == cases.size())
			return this;
		return new SymbolicStore(Collections.unmodifiableSet(result), factory, maxCases,
				Collections.unmodifiableMap(kept));
	}

	/**
	 * Adds to the given list the sub-expressions of the given expression that
	 * are (by identity) among the given ones.
	 */
	private static void collect(
			SymbolicExpression e,
			Set<SymbolicExpression> tags,
			List<SymbolicExpression> relevant) {
		if (e == null)
			return;
		if (tags.contains(e)) {
			relevant.add(e);
			return;
		}
		if (e instanceof UnaryExpression)
			collect(((UnaryExpression) e).getExpression(), tags, relevant);
		else if (e instanceof BinaryExpression) {
			collect(((BinaryExpression) e).getLeft(), tags, relevant);
			collect(((BinaryExpression) e).getRight(), tags, relevant);
		} else if (e instanceof TernaryExpression) {
			collect(((TernaryExpression) e).getLeft(), tags, relevant);
			collect(((TernaryExpression) e).getMiddle(), tags, relevant);
			collect(((TernaryExpression) e).getRight(), tags, relevant);
		} else if (e instanceof AccessChild) {
			collect(((AccessChild) e).getContainer(), tags, relevant);
			collect(((AccessChild) e).getChild(), tags, relevant);
		} else if (e instanceof HeapReference)
			collect(((HeapReference) e).getExpression(), tags, relevant);
		else if (e instanceof HeapDereference)
			collect(((HeapDereference) e).getExpression(), tags, relevant);
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
		return new SymbolicStore(Collections.singleton(SymbolicCase.IDENTITY), factory, maxCases);
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
	 * Yields {@code true} if this store loses all values, under no condition.
	 *
	 * @return whether or not this store is the top one
	 */
	public boolean isTop() {
		return cases.stream().anyMatch(SymbolicStore::isTop);
	}

	private static boolean isTop(
			SymbolicCase c) {
		return c.isHavocAll() && c.getPathCondition().isEmpty() && c.getUpdates().isEmpty();
	}

	private SymbolicStore map(
			Function<SymbolicCase, SymbolicCase> f) {
		Set<SymbolicCase> result = new LinkedHashSet<>();
		// the expressions computed by each case are the ones of the cases it
		// comes from (unknown if any of them is)
		Map<SymbolicCase, List<SymbolicExpression>> tags = new LinkedHashMap<>();
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
			Map<SymbolicCase, List<SymbolicExpression>> tags,
			Set<SymbolicCase> unknown,
			SymbolicCase c,
			List<SymbolicExpression> exprs) {
		if (exprs == null) {
			unknown.add(c);
			return;
		}
		List<SymbolicExpression> previous = tags.get(c);
		if (previous == null)
			tags.put(c, exprs);
		else {
			List<SymbolicExpression> union = new ArrayList<>(previous);
			for (SymbolicExpression e : exprs)
				if (!union.contains(e))
					union.add(e);
			tags.put(c, Collections.unmodifiableList(union));
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
		Map<SymbolicCase, List<SymbolicExpression>> tags = new LinkedHashMap<>();
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
			Map<SymbolicCase, List<SymbolicExpression>> all = new LinkedHashMap<>();
			Set<SymbolicCase> none = new HashSet<>();
			if (!unknown.isEmpty())
				none.add(merged);
			for (List<SymbolicExpression> exprs : tags.values())
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
