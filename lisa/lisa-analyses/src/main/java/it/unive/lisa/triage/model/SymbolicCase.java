package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * One case of a symbolic transition: a path condition, under which the
 * transition updates some identifiers with symbolic values. Both the conditions
 * and the values are expressed over the values that identifiers have
 * <b>before</b> the transition (identifiers that are not updated keep their
 * value), and over {@link FreshValue}s. A case also records:
 * <ul>
 * <li>the identifiers that are forgotten by the transition, whose value after
 * the transition is unconstrained;</li>
 * <li>whether the transition writes the heap ({@code heapHavoc}), in which case
 * the whole heap is unconstrained after the transition;</li>
 * <li>whether the transition loses track of all values ({@code havocAll}), in
 * which case every identifier that is not explicitly updated afterwards is
 * unconstrained after the transition.</li>
 * </ul>
 * Instances of this class are immutable.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class SymbolicCase {

	/**
	 * The identity transition.
	 */
	public static final SymbolicCase IDENTITY = new SymbolicCase(
			Collections.emptyList(),
			Collections.emptyMap(),
			Collections.emptySet(),
			Collections.emptyList(),
			false,
			false);

	private final List<SymbolicExpression> pathCondition;

	private final Map<Identifier, SymbolicExpression> updates;

	private final Set<Identifier> forgotten;

	private final List<Predicate<Identifier>> forgottenIf;

	private final boolean heapHavoc;

	private final boolean havocAll;

	private SymbolicCase(
			List<SymbolicExpression> pathCondition,
			Map<Identifier, SymbolicExpression> updates,
			Set<Identifier> forgotten,
			List<Predicate<Identifier>> forgottenIf,
			boolean heapHavoc,
			boolean havocAll) {
		this.pathCondition = pathCondition;
		this.updates = updates;
		this.forgotten = forgotten;
		this.forgottenIf = forgottenIf;
		this.heapHavoc = heapHavoc;
		this.havocAll = havocAll;
	}

	/**
	 * Yields the path condition of this case, as a conjunction of boolean
	 * expressions.
	 *
	 * @return the conjuncts of the path condition
	 */
	public List<SymbolicExpression> getPathCondition() {
		return pathCondition;
	}

	/**
	 * Yields the identifiers updated by this case, mapped to their values after
	 * the transition.
	 *
	 * @return the updates
	 */
	public Map<Identifier, SymbolicExpression> getUpdates() {
		return updates;
	}

	/**
	 * Yields {@code true} if the value of the given identifier after the
	 * transition is unconstrained, that is, if it is not updated and it has
	 * been forgotten or all values have been lost.
	 *
	 * @param id the identifier
	 *
	 * @return whether or not the identifier is unconstrained after the
	 *             transition
	 */
	public boolean isUnconstrained(
			Identifier id) {
		if (updates.containsKey(id))
			return false;
		if (havocAll || forgotten.contains(id) || (heapHavoc && isHeap(id)))
			return true;
		for (Predicate<Identifier> test : forgottenIf)
			if (test.test(id))
				return true;
		return false;
	}

	/**
	 * Yields {@code true} if this case writes the heap, making it unconstrained
	 * after the transition.
	 *
	 * @return whether or not the heap is unconstrained after the transition
	 */
	public boolean isHeapHavoc() {
		return heapHavoc;
	}

	/**
	 * Yields {@code true} if this case loses track of all values.
	 *
	 * @return whether or not all identifiers that are not updated are
	 *             unconstrained after the transition
	 */
	public boolean isHavocAll() {
		return havocAll;
	}

	/**
	 * Yields {@code true} if the given identifier denotes a heap location.
	 *
	 * @param id the identifier
	 *
	 * @return whether or not the identifier is a heap location
	 */
	public static boolean isHeap(
			Identifier id) {
		return id instanceof HeapLocation || id instanceof MemoryPointer;
	}

	/**
	 * Rewrites the given expression, evaluated at the end of this case, into an
	 * expression over the values before the transition and fresh values.
	 * Identifiers are replaced by their current values, unknown values
	 * ({@link PushAny}) by fresh values, and anything that cannot be tracked
	 * (heap accesses, unconstrained identifiers, unsupported expressions) by
	 * {@link SummaryValue}s.
	 *
	 * @param expression the expression
	 * @param factory    the factory of fresh values
	 * @param pp         the program point where the expression is evaluated
	 *                       (can be {@code null})
	 *
	 * @return the rewritten expression
	 */
	public SymbolicExpression substitute(
			SymbolicExpression expression,
			FreshValueFactory factory,
			ProgramPoint pp) {
		if (expression instanceof FreshValue)
			return expression;
		if (expression instanceof Identifier) {
			Identifier id = (Identifier) expression;
			SymbolicExpression value = updates.get(id);
			if (value != null)
				return value;
			if (isHeap(id))
				return summary(expression, SummaryValue.Kind.HEAP, "value read from the heap", factory);
			if (isUnconstrained(id))
				return summary(expression, SummaryValue.Kind.UNCONSTRAINED, "unconstrained value of " + id, factory);
			return id;
		}
		if (expression instanceof PushInv)
			return summary(expression, SummaryValue.Kind.INVALID, "invalid value", factory);
		if (expression instanceof PushAny)
			return factory.fresh((PushAny) expression, pp);
		if (expression instanceof Constant || expression instanceof NullConstant || expression instanceof Skip)
			return expression;
		if (expression instanceof UnaryExpression) {
			UnaryExpression u = (UnaryExpression) expression;
			return new UnaryExpression(
					u.getStaticType(),
					substitute(u.getExpression(), factory, pp),
					u.getOperator(),
					u.getCodeLocation());
		}
		if (expression instanceof BinaryExpression) {
			BinaryExpression b = (BinaryExpression) expression;
			return new BinaryExpression(
					b.getStaticType(),
					substitute(b.getLeft(), factory, pp),
					substitute(b.getRight(), factory, pp),
					b.getOperator(),
					b.getCodeLocation());
		}
		if (expression instanceof TernaryExpression) {
			TernaryExpression t = (TernaryExpression) expression;
			return new TernaryExpression(
					t.getStaticType(),
					substitute(t.getLeft(), factory, pp),
					substitute(t.getMiddle(), factory, pp),
					substitute(t.getRight(), factory, pp),
					t.getOperator(),
					t.getCodeLocation());
		}
		if (expression instanceof HeapExpression)
			return summary(expression, SummaryValue.Kind.HEAP, "heap expression " + expression, factory);
		return summary(expression, SummaryValue.Kind.UNSUPPORTED, "unsupported expression " + expression, factory);
	}

	private static SummaryValue summary(
			SymbolicExpression expression,
			SummaryValue.Kind kind,
			String reason,
			FreshValueFactory factory) {
		return factory.summary(expression.getStaticType(), kind, reason, expression.getCodeLocation());
	}

	/**
	 * Yields the case obtained by assigning the given value to the given
	 * identifier at the end of this case. Assignments to heap locations make
	 * the heap unconstrained instead.
	 *
	 * @param id      the assigned identifier
	 * @param value   the assigned value
	 * @param factory the factory of fresh values
	 * @param pp      the program point of the assignment (can be {@code null})
	 *
	 * @return the resulting case
	 */
	public SymbolicCase assign(
			Identifier id,
			SymbolicExpression value,
			FreshValueFactory factory,
			ProgramPoint pp) {
		if (isHeap(id))
			return withHeapHavoc();
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(updates);
		upd.put(id, substitute(value, factory, pp));
		Set<Identifier> forg = forgotten;
		if (forgotten.contains(id)) {
			forg = new HashSet<>(forgotten);
			forg.remove(id);
			forg = Collections.unmodifiableSet(forg);
		}
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), forg, forgottenIf, heapHavoc,
				havocAll);
	}

	/**
	 * Yields the case obtained by assuming that the given condition holds at
	 * the end of this case.
	 *
	 * @param condition the condition
	 * @param factory   the factory of fresh values
	 * @param pp        the program point where the condition is assumed (can be
	 *                      {@code null})
	 *
	 * @return the resulting case
	 */
	public SymbolicCase assume(
			SymbolicExpression condition,
			FreshValueFactory factory,
			ProgramPoint pp) {
		List<SymbolicExpression> pc = new ArrayList<>(pathCondition);
		pc.add(substitute(condition, factory, pp));
		return new SymbolicCase(Collections.unmodifiableList(pc), updates, forgotten, forgottenIf, heapHavoc,
				havocAll);
	}

	/**
	 * Yields the case obtained by forgetting the given identifiers at the end
	 * of this case.
	 *
	 * @param ids the identifiers to forget
	 *
	 * @return the resulting case
	 */
	public SymbolicCase forget(
			Iterable<Identifier> ids) {
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(updates);
		Set<Identifier> forg = new HashSet<>(forgotten);
		for (Identifier id : ids) {
			upd.remove(id);
			forg.add(id);
		}
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), Collections.unmodifiableSet(forg),
				forgottenIf, heapHavoc, havocAll);
	}

	/**
	 * Yields the case obtained by forgetting, at the end of this case, all the
	 * identifiers satisfying the given predicate.
	 *
	 * @param test the predicate
	 *
	 * @return the resulting case
	 */
	public SymbolicCase forgetIf(
			Predicate<Identifier> test) {
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(updates);
		upd.keySet().removeIf(test);
		List<Predicate<Identifier>> tests = new ArrayList<>(forgottenIf);
		tests.add(test);
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), forgotten,
				Collections.unmodifiableList(tests), heapHavoc, havocAll);
	}

	/**
	 * Yields this case, where the heap is unconstrained after the transition.
	 *
	 * @return the resulting case
	 */
	public SymbolicCase withHeapHavoc() {
		if (heapHavoc)
			return this;
		return new SymbolicCase(pathCondition, updates, forgotten, forgottenIf, true, havocAll);
	}

	/**
	 * Yields this case, where all values are lost: every identifier is
	 * unconstrained after the transition, unless it is updated afterwards.
	 *
	 * @return the resulting case
	 */
	public SymbolicCase withHavocAll() {
		return new SymbolicCase(pathCondition, Collections.emptyMap(), Collections.emptySet(),
				Collections.emptyList(), true, true);
	}

	/**
	 * Merges two cases into a single one that over-approximates both. The
	 * resulting path condition contains only the conjuncts common to both
	 * cases, and identifiers whose values differ between the cases become
	 * unconstrained (they are assigned a fresh {@link SummaryValue}).
	 *
	 * @param first    the first case
	 * @param second   the second case
	 * @param factory  the factory of fresh values
	 * @param location the location to attribute fresh values to
	 *
	 * @return the merged case
	 */
	public static SymbolicCase merge(
			SymbolicCase first,
			SymbolicCase second,
			FreshValueFactory factory,
			CodeLocation location) {
		List<SymbolicExpression> pc = new ArrayList<>(first.pathCondition);
		pc.retainAll(second.pathCondition);

		Set<Identifier> keys = new HashSet<>(first.updates.keySet());
		keys.addAll(second.updates.keySet());
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>();
		Set<Identifier> forg = new HashSet<>(first.forgotten);
		forg.addAll(second.forgotten);
		for (Identifier id : keys) {
			SymbolicExpression v1 = first.valueOf(id);
			SymbolicExpression v2 = second.valueOf(id);
			if (v1 != null && v1.equals(v2)) {
				if (!v1.equals(id))
					upd.put(id, v1);
			} else
				upd.put(id, factory.summary(id.getStaticType(), SummaryValue.Kind.MERGE, "merged value of " + id, location));
		}

		List<Predicate<Identifier>> tests = new ArrayList<>(first.forgottenIf);
		tests.addAll(second.forgottenIf);
		return new SymbolicCase(
				Collections.unmodifiableList(pc),
				Collections.unmodifiableMap(upd),
				Collections.unmodifiableSet(forg),
				Collections.unmodifiableList(tests),
				first.heapHavoc || second.heapHavoc,
				first.havocAll || second.havocAll);
	}

	/**
	 * The value of the identifier after the transition, or {@code null} if it
	 * is unconstrained.
	 */
	private SymbolicExpression valueOf(
			Identifier id) {
		SymbolicExpression value = updates.get(id);
		if (value != null)
			return value;
		return isUnconstrained(id) ? null : id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(forgotten, forgottenIf, havocAll, heapHavoc, pathCondition, updates);
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
		SymbolicCase other = (SymbolicCase) obj;
		return havocAll == other.havocAll
				&& heapHavoc == other.heapHavoc
				&& Objects.equals(forgotten, other.forgotten)
				&& Objects.equals(forgottenIf, other.forgottenIf)
				&& Objects.equals(pathCondition, other.pathCondition)
				&& Objects.equals(updates, other.updates);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(pathCondition.isEmpty() ? "true" : pathCondition.toString()).append(" => ").append(updates);
		if (!forgotten.isEmpty())
			sb.append(" forget ").append(forgotten);
		if (!forgottenIf.isEmpty())
			sb.append(" forget-if ").append(forgottenIf.size());
		if (heapHavoc)
			sb.append(" heap-havoc");
		if (havocAll)
			sb.append(" havoc-all");
		return sb.toString();
	}
}
