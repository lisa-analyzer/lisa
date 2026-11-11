package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
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
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 * and array cells become reads and writes of the identifiers of the heap model,
 * and allocations yield fresh references. Instances of this class are
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
			false,
			Collections.emptyList());

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

	/**
	 * The scopes pushed and popped by the transition, in order: the identifiers
	 * that are not updated are renamed accordingly (see
	 * {@link #preIdentifier(Identifier)}).
	 */
	private final List<Scoping> scopings;

	private SymbolicCase(
			List<SymbolicExpression> pathCondition,
			Map<Identifier, SymbolicExpression> updates,
			Set<Identifier> forgotten,
			List<Predicate<Identifier>> forgottenIf,
			boolean heapHavoc,
			Set<Identifier> preservedHeap,
			boolean havocAll,
			List<Scoping> scopings) {
		this.pathCondition = pathCondition;
		this.updates = updates;
		this.forgotten = forgotten;
		this.forgottenIf = forgottenIf;
		this.heapHavoc = heapHavoc;
		this.preservedHeap = preservedHeap;
		this.havocAll = havocAll;
		this.scopings = scopings;
	}

	/**
	 * A scope pushed or popped by a transition.
	 */
	private static final class Scoping {

		private final ScopeToken token;

		private final boolean push;

		private Scoping(
				ScopeToken token,
				boolean push) {
			this.token = token;
			this.push = push;
		}

		/**
		 * Yields the name of the given identifier after this scoping, or
		 * {@code null} if the identifier is not visible afterwards. The
		 * identifiers of the heap model are never scoped.
		 */
		private Identifier rename(
				Identifier id) {
			if (HeapModel.isHeapState(id))
				return id;
			try {
				SymbolicExpression res = push ? id.pushScope(token, null) : id.popScope(token, null);
				return res instanceof Identifier ? (Identifier) res : null;
			} catch (SemanticException e) {
				return null;
			}
		}

		/**
		 * Yields the name before this scoping of the given identifier, or
		 * {@code null} if the identifier did not exist before (e.g., a variable
		 * of the scope that has been pushed).
		 */
		private Identifier inverse(
				Identifier id) {
			if (HeapModel.isHeapState(id))
				return id;
			if (push) {
				if (id.equals(rename(id)))
					// not affected by scoping (e.g., globals)
					return id;
				if (!(id instanceof OutOfScopeIdentifier))
					return null;
				try {
					return ((OutOfScopeIdentifier) id).popScope(token, null);
				} catch (SemanticException e) {
					return null;
				}
			}
			try {
				SymbolicExpression pre = id.pushScope(token, null);
				return pre instanceof Identifier ? (Identifier) pre : null;
			} catch (SemanticException e) {
				return null;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(token, push);
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Scoping))
				return false;
			Scoping other = (Scoping) obj;
			return push == other.push && Objects.equals(token, other.token);
		}

		@Override
		public String toString() {
			return (push ? "push " : "pop ") + token;
		}
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
		if (heapHavoc && (isHeap(id) || (isHavockedWithHeap(id) && !preservedHeap.contains(id))))
			return true;
		for (Predicate<Identifier> test : forgottenIf)
			if (test.test(id))
				return true;
		return preIdentifier(id) == null;
	}

	/**
	 * Yields the identifier whose value before the transition is the value of
	 * the given identifier after it, if the identifier is not updated: the
	 * identifier itself, renamed back through the scopes pushed and popped by
	 * the transition. Yields {@code null} if no such identifier exists (e.g.,
	 * for a variable of a scope pushed by the transition, that did not exist
	 * before).
	 *
	 * @param id the identifier, after the transition
	 *
	 * @return the corresponding identifier before the transition, or
	 *             {@code null}
	 */
	public Identifier preIdentifier(
			Identifier id) {
		Identifier pre = id;
		for (int i = scopings.size() - 1; i >= 0 && pre != null; i--)
			pre = scopings.get(i).inverse(pre);
		return pre;
	}

	/**
	 * Yields {@code true} if the transition pushes or pops scopes, that is, if
	 * {@link #preIdentifier(Identifier)} might rename identifiers.
	 *
	 * @return whether or not the transition pushes or pops scopes
	 */
	public boolean hasScopings() {
		return !scopings.isEmpty();
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
	 * Yields whether the given identifier is unconstrained when the heap is
	 * (unless preserved): the identifiers of the heap model, and global
	 * variables (that code not modeled might write as well).
	 */
	private static boolean isHavockedWithHeap(
			Identifier id) {
		// the runtime types of objects never change
		return (HeapModel.isHeapState(id) && !id.equals(HeapModel.TYPES)) || HeapModel.tracksDefinition(id);
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
			SymbolicExpression before = valueBefore(id, factory);
			if (HeapModel.tracksDefinition(id) && !(before instanceof FreshValue))
				// the value before the transition might not be known
				return HeapModel.definedRead(before, substitute(HeapModel.defined(id), factory, pp));
			return before;
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
			if (b.getOperator() instanceof HeapOperators.DefinedRead && b.getLeft() instanceof Identifier) {
				// a read already guarded by whether the variable is defined
				Identifier id = (Identifier) b.getLeft();
				SymbolicExpression value = updates.get(id);
				if (value == null)
					value = valueBefore(id, factory);
				// values written by the model, or unknown, are not guarded
				return value instanceof Identifier && !(value instanceof FreshValue)
						? HeapModel.definedRead(value, substitute(b.getRight(), factory, pp))
						: value;
			}
			if (b.getOperator() instanceof TypeCheck && isReferenceTyped(b.getLeft())) {
				// tests of the runtime types of objects read the runtime
				// types recorded by the model
				Type target = singleToken(b.getRight());
				Set<Type> instances = target == null ? null : factory.getHeapModel().instancesOf(target);
				if (instances != null)
					return new BinaryExpression(b.getStaticType(), substitute(HeapModel.TYPES, factory, pp),
							substitute(b.getLeft(), factory, pp), new HeapOperators.RuntimeTypeIn(instances),
							b.getCodeLocation());
			}
			return fold(new BinaryExpression(
					b.getStaticType(),
					substitute(b.getLeft(), factory, pp),
					substitute(b.getRight(), factory, pp),
					b.getOperator(),
					b.getCodeLocation()));
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
			SymbolicExpression key = new BinaryExpression(HeapType.CELL_KEY, ref,
					substitute(access.getChild(), factory, pp), HeapOperators.CellKey.INSTANCE, loc);
			return new TernaryExpression(access.getStaticType(),
					substitute(HeapModel.CELLS, factory, pp),
					substitute(HeapModel.WRITTEN_CELLS, factory, pp),
					key, HeapOperators.ReferenceRead.INSTANCE, loc);
		}
		if (expression instanceof HeapExpression)
			return summary(expression, SummaryValue.Kind.HEAP, "heap expression " + expression, factory);
		return summary(expression, SummaryValue.Kind.UNSUPPORTED, "unsupported expression " + expression, factory);
	}

	/**
	 * Yields the value before the transition of the given identifier, that is
	 * not updated by this case: an unknown value if it is unconstrained, the
	 * identifier holding it otherwise.
	 */
	private SymbolicExpression valueBefore(
			Identifier id,
			FreshValueFactory factory) {
		if (isHavockedWithHeap(id) && isUnconstrained(id))
			return summary(id, SummaryValue.Kind.HEAP, "contents of " + id + " after unmodeled writes", factory);
		if (isUnconstrained(id))
			return summary(id, SummaryValue.Kind.UNCONSTRAINED, "unconstrained value of " + id, factory);
		return preIdentifier(id);
	}

	/**
	 * Folds the constants added to the allocation counter: {@code (c + n) + m}
	 * becomes {@code c + (n + m)}, so that the counter does not grow with the
	 * number of allocations (that are composed by substitutions).
	 */
	private static BinaryExpression fold(
			BinaryExpression b) {
		if (b.getStaticType() != HeapType.COUNTER
				|| b.getOperator() != NumericNonOverflowingAdd.INSTANCE
				|| !(b.getLeft() instanceof BinaryExpression)
				|| !(b.getRight() instanceof Constant)
				|| !(((Constant) b.getRight()).getValue() instanceof Integer))
			return b;
		BinaryExpression left = (BinaryExpression) b.getLeft();
		if (left.getStaticType() != HeapType.COUNTER
				|| left.getOperator() != NumericNonOverflowingAdd.INSTANCE
				|| !(left.getRight() instanceof Constant)
				|| !(((Constant) left.getRight()).getValue() instanceof Integer))
			return b;
		int sum = (Integer) ((Constant) left.getRight()).getValue() + (Integer) ((Constant) b.getRight()).getValue();
		return new BinaryExpression(HeapType.COUNTER, left.getLeft(),
				new Constant(HeapType.COUNTER, sum, b.getCodeLocation()), NumericNonOverflowingAdd.INSTANCE,
				b.getCodeLocation());
	}

	private static SummaryValue summary(
			SymbolicExpression expression,
			SummaryValue.Kind kind,
			String reason,
			FreshValueFactory factory) {
		return factory.summary(expression.getStaticType(), kind, reason, expression.getCodeLocation());
	}

	/**
	 * The result of
	 * {@link #fieldOf(AccessChild, FreshValueFactory, ProgramPoint)} when it
	 * cannot tell whether the access reads a field or a cell.
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
		if (child instanceof GlobalVariable) {
			// qualified by the class declaring it, when the static type of
			// the accessed object tells it
			String name = ((GlobalVariable) child).getName();
			String qualified = factory.getHeapModel().qualify(access.getContainer().getStaticType(), name);
			return qualified == null ? name : qualified;
		}
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

	/**
	 * Yields whether the given expression has the type of references (or
	 * pointers).
	 */
	private static boolean isReferenceTyped(
			SymbolicExpression e) {
		Type type = e.getStaticType();
		return type.isReferenceType() || type.isPointerType();
	}

	/**
	 * Yields the type denoted by the given type token, if it is a constant
	 * denoting a single type, or {@code null}.
	 */
	private static Type singleToken(
			SymbolicExpression token) {
		if (!(token instanceof Constant))
			return null;
		Object value = ((Constant) token).getValue();
		if (value instanceof Type)
			return (Type) value;
		Type type = token.getStaticType();
		if (type instanceof TypeTokenType && ((TypeTokenType) type).getTypes().size() == 1)
			return ((TypeTokenType) type).getTypes().iterator().next();
		return null;
	}

	private static boolean isArray(
			Type type) {
		return type.isArrayType()
				|| (type.isReferenceType() && type.asReferenceType().getInnerType().isArrayType())
				|| (type.isPointerType() && type.asPointerType().getInnerType().isArrayType());
	}

	/**
	 * The maximum size of the values of the updates of a case (as the number of
	 * their distinct sub-expressions, that is, of the size of their encodings):
	 * larger values are not tracked, since their size might grow quickly (e.g.,
	 * with repeated writes of the same heap contents within one transition).
	 */
	public static final long MAX_VALUE_SIZE = 1000;

	/**
	 * The maximum size of the trees of the values of the updates of a case (see
	 * {@link #MAX_VALUE_SIZE}): values are encoded once per shared
	 * sub-expression, but other operations (e.g., comparing cases) still visit
	 * their trees.
	 */
	public static final long MAX_VALUE_TREE_SIZE = 100_000;

	/**
	 * Yields the given value, or a summary if it is too large to be tracked
	 * (see {@link #MAX_VALUE_SIZE}).
	 */
	private static SymbolicExpression bounded(
			Identifier id,
			SymbolicExpression value,
			FreshValueFactory factory) {
		if (distinct(value, Collections.newSetFromMap(new IdentityHashMap<>()), MAX_VALUE_SIZE) <= MAX_VALUE_SIZE
				&& size(value, new IdentityHashMap<>(), MAX_VALUE_TREE_SIZE) <= MAX_VALUE_TREE_SIZE)
			return value;
		return factory.summary(id.getStaticType(),
				HeapModel.isHeapState(id) ? SummaryValue.Kind.HEAP : SummaryValue.Kind.UNSUPPORTED,
				"value of " + id + " too large to track", value.getCodeLocation());
	}

	/**
	 * Yields whether the given expression is too large to be tracked: the
	 * number of nodes of its tree, that is the size of its encoding, exceeds
	 * the given bound.
	 *
	 * @param expression the expression
	 * @param bound      the bound
	 *
	 * @return whether the expression is too large
	 */
	public static boolean isTooLarge(
			SymbolicExpression expression,
			long bound) {
		return size(expression, new IdentityHashMap<>(), bound) > bound;
	}

	/**
	 * The number of distinct sub-expressions (by identity) of the given
	 * expression, that is, the size of its encoding (at most the given bound
	 * plus one), adding the ones not yet visited to the given set.
	 */
	private static long distinct(
			SymbolicExpression e,
			Set<SymbolicExpression> visited,
			long bound) {
		if (visited.size() > bound || !visited.add(e))
			return visited.size();
		if (e instanceof UnaryExpression)
			distinct(((UnaryExpression) e).getExpression(), visited, bound);
		else if (e instanceof BinaryExpression) {
			distinct(((BinaryExpression) e).getLeft(), visited, bound);
			distinct(((BinaryExpression) e).getRight(), visited, bound);
		} else if (e instanceof TernaryExpression) {
			distinct(((TernaryExpression) e).getLeft(), visited, bound);
			distinct(((TernaryExpression) e).getMiddle(), visited, bound);
			distinct(((TernaryExpression) e).getRight(), visited, bound);
		}
		return visited.size();
	}

	/**
	 * The number of nodes of the tree of the given expression (at most the
	 * given bound plus one), computed over the shared sub-expressions.
	 */
	private static long size(
			SymbolicExpression e,
			Map<SymbolicExpression, Long> memo,
			long bound) {
		Long known = memo.get(e);
		if (known != null)
			return known;
		long res = 1;
		if (e instanceof UnaryExpression)
			res += size(((UnaryExpression) e).getExpression(), memo, bound);
		else if (e instanceof BinaryExpression)
			res += size(((BinaryExpression) e).getLeft(), memo, bound)
					+ size(((BinaryExpression) e).getRight(), memo, bound);
		else if (e instanceof TernaryExpression)
			res += size(((TernaryExpression) e).getLeft(), memo, bound)
					+ size(((TernaryExpression) e).getMiddle(), memo, bound)
					+ size(((TernaryExpression) e).getRight(), memo, bound);
		res = Math.min(res, bound + 1);
		memo.put(e, res);
		return res;
	}

	private SymbolicCase update(
			Identifier id,
			SymbolicExpression value,
			FreshValueFactory factory) {
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(updates);
		upd.put(id, bounded(id, value, factory));
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), forgotten, forgottenIf, heapHavoc,
				preservedHeap, havocAll, scopings);
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
		if (field == UNDECIDED) {
			// global variables are not part of the heap
			Set<Identifier> preserved = new LinkedHashSet<>(factory.getHeapModel().immutableContents());
			preserved.addAll(factory.getHeapModel().globalVariables());
			return withHeapHavoc(preserved);
		}
		SymbolicExpression v = substitute(value, factory, pp);
		SymbolicExpression ref = substitute(target.getContainer(), factory, pp);
		CodeLocation loc = target.getCodeLocation();
		Constant yes = new Constant(Untyped.INSTANCE, true, loc);
		if (field != null) {
			Variable contents = HeapModel.field(field);
			Variable written = HeapModel.written(contents);
			SymbolicCase res = update(contents, new TernaryExpression(HeapType.FIELD,
					substitute(contents, factory, pp), ref, v, HeapOperators.ReferenceStore.INSTANCE, loc), factory);
			res = res.update(written, new TernaryExpression(HeapType.WRITTEN_FIELD,
					substitute(written, factory, pp), ref, yes, HeapOperators.ReferenceStore.INSTANCE, loc),
					factory);
			// the same field of the same object might be accessed in the other
			// way (by name only, or qualified by its class): if the write
			// surely denotes it, it is written there too, otherwise it is no
			// longer written by the model there
			List<Identifier> aliases = new ArrayList<>();
			for (Identifier alias : factory.getHeapModel().aliasesOf(field, target.getContainer().getStaticType()))
				if (alias.getStaticType() == HeapType.FIELD)
					aliases.add(alias);
			if (aliases.size() == 1)
				return res.mirror(aliases.get(0), ref, v, yes, factory, pp, loc);
			Constant no = new Constant(Untyped.INSTANCE, false, loc);
			for (Identifier alias : aliases) {
				Variable aliasWritten = HeapModel.written(alias);
				res = res.update(aliasWritten, new TernaryExpression(HeapType.WRITTEN_FIELD,
						substitute(aliasWritten, factory, pp), ref, no, HeapOperators.ReferenceStore.INSTANCE, loc),
						factory);
			}
			return res;
		}
		SymbolicExpression key = new BinaryExpression(HeapType.CELL_KEY, ref,
				substitute(target.getChild(), factory, pp), HeapOperators.CellKey.INSTANCE, loc);
		SymbolicCase res = update(HeapModel.CELLS, new TernaryExpression(HeapType.CELLS,
				substitute(HeapModel.CELLS, factory, pp), key, v, HeapOperators.ReferenceStore.INSTANCE, loc),
				factory);
		return res.update(HeapModel.WRITTEN_CELLS, new TernaryExpression(HeapType.WRITTEN_CELLS,
				substitute(HeapModel.WRITTEN_CELLS, factory, pp), key, yes, HeapOperators.ReferenceStore.INSTANCE,
				loc), factory);
	}

	/**
	 * Writes the given value in the given field contents, at the given
	 * reference, recording that the location has been written.
	 */
	private SymbolicCase mirror(
			Identifier contents,
			SymbolicExpression ref,
			SymbolicExpression value,
			Constant yes,
			FreshValueFactory factory,
			ProgramPoint pp,
			CodeLocation loc) {
		Variable written = HeapModel.written(contents);
		SymbolicCase res = update(contents, new TernaryExpression(HeapType.FIELD,
				substitute(contents, factory, pp), ref, value, HeapOperators.ReferenceStore.INSTANCE, loc), factory);
		return res.update(written, new TernaryExpression(HeapType.WRITTEN_FIELD,
				res.substitute(written, factory, pp), ref, yes, HeapOperators.ReferenceStore.INSTANCE, loc), factory);
	}

	/**
	 * Yields the case obtained by performing the given allocation at the end of
	 * this case: the allocation site holds a new reference, different from all
	 * existing ones, and the counter of allocations is incremented.
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
		UnaryExpression reference = new UnaryExpression(HeapType.REFERENCE, counter,
				HeapOperators.NewReference.INSTANCE, loc);
		SymbolicCase res = update(HeapModel.site(allocation), reference, factory);
		// the runtime type of the new object
		Type type = HeapModel.objectType(allocation.getStaticType());
		if (!type.isUntyped())
			res = res.update(HeapModel.TYPES, new TernaryExpression(HeapType.TYPES,
					res.substitute(HeapModel.TYPES, factory, pp), reference,
					new Constant(new TypeTokenType(Collections.singleton(type)), type, loc),
					HeapOperators.ReferenceStore.INSTANCE, loc), factory);
		return res.update(HeapModel.ALLOC, fold(new BinaryExpression(HeapType.COUNTER, counter,
				new Constant(HeapType.COUNTER, 1, loc), NumericNonOverflowingAdd.INSTANCE, loc)), factory);
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
		upd.put(id, bounded(id, substitute(value, factory, pp), factory));
		Set<Identifier> forg = forgotten;
		if (forgotten.contains(id)) {
			forg = new LinkedHashSet<>(forgotten);
			forg.remove(id);
			forg = Collections.unmodifiableSet(forg);
		}
		if (HeapModel.tracksDefinition(id))
			upd.put(HeapModel.defined(id), new Constant(Untyped.INSTANCE, true, value.getCodeLocation()));
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), forg, forgottenIf, heapHavoc,
				preservedHeap, havocAll, scopings);
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
				preservedHeap, havocAll, scopings);
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
		Set<Identifier> forg = new LinkedHashSet<>(forgotten);
		for (Identifier id : ids) {
			upd.remove(id);
			forg.add(id);
		}
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), Collections.unmodifiableSet(forg),
				forgottenIf, heapHavoc, preservedHeap, havocAll, scopings);
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
				Collections.unmodifiableList(tests), heapHavoc, preservedHeap, havocAll, scopings);
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
				havocAll, scopings);
	}

	/**
	 * Yields this case, where the heap is unconstrained after the transition
	 * (e.g., because of a write that the heap model does not represent), except
	 * for the given heap contents.
	 *
	 * @param preserved the heap contents that are preserved
	 *
	 * @return the resulting case
	 */
	public SymbolicCase withHeapHavoc(
			Set<Identifier> preserved) {
		Set<Identifier> kept = new LinkedHashSet<>(preserved);
		if (heapHavoc)
			kept.retainAll(preservedHeap);
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(updates);
		// heap contents that are not preserved are no longer updated
		upd.keySet().removeIf(id -> isHavockedWithHeap(id) && !kept.contains(id));
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), forgotten, forgottenIf, true,
				Collections.unmodifiableSet(kept), havocAll, scopings);
	}

	/**
	 * Yields this case, where the heap is unconstrained after the transition,
	 * except for the given heap contents, as after a call whose callees are not
	 * modeled. The callees might allocate objects: the counter of allocations
	 * is increased by an unknown amount, so that the references allocated
	 * afterwards are still new.
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
		AllocationOffset allocated = factory.allocationOffset(location);
		Set<Identifier> kept = new LinkedHashSet<>(preserved);
		if (heapHavoc)
			kept.retainAll(preservedHeap);
		List<SymbolicExpression> pc = new ArrayList<>(pathCondition);
		pc.add(new BinaryExpression(Untyped.INSTANCE, allocated, new Constant(HeapType.COUNTER, 0, location),
				ComparisonGe.INSTANCE, location));
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(updates);
		// heap contents that are not preserved are no longer updated
		upd.keySet().removeIf(id -> isHavockedWithHeap(id) && !kept.contains(id));
		upd.put(HeapModel.ALLOC, new BinaryExpression(HeapType.COUNTER, counter, allocated,
				NumericNonOverflowingAdd.INSTANCE, location));
		return new SymbolicCase(Collections.unmodifiableList(pc), Collections.unmodifiableMap(upd), forgotten,
				forgottenIf, true, Collections.unmodifiableSet(kept), havocAll, scopings);
	}

	/**
	 * Yields this case, followed by the push (if {@code push} holds) or the pop
	 * of the scope identified by the given token: identifiers are renamed as
	 * the analysis renames them ({@link Identifier#pushScope} and
	 * {@link Identifier#popScope}), and the ones that are not visible after the
	 * scoping are forgotten. The identifiers of the heap model are never
	 * renamed.
	 *
	 * @param token the token of the scope
	 * @param push  whether the scope is pushed or popped
	 *
	 * @return the resulting case
	 */
	public SymbolicCase scope(
			ScopeToken token,
			boolean push) {
		if (havocAll && updates.isEmpty())
			return this;
		Scoping scoping = new Scoping(token, push);
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>();
		for (Map.Entry<Identifier, SymbolicExpression> u : updates.entrySet()) {
			Identifier renamed = scoping.rename(u.getKey());
			if (renamed != null)
				upd.put(renamed, u.getValue());
		}
		Set<Identifier> forg = new LinkedHashSet<>();
		for (Identifier id : forgotten) {
			Identifier renamed = scoping.rename(id);
			if (renamed != null)
				forg.add(renamed);
		}
		List<Predicate<Identifier>> tests = new ArrayList<>();
		for (Predicate<Identifier> test : forgottenIf)
			tests.add(id -> {
				Identifier pre = scoping.inverse(id);
				return pre != null && test.test(pre);
			});
		List<Scoping> scs = new ArrayList<>(scopings);
		scs.add(scoping);
		return new SymbolicCase(pathCondition, Collections.unmodifiableMap(upd), Collections.unmodifiableSet(forg),
				Collections.unmodifiableList(tests), heapHavoc, preservedHeap, havocAll,
				Collections.unmodifiableList(scs));
	}

	/**
	 * Yields this case, where all values are lost: every identifier is
	 * unconstrained after the transition, unless it is updated afterwards.
	 *
	 * @return the resulting case
	 */
	public SymbolicCase withHavocAll() {
		return new SymbolicCase(pathCondition, Collections.emptyMap(), Collections.emptySet(),
				Collections.emptyList(), true, Collections.emptySet(), true, Collections.emptyList());
	}

	/**
	 * Yields the sequential composition of this case with the given one, that
	 * is, a case describing a transition of this case followed by a transition
	 * of the given one: its path condition holds if this case's holds and the
	 * one of the given case holds after it, and identifiers have the values the
	 * given case gives them, evaluated after this case. Yields {@code null} if
	 * some condition or value of the composition is larger than the given bound
	 * (see {@link #isTooLarge(SymbolicExpression, long)}), since tracking it
	 * would be expensive (it would be summarized, losing precision, when
	 * assigned).
	 *
	 * @param next    the case following this one
	 * @param factory the factory of fresh values (for the identifiers that are
	 *                    unconstrained after this case, but read by the next
	 *                    one)
	 * @param bound   the maximum size of the conditions and values of the
	 *                    composition
	 *
	 * @return the composition, or {@code null}
	 */
	public SymbolicCase then(
			SymbolicCase next,
			FreshValueFactory factory,
			long bound) {
		// the conditions and values of the next case are over the values
		// before it, that is, after this case (identifiers unconstrained
		// after this case are read as the same unknown value everywhere)
		Map<Identifier, SymbolicExpression> reads = new LinkedHashMap<>();
		List<SymbolicExpression> pc = new ArrayList<>(pathCondition);
		for (SymbolicExpression cond : next.pathCondition) {
			SymbolicExpression c = substituteOnce(cond, factory, reads);
			if (isTooLarge(c, bound))
				return null;
			pc.add(c);
		}
		Map<Identifier, SymbolicExpression> values = new LinkedHashMap<>();
		for (Map.Entry<Identifier, SymbolicExpression> u : next.updates.entrySet()) {
			SymbolicExpression v = substituteOnce(u.getValue(), factory, reads);
			if (isTooLarge(v, bound))
				return null;
			values.put(u.getKey(), v);
		}

		// the identifiers that the next case does not update are renamed by
		// its scopings, and might be unconstrained after it
		SymbolicCase result = this;
		for (Scoping scoping : next.scopings)
			result = result.scope(scoping.token, scoping.push);
		if (next.havocAll)
			result = result.withHavocAll();
		else {
			if (!next.forgotten.isEmpty())
				result = result.forget(next.forgotten);
			for (Predicate<Identifier> test : next.forgottenIf)
				result = result.forgetIf(test);
			if (next.heapHavoc)
				result = result.withHeapHavoc(next.preservedHeap);
		}

		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>(result.updates);
		upd.putAll(values);
		Set<Identifier> forg = result.forgotten;
		if (!Collections.disjoint(forg, values.keySet())) {
			forg = new LinkedHashSet<>(forg);
			forg.removeAll(values.keySet());
			forg = Collections.unmodifiableSet(forg);
		}
		return new SymbolicCase(Collections.unmodifiableList(pc), Collections.unmodifiableMap(upd), forg,
				result.forgottenIf, result.heapHavoc, result.preservedHeap, result.havocAll, result.scopings);
	}

	/**
	 * Rewrites the given expression of a case following this one (thus, over
	 * identifiers and fresh values) into an expression over the values before
	 * this case, as
	 * {@link #substitute(SymbolicExpression, FreshValueFactory, ProgramPoint)}
	 * does, rewriting each identifier once (so that the unknown values
	 * replacing unconstrained identifiers are shared).
	 */
	private SymbolicExpression substituteOnce(
			SymbolicExpression expression,
			FreshValueFactory factory,
			Map<Identifier, SymbolicExpression> reads) {
		if (expression instanceof FreshValue)
			return expression;
		if (expression instanceof Identifier) {
			Identifier id = (Identifier) expression;
			SymbolicExpression known = reads.get(id);
			if (known == null) {
				known = substitute(id, factory, null);
				reads.put(id, known);
			}
			return known;
		}
		if (expression instanceof UnaryExpression) {
			UnaryExpression u = (UnaryExpression) expression;
			return new UnaryExpression(u.getStaticType(), substituteOnce(u.getExpression(), factory, reads),
					u.getOperator(), u.getCodeLocation());
		}
		if (expression instanceof BinaryExpression) {
			BinaryExpression b = (BinaryExpression) expression;
			if (b.getOperator() instanceof HeapOperators.DefinedRead && b.getLeft() instanceof Identifier) {
				// a read already guarded by whether the variable is defined
				Identifier id = (Identifier) b.getLeft();
				SymbolicExpression value = updates.get(id);
				if (value == null)
					value = valueBefore(id, factory);
				// values written by the model, or unknown, are not guarded
				return value instanceof Identifier && !(value instanceof FreshValue)
						? HeapModel.definedRead(value, substituteOnce(b.getRight(), factory, reads))
						: value;
			}
			return fold(new BinaryExpression(b.getStaticType(), substituteOnce(b.getLeft(), factory, reads),
					substituteOnce(b.getRight(), factory, reads), b.getOperator(), b.getCodeLocation()));
		}
		if (expression instanceof TernaryExpression) {
			TernaryExpression t = (TernaryExpression) expression;
			return new TernaryExpression(t.getStaticType(), substituteOnce(t.getLeft(), factory, reads),
					substituteOnce(t.getMiddle(), factory, reads), substituteOnce(t.getRight(), factory, reads),
					t.getOperator(), t.getCodeLocation());
		}
		return substitute(expression, factory, null);
	}

	/**
	 * Merges two cases into a single one that over-approximates both. The
	 * resulting path condition contains only the conjuncts common to both
	 * cases, plus an unknown condition (a fresh {@link SummaryValue}) if some
	 * conjuncts are not common, and identifiers whose values differ between the
	 * cases become unconstrained (they are assigned a fresh
	 * {@link SummaryValue}).
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
		// the conditions that are not common are replaced by an unknown one:
		// the merged case over-approximates both, and executions relying on
		// it are not represented precisely
		SymbolicExpression dropped = null;
		for (SymbolicExpression cond : first.pathCondition)
			if (!pc.contains(cond))
				dropped = cond;
		for (SymbolicExpression cond : second.pathCondition)
			if (!pc.contains(cond))
				dropped = cond;
		if (dropped != null)
			pc.add(factory.summary(dropped.getStaticType(), SummaryValue.Kind.MERGE, SummaryValue.MERGED_CONDITIONS,
					location));

		Set<Identifier> keys = new LinkedHashSet<>(first.updates.keySet());
		keys.addAll(second.updates.keySet());
		Map<Identifier, SymbolicExpression> upd = new LinkedHashMap<>();
		Set<Identifier> forg = new LinkedHashSet<>(first.forgotten);
		forg.addAll(second.forgotten);
		// identifiers that are not updated keep their values only if both
		// cases rename them in the same way
		boolean sameScopings = first.scopings.equals(second.scopings);
		for (Identifier id : keys) {
			SymbolicExpression v1 = first.valueOf(id);
			SymbolicExpression v2 = second.valueOf(id);
			if (v1 != null && v1.equals(v2)) {
				if (!sameScopings || !v1.equals(first.preIdentifier(id)))
					upd.put(id, v1);
			} else
				upd.put(id, factory.summary(id.getStaticType(), SummaryValue.Kind.MERGE, "merged value of " + id,
						location));
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
				first.havocAll || second.havocAll || !sameScopings,
				sameScopings ? first.scopings : Collections.emptyList());
	}

	/**
	 * The heap contents preserved by the merge of the given cases: the ones
	 * preserved by all the cases that make the heap unconstrained.
	 */
	private static Set<Identifier> mergePreserved(
			SymbolicCase first,
			SymbolicCase second) {
		if (first.heapHavoc && second.heapHavoc) {
			Set<Identifier> kept = new LinkedHashSet<>(first.preservedHeap);
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
		return isUnconstrained(id) ? null : preIdentifier(id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(forgotten, forgottenIf, havocAll, heapHavoc, preservedHeap, pathCondition, updates,
				scopings);
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
				&& Objects.equals(updates, other.updates)
				&& Objects.equals(scopings, other.scopings);
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
		if (!scopings.isEmpty())
			sb.append(" scopes ").append(scopings);
		return sb.toString();
	}
}
