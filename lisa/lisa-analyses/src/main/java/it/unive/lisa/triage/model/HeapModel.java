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
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * The representation of the heap in alarm models. The heap is part of the state
 * of the model, through synthetic identifiers that the analysis never sees:
 * <ul>
 * <li>for each field name {@code f}, the identifier {@code $field:f} (of type
 * {@link HeapType#FIELD}) maps the identifier of each reference to the value of
 * field {@code f} of the referenced object (properties of arrays, such as their
 * length, are fields too, see {@link #isArrayProperty(String)});</li>
 * <li>the identifier {@code $cells} (of type {@link HeapType#CELLS}) maps the
 * key of each cell of each array (the identifier of the reference to the array
 * and the index) to its value;</li>
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
 * the site;</li>
 * <li>for each global variable (e.g., a static field), an identifier (of type
 * {@link HeapType#DEFINED}) records whether its value has been written by the
 * model: global variables are initialized by code that might not be part of the
 * model (e.g., static initializers), so that reading one that has not been
 * written yields a value that is not computed precisely (see
 * {@link #defined(Identifier)}).</li>
 * </ul>
 * Fields are identified by the class declaring them and their name
 * ({@code $field:C::f}, see {@link #qualify(Type, String)}) when the static
 * type of the accessed object tells the class, and by their name only
 * ({@code $field:f}) otherwise (e.g., in dynamically typed languages): since
 * different objects have different references, fields with the same name of
 * different classes never interfere in the latter case. Since the same field
 * might be accessed in both ways, writing a field in one way makes the contents
 * of the field with the same name in the other way unknown (see
 * {@link #aliasesOf(String, Type)}). A field identified by its class is
 * immutable if it is final, while a field identified by its name only is
 * immutable if all the fields with that name in the program are final: their
 * contents can only be written by the code of the classes declaring them (e.g.,
 * their constructors), and they are preserved by calls to other code. The same
 * holds for final global variables, that can only be written by the code of the
 * units declaring them.
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
	 * The prefix of the names of the identifiers recording whether global
	 * variables have been written.
	 */
	public static final String DEFINED_PREFIX = "$defined:";

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
	 * For each field name, the names (qualified by the class declaring them,
	 * see {@link #qualify(Type, String)}) of the fields with that name.
	 */
	private final Map<String, Set<String>> qualified;

	/**
	 * For each qualified name of an immutable field, the unit declaring it.
	 */
	private final Map<String, Unit> finals;

	/**
	 * For each qualified name of a field, the unit declaring it.
	 */
	private final Map<String, CompilationUnit> declarers;

	/**
	 * For each name of a final global variable (see
	 * {@link #globalName(Unit, Global)}), the unit declaring it.
	 */
	private final Map<String, Unit> finalGlobals;

	/**
	 * The names of all global variables (see
	 * {@link #globalName(Unit, Global)}).
	 */
	private final Set<String> globals;

	/**
	 * The separator between the name of a class and the name of a field in
	 * qualified field names.
	 */
	public static final String QUALIFIER = "::";

	/**
	 * Builds a heap model where no field is immutable.
	 */
	public HeapModel() {
		this.immutable = Collections.emptyMap();
		this.qualified = Collections.emptyMap();
		this.finals = Collections.emptyMap();
		this.finalGlobals = Collections.emptyMap();
		this.globals = Collections.emptySet();
		this.declarers = Collections.emptyMap();
		this.arrayProperties = DEFAULT_ARRAY_PROPERTIES;
	}

	/**
	 * Builds the heap model of the given program, where final instance fields
	 * (and the field names that are only used by final instance fields) are
	 * immutable.
	 *
	 * @param program the program
	 */
	public HeapModel(
			Program program) {
		this(program, DEFAULT_ARRAY_PROPERTIES);
	}

	/**
	 * Builds the heap model of the given program, where final instance fields
	 * (and the field names that are only used by final instance fields) are
	 * immutable, and the variables with the given names denote properties of
	 * arrays.
	 *
	 * @param program         the program
	 * @param arrayProperties the names of the variables that, when accessed on
	 *                            an array, denote a property of the array
	 *                            (e.g., its length) rather than an index
	 */
	public HeapModel(
			Program program,
			Set<String> arrayProperties) {
		this.arrayProperties = arrayProperties;
		Map<String, Set<Unit>> declaring = new LinkedHashMap<>();
		Set<String> mutable = new LinkedHashSet<>();
		this.qualified = new LinkedHashMap<>();
		this.finals = new LinkedHashMap<>();
		this.declarers = new LinkedHashMap<>();
		this.finalGlobals = new LinkedHashMap<>();
		this.globals = new LinkedHashSet<>();
		for (Unit unit : program.getUnits())
			for (Global g : unit.getGlobals()) {
				String name = globalName(unit, g);
				globals.add(name);
				if (g.isFinal())
					finalGlobals.put(name, unit);
			}
		for (Unit unit : program.getUnits())
			if (unit instanceof CompilationUnit)
				for (Global g : ((CompilationUnit) unit).getInstanceGlobals(false)) {
					declaring.computeIfAbsent(g.getName(), n -> new LinkedHashSet<>()).add(unit);
					String name = unit.getName() + QUALIFIER + g.getName();
					qualified.computeIfAbsent(g.getName(), n -> new LinkedHashSet<>()).add(name);
					declarers.put(name, (CompilationUnit) unit);
					if (g.isFinal())
						finals.put(name, unit);
					else
						mutable.add(g.getName());
				}
		// properties of arrays cannot change (unless a mutable field shares
		// their name)
		for (String property : arrayProperties)
			declaring.computeIfAbsent(property, n -> new LinkedHashSet<>());
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
	 * Yields the name of the variable holding the value of the given global of
	 * the given unit, as accessed by the program (e.g., {@code C::f}).
	 *
	 * @param unit   the unit
	 * @param global the global
	 *
	 * @return the name
	 */
	public static String globalName(
			Unit unit,
			Global global) {
		return unit.getName() + QUALIFIER + global.getName();
	}

	/**
	 * Yields whether the model records whether the given identifier has been
	 * written (see {@link #defined(Identifier)}): this happens for global
	 * variables.
	 *
	 * @param id the identifier
	 *
	 * @return whether the definition of the identifier is tracked
	 */
	public static boolean tracksDefinition(
			Identifier id) {
		return id instanceof GlobalVariable;
	}

	/**
	 * Yields the identifier recording whether the given global variable has
	 * been written by the model.
	 *
	 * @param variable the global variable
	 *
	 * @return the identifier
	 */
	public static Variable defined(
			Identifier variable) {
		return new Variable(HeapType.DEFINED, DEFINED_PREFIX + variable.getName(), SyntheticLocation.INSTANCE);
	}

	/**
	 * Yields the value of the given global variable, as read by the program:
	 * the read is precise only if the variable has been written by the model
	 * (see {@link HeapOperators.DefinedRead}).
	 *
	 * @param value   the value of the variable
	 * @param defined whether the variable has been written
	 *
	 * @return the read
	 */
	public static BinaryExpression definedRead(
			SymbolicExpression value,
			SymbolicExpression defined) {
		return new BinaryExpression(value.getStaticType(), value, defined, HeapOperators.DefinedRead.INSTANCE,
				value.getCodeLocation());
	}

	/**
	 * Yields the given expression where reads of global variables (see
	 * {@link #definedRead(SymbolicExpression, SymbolicExpression)}) are
	 * replaced by the variables, that the analysis can evaluate.
	 *
	 * @param expression the expression
	 *
	 * @return the resulting expression
	 */
	public static SymbolicExpression withoutDefinitions(
			SymbolicExpression expression) {
		if (expression instanceof UnaryExpression) {
			UnaryExpression u = (UnaryExpression) expression;
			SymbolicExpression e = withoutDefinitions(u.getExpression());
			return e == u.getExpression() ? u
					: new UnaryExpression(u.getStaticType(), e, u.getOperator(), u.getCodeLocation());
		}
		if (expression instanceof BinaryExpression) {
			BinaryExpression b = (BinaryExpression) expression;
			if (b.getOperator() instanceof HeapOperators.DefinedRead)
				return withoutDefinitions(b.getLeft());
			SymbolicExpression l = withoutDefinitions(b.getLeft());
			SymbolicExpression r = withoutDefinitions(b.getRight());
			return l == b.getLeft() && r == b.getRight() ? b
					: new BinaryExpression(b.getStaticType(), l, r, b.getOperator(), b.getCodeLocation());
		}
		if (expression instanceof TernaryExpression) {
			TernaryExpression t = (TernaryExpression) expression;
			SymbolicExpression l = withoutDefinitions(t.getLeft());
			SymbolicExpression m = withoutDefinitions(t.getMiddle());
			SymbolicExpression r = withoutDefinitions(t.getRight());
			return l == t.getLeft() && m == t.getMiddle() && r == t.getRight() ? t
					: new TernaryExpression(t.getStaticType(), l, m, r, t.getOperator(), t.getCodeLocation());
		}
		return expression;
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
						|| expression.getStaticType() == HeapType.COUNTER
						|| expression.getStaticType() == HeapType.DEFINED);
	}

	/**
	 * Yields whether the given expression mentions the synthetic identifiers or
	 * operators of the heap model, and thus cannot be evaluated by the
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
			return b.getOperator() instanceof HeapOperators.CellKey
					|| b.getOperator() instanceof HeapOperators.WrittenAt
					|| b.getOperator() instanceof HeapOperators.DefinedRead
					|| mentionsHeapModel(b.getLeft())
					|| mentionsHeapModel(b.getRight());
		}
		if (expression instanceof TernaryExpression) {
			TernaryExpression t = (TernaryExpression) expression;
			return t.getOperator() instanceof HeapOperators.ReferenceStore
					|| t.getOperator() instanceof HeapOperators.ReferenceRead
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
		if (field.getStaticType() != HeapType.FIELD || !field.getName().startsWith(FIELD_PREFIX))
			return false;
		String name = field.getName().substring(FIELD_PREFIX.length());
		return finals.containsKey(name) || immutable.containsKey(name);
	}

	/**
	 * Yields the name of the field with the given name accessed on objects of
	 * the given static type, qualified by the class declaring it (as
	 * {@code C::f}): the first class declaring an instance field with that name
	 * among the class of the static type and its ancestors, as fields are
	 * resolved statically. Yields {@code null} if the static type does not tell
	 * the class (e.g., it is unknown), or if no such class exists.
	 *
	 * @param type the static type of the accessed object (or of a reference or
	 *                 pointer to it)
	 * @param name the name of the field
	 *
	 * @return the qualified name, or {@code null}
	 */
	public String qualify(
			Type type,
			String name) {
		if (!qualified.containsKey(name))
			return null;
		CompilationUnit unit = unitOf(type);
		if (unit == null)
			return null;
		for (CompilationUnit current : hierarchy(unit))
			if (current.getInstanceGlobal(name, false) != null)
				return current.getName() + QUALIFIER + name;
		return null;
	}

	/**
	 * Yields the unit of the class of the given static type (of an object, or
	 * of a reference or pointer to it), or {@code null} if the type does not
	 * tell it.
	 */
	private static CompilationUnit unitOf(
			Type type) {
		Type inner = type;
		if (inner.isPointerType())
			inner = inner.asPointerType().getInnerType();
		else if (inner.isReferenceType())
			inner = inner.asReferenceType().getInnerType();
		return inner.isUnitType() ? inner.asUnitType().getUnit() : null;
	}

	/**
	 * Yields the given unit and its ancestors, from the closest ones.
	 */
	private static Set<CompilationUnit> hierarchy(
			CompilationUnit unit) {
		Set<CompilationUnit> seen = new LinkedHashSet<>();
		LinkedList<CompilationUnit> ws = new LinkedList<>();
		ws.add(unit);
		while (!ws.isEmpty()) {
			CompilationUnit current = ws.removeFirst();
			if (seen.add(current))
				ws.addAll(current.getImmediateAncestors());
		}
		return seen;
	}

	/**
	 * Yields the identifiers of the contents of the fields that might denote
	 * the same fields as the given one, accessed in the other way (see
	 * {@link #qualify(Type, String)}), and the ones recording which locations
	 * have been written: for a qualified name {@code C::f}, the field named
	 * {@code f}, and for a name {@code f}, the fields named {@code f} qualified
	 * by their classes, if these might be the class of the accessed object
	 * (i.e., they are related to the class of its static type, or the static
	 * type is unknown).
	 *
	 * @param name the name of the field, possibly qualified
	 * @param type the static type of the accessed object (or of a reference or
	 *                 pointer to it)
	 *
	 * @return the identifiers
	 */
	public Set<Identifier> aliasesOf(
			String name,
			Type type) {
		Set<Identifier> result = new LinkedHashSet<>();
		int sep = name.lastIndexOf(QUALIFIER);
		Collection<String> others;
		if (sep >= 0)
			others = Collections.singleton(name.substring(sep + QUALIFIER.length()));
		else {
			others = new LinkedHashSet<>(qualified.getOrDefault(name, Collections.emptySet()));
			CompilationUnit unit = unitOf(type);
			if (unit != null) {
				Set<CompilationUnit> ancestors = hierarchy(unit);
				// fields of classes that are neither ancestors nor
				// descendants of the class of the object are not its fields
				others.removeIf(other -> {
					CompilationUnit declarer = declarers.get(other);
					return declarer != null && !ancestors.contains(declarer)
							&& !hierarchy(declarer).contains(unit);
				});
			}
		}
		for (String other : others) {
			Variable contents = field(other);
			result.add(contents);
			result.add(written(contents));
		}
		return result;
	}

	/**
	 * Yields the identifiers of the immutable field contents that a call to the
	 * given code members cannot write: final fields can only be written by the
	 * code of the classes declaring them, which includes the code of their
	 * subclasses (e.g., constructors invoking the ones of their superclasses).
	 *
	 * @param targets the code members that might be called
	 *
	 * @return the identifiers of the preserved field contents
	 */
	public Set<Identifier> preservedBy(
			Collection<? extends CodeMember> targets) {
		Set<Unit> writers = new LinkedHashSet<>();
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
		for (Map.Entry<String, Unit> e : finals.entrySet())
			if (!writers.contains(e.getValue())) {
				Variable contents = field(e.getKey());
				preserved.add(contents);
				preserved.add(written(contents));
			}
		for (Map.Entry<String, Unit> e : finalGlobals.entrySet())
			if (!writers.contains(e.getValue())) {
				GlobalVariable variable = global(e.getKey());
				preserved.add(variable);
				preserved.add(defined(variable));
			}
		return preserved;
	}

	/**
	 * Yields the identifiers of the contents of all the immutable fields: the
	 * values of final fields of existing objects cannot be changed by any code,
	 * as they are only written when objects are initialized.
	 *
	 * @return the identifiers of the immutable field contents
	 */
	public Set<Identifier> immutableContents() {
		Set<Identifier> preserved = new LinkedHashSet<>();
		Set<String> names = new LinkedHashSet<>(immutable.keySet());
		names.addAll(finals.keySet());
		for (String name : names) {
			Variable contents = field(name);
			preserved.add(contents);
			preserved.add(written(contents));
		}
		return preserved;
	}

	/**
	 * Yields the identifiers of all global variables, and of the identifiers
	 * recording whether they have been written: writes of the heap never affect
	 * them.
	 *
	 * @return the identifiers
	 */
	public Set<Identifier> globalVariables() {
		Set<Identifier> result = new LinkedHashSet<>();
		for (String name : globals) {
			GlobalVariable variable = global(name);
			result.add(variable);
			result.add(defined(variable));
		}
		return result;
	}

	/**
	 * Yields the identifiers of the global variables whose names are not among
	 * the given ones, and of the identifiers recording whether they have been
	 * written.
	 *
	 * @param names the names of the global variables
	 *
	 * @return the identifiers
	 */
	public Set<Identifier> globalsNotIn(
			Set<String> names) {
		Set<Identifier> result = new LinkedHashSet<>();
		for (String name : globals)
			if (!names.contains(name)) {
				GlobalVariable variable = global(name);
				result.add(variable);
				result.add(defined(variable));
			}
		return result;
	}

	private static GlobalVariable global(
			String name) {
		return new GlobalVariable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	private static Set<CompilationUnit> ancestors(
			CompilationUnit unit) {
		Set<CompilationUnit> result = new LinkedHashSet<>();
		LinkedList<CompilationUnit> ws = new LinkedList<>(unit.getImmediateAncestors());
		while (!ws.isEmpty()) {
			CompilationUnit current = ws.removeFirst();
			if (result.add(current))
				ws.addAll(current.getImmediateAncestors());
		}
		return result;
	}
}
