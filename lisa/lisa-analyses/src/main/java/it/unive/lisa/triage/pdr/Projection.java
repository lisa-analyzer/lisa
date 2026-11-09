package it.unive.lisa.triage.pdr;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.triage.encoding.SortPolicy;
import it.unive.lisa.triage.encoding.ValueSort;
import it.unive.lisa.triage.model.FreshValue;
import it.unive.lisa.triage.model.HeapOperators;
import it.unive.lisa.triage.model.HeapType;
import it.unive.lisa.triage.model.SymbolicCase;
import it.unive.lisa.triage.smt.FloatingPoints;
import it.unive.lisa.triage.smt.SExpression;
import it.unive.lisa.triage.smt.SmtException;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Model-based projection over symbolic expressions. Since the cases of a
 * transition ({@link SymbolicCase}) are functional updates, the exact pre-image
 * of a cube through a case is obtained by substituting the updated identifiers
 * in the literals of the cube
 * ({@link SymbolicCase#substitute(SymbolicExpression, it.unive.lisa.triage.model.FreshValueFactory, it.unive.lisa.program.cfg.ProgramPoint)})
 * and by conjoining the path condition of the case. The only existentially
 * quantified values left are the fresh values of the case, that are eliminated
 * by replacing them with their values in a model of the query (when these can
 * be expressed as constants): this under-approximates the pre-image, but always
 * contains the predecessor found in the model.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Projection {

	private Projection() {
	}

	/**
	 * Splits the given boolean expressions into their conjuncts.
	 *
	 * @param expressions the expressions
	 *
	 * @return the conjuncts, without duplicates and without {@code true}
	 */
	public static Set<SymbolicExpression> conjuncts(
			Collection<SymbolicExpression> expressions) {
		Set<SymbolicExpression> result = new LinkedHashSet<>();
		for (SymbolicExpression e : expressions)
			conjuncts(e, result);
		return result;
	}

	private static void conjuncts(
			SymbolicExpression e,
			Set<SymbolicExpression> result) {
		if (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalAnd.INSTANCE) {
			conjuncts(((BinaryExpression) e).getLeft(), result);
			conjuncts(((BinaryExpression) e).getRight(), result);
		} else if (!(e instanceof Constant && Boolean.TRUE.equals(((Constant) e).getValue())))
			result.add(e);
	}

	/**
	 * Yields the fresh values occurring in the given expressions.
	 *
	 * @param expressions the expressions
	 *
	 * @return the fresh values
	 */
	public static Set<FreshValue> freshValues(
			Collection<SymbolicExpression> expressions) {
		Set<FreshValue> result = new LinkedHashSet<>();
		for (SymbolicExpression e : expressions)
			freshValues(e, result);
		return result;
	}

	private static void freshValues(
			SymbolicExpression e,
			Set<FreshValue> result) {
		if (e instanceof FreshValue)
			result.add((FreshValue) e);
		else if (e instanceof UnaryExpression)
			freshValues(((UnaryExpression) e).getExpression(), result);
		else if (e instanceof BinaryExpression) {
			freshValues(((BinaryExpression) e).getLeft(), result);
			freshValues(((BinaryExpression) e).getRight(), result);
		} else if (e instanceof TernaryExpression) {
			freshValues(((TernaryExpression) e).getLeft(), result);
			freshValues(((TernaryExpression) e).getMiddle(), result);
			freshValues(((TernaryExpression) e).getRight(), result);
		}
	}

	/**
	 * Yields the identifiers occurring in the given expressions, excluding
	 * fresh values.
	 *
	 * @param expressions the expressions
	 *
	 * @return the identifiers
	 */
	public static Set<Identifier> identifiers(
			Collection<SymbolicExpression> expressions) {
		Set<Identifier> result = new LinkedHashSet<>();
		for (SymbolicExpression e : expressions)
			identifiers(e, result);
		return result;
	}

	private static void identifiers(
			SymbolicExpression e,
			Set<Identifier> result) {
		if (e instanceof Identifier) {
			if (!(e instanceof FreshValue))
				result.add((Identifier) e);
		} else if (e instanceof UnaryExpression)
			identifiers(((UnaryExpression) e).getExpression(), result);
		else if (e instanceof BinaryExpression) {
			identifiers(((BinaryExpression) e).getLeft(), result);
			identifiers(((BinaryExpression) e).getRight(), result);
		} else if (e instanceof TernaryExpression) {
			identifiers(((TernaryExpression) e).getLeft(), result);
			identifiers(((TernaryExpression) e).getMiddle(), result);
			identifiers(((TernaryExpression) e).getRight(), result);
		}
	}

	/**
	 * Replaces fresh values in the given expressions.
	 *
	 * @param expressions the expressions
	 * @param values      the replacement of each fresh value (fresh values not
	 *                        in the map are left as they are)
	 *
	 * @return the rewritten expressions
	 */
	public static List<SymbolicExpression> replace(
			Collection<SymbolicExpression> expressions,
			Map<FreshValue, SymbolicExpression> values) {
		List<SymbolicExpression> result = new ArrayList<>();
		for (SymbolicExpression e : expressions) {
			SymbolicExpression r = e;
			for (Map.Entry<FreshValue, SymbolicExpression> v : values.entrySet())
				r = r.replace(v.getKey(), v.getValue());
			result.add(r);
		}
		return result;
	}

	/**
	 * Converts a value printed by a solver in a model into a constant
	 * expression. Integers, booleans, strings, floating point numbers, the null
	 * value and references (also wrapped in {@link ValueSort values}) are
	 * supported.
	 *
	 * @param smtlib the value, in SMT-LIB syntax
	 * @param types  the type system providing the types of constants
	 *
	 * @return the constant, or {@code null} if the value is not supported
	 */
	public static SymbolicExpression toConstant(
			String smtlib,
			TypeSystem types) {
		if (smtlib == null)
			return null;
		try {
			return toConstant(SExpression.parse(smtlib), types);
		} catch (SmtException | NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Yields a floating point type of the program whose values are encoded with
	 * the given sort, or {@code null} if there is none.
	 */
	private static Type floatType(
			TypeSystem types,
			Sort sort) {
		for (Type t : types.getTypes())
			if (SortPolicy.isFloat(t)
					&& (sort.equals(Sort.FLOAT32) ? t.asNumericType().is32Bits() : t.asNumericType().is64Bits()))
				return t;
		return null;
	}

	private static SymbolicExpression toConstant(
			SExpression value,
			TypeSystem types) {
		SyntheticLocation loc = SyntheticLocation.INSTANCE;
		if (value.isAtom()) {
			String atom = value.getAtom();
			if (atom.equals("true") || atom.equals("false"))
				return new Constant(types.getBooleanType(), Boolean.parseBoolean(atom), loc);
			if (atom.equals(ValueSort.NULL))
				return new NullConstant(loc);
			if (atom.startsWith("\""))
				return new Constant(types.getStringType(), unquote(atom), loc);
			return new Constant(types.getIntegerType(), new BigInteger(atom), loc);
		}
		Sort fp = FloatingPoints.sortOf(value);
		if (fp != null) {
			Type type = floatType(types, fp);
			return type == null ? null : new Constant(type, FloatingPoints.parse(value, fp), loc);
		}
		List<SExpression> children = value.getChildren();
		if (children.size() != 2 || !children.get(0).isAtom())
			return null;
		String head = children.get(0).getAtom();
		if (head.equals(ValueSort.FLOAT32) || head.equals(ValueSort.FLOAT64))
			return toConstant(children.get(1), types);
		if (head.equals("-")) {
			SymbolicExpression inner = toConstant(children.get(1), types);
			if (inner instanceof Constant && ((Constant) inner).getValue() instanceof BigInteger)
				return new Constant(types.getIntegerType(), ((BigInteger) ((Constant) inner).getValue()).negate(),
						loc);
			return null;
		}
		if (head.equals(ValueSort.INT) || head.equals(ValueSort.BOOL) || head.equals(ValueSort.STRING))
			return toConstant(children.get(1), types);
		if (head.equals(ValueSort.REF)) {
			// the reference with the given identifier (see HeapModel)
			SymbolicExpression id = toConstant(children.get(1), types);
			return id instanceof Constant && ((Constant) id).getValue() instanceof BigInteger
					&& ((BigInteger) ((Constant) id).getValue()).bitLength() < 32
							? new UnaryExpression(HeapType.REFERENCE,
							new Constant(HeapType.COUNTER, ((BigInteger) ((Constant) id).getValue()).intValueExact(),
									loc),
							HeapOperators.NewReference.INSTANCE, loc)
					: null;
		}
		return null;
	}

	/**
	 * Decodes an SMT-LIB string literal: quotes are doubled, and characters
	 * might be written as {@code \\u{XX}}.
	 */
	private static String unquote(
			String literal) {
		String body = literal.substring(1, literal.length() - 1).replace("\"\"", "\"");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < body.length(); i++) {
			if (body.startsWith("\\u{", i)) {
				int end = body.indexOf('}', i);
				if (end > 0) {
					sb.appendCodePoint(Integer.parseInt(body.substring(i + 3, end), 16));
					i = end;
					continue;
				}
			}
			sb.append(body.charAt(i));
		}
		return sb.toString();
	}
}
