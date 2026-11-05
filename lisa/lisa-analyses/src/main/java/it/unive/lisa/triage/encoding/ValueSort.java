package it.unive.lisa.triage.encoding;

import it.unive.lisa.triage.smt.Datatype;
import it.unive.lisa.triage.smt.FloatingPoints;
import it.unive.lisa.triage.smt.Datatype.Field;
import it.unive.lisa.triage.smt.SExpression;
import it.unive.lisa.triage.smt.SmtException;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Type;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code Value} algebraic datatype, used to encode program values whose
 * type is not statically known or not unique (e.g., variables of dynamically
 * typed languages). A value is either an integer, a boolean, a string, the null
 * value, a reference (identified by an integer), or an opaque value of a type
 * that is not encoded precisely (e.g., floating point numbers), identified by
 * an integer. The datatype must be declared through
 * {@link it.unive.lisa.triage.smt.SmtSolver#declareDatatype(Datatype)} before
 * being used.<br>
 * <br>
 * Selecting a field of a value built with a different constructor yields an
 * unspecified value of the field's sort: this is used to soundly read values of
 * unknown types as values of a given sort.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ValueSort {

	/**
	 * The name of the constructor of integer values.
	 */
	public static final String INT = "Value.Int";

	/**
	 * The name of the constructor of boolean values.
	 */
	public static final String BOOL = "Value.Bool";

	/**
	 * The name of the constructor of string values.
	 */
	public static final String STRING = "Value.String";

	/**
	 * The name of the constructor of the null value.
	 */
	public static final String NULL = "Value.Null";

	/**
	 * The name of the constructor of references.
	 */
	public static final String REF = "Value.Ref";

	/**
	 * The name of the constructor of opaque values.
	 */
	public static final String OPAQUE = "Value.Opaque";

	/**
	 * The name of the constructor of single precision floating point values.
	 */
	public static final String FLOAT32 = "Value.F32";

	/**
	 * The name of the constructor of double precision floating point values.
	 */
	public static final String FLOAT64 = "Value.F64";

	private static final String INT_FIELD = "Value.int";

	private static final String BOOL_FIELD = "Value.bool";

	private static final String STRING_FIELD = "Value.string";

	private static final String REF_FIELD = "Value.ref";

	private static final String OPAQUE_FIELD = "Value.opaque";

	private static final String FLOAT32_FIELD = "Value.f32";

	private static final String FLOAT64_FIELD = "Value.f64";

	/**
	 * The datatype.
	 */
	public static final Datatype DATATYPE;

	static {
		Map<String, List<Field>> constructors = new LinkedHashMap<>();
		constructors.put(INT, List.of(new Field(INT_FIELD, Sort.INT)));
		constructors.put(BOOL, List.of(new Field(BOOL_FIELD, Sort.BOOL)));
		constructors.put(STRING, List.of(new Field(STRING_FIELD, Sort.STRING)));
		constructors.put(NULL, List.of());
		constructors.put(REF, List.of(new Field(REF_FIELD, Sort.INT)));
		constructors.put(OPAQUE, List.of(new Field(OPAQUE_FIELD, Sort.INT)));
		constructors.put(FLOAT32, List.of(new Field(FLOAT32_FIELD, Sort.FLOAT32)));
		constructors.put(FLOAT64, List.of(new Field(FLOAT64_FIELD, Sort.FLOAT64)));
		DATATYPE = new Datatype("Value", constructors);
	}

	/**
	 * The sort of values.
	 */
	public static final Sort SORT = DATATYPE.getSort();

	private ValueSort() {
	}

	/**
	 * Yields the null value.
	 *
	 * @return the null value
	 */
	public static Term nullValue() {
		return DATATYPE.construct(NULL);
	}

	/**
	 * Wraps the given term, of sort {@link Sort#INT}, {@link Sort#BOOL}, or
	 * {@link Sort#STRING}, into a value.
	 *
	 * @param term the term to wrap
	 *
	 * @return the value, or {@code null} if the sort of the term cannot be
	 *             wrapped
	 */
	public static Term wrap(
			Term term) {
		Sort sort = term.getSort();
		if (sort.equals(SORT))
			return term;
		if (sort.equals(Sort.INT))
			return DATATYPE.construct(INT, term);
		if (sort.equals(Sort.BOOL))
			return DATATYPE.construct(BOOL, term);
		if (sort.equals(Sort.STRING))
			return DATATYPE.construct(STRING, term);
		if (sort.equals(Sort.FLOAT32))
			return DATATYPE.construct(FLOAT32, term);
		if (sort.equals(Sort.FLOAT64))
			return DATATYPE.construct(FLOAT64, term);
		return null;
	}

	/**
	 * Reads the given value as a term of the given sort, that must be one of
	 * {@link Sort#INT}, {@link Sort#BOOL}, or {@link Sort#STRING}. If the value
	 * has not been built with the corresponding constructor, the result is an
	 * unspecified term of the given sort.
	 *
	 * @param value the value
	 * @param sort  the sort to read the value as
	 *
	 * @return the term, or {@code null} if values cannot be read as terms of
	 *             the given sort
	 */
	public static Term unwrap(
			Term value,
			Sort sort) {
		if (sort.equals(Sort.INT))
			return DATATYPE.select(INT_FIELD, value);
		if (sort.equals(Sort.BOOL))
			return DATATYPE.select(BOOL_FIELD, value);
		if (sort.equals(Sort.STRING))
			return DATATYPE.select(STRING_FIELD, value);
		if (sort.equals(Sort.FLOAT32))
			return DATATYPE.select(FLOAT32_FIELD, value);
		if (sort.equals(Sort.FLOAT64))
			return DATATYPE.select(FLOAT64_FIELD, value);
		return null;
	}

	/**
	 * Yields the boolean term that holds if the given value has been built with
	 * the constructor of terms of the given sort, that must be one of
	 * {@link Sort#INT}, {@link Sort#BOOL}, or {@link Sort#STRING}.
	 *
	 * @param value the value
	 * @param sort  the sort
	 *
	 * @return the boolean term, or {@code null} if no constructor builds values
	 *             from terms of the given sort
	 */
	public static Term is(
			Term value,
			Sort sort) {
		if (sort.equals(Sort.INT))
			return DATATYPE.test(INT, value);
		if (sort.equals(Sort.BOOL))
			return DATATYPE.test(BOOL, value);
		if (sort.equals(Sort.STRING))
			return DATATYPE.test(STRING, value);
		if (sort.equals(Sort.FLOAT32))
			return DATATYPE.test(FLOAT32, value);
		if (sort.equals(Sort.FLOAT64))
			return DATATYPE.test(FLOAT64, value);
		return null;
	}

	/**
	 * Yields the value representing the given type when used as a type token
	 * (e.g., the right operand of a type check): an opaque value whose
	 * identifier is unique for each type name, so that tokens of the same type
	 * are equal and tokens of different types are different. The identifier is
	 * the (negated) number whose bytes are the ones of the name of the type, so
	 * that it does not depend on the order in which types are encoded.
	 *
	 * @param type the type
	 *
	 * @return the value
	 */
	public static Term typeToken(
			Type type) {
		BigInteger id = new BigInteger(1, type.toString().getBytes(StandardCharsets.UTF_8));
		return DATATYPE.construct(OPAQUE, Term.integer(id.negate().subtract(BigInteger.ONE)));
	}

	/**
	 * Yields the boolean term that holds if the given value is a reference.
	 *
	 * @param value the value
	 *
	 * @return the boolean term
	 */
	public static Term isReference(
			Term value) {
		return DATATYPE.test(REF, value);
	}

	/**
	 * Yields the identifier of the given value, read as a reference (an
	 * unspecified integer if the value is not a reference).
	 *
	 * @param value the value
	 *
	 * @return the integer term
	 */
	public static Term referenceId(
			Term value) {
		return Term.app(Term.symbol(REF_FIELD), Sort.INT, value);
	}

	/**
	 * Yields the reference with the given identifier.
	 *
	 * @param id the identifier, an integer term
	 *
	 * @return the reference
	 */
	public static Term reference(
			Term id) {
		return DATATYPE.construct(REF, id);
	}

	/**
	 * Yields the boolean term that holds if the given value is opaque, that
	 * is, a value whose meaning is not modeled (e.g., a floating point number,
	 * or a type token).
	 *
	 * @param value the value
	 *
	 * @return the boolean term
	 */
	public static Term isOpaque(
			Term value) {
		return DATATYPE.test(OPAQUE, value);
	}

	/**
	 * Yields the boolean term that holds if the given value might be a number:
	 * an integer, or an opaque value (that might be, e.g., a floating point
	 * number).
	 *
	 * @param value the value
	 *
	 * @return the boolean term
	 */
	public static Term isNumeric(
			Term value) {
		return Term.or(DATATYPE.test(INT, value), DATATYPE.test(OPAQUE, value), DATATYPE.test(FLOAT32, value),
				DATATYPE.test(FLOAT64, value));
	}

	/**
	 * Yields a human-readable representation of the given value, as returned by
	 * a solver in SMT-LIB syntax (e.g., {@code (Value.Int (- 3))} becomes
	 * {@code -3}, and {@code Value.Null} becomes {@code null}). Values that are
	 * not recognized are returned unchanged.
	 *
	 * @param smtlib the value, in SMT-LIB syntax
	 *
	 * @return the readable representation
	 */
	public static String display(
			String smtlib) {
		try {
			return display(SExpression.parse(smtlib));
		} catch (SmtException e) {
			return smtlib;
		}
	}

	/**
	 * Yields a readable representation of a floating point value of the given
	 * sort, as printed in models: the Java representation of the number.
	 *
	 * @param value the value
	 * @param sort  the sort of the value
	 *
	 * @return the representation
	 */
	public static String displayFloat(
			SExpression value,
			Sort sort) {
		try {
			return FloatingPoints.parse(value, sort).toString();
		} catch (SmtException e) {
			return value.toString();
		}
	}

	private static String display(
			SExpression value) {
		if (value.isAtom())
			return value.getAtom().equals(NULL) ? "null" : value.getAtom();
		List<SExpression> children = value.getChildren();
		Sort fp = FloatingPoints.sortOf(value);
		if (fp != null)
			return displayFloat(value, fp);
		if (children.size() == 2 && children.get(0).isAtom()) {
			String head = children.get(0).getAtom();
			if (head.equals("-"))
				return "-" + display(children.get(1));
			if (head.equals(INT) || head.equals(BOOL) || head.equals(STRING))
				return display(children.get(1));
			if (head.equals(REF))
				return "ref#" + display(children.get(1));
			if (head.equals(OPAQUE))
				return "opaque#" + display(children.get(1));
			if (head.equals(FLOAT32))
				return displayFloat(children.get(1), Sort.FLOAT32);
			if (head.equals(FLOAT64))
				return displayFloat(children.get(1), Sort.FLOAT64);
		}
		return value.toString();
	}
}
