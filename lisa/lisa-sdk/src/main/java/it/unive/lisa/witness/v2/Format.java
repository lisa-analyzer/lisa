package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The expression formats of the witness format, written as the {@code format}
 * of expressions. Each format belongs to a language. An extended format also
 * allows the extension keywords of the witness format, such as {@code \result}
 * and {@code \at}.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum Format {

	/**
	 * A C expression.
	 */
	C_EXPRESSION("c_expression", Language.C, false),

	/**
	 * A C expression that can use the extension keywords.
	 */
	EXT_C_EXPRESSION("ext_c_expression", Language.C, true),

	/**
	 * An ACSL expression, only allowed as the constraint of a function return
	 * waypoint.
	 * 
	 * @deprecated {@link #EXT_C_EXPRESSION} replaces it
	 */
	@Deprecated
	ACSL_EXPRESSION("acsl_expression", Language.C, true),

	/**
	 * A Java expression.
	 */
	JAVA_EXPRESSION("java_expression", Language.JAVA, false),

	/**
	 * A Java expression that can use the extension keywords.
	 */
	EXT_JAVA_EXPRESSION("ext_java_expression", Language.JAVA, true),

	/**
	 * A Python expression.
	 */
	PYTHON_EXPRESSION("python_expression", Language.PYTHON, false),

	/**
	 * A Python expression that can use the extension keywords.
	 */
	EXT_PYTHON_EXPRESSION("ext_python_expression", Language.PYTHON, true);

	/**
	 * The name of this format, as it is written in a witness.
	 */
	private final String name;

	/**
	 * The language of the expressions of this format.
	 */
	private final Language language;

	/**
	 * Whether this format allows the extension keywords.
	 */
	private final boolean extended;

	/**
	 * Builds the format.
	 * 
	 * @param name     the name of the format, as it is written in a witness
	 * @param language the language of the expressions of the format
	 * @param extended whether the format allows the extension keywords
	 */
	private Format(
			String name,
			Language language,
			boolean extended) {
		this.name = name;
		this.language = language;
		this.extended = extended;
	}

	/**
	 * Returns the name of this format, as it is written in a witness.
	 * 
	 * @return the name of this format
	 */
	@JsonValue
	public String getName() {
		return name;
	}

	/**
	 * Returns the language of the expressions of this format.
	 * 
	 * @return the language
	 */
	public Language getLanguage() {
		return language;
	}

	/**
	 * Returns whether this format allows the extension keywords of the witness
	 * format.
	 * 
	 * @return {@code true} if this format is extended
	 */
	public boolean isExtended() {
		return extended;
	}

	@Override
	public String toString() {
		return name;
	}
}
