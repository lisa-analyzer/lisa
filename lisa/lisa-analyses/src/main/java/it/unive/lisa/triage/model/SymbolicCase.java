package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
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
 * <li>whether the transition writes the heap in ways that are not modeled
 * ({@code heapHavoc}, e.g., through a call whose callee is not modeled), in
 * which case the heap is unconstrained after the transition, except for the
 * immutable field contents that the transition cannot write (see
 * {@link HeapModel});</li>
 * <li>whether the transition loses track of all values ({@code havocAll}), in
 * which case every identifier that is not explicitly updated afterwards is
 * unconstrained after the transition.</li>
 * </ul>
 * The heap is represented as in {@link HeapModel}: reads and writes of fields
 * and array cells become reads and writes of the identifiers of the heap
 * model, and allocations yield fresh references. Instances of this class are
 * immutable.
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
			Collections.emptySet(),
			false);

	private final List<SymbolicExpression> pathCondition;

	private final Map<Identifier, SymbolicExpression> updates;

	private final Set<Identifier> forgotten;

	private final List<Predicate<Identifier>> forgottenIf;

	private final boolean heapHavoc;

	/**
	 * The heap contents that are preserved when {@link #heapHavoc} holds.
	 */
	private final Set<Identifier> preservedHeap;

	private final boolean havocAll;

	private SymbolicCase(
			List<SymbolicExpression> pathCondition,
			Map<Identifier, SymbolicExpression> updates,
			Set<Identifier> forgotten,
			List<Predicate<Identifier>> forgottenIf,
			boolean heapHavoc,
			Set<Identifier> preservedHeap,
			boolean havocAll) {
		this.pathCondition = pathCondition;
		this.updates = updates;
		this.forgotten = forgotten;
		this.forgottenIf = forgottenIf;
		this.heapHavoc = heapHavoc;
		this.preservedHeap = preservedHeap;
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
		if (havocAll || forgotten.contains(id))
			return true;
		if (heapHavoc && (isHeap(id) || (HeapModel.isHeapState(id) && !preservedHeap.contains(id))))
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
			if (HeapModel.isHeapState(id) && isUnconstrained(id))
				return summary(expression, SummaryValue.Kind.HEAP, "contents of " + id + " after unmodeled writes",
						factory);
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
		if (expression instanceof HeapReference) {
			SymbolicExpression location = ((HeapReference) expression).getExpression();
			if (location instanceof MemoryAllocation)
				// the reference most recently allocated by the site
				return substitute(HeapModel.site((MemoryAllocation) location), factory, pp);
			return summary(expression, SummaryValue.Kind.HEAP, "heap expression " + expression, factory);
		}
		if (expression instanceof MemoryAllocation)
			return substitute(HeapModel.site((MemoryAllocation) expression), factory, pp);
		if (expression instanceof HeapDereference)
			// the object is denoted by its reference
			return substitute(((HeapDereference) expression).getExpression(), factory, pp);
		if (expression instanceof AccessChild) {
			AccessChild access = (AccessChild) expression;
			String field = fieldOf(access, factory, pp);
			if (field == UNDECIDED)
				return summary(expression, SummaryValue.Kind.HEAP, "heap expression " + expression, factory);
			SymbolicExpression ref = substitute(access.getContainer(), factory, pp);
			CodeLocation loc = access.getCodeLocation();
			if (field != null) {
				Variable contents = HeapModel.field(field);
				return new TernaryExpression(access.getStaticType(),
						substitute(contents, factory, pp),
						substitute(HeapModel.written(contents), factory, pp),
						ref, HeapOperators.ReferenceRead.INSTANCE, loc);
			}
			SymbolicExpression row = new BinaryExpression(HeapType.FIELD,
					substitute(HeapModel.CELLS, factory, pp), ref, HeapOperators.ReferenceSelect.INSTANCE, loc);
			SymbolicExpression writtenRow = new BinaryExpression(HeapType.WRITTEN_FIELD,
					substitute(HeapModel.WRITTEN_CELLS, factory, pp), ref, HeapOperators.ReferenceSelect.INSTANCE,
					loc);
			return new TernaryExpression(access.getStaticType(), row, writtenRow,
					substitute(access.getChild(), factory, pp), HeapOperators.IndexRead.INSTANCE, loc);
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
	 * The result of {@link #fieldOf(AccessChild, FreshValueFactory, ProgramPoint)}
	 * when it cannot tell whether the access reads a field or a cell.
	 */
	private static final String UNDECIDED = "?";

	/**
	 * Yields the name of the field accessed by the given access, {@code null}
	 * if the access reads a cell of an array, or {@link #UNDECIDED} if this
	 * cannot be told. Fields are named by {@link GlobalVariable}s, while the
	 * properties of arrays (e.g., their length) are named by variables whose
	 * names are known to the heap model
	 * ({@link HeapModel#isArrayProperty(String)}), provided that they are not
	 * variables of the program, that are indexes.
	 */
	private static String fieldOf(
			AccessChild access,
			FreshValueFactory factory,
			ProgramPoint pp) {
		SymbolicExpression child = access.getChild();
		if (child instanceof GlobalVariable)
			return ((GlobalVariable) child).getName();
		if (child instanceof Variable && factory.getHeapModel().isArrayProperty(((Variable) child).getName())
				&& isArray(access.getContainer().getStaticType())) {
			String name = ((Variable) child).getName();
			if (pp == null)
				return UNDECIDED;
			return isProgramVariable(name, pp) ? null : name;
		}
		return null;
	}

	private static boolean isProgramVariable(
			String name,
			ProgramPoint pp) {
		CodeMemberDescriptor descriptor = pp.getCFG().getDescriptor();
		for (Parameter formal : descriptor.getFormals())
			if (formal.getName().equals(name))
				return true;
		for (VariableTableEntry entry : descriptor.getVariables())
			if (entry.getName().equals(name))
				return true;
		return false;
	}

	private static boolean isArray(
			Type type) {
		return type.isArrayType()
				|| (type.isReferenceType() && type.asReferenceType().getInnerType().isArrayType())
				|| (type.isPointerType() && type.asPointerType().getInnerType().isArrayType());
	}

	private SymbolicCase update(
			Identifier id,
			SymbolicExpression value) {
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(updates);
		upd.put(id, value);
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), forgotten, forgottenIf, heapHavoc,
				preservedHeap, havocAll);
	}

	/**
	 * Yields the case obtained by writing the given value in the given heap
	 * location (a field of an object or a cell of an array) at the end of this
	 * case.
	 *
	 * @param target  the written location
	 * @param value   the written value
	 * @param factory the factory of fresh values
	 * @param pp      the program point of the write (can be {@code null})
	 *
	 * @return the resulting case
	 */
	public SymbolicCase heapWrite(
			AccessChild target,
			SymbolicExpression value,
			FreshValueFactory factory,
			ProgramPoint pp) {
		String field = fieldOf(target, factory, pp);
		if (field == UNDECIDED)
			return withHeapHavoc();
		SymbolicExpression v = substitute(value, factory, pp);
		SymbolicExpression ref = substitute(target.getContainer(), factory, pp);
		CodeLocation loc = target.getCodeLocation();
		Constant yes = new Constant(Untyped.INSTANCE, true, loc);
		if (field != null) {
			Variable contents = HeapModel.field(field);
			Variable written = HeapModel.written(contents);
			SymbolicCase res = update(contents, new TernaryExpression(HeapType.FIELD,
					substitute(contents, factory, pp), ref, v, HeapOperators.ReferenceStore.INSTANCE, loc));
			return res.update(written, new TernaryExpression(HeapType.WRITTEN_FIELD,
					substitute(written, factory, pp), ref, yes, HeapOperators.ReferenceStore.INSTANCE, loc));
		}
		SymbolicExpression index = substitute(target.getChild(), factory, pp);
		SymbolicExpression cells = substitute(HeapModel.CELLS, factory, pp);
		SymbolicExpression row = new BinaryExpression(HeapType.FIELD, cells, ref,
				HeapOperators.ReferenceSelect.INSTANCE, loc);
		SymbolicExpression newRow = new TernaryExpression(HeapType.FIELD, row, index, v,
				HeapOperators.IndexStore.INSTANCE, loc);
		SymbolicExpression writtenCells = substitute(HeapModel.WRITTEN_CELLS, factory, pp);
		SymbolicExpression writtenRow = new BinaryExpression(HeapType.WRITTEN_FIELD, writtenCells, ref,
				HeapOperators.ReferenceSelect.INSTANCE, loc);
		SymbolicExpression newWrittenRow = new TernaryExpression(HeapType.WRITTEN_FIELD, writtenRow, index, yes,
				HeapOperators.IndexStore.INSTANCE, loc);
		SymbolicCase res = update(HeapModel.CELLS, new TernaryExpression(HeapType.CELLS, cells, ref, newRow,
				HeapOperators.ReferenceStore.INSTANCE, loc));
		return res.update(HeapModel.WRITTEN_CELLS, new TernaryExpression(HeapType.WRITTEN_CELLS, writtenCells, ref,
				newWrittenRow, HeapOperators.ReferenceStore.INSTANCE, loc));
	}

	/**
	 * Yields the case obtained by performing the given allocation at the end
	 * of this case: the allocation site holds a new reference, different from
	 * all existing ones, and the counter of allocations is incremented.
	 *
	 * @param allocation the allocation
	 * @param factory    the factory of fresh values
	 * @param pp         the program point of the allocation (can be
	 *                       {@code null})
	 *
	 * @return the resulting case
	 */
	public SymbolicCase allocate(
			MemoryAllocation allocation,
			FreshValueFactory factory,
			ProgramPoint pp) {
		CodeLocation loc = allocation.getCodeLocation();
		SymbolicExpression counter = substitute(HeapModel.ALLOC, factory, pp);
		SymbolicCase res = update(HeapModel.site(allocation),
				new UnaryExpression(HeapType.REFERENCE, counter, HeapOperators.NewReference.INSTANCE, loc));
		return res.update(HeapModel.ALLOC, new BinaryExpression(HeapType.COUNTER, counter,
				new Constant(HeapType.COUNTER, 1, loc), NumericNonOverflowingAdd.INSTANCE, loc));
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
			// locations of the abstraction of the heap of the analysis: the
			// write is modeled where the program writes the heap (see
			// heapWrite)
			return this;
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(updates);
		upd.put(id, substitute(value, factory, pp));
		Set<Identifier> forg = forgotten;
		if (forgotten.contains(id)) {
			forg = new HashSet<>(forgotten);
			forg.remove(id);
			forg = Collections.unmodifiableSet(forg);
		}
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), forg, forgottenIf, heapHavoc,
				preservedHeap, havocAll);
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
				preservedHeap, havocAll);
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
				forgottenIf, heapHavoc, preservedHeap, havocAll);
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
				Collections.unmodifiableList(tests), heapHavoc, preservedHeap, havocAll);
	}

	/**
	 * Yields this case, where the heap is unconstrained after the transition.
	 *
	 * @return the resulting case
	 */
	public SymbolicCase withHeapHavoc() {
		if (heapHavoc && preservedHeap.isEmpty())
			return this;
		return new SymbolicCase(pathCondition, updates, forgotten, forgottenIf, true, Collections.emptySet(),
				havocAll);
	}

	/**
	 * Yields this case, where the heap is unconstrained after the transition,
	 * except for the given heap contents, as after a call whose callees are
	 * not modeled. The callees might allocate objects: the counter of
	 * allocations is increased by an unknown amount, so that the references
	 * allocated afterwards are still new.
	 *
	 * @param preserved the heap contents that are preserved
	 * @param factory   the factory of fresh values
	 * @param location  the location of the transition
	 *
	 * @return the resulting case
	 */
	public SymbolicCase withHeapHavoc(
			Set<Identifier> preserved,
			FreshValueFactory factory,
			CodeLocation location) {
		SymbolicExpression counter = substitute(HeapModel.ALLOC, factory, null);
		SummaryValue allocated = factory.summary(HeapType.COUNTER, SummaryValue.Kind.HEAP,
				"objects allocated by unmodeled code", location);
		Set<Identifier> kept = new HashSet<>(preserved);
		if (heapHavoc)
			kept.retainAll(preservedHeap);
		List<SymbolicExpression> pc = new ArrayList<>(pathCondition);
		pc.add(new BinaryExpression(Untyped.INSTANCE, allocated, new Constant(HeapType.COUNTER, 0, location),
				ComparisonGe.INSTANCE, location));
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(updates);
		// heap contents that are not preserved are no longer updated
		upd.keySet().removeIf(id -> HeapModel.isHeapState(id) && !kept.contains(id));
		upd.put(HeapModel.ALLOC, new BinaryExpression(HeapType.COUNTER, counter, allocated,
				NumericNonOverflowingAdd.INSTANCE, location));
		return new SymbolicCase(Collections.unmodifiableList(pc), Collections.unmodifiableMap(upd), forgotten,
				forgottenIf, true, Collections.unmodifiableSet(kept), havocAll);
	}

	/**
	 * Yields this case, where all values are lost: every identifier is
	 * unconstrained after the transition, unless it is updated afterwards.
	 *
	 * @return the resulting case
	 */
	public SymbolicCase withHavocAll() {
		return new SymbolicCase(pathCondition, Collections.emptyMap(), Collections.emptySet(),
				Collections.emptyList(), true, Collections.emptySet(), true);
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
				mergePreserved(first, second),
				first.havocAll || second.havocAll);
	}

	/**
	 * The heap contents preserved by the merge of the given cases: the ones
	 * preserved by all the cases that make the heap unconstrained.
	 */
	private static Set<Identifier> mergePreserved(
			SymbolicCase first,
			SymbolicCase second) {
		if (first.heapHavoc && second.heapHavoc) {
			Set<Identifier> kept = new HashSet<>(first.preservedHeap);
			kept.retainAll(second.preservedHeap);
			return Collections.unmodifiableSet(kept);
		}
		return first.heapHavoc ? first.preservedHeap : second.preservedHeap;
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
		return Objects.hash(forgotten, forgottenIf, havocAll, heapHavoc, preservedHeap, pathCondition, updates);
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
				&& Objects.equals(preservedHeap, other.preservedHeap)
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
			sb.append(" heap-havoc").append(preservedHeap.isEmpty() ? "" : " preserving " + preservedHeap);
		if (havocAll)
			sb.append(" havoc-all");
		return sb.toString();
	}
}
