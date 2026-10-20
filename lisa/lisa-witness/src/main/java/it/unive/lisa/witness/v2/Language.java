package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum Language {

	C(
			"C",
			LanguageFeature.DATA_MODEL,
			LanguageFeature.GHOST_INSTRUMENTATION,
			LanguageFeature.TRANSITION_INVARIANTS,
			LanguageFeature.FUNCTION_CONTRACTS),

	JAVA("Java", LanguageFeature.FUNCTION_CONTRACTS),

	PYTHON("Python", LanguageFeature.FUNCTION_CONTRACTS);

	private final String name;

	private final Set<LanguageFeature> features;

	private Language(
			String name,
			LanguageFeature... features) {
		this.name = name;
		this.features = Collections.unmodifiableSet(features.length == 0 ? EnumSet.noneOf(LanguageFeature.class)
				: EnumSet.copyOf(Arrays.asList(features)));
	}

	@JsonValue
	public String getName() {
		return name;
	}

	public Set<LanguageFeature> getFeatures() {
		return features;
	}

	public boolean supports(
			LanguageFeature feature) {
		return features.contains(feature);
	}

	public boolean requiresDataModel() {
		return supports(LanguageFeature.DATA_MODEL);
	}

	public boolean supportsGhost() {
		return supports(LanguageFeature.GHOST_INSTRUMENTATION);
	}

	public boolean supportsTransitionInvariants() {
		return supports(LanguageFeature.TRANSITION_INVARIANTS);
	}

	public boolean supportsContracts() {
		return supports(LanguageFeature.FUNCTION_CONTRACTS);
	}

	public boolean allows(
			Format format) {
		return format.getLanguage() == this;
	}

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
