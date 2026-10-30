package it.unive.lisa.triage.encoding;

import it.unive.lisa.triage.smt.Datatype;
import it.unive.lisa.triage.smt.Datatype.Field;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code Value} algebraic datatype, used to encode program values whose
 * type is not statically known or not unique (e.g., variables of dynamically
 * typed languages). A value is either an integer, a boolean, a string, the
 * null value, a reference (identified by an integer), or an opaque value of a
 * type that is not encoded precisely (e.g., floating point numbers),
 * identified by an integer. The datatype must be declared through
 * {@link it.unive.lisa.triage.smt.SmtSolver#declareDatatype(Datatype)} before
 * being used.<br>
 * <br>
 * Selecting a field of a value built with a different constructor yields an
 * unspecified value of the field's sort: this is used to soundly read values
 * of unknown types as values of a given sort.
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

	private static final String INT_FIELD = "Value.int";

	private static final String BOOL_FIELD = "Value.bool";

	private static final String STRING_FIELD = "Value.string";

	private static final String REF_FIELD = "Value.ref";

	private static final String OPAQUE_FIELD = "Value.opaque";

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
		return null;
	}

	/**
	 * Yields the boolean term that holds if the given value has been built
	 * with the constructor of terms of the given sort, that must be one of
	 * {@link Sort#INT}, {@link Sort#BOOL}, or {@link Sort#STRING}.
	 *
	 * @param value the value
	 * @param sort  the sort
	 *
	 * @return the boolean term, or {@code null} if no constructor builds
	 *             values from terms of the given sort
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
		return null;
	}
}
