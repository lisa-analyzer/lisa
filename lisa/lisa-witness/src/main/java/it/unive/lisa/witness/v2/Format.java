package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Format {

	C_EXPRESSION("c_expression", Language.C, false),

	EXT_C_EXPRESSION("ext_c_expression", Language.C, true),

	@Deprecated
	ACSL_EXPRESSION("acsl_expression", Language.C, true),

	JAVA_EXPRESSION("java_expression", Language.JAVA, false),

	EXT_JAVA_EXPRESSION("ext_java_expression", Language.JAVA, true),

	PYTHON_EXPRESSION("python_expression", Language.PYTHON, false),

	EXT_PYTHON_EXPRESSION("ext_python_expression", Language.PYTHON, true);

	private final String name;

	private final Language language;

	private final boolean extended;

	private Format(
			String name,
			Language language,
			boolean extended) {
		this.name = name;
		this.language = language;
		this.extended = extended;
	}

	@JsonValue
	public String getName() {
		return name;
	}

	public Language getLanguage() {
		return language;
	}

	public boolean isExtended() {
		return extended;
	}

	@Override
	public String toString() {
		return name;
	}
}
