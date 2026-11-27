package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The languages of the programs a witness can refer to. Each language allows
 * its own expression formats and supports a subset of the
 * {@link LanguageFeature}s of the witness format.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum Language {

	/**
	 * The C language, which supports every feature of the witness format.
	 */
	C(
			"C",
			LanguageFeature.DATA_MODEL,
			LanguageFeature.GHOST_INSTRUMENTATION,
			LanguageFeature.TRANSITION_INVARIANTS,
			LanguageFeature.FUNCTION_CONTRACTS),

	/**
	 * The Java language, which supports function contracts.
	 */
	JAVA("Java", LanguageFeature.FUNCTION_CONTRACTS),

	/**
	 * The Python language, which supports function contracts.
	 */
	PYTHON("Python", LanguageFeature.FUNCTION_CONTRACTS);

	/**
	 * The name of this language, as it is written in a witness.
	 */
	private final String name;

	/**
	 * The features of the witness format supported by this language.
	 */
	private final Set<LanguageFeature> features;

	/**
	 * Builds the language.
	 * 
	 * @param name     the name of the language, as it is written in a witness
	 * @param features the features of the witness format supported by the
	 *                     language
	 */
	private Language(
			String name,
			LanguageFeature... features) {
		this.name = name;
		this.features = Collections.unmodifiableSet(features.length == 0 ? EnumSet.noneOf(LanguageFeature.class)
				: EnumSet.copyOf(Arrays.asList(features)));
	}

	/**
	 * Returns the name of this language, as it is written in a witness.
	 * 
	 * @return the name of this language
	 */
	@JsonValue
	public String getName() {
		return name;
	}

	/**
	 * Returns the features of the witness format supported by this language.
	 * 
	 * @return the features, which cannot be modified
	 */
	public Set<LanguageFeature> getFeatures() {
		return features;
	}

	/**
	 * Returns whether this language supports the given feature.
	 * 
	 * @param feature the feature
	 * 
	 * @return {@code true} if this language supports the feature
	 */
	public boolean supports(
			LanguageFeature feature) {
		return features.contains(feature);
	}

	/**
	 * Returns whether the tasks of this language need a {@link DataModel}.
	 * 
	 * @return {@code true} if a data model is needed
	 */
	public boolean requiresDataModel() {
		return supports(LanguageFeature.DATA_MODEL);
	}

	/**
	 * Returns whether this language supports ghost code.
	 * 
	 * @return {@code true} if ghost code is supported
	 */
	public boolean supportsGhost() {
		return supports(LanguageFeature.GHOST_INSTRUMENTATION);
	}

	/**
	 * Returns whether this language supports transition invariants.
	 * 
	 * @return {@code true} if transition invariants are supported
	 */
	public boolean supportsTransitionInvariants() {
		return supports(LanguageFeature.TRANSITION_INVARIANTS);
	}

	/**
	 * Returns whether this language supports function contracts.
	 * 
	 * @return {@code true} if function contracts are supported
	 */
	public boolean supportsContracts() {
		return supports(LanguageFeature.FUNCTION_CONTRACTS);
	}

	/**
	 * Returns the plain expression format of this language.
	 * 
	 * @return the plain expression format
	 */
	public Format getExpressionFormat() {
		return format(false);
	}

	/**
	 * Returns the extended expression format of this language, which allows the
	 * extension keywords of the witness format.
	 * 
	 * @return the extended expression format
	 */
	public Format getExtendedExpressionFormat() {
		return format(true);
	}

	/**
	 * Returns the format needed by the given expressions. Outside string and
	 * character literals, a backslash only appears in the extension keywords,
	 * such as {@code \result}, {@code \at} and {@code \old}, and it is never
	 * valid in a plain C, Java or Python expression. The extended format is
	 * yielded if any expression has such a backslash, and the plain format
	 * otherwise.
	 * 
	 * @param expressions the expressions, some of which can be {@code null}
	 * 
	 * @return the format needed by the expressions
	 */
	public Format getFormatFor(
			String... expressions) {
		for (String expression : expressions)
			if (expression != null && hasBackslashOutsideLiterals(expression))
				return getExtendedExpressionFormat();
		return getExpressionFormat();
	}

	/**
	 * Returns whether the given expression has a backslash outside its string
	 * and character literals. The expression is read once, without regular
	 * expressions, so that a long literal cannot exhaust the stack.
	 * 
	 * @param expression the expression
	 * 
	 * @return {@code true} if a backslash appears outside the literals
	 */
	private static boolean hasBackslashOutsideLiterals(
			String expression) {
		char quote = 0;
		for (int i = 0; i < expression.length(); i++) {
			char c = expression.charAt(i);
			if (quote != 0) {
				if (c == '\\')
					// the escaped character belongs to the literal
					i++;
				else if (c == quote)
					quote = 0;
			} else if (c == '"' || c == '\'')
				quote = c;
			else if (c == '\\')
				return true;
		}
		return false;
	}

	/**
	 * Returns the plain or extended expression format of this language. The
	 * deprecated {@link Format#ACSL_EXPRESSION} is never yielded, since
	 * {@link Format#EXT_C_EXPRESSION} replaces it.
	 * 
	 * @param extended whether the extended format is wanted
	 * 
	 * @return the format
	 */
	@SuppressWarnings("deprecation")
	private Format format(
			boolean extended) {
		for (Format format : Format.values())
			if (format.getLanguage() == this && format.isExtended() == extended && format != Format.ACSL_EXPRESSION)
				return format;
		throw new IllegalStateException("No " + (extended ? "extended " : "") + "expression format for " + this);
	}

	/**
	 * Returns whether the given format is allowed for this language.
	 * 
	 * @param format the format
	 * 
	 * @return {@code true} if the format belongs to this language
	 */
	public boolean allows(
			Format format) {
		return format.getLanguage() == this;
	}

	/**
	 * Checks that the given format is allowed for this language.
	 * 
	 * @param format  the format
	 * @param element the element using the format, reported in the message of
	 *                    the exception
	 * 
	 * @throws InvalidWitnessException if the format is not allowed
	 */
	public void checkAllows(
			Format format,
			Object element) {
		if (!allows(format))
			throw new InvalidWitnessException(
					"Format " + format + " is not allowed for " + this + " (in " + element + ")");
	}

	@Override
	public String toString() {
		return name;
	}
}
