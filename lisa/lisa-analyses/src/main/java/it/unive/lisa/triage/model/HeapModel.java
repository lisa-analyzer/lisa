package it.unive.lisa.triage.model;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * The representation of the heap in alarm models. The heap is part of the
 * state of the model, through synthetic identifiers that the analysis never
 * sees:
 * <ul>
 * <li>for each field name {@code f}, the identifier {@code $field:f} (of type
 * {@link HeapType#FIELD}) maps the identifier of each reference to the value of
 * field {@code f} of the referenced object (properties of arrays, such as their
 * length, are fields too, see {@link #isArrayProperty(String)});</li>
 * <li>the identifier {@code $cells} (of type {@link HeapType#CELLS}) maps the
 * identifier of each reference to the cells of the referenced array, that map
 * indexes to values;</li>
 * <li>for each of the identifiers above, an identifier (of type
 * {@link HeapType#WRITTEN_FIELD} or {@link HeapType#WRITTEN_CELLS}) records
 * which locations have been written by the model: reading a location that has
 * not been written (e.g., a field of an object existing in the initial states,
 * or a cell of a new array that has not been initialized) yields a value that
 * is not computed precisely;</li>
 * <li>the identifier {@code $alloc} (of type {@link HeapType#COUNTER}) is the
 * identifier of the next reference to be allocated, so that each allocation
 * yields a reference that is different from all the existing ones;</li>
 * <li>for each allocation site, an identifier (of type
 * {@link HeapType#REFERENCE}) holds the reference most recently allocated by
 * the site.</li>
 * </ul>
 * Fields are identified by their name, regardless of the class declaring them:
 * since different objects have different references, fields with the same
 * name of different classes never interfere. A field name is immutable if all
 * the fields with that name in the program are final: its contents can only be
 * written by the code of the classes declaring it (e.g., their constructors),
 * and they are preserved by calls to other code.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapModel {

	/**
	 * The prefix of the names of the identifiers of field contents.
	 */
	public static final String FIELD_PREFIX = "$field:";

	/**
	 * The prefix of the names of the identifiers of allocation sites.
	 */
	public static final String SITE_PREFIX = "$new@";

	/**
	 * The identifier of the cells of all arrays.
	 */
	public static final Variable CELLS = new Variable(HeapType.CELLS, "$cells", SyntheticLocation.INSTANCE);

	/**
	 * The identifier recording which cells of which arrays have been written.
	 */
	public static final Variable WRITTEN_CELLS = new Variable(HeapType.WRITTEN_CELLS, "$written-cells",
			SyntheticLocation.INSTANCE);

	/**
	 * The prefix of the names of the identifiers recording which fields have
	 * been written.
	 */
	public static final String WRITTEN_PREFIX = "$written:";

	/**
	 * The identifier of the counter of allocations.
	 */
	public static final Variable ALLOC = new Variable(HeapType.COUNTER, "$alloc", SyntheticLocation.INSTANCE);

	/**
	 * The name of the field holding the length of arrays.
	 */
	public static final String LENGTH = "length";

	/**
	 * The default names of the properties of arrays (e.g., their length), as
	 * frontends name the variables accessing them: {@code len} (IMP) and
	 * {@code length} (e.g., Java).
	 */
	public static final Set<String> DEFAULT_ARRAY_PROPERTIES = Set.of("len", LENGTH);

	/**
	 * The names of the variables that, when accessed on an array, denote a
	 * property of the array (e.g., its length) rather than an index.
	 */
	private final Set<String> arrayProperties;

	/**
	 * For each immutable field name, the units declaring a field with that
	 * name.
	 */
	private final Map<String, Set<Unit>> immutable;

	/**
	 * Builds a heap model where no field is immutable.
	 */
	public HeapModel() {
		this.immutable = Collections.emptyMap();
		this.arrayProperties = DEFAULT_ARRAY_PROPERTIES;
	}

	/**
	 * Builds the heap model of the given program, where the field names that
	 * are only used by final instance fields are immutable.
	 *
	 * @param program the program
	 */
	public HeapModel(
			Program program) {
		this(program, DEFAULT_ARRAY_PROPERTIES);
	}

	/**
	 * Builds the heap model of the given program, where the field names that
	 * are only used by final instance fields are immutable, and the variables
	 * with the given names denote properties of arrays.
	 *
	 * @param program         the program
	 * @param arrayProperties the names of the variables that, when accessed
	 *                            on an array, denote a property of the array
	 *                            (e.g., its length) rather than an index
	 */
	public HeapModel(
			Program program,
			Set<String> arrayProperties) {
		this.arrayProperties = arrayProperties;
		Map<String, Set<Unit>> declaring = new HashMap<>();
		Set<String> mutable = new HashSet<>();
		for (Unit unit : program.getUnits())
			if (unit instanceof CompilationUnit)
				for (Global g : ((CompilationUnit) unit).getInstanceGlobals(false)) {
					declaring.computeIfAbsent(g.getName(), n -> new HashSet<>()).add(unit);
					if (!g.isFinal())
						mutable.add(g.getName());
				}
		// properties of arrays cannot change (unless a mutable field shares
		// their name)
		for (String property : arrayProperties)
			declaring.computeIfAbsent(property, n -> new HashSet<>());
		declaring.keySet().removeAll(mutable);
		this.immutable = declaring;
	}

	/**
	 * Yields whether variables with the given name, when accessed on an array,
	 * denote a property of the array (e.g., its length) rather than an index.
	 *
	 * @param name the name of the variable
	 *
	 * @return whether the variable denotes a property
	 */
	public boolean isArrayProperty(
			String name) {
		return arrayProperties.contains(name);
	}

	/**
	 * Yields the identifier of the contents of the field with the given name.
	 *
	 * @param name the name of the field
	 *
	 * @return the identifier
	 */
	public static Variable field(
			String name) {
		return new Variable(HeapType.FIELD, FIELD_PREFIX + name, SyntheticLocation.INSTANCE);
	}

	/**
	 * Yields the identifier recording which locations of the given heap
	 * contents (as yielded by {@link #field(String)}, or {@link #CELLS}) have
	 * been written.
	 *
	 * @param contents the heap contents
	 *
	 * @return the identifier
	 */
	public static Variable written(
			Identifier contents) {
		if (contents.equals(CELLS))
			return WRITTEN_CELLS;
		return new Variable(HeapType.WRITTEN_FIELD,
				WRITTEN_PREFIX + contents.getName().substring(FIELD_PREFIX.length()), SyntheticLocation.INSTANCE);
	}

	/**
	 * Yields the identifier of the reference most recently allocated by the
	 * given allocation.
	 *
	 * @param allocation the allocation
	 *
	 * @return the identifier
	 */
	public static Variable site(
			MemoryAllocation allocation) {
		return new Variable(HeapType.REFERENCE,
				SITE_PREFIX + allocation.getCodeLocation() + ":" + allocation.getStaticType(),
				SyntheticLocation.INSTANCE);
	}

	/**
	 * Yields whether the given expression is an identifier representing (part
	 * of) the heap, that is, field contents, the cells of arrays, or the
	 * counter of allocations.
	 *
	 * @param expression the expression
	 *
	 * @return whether the expression represents the heap
	 */
	public static boolean isHeapState(
			SymbolicExpression expression) {
		return expression instanceof Identifier
				&& (expression.getStaticType() == HeapType.FIELD
						|| expression.getStaticType() == HeapType.CELLS
						|| expression.getStaticType() == HeapType.WRITTEN_FIELD
						|| expression.getStaticType() == HeapType.WRITTEN_CELLS
						|| expression.getStaticType() == HeapType.COUNTER);
	}

	/**
	 * Yields whether the given expression mentions the synthetic identifiers
	 * or operators of the heap model, and thus cannot be evaluated by the
	 * analysis.
	 *
	 * @param expression the expression
	 *
	 * @return whether the expression mentions the heap model
	 */
	public static boolean mentionsHeapModel(
			SymbolicExpression expression) {
		if (expression.getStaticType() instanceof HeapType)
			return true;
		if (expression instanceof UnaryExpression) {
			UnaryExpression u = (UnaryExpression) expression;
			return u.getOperator() instanceof HeapOperators.NewReference || mentionsHeapModel(u.getExpression());
		}
		if (expression instanceof BinaryExpression) {
			BinaryExpression b = (BinaryExpression) expression;
			return b.getOperator() instanceof HeapOperators.ReferenceSelect
					|| b.getOperator() instanceof HeapOperators.IndexSelect
					|| mentionsHeapModel(b.getLeft())
					|| mentionsHeapModel(b.getRight());
		}
		if (expression instanceof TernaryExpression) {
			TernaryExpression t = (TernaryExpression) expression;
			return t.getOperator() instanceof HeapOperators.ReferenceStore
					|| t.getOperator() instanceof HeapOperators.IndexStore
					|| t.getOperator() instanceof HeapOperators.ReferenceRead
					|| t.getOperator() instanceof HeapOperators.IndexRead
					|| mentionsHeapModel(t.getLeft())
					|| mentionsHeapModel(t.getMiddle())
					|| mentionsHeapModel(t.getRight());
		}
		return false;
	}

	/**
	 * Yields whether the contents of the given field (as yielded by
	 * {@link #field(String)}) are immutable.
	 *
	 * @param field the identifier of the field contents
	 *
	 * @return whether the field is immutable
	 */
	public boolean isImmutable(
			Identifier field) {
		return field.getStaticType() == HeapType.FIELD
				&& field.getName().startsWith(FIELD_PREFIX)
				&& immutable.containsKey(field.getName().substring(FIELD_PREFIX.length()));
	}

	/**
	 * Yields the identifiers of the immutable field contents that a call to
	 * the given code members cannot write: final fields can only be written by
	 * the code of the classes declaring them, which includes the code of their
	 * subclasses (e.g., constructors invoking the ones of their superclasses).
	 *
	 * @param targets the code members that might be called
	 *
	 * @return the identifiers of the preserved field contents
	 */
	public Set<Identifier> preservedBy(
			Collection<? extends CodeMember> targets) {
		Set<Unit> writers = new HashSet<>();
		for (CodeMember target : targets) {
			Unit unit = target.getDescriptor().getUnit();
			writers.add(unit);
			if (unit instanceof CompilationUnit)
				writers.addAll(ancestors((CompilationUnit) unit));
		}
		Set<Identifier> preserved = new LinkedHashSet<>();
		for (Map.Entry<String, Set<Unit>> e : immutable.entrySet())
			if (Collections.disjoint(e.getValue(), writers)) {
				Variable contents = field(e.getKey());
				preserved.add(contents);
				preserved.add(written(contents));
			}
		return preserved;
	}

	private static Set<CompilationUnit> ancestors(
			CompilationUnit unit) {
		Set<CompilationUnit> result = new HashSet<>();
		LinkedList<CompilationUnit> ws = new LinkedList<>(unit.getImmediateAncestors());
		while (!ws.isEmpty()) {
			CompilationUnit current = ws.removeFirst();
			if (result.add(current))
				ws.addAll(current.getImmediateAncestors());
		}
		return result;
	}
}
