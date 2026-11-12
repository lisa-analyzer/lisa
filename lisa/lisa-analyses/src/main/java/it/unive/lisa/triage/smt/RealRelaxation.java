package it.unive.lisa.triage.smt;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A relaxation of the assertions sent to a solver, where floating point
 * numbers are replaced by real numbers, used to look for values of the
 * floating point constants that satisfy queries the solver cannot decide over
 * floating point numbers: the values of the constants in a model of the relaxed
 * assertions, rounded to floating point numbers, are candidates to be checked
 * against the original assertions (where fixing them makes the query much
 * easier). The relaxation is neither an over- nor an under-approximation
 * (rounding is ignored, NaNs and infinities are not numbers, and operations
 * that cannot be expressed over real numbers are uninterpreted functions), so
 * it is only a source of candidates.<br>
 * <br>
 * The declarations, definitions and assertions sent to the solver are recorded
 * (with the scopes of the assertions), and {@link #relax()} yields the
 * commands asserting the relaxation of the current assertions, over copies of
 * the symbols whose sorts involve floating point numbers (whose names end with
 * {@link #SUFFIX}), declared as needed: the copies are declared once, as
 * declarations are global (they survive the scopes where they are made).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
final class RealRelaxation {

	/**
	 * The suffix of the names of the copies of the symbols in the relaxed
	 * assertions.
	 */
	static final String SUFFIX = "!real";

	private static final SExpression BOOL = SExpression.parse("Bool");

	private static final SExpression INT = SExpression.parse("Int");

	private static final SExpression REAL = SExpression.parse("Real");

	private static final SExpression STRING = SExpression.parse("String");

	private static final SExpression ROUNDING_MODE = SExpression.parse("RoundingMode");

	private static final Set<String> ROUNDING_MODES = Set.of("RNE", "RNA", "RTP", "RTN", "RTZ",
			"roundNearestTiesToEven", "roundNearestTiesToAway", "roundTowardPositive", "roundTowardNegative",
			"roundTowardZero");

	private static final Set<String> BOOLEAN_OPERATORS = Set.of("and", "or", "not", "=>", "xor", "=", "distinct",
			"<", "<=", ">", ">=", "forall", "exists", "is_int", "fp.lt", "fp.leq", "fp.gt", "fp.geq", "fp.eq",
			"fp.isNaN", "fp.isInfinite", "fp.isZero", "fp.isNormal", "fp.isSubnormal", "fp.isNegative",
			"fp.isPositive", "str.prefixof", "str.suffixof", "str.contains", "str.in_re", "str.<", "str.<=",
			"str.is_digit", "bvult", "bvule", "bvugt", "bvuge", "bvslt", "bvsle", "bvsgt", "bvsge");

	private static final Set<String> INTEGER_OPERATORS = Set.of("str.len", "str.indexof", "str.to_int",
			"str.to.int", "str.to_code", "bv2int", "bv2nat", "to_int", "div", "mod");

	private static final Set<String> STRING_OPERATORS = Set.of("str.++", "str.at", "str.substr", "str.replace",
			"str.replace_all", "str.from_int", "int.to.str", "str.from_code");

	/**
	 * A floating point constant of the original assertions, and its copy in
	 * the relaxed ones.
	 */
	static final class Pin {

		private final String original;

		private final String relaxed;

		private final Sort sort;

		private Pin(
				String original,
				String relaxed,
				Sort sort) {
			this.original = original;
			this.relaxed = relaxed;
			this.sort = sort;
		}

		/**
		 * Yields the constant of the original assertions.
		 *
		 * @return the constant, as an SMT-LIB symbol
		 */
		String getOriginal() {
			return original;
		}

		/**
		 * Yields the copy of the constant in the relaxed assertions, whose sort
		 * is {@code Real}.
		 *
		 * @return the copy, as an SMT-LIB symbol
		 */
		String getRelaxed() {
			return relaxed;
		}

		/**
		 * Yields the assertion fixing the constant of the original assertions
		 * to the floating point number closest to the given value of its copy
		 * in a model of the relaxed ones.
		 *
		 * @param value the value of the copy, as printed by the solver
		 *
		 * @return the assertion, or {@code null} if the value is not a rational
		 *             number (e.g., an algebraic one)
		 */
		String fix(
				String value) {
			BigDecimal number;
			try {
				number = rational(SExpression.parse(value));
			} catch (SmtException | ArithmeticException e) {
				return null;
			}
			if (number == null)
				return null;
			Term literal = sort.equals(Sort.FLOAT32) ? FloatingPoints.literal(number.floatValue())
					: FloatingPoints.literal(number.doubleValue());
			return "(assert (= " + original + " " + literal.toSmtLib() + "))";
		}
	}

	/**
	 * The relaxation of the current assertions.
	 */
	static final class Relaxed {

		private final List<String> commands;

		private final List<Pin> pins;

		private Relaxed(
				List<String> commands,
				List<Pin> pins) {
			this.commands = commands;
			this.pins = pins;
		}

		/**
		 * Yields the commands declaring the copies of the symbols that have not
		 * been declared yet, and asserting the relaxed assertions.
		 *
		 * @return the commands
		 */
		List<String> getCommands() {
			return commands;
		}

		/**
		 * Yields the floating point constants occurring in the assertions.
		 *
		 * @return the constants
		 */
		List<Pin> getPins() {
			return pins;
		}
	}

	/**
	 * Signals that an assertion cannot be relaxed.
	 */
	private static final class GiveUp
			extends
			RuntimeException {

		private static final long serialVersionUID = 1L;

		private GiveUp(
				String message) {
			super(message, null, false, false);
		}
	}

	// the vocabulary of the solver

	private final Map<String, SExpression> constants = new HashMap<>();

	private final Map<String, String> definitions = new HashMap<>();

	private final Map<String, SExpression> definitionSorts = new HashMap<>();

	private final Map<String, List<SExpression>> functionArguments = new HashMap<>();

	private final Map<String, SExpression> functionResults = new HashMap<>();

	private final Map<String, Datatype> datatypes = new HashMap<>();

	private final Map<String, Datatype> constructors = new HashMap<>();

	private final Map<String, SExpression> selectors = new HashMap<>();

	private final Map<String, Datatype> selectorDatatypes = new HashMap<>();

	private final Set<String> relaxableDatatypes = new HashSet<>();

	private final List<List<String>> scopes = new ArrayList<>();

	// the copies already sent to the solver

	private final Set<String> declared = new HashSet<>();

	/**
	 * The name used in relaxed assertions for each definition (itself, if its
	 * relaxation is the same).
	 */
	private final Map<String, String> relaxedDefinitions = new HashMap<>();

	/**
	 * The floating point constants occurring in each definition.
	 */
	private final Map<String, Map<String, Pin>> definitionPins = new HashMap<>();

	// the state of the current relaxation

	private List<String> commands;

	private Map<String, Pin> pins;

	private Set<String> staged;

	private Map<String, String> stagedDefinitions;

	private Map<String, Map<String, Pin>> stagedPins;

	private Map<SExpression, SExpression> sorts;

	/**
	 * Builds an empty relaxation.
	 */
	RealRelaxation() {
		scopes.add(new ArrayList<>());
	}

	/**
	 * Records the declaration of a constant.
	 *
	 * @param symbol the constant, as an SMT-LIB symbol
	 * @param sort   its sort
	 */
	void declareConstant(
			String symbol,
			Sort sort) {
		constants.put(symbol, SExpression.parse(sort.toSmtLib()));
	}

	/**
	 * Records the declaration of a function.
	 *
	 * @param symbol    the function, as an SMT-LIB symbol
	 * @param arguments the sorts of its arguments
	 * @param result    the sort of its result
	 */
	void declareFunction(
			String symbol,
			List<Sort> arguments,
			Sort result) {
		List<SExpression> args = new ArrayList<>();
		for (Sort s : arguments)
			args.add(SExpression.parse(s.toSmtLib()));
		functionArguments.put(symbol, args);
		functionResults.put(symbol, SExpression.parse(result.toSmtLib()));
	}

	/**
	 * Records the definition of a constant.
	 *
	 * @param symbol the constant, as an SMT-LIB symbol
	 * @param sort   its sort
	 * @param body   its definition, in SMT-LIB syntax
	 */
	void define(
			String symbol,
			Sort sort,
			String body) {
		definitions.put(symbol, body);
		definitionSorts.put(symbol, SExpression.parse(sort.toSmtLib()));
	}

	/**
	 * Records the declaration of a datatype.
	 *
	 * @param datatype the datatype
	 */
	void declareDatatype(
			Datatype datatype) {
		String name = datatype.getSort().toSmtLib();
		datatypes.put(name, datatype);
		boolean relaxable = false;
		for (Map.Entry<String, List<Datatype.Field>> ctor : datatype.getConstructors().entrySet()) {
			constructors.put(Term.symbol(ctor.getKey()), datatype);
			for (Datatype.Field f : ctor.getValue()) {
				SExpression sort = SExpression.parse(f.getSort().toSmtLib());
				selectors.put(Term.symbol(f.getName()), sort);
				selectorDatatypes.put(Term.symbol(f.getName()), datatype);
				relaxable |= relaxable(sort);
			}
		}
		if (relaxable)
			relaxableDatatypes.add(name);
	}

	/**
	 * Records an assertion in the current scope.
	 *
	 * @param term the asserted term, in SMT-LIB syntax
	 */
	void assertion(
			String term) {
		scopes.get(scopes.size() - 1).add(term);
	}

	/**
	 * Records the opening of a scope.
	 */
	void push() {
		scopes.add(new ArrayList<>());
	}

	/**
	 * Records the closing of a scope.
	 */
	void pop() {
		if (scopes.size() > 1)
			scopes.remove(scopes.size() - 1);
	}

	/**
	 * Forgets everything, as the solver has been reset.
	 */
	void reset() {
		constants.clear();
		definitions.clear();
		definitionSorts.clear();
		functionArguments.clear();
		functionResults.clear();
		datatypes.clear();
		constructors.clear();
		selectors.clear();
		selectorDatatypes.clear();
		relaxableDatatypes.clear();
		scopes.clear();
		scopes.add(new ArrayList<>());
		declared.clear();
		relaxedDefinitions.clear();
		definitionPins.clear();
	}

	/**
	 * Yields the relaxation of the current assertions. The copies of the
	 * symbols declared by the commands are considered declared from now on, so
	 * the commands must be sent to the solver.
	 *
	 * @return the relaxation, or {@code null} if the assertions cannot be
	 *             relaxed, or if no floating point constant occurs in them
	 */
	Relaxed relax() {
		commands = new ArrayList<>();
		pins = new LinkedHashMap<>();
		staged = new HashSet<>();
		stagedDefinitions = new HashMap<>();
		stagedPins = new HashMap<>();
		sorts = new IdentityHashMap<>();
		try {
			List<String> asserted = new ArrayList<>();
			for (List<String> scope : scopes)
				for (String assertion : scope)
					asserted.add("(assert " + relax(SExpression.parse(assertion), Collections.emptyMap()) + ")");
			if (pins.isEmpty())
				return null;
			commands.addAll(asserted);
		} catch (RuntimeException | StackOverflowError e) {
			// e.g., unexpected syntax
			return null;
		} finally {
			sorts = null;
		}
		declared.addAll(staged);
		relaxedDefinitions.putAll(stagedDefinitions);
		definitionPins.putAll(stagedPins);
		return new Relaxed(commands, new ArrayList<>(pins.values()));
	}

	// sorts

	private static boolean isFloat(
			SExpression sort) {
		if (sort.isAtom())
			return sort.getAtom().matches("Float(16|32|64|128)");
		List<SExpression> ch = sort.getChildren();
		return ch.size() == 4 && "_".equals(atom(ch, 0)) && "FloatingPoint".equals(atom(ch, 1));
	}

	private static boolean sameSort(
			SExpression sort,
			SExpression kind) {
		return sort.toString().equals(kind.toString());
	}

	private static boolean isBitVector(
			SExpression sort) {
		List<SExpression> ch = sort.getChildren();
		return !sort.isAtom() && ch.size() == 3 && "_".equals(atom(ch, 0)) && "BitVec".equals(atom(ch, 1));
	}

	private static int width(
			SExpression sort) {
		if (!isBitVector(sort))
			throw new GiveUp("not a bit-vector sort: " + sort);
		return Integer.parseInt(sort.getChildren().get(2).getAtom());
	}

	private static int floatWidth(
			SExpression sort) {
		if (sort.isAtom())
			return Integer.parseInt(sort.getAtom().substring("Float".length()));
		List<SExpression> ch = sort.getChildren();
		return Integer.parseInt(ch.get(2).getAtom()) + Integer.parseInt(ch.get(3).getAtom());
	}

	private static SExpression bitVector(
			int width) {
		return SExpression.parse("(_ BitVec " + width + ")");
	}

	private static String atom(
			List<SExpression> children,
			int index) {
		if (index >= children.size() || !children.get(index).isAtom())
			return null;
		return children.get(index).getAtom();
	}

	private boolean relaxable(
			SExpression sort) {
		if (isFloat(sort))
			return true;
		if (sort.isAtom())
			return relaxableDatatypes.contains(sort.getAtom());
		for (SExpression child : sort.getChildren())
			if (relaxable(child))
				return true;
		return false;
	}

	private String relaxSort(
			SExpression sort) {
		if (isFloat(sort))
			return "Real";
		if (sort.isAtom()) {
			if (!relaxableDatatypes.contains(sort.getAtom()))
				return sort.getAtom();
			ensureDatatype(sort.getAtom());
			return rename(sort.getAtom());
		}
		List<SExpression> ch = sort.getChildren();
		if ("Array".equals(atom(ch, 0)))
			return "(Array " + relaxSort(ch.get(1)) + " " + relaxSort(ch.get(2)) + ")";
		return sort.toString();
	}

	private SExpression sortOf(
			SExpression e,
			Map<String, SExpression> bound) {
		SExpression known = sorts.get(e);
		if (known != null)
			return known;
		SExpression sort = computeSort(e, bound);
		if (sort == null)
			throw new GiveUp("unknown sort: " + e);
		sorts.put(e, sort);
		return sort;
	}

	private SExpression computeSort(
			SExpression e,
			Map<String, SExpression> bound) {
		if (e.isAtom()) {
			String a = e.getAtom();
			if (bound.containsKey(a))
				return bound.get(a);
			if (a.equals("true") || a.equals("false"))
				return BOOL;
			char c = a.charAt(0);
			if (Character.isDigit(c))
				return a.contains(".") ? REAL : INT;
			if (a.startsWith("#b"))
				return bitVector(a.length() - 2);
			if (a.startsWith("#x"))
				return bitVector(4 * (a.length() - 2));
			if (c == '"')
				return STRING;
			if (ROUNDING_MODES.contains(a))
				return ROUNDING_MODE;
			if (constants.containsKey(a))
				return constants.get(a);
			if (definitionSorts.containsKey(a))
				return definitionSorts.get(a);
			Datatype d = constructors.get(a);
			return d == null ? null : SExpression.parse(d.getSort().toSmtLib());
		}
		List<SExpression> ch = e.getChildren();
		if (ch.isEmpty())
			return null;
		SExpression head = ch.get(0);
		if (!head.isAtom()) {
			List<SExpression> idx = head.getChildren();
			if ("as".equals(atom(idx, 0)) && idx.size() == 3)
				return idx.get(2);
			if (!"_".equals(atom(idx, 0)))
				return null;
			String op = atom(idx, 1);
			if (op == null)
				return null;
			switch (op) {
			case "is":
				return BOOL;
			case "to_fp":
			case "to_fp_unsigned":
				return SExpression.parse("(_ FloatingPoint " + idx.get(2) + " " + idx.get(3) + ")");
			case "fp.to_sbv":
			case "fp.to_ubv":
			case "int2bv":
				return bitVector(Integer.parseInt(idx.get(2).getAtom()));
			case "extract":
				return bitVector(
						Integer.parseInt(idx.get(2).getAtom()) - Integer.parseInt(idx.get(3).getAtom()) + 1);
			case "zero_extend":
			case "sign_extend":
				return bitVector(width(sortOf(ch.get(1), bound)) + Integer.parseInt(idx.get(2).getAtom()));
			case "repeat":
				return bitVector(width(sortOf(ch.get(1), bound)) * Integer.parseInt(idx.get(2).getAtom()));
			default:
				return sortOf(ch.get(1), bound);
			}
		}
		String h = head.getAtom();
		if (BOOLEAN_OPERATORS.contains(h))
			return BOOL;
		if (INTEGER_OPERATORS.contains(h))
			return INT;
		if (STRING_OPERATORS.contains(h))
			return STRING;
		switch (h) {
		case "to_real":
		case "/":
		case "fp.to_real":
			return REAL;
		case "fp": {
			Sort sort = FloatingPoints.sortOf(e);
			return sort == null ? null : SExpression.parse(sort.toSmtLib());
		}
		case "_": {
			Sort sort = FloatingPoints.sortOf(e);
			if (sort != null)
				return SExpression.parse(sort.toSmtLib());
			String literal = atom(ch, 1);
			if (ch.size() == 3 && literal != null && literal.startsWith("bv"))
				return bitVector(Integer.parseInt(ch.get(2).getAtom()));
			return null;
		}
		case "ite":
			return sortOf(ch.get(2), bound);
		case "select": {
			SExpression array = sortOf(ch.get(1), bound);
			return array.isAtom() ? null : array.getChildren().get(2);
		}
		case "store":
			return sortOf(ch.get(1), bound);
		case "!":
			return sortOf(ch.get(1), bound);
		case "let":
			return sortOf(ch.get(2), letBindings(ch.get(1), bound));
		case "fp.to_ieee_bv":
			return bitVector(floatWidth(sortOf(ch.get(1), bound)));
		case "concat":
			return bitVector(width(sortOf(ch.get(1), bound)) + width(sortOf(ch.get(2), bound)));
		default:
			break;
		}
		if (functionResults.containsKey(h))
			return functionResults.get(h);
		if (constructors.containsKey(h))
			return SExpression.parse(constructors.get(h).getSort().toSmtLib());
		if (selectors.containsKey(h))
			return selectors.get(h);
		// arithmetic and bit-vector operators: the sort of the operands
		for (int i = 1; i < ch.size(); i++) {
			SExpression sort = sortOf(ch.get(i), bound);
			if (!sameSort(sort, ROUNDING_MODE))
				return sort;
		}
		return null;
	}

	private Map<String, SExpression> letBindings(
			SExpression bindings,
			Map<String, SExpression> bound) {
		Map<String, SExpression> inner = new HashMap<>(bound);
		for (SExpression b : bindings.getChildren())
			inner.put(b.getChildren().get(0).getAtom(), sortOf(b.getChildren().get(1), bound));
		return inner;
	}

	// symbols

	private static String rename(
			String symbol) {
		String name = symbol.startsWith("|") && symbol.endsWith("|") && symbol.length() > 1
				? symbol.substring(1, symbol.length() - 1)
				: symbol;
		return Term.symbol(name + SUFFIX);
	}

	private boolean isDeclared(
			String symbol) {
		return declared.contains(symbol) || staged.contains(symbol);
	}

	private void ensureDatatype(
			String name) {
		String relaxed = rename(name);
		if (isDeclared(relaxed))
			return;
		staged.add(relaxed);
		Datatype d = datatypes.get(name);
		StringBuilder sb = new StringBuilder("(declare-datatypes ((").append(relaxed).append(" 0)) ((");
		boolean first = true;
		for (Map.Entry<String, List<Datatype.Field>> ctor : d.getConstructors().entrySet()) {
			sb.append(first ? "" : " ").append("(").append(rename(Term.symbol(ctor.getKey())));
			first = false;
			for (Datatype.Field f : ctor.getValue())
				sb.append(" (").append(rename(Term.symbol(f.getName()))).append(" ")
						.append(relaxSort(SExpression.parse(f.getSort().toSmtLib()))).append(")");
			sb.append(")");
		}
		commands.add(sb.append(")))").toString());
	}

	private String relaxedConstant(
			String symbol,
			SExpression sort) {
		if (!relaxable(sort))
			return symbol;
		String relaxed = rename(symbol);
		if (!isDeclared(relaxed)) {
			String s = relaxSort(sort);
			staged.add(relaxed);
			commands.add("(declare-const " + relaxed + " " + s + ")");
		}
		if (isFloat(sort) && (floatWidth(sort) == 32 || floatWidth(sort) == 64))
			pins.put(symbol, new Pin(symbol, relaxed, floatWidth(sort) == 32 ? Sort.FLOAT32 : Sort.FLOAT64));
		return relaxed;
	}

	private String relaxedFunction(
			String symbol) {
		List<SExpression> args = functionArguments.get(symbol);
		SExpression result = functionResults.get(symbol);
		boolean relaxable = relaxable(result);
		for (SExpression arg : args)
			relaxable |= relaxable(arg);
		if (!relaxable)
			return symbol;
		String relaxed = rename(symbol);
		if (!isDeclared(relaxed))
			declareFunction(relaxed, args, result);
		return relaxed;
	}

	private void declareFunction(
			String relaxed,
			List<SExpression> args,
			SExpression result) {
		StringBuilder sb = new StringBuilder("(declare-fun ").append(relaxed).append(" (");
		for (int i = 0; i < args.size(); i++)
			sb.append(i == 0 ? "" : " ").append(relaxSort(args.get(i)));
		sb.append(") ").append(relaxSort(result)).append(")");
		staged.add(relaxed);
		commands.add(sb.toString());
	}

	private String relaxedDefinition(
			String symbol) {
		String relaxed = relaxedDefinitions.get(symbol);
		if (relaxed == null)
			relaxed = stagedDefinitions.get(symbol);
		if (relaxed != null) {
			Map<String, Pin> inner = definitionPins.containsKey(symbol) ? definitionPins.get(symbol)
					: stagedPins.get(symbol);
			if (inner != null)
				pins.putAll(inner);
			return relaxed;
		}
		Map<String, Pin> outer = pins;
		pins = new LinkedHashMap<>();
		SExpression body = SExpression.parse(definitions.get(symbol));
		String relaxedBody = relax(body, Collections.emptyMap());
		if (relaxedBody.equals(body.toString()))
			relaxed = symbol;
		else {
			relaxed = rename(symbol);
			commands.add("(define-fun " + relaxed + " () " + relaxSort(definitionSorts.get(symbol)) + " "
					+ relaxedBody + ")");
			staged.add(relaxed);
		}
		stagedDefinitions.put(symbol, relaxed);
		stagedPins.put(symbol, pins);
		outer.putAll(pins);
		pins = outer;
		return relaxed;
	}

	// terms

	private String relax(
			SExpression e,
			Map<String, SExpression> bound) {
		if (e.isAtom())
			return relaxAtom(e.getAtom(), bound);
		List<SExpression> ch = e.getChildren();
		if (ch.isEmpty())
			return "()";
		SExpression head = ch.get(0);
		if (!head.isAtom())
			return relaxIndexed(e, head.getChildren(), ch, bound);
		String h = head.getAtom();
		switch (h) {
		case "forall":
		case "exists": {
			Map<String, SExpression> inner = new HashMap<>(bound);
			StringBuilder sb = new StringBuilder("(").append(h).append(" (");
			for (SExpression v : ch.get(1).getChildren()) {
				String name = v.getChildren().get(0).getAtom();
				SExpression sort = v.getChildren().get(1);
				inner.put(name, sort);
				sb.append("(").append(name).append(" ").append(relaxSort(sort)).append(")");
			}
			return sb.append(") ").append(relax(ch.get(2), inner)).append(")").toString();
		}
		case "let": {
			StringBuilder sb = new StringBuilder("(let (");
			for (SExpression b : ch.get(1).getChildren())
				sb.append("(").append(b.getChildren().get(0).getAtom()).append(" ")
						.append(relax(b.getChildren().get(1), bound)).append(")");
			return sb.append(") ").append(relax(ch.get(2), letBindings(ch.get(1), bound))).append(")").toString();
		}
		case "!":
			return relax(ch.get(1), bound);
		case "fp":
			return literal(e);
		case "_":
			return special(e);
		case "fp.add":
			return apply("+", ch, 2, bound);
		case "fp.sub":
			return apply("-", ch, 2, bound);
		case "fp.mul":
			return apply("*", ch, 2, bound);
		case "fp.div":
			return apply("/", ch, 2, bound);
		case "fp.fma":
			return "(+ (* " + relax(ch.get(2), bound) + " " + relax(ch.get(3), bound) + ") "
					+ relax(ch.get(4), bound) + ")";
		case "fp.neg":
			return apply("-", ch, 1, bound);
		case "fp.abs": {
			String a = relax(ch.get(1), bound);
			return "(ite (< " + a + " 0.0) (- " + a + ") " + a + ")";
		}
		case "fp.min":
		case "fp.max": {
			String a = relax(ch.get(1), bound);
			String b = relax(ch.get(2), bound);
			return "(ite (" + (h.equals("fp.min") ? "<=" : ">=") + " " + a + " " + b + ") " + a + " " + b + ")";
		}
		case "fp.lt":
			return apply("<", ch, 1, bound);
		case "fp.leq":
			return apply("<=", ch, 1, bound);
		case "fp.gt":
			return apply(">", ch, 1, bound);
		case "fp.geq":
			return apply(">=", ch, 1, bound);
		case "fp.eq":
			return apply("=", ch, 1, bound);
		case "fp.isNaN":
		case "fp.isInfinite":
		case "fp.isSubnormal":
			return "false";
		case "fp.isZero":
			return "(= " + relax(ch.get(1), bound) + " 0.0)";
		case "fp.isNormal":
			return "(not (= " + relax(ch.get(1), bound) + " 0.0))";
		case "fp.isNegative":
			return "(< " + relax(ch.get(1), bound) + " 0.0)";
		case "fp.isPositive":
			return "(> " + relax(ch.get(1), bound) + " 0.0)";
		case "fp.to_real":
			return relax(ch.get(1), bound);
		case "fp.roundToIntegral":
			return round(ch.get(1).toString(), relax(ch.get(2), bound));
		case "fp.sqrt":
		case "fp.rem":
		case "fp.to_ieee_bv":
			return uninterpreted(e, ch, bound);
		default:
			break;
		}
		String symbol = h;
		Datatype d = constructors.containsKey(h) ? constructors.get(h) : selectorDatatypes.get(h);
		if (d != null && relaxableDatatypes.contains(d.getSort().toSmtLib())) {
			ensureDatatype(d.getSort().toSmtLib());
			symbol = rename(h);
		} else if (functionResults.containsKey(h))
			symbol = relaxedFunction(h);
		return apply(symbol, ch, 1, bound);
	}

	private String relaxAtom(
			String a,
			Map<String, SExpression> bound) {
		if (bound.containsKey(a))
			return a;
		if (constants.containsKey(a))
			return relaxedConstant(a, constants.get(a));
		if (definitions.containsKey(a))
			return relaxedDefinition(a);
		Datatype d = constructors.get(a);
		if (d != null && relaxableDatatypes.contains(d.getSort().toSmtLib())) {
			ensureDatatype(d.getSort().toSmtLib());
			return rename(a);
		}
		return a;
	}

	private String relaxIndexed(
			SExpression e,
			List<SExpression> idx,
			List<SExpression> ch,
			Map<String, SExpression> bound) {
		String kind = atom(idx, 0);
		if ("as".equals(kind) && "const".equals(atom(idx, 1)) && idx.size() == 3)
			return "((as const " + relaxSort(idx.get(2)) + ") " + relax(ch.get(1), bound) + ")";
		String op = "_".equals(kind) ? atom(idx, 1) : null;
		if (op != null)
			switch (op) {
			case "is": {
				String ctor = idx.get(2).getAtom();
				Datatype d = constructors.get(ctor);
				if (d != null && relaxableDatatypes.contains(d.getSort().toSmtLib())) {
					ensureDatatype(d.getSort().toSmtLib());
					ctor = rename(ctor);
				}
				return "((_ is " + ctor + ") " + relax(ch.get(1), bound) + ")";
			}
			case "to_fp": {
				SExpression arg = ch.get(ch.size() - 1);
				SExpression sort = sortOf(arg, bound);
				if (isFloat(sort) || sameSort(sort, REAL))
					return relax(arg, bound);
				if (sameSort(sort, INT))
					return "(to_real " + relax(arg, bound) + ")";
				// bit-vectors: the bits of a number, or a signed integer
				return uninterpreted(e, ch, bound);
			}
			case "to_fp_unsigned":
				return "(to_real (bv2int " + relax(ch.get(2), bound) + "))";
			case "fp.to_sbv":
			case "fp.to_ubv":
				return "((_ int2bv " + idx.get(2) + ") (to_int " + relax(ch.get(2), bound) + "))";
			default:
				break;
			}
		return apply(ch.get(0).toString(), ch, 1, bound);
	}

	private String apply(
			String operator,
			List<SExpression> ch,
			int first,
			Map<String, SExpression> bound) {
		StringBuilder sb = new StringBuilder("(").append(operator);
		for (int i = first; i < ch.size(); i++)
			sb.append(" ").append(relax(ch.get(i), bound));
		return sb.append(")").toString();
	}

	/**
	 * Relaxes an operation that cannot be expressed over real numbers as the
	 * application of an uninterpreted function to the relaxed operands (but
	 * the rounding mode).
	 */
	private String uninterpreted(
			SExpression e,
			List<SExpression> ch,
			Map<String, SExpression> bound) {
		List<SExpression> sorts = new ArrayList<>();
		List<String> args = new ArrayList<>();
		for (int i = 1; i < ch.size(); i++) {
			SExpression sort = sortOf(ch.get(i), bound);
			if (sameSort(sort, ROUNDING_MODE))
				continue;
			sorts.add(sort);
			args.add(relax(ch.get(i), bound));
		}
		SExpression result = sortOf(e, bound);
		String function = Term.symbol("relaxed " + ch.get(0) + " " + sorts + " " + result);
		if (!isDeclared(function))
			declareFunction(function, sorts, result);
		if (args.isEmpty())
			return function;
		return "(" + function + " " + String.join(" ", args) + ")";
	}

	private String literal(
			SExpression e) {
		Sort sort = FloatingPoints.sortOf(e);
		if (sort == null)
			throw new GiveUp("unknown floating point literal: " + e);
		double value = FloatingPoints.parse(e, sort).doubleValue();
		if (Double.isNaN(value))
			return specialConstant("NaN");
		if (Double.isInfinite(value))
			return specialConstant(value > 0 ? "+oo" : "-oo");
		return real(new BigDecimal(value));
	}

	private String special(
			SExpression e) {
		List<SExpression> ch = e.getChildren();
		String kind = atom(ch, 1);
		if (FloatingPoints.sortOf(e) == null || kind == null)
			return e.toString();
		switch (kind) {
		case "+zero":
		case "-zero":
			return "0.0";
		default:
			return specialConstant(kind);
		}
	}

	/**
	 * Yields an unconstrained real number standing for a special value (a NaN
	 * or an infinity), that are not real numbers.
	 */
	private String specialConstant(
			String kind) {
		String symbol = Term.symbol("relaxed " + kind);
		if (!isDeclared(symbol)) {
			staged.add(symbol);
			commands.add("(declare-const " + symbol + " Real)");
		}
		return symbol;
	}

	private static String real(
			BigDecimal value) {
		String digits = value.abs().toPlainString();
		if (!digits.contains("."))
			digits += ".0";
		return value.signum() < 0 ? "(- " + digits + ")" : digits;
	}

	private static String round(
			String mode,
			String a) {
		String floor = "(to_real (to_int " + a + "))";
		String ceiling = "(- (to_real (to_int (- " + a + "))))";
		switch (mode) {
		case "RTN":
		case "roundTowardNegative":
			return floor;
		case "RTP":
		case "roundTowardPositive":
			return ceiling;
		case "RTZ":
		case "roundTowardZero":
			return "(ite (>= " + a + " 0.0) " + floor + " " + ceiling + ")";
		default:
			return "(to_real (to_int (+ " + a + " 0.5)))";
		}
	}

	/**
	 * Yields the rational number printed by a solver as the given value (e.g.,
	 * {@code (- (/ 1.0 3.0))}), approximated to 34 digits, or {@code null} if
	 * it is not a rational number.
	 */
	private static BigDecimal rational(
			SExpression value) {
		if (value.isAtom()) {
			String a = value.getAtom();
			return a.isEmpty() || !Character.isDigit(a.charAt(0)) ? null : new BigDecimal(a);
		}
		List<SExpression> ch = value.getChildren();
		String op = atom(ch, 0);
		if ("-".equals(op) && ch.size() == 2) {
			BigDecimal a = rational(ch.get(1));
			return a == null ? null : a.negate();
		}
		if ("/".equals(op) && ch.size() == 3) {
			BigDecimal a = rational(ch.get(1));
			BigDecimal b = rational(ch.get(2));
			return a == null || b == null || b.signum() == 0 ? null : a.divide(b, MathContext.DECIMAL128);
		}
		return null;
	}
}
