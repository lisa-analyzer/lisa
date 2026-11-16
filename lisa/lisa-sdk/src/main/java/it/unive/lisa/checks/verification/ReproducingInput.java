package it.unive.lisa.checks.verification;

import it.unive.lisa.program.cfg.CodeLocation;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A concrete value that, together with other {@link ReproducingInput}s,
 * reproduces a violation. Each input is the value produced by a source of
 * nondeterminism of the program (e.g., a parameter of an entrypoint, or the
 * result of a call returning an unknown value) at a given location, in a given
 * context, the {@code n}-th time that the source is evaluated (sources inside
 * loops or recursive calls might be evaluated more than once in the same
 * execution).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReproducingInput
		implements
		Comparable<ReproducingInput> {

	private final CodeLocation location;

	private final String source;

	private final String context;

	private final int occurrence;

	private final String value;

	/**
	 * Builds the input.
	 *
	 * @param location   the location of the source of nondeterminism
	 * @param source     a human-readable description of the source of
	 *                       nondeterminism (e.g., {@code parameter x of main})
	 * @param context    a description of the context where the source is
	 *                       evaluated (can be {@code null} if the source is not
	 *                       context-dependent)
	 * @param occurrence the number of times the source has been evaluated, in
	 *                       the same context, before producing this value
	 *                       (starting from {@code 0})
	 * @param value      the textual representation of the value
	 */
	public ReproducingInput(
			CodeLocation location,
			String source,
			String context,
			int occurrence,
			String value) {
		Objects.requireNonNull(location);
		Objects.requireNonNull(source);
		Objects.requireNonNull(value);
		this.location = location;
		this.source = source;
		this.context = context;
		this.occurrence = occurrence;
		this.value = value;
	}

	/**
	 * Yields the location of the source of nondeterminism.
	 *
	 * @return the location
	 */
	public CodeLocation getLocation() {
		return location;
	}

	/**
	 * Yields a human-readable description of the source of nondeterminism.
	 *
	 * @return the description
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Yields a description of the context where the source is evaluated.
	 *
	 * @return the context, or {@code null} if the source is not
	 *             context-dependent
	 */
	public String getContext() {
		return context;
	}

	/**
	 * Yields the number of times the source has been evaluated, in the same
	 * context, before producing this value (starting from {@code 0}).
	 *
	 * @return the occurrence
	 */
	public int getOccurrence() {
		return occurrence;
	}

	/**
	 * Yields the textual representation of the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	@Override
	public int compareTo(
			ReproducingInput o) {
		int cmp;
		if ((cmp = location.compareTo(o.location)) != 0)
			return cmp;
		if ((cmp = source.compareTo(o.source)) != 0)
			return cmp;
		if ((cmp = StringUtils.compare(context, o.context)) != 0)
			return cmp;
		if ((cmp = Integer.compare(occurrence, o.occurrence)) != 0)
			return cmp;
		return value.compareTo(o.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(context, location, occurrence, source, value);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReproducingInput other = (ReproducingInput) obj;
		return Objects.equals(context, other.context)
				&& Objects.equals(location, other.location)
				&& occurrence == other.occurrence
				&& Objects.equals(source, other.source)
				&& Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return source
				+ " at "
				+ location
				+ (context == null ? "" : " in " + context)
				+ (occurrence == 0 ? "" : " (occurrence " + occurrence + ")")
				+ " = "
				+ value;
	}
}
