package it.unive.lisa.triage.inputs;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * A source of nondeterminism of the program, that is, a point where the
 * analysis introduces an unknown value (a
 * {@link it.unive.lisa.symbolic.value.PushAny}, possibly with constraints as a
 * {@link it.unive.lisa.symbolic.value.PushFromConstraints}). Each evaluation
 * of a source produces a value that is part of the inputs of an execution:
 * fixing the values of all evaluations of all sources fixes the execution, and
 * a concrete assignment of these values can be reported as a list of
 * {@link ReproducingInput}s (see
 * {@link #toReproducingInput(ScopeId, int, String)}).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InputSource {

	/**
	 * The kinds of sources of nondeterminism.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public enum Kind {

		/**
		 * The value of a parameter of a cfg whose callers are not analyzed
		 * (e.g., an entrypoint).
		 */
		PARAMETER,

		/**
		 * The value returned by a call whose target is not analyzed, either
		 * because it is modeled natively or because it cannot be resolved.
		 */
		CALL_RESULT,

		/**
		 * The value returned by a call that might target code of the
		 * program, whose result is unknown only because the analysis did not
		 * compute it precisely (e.g., because calls are analyzed modularly).
		 * This is not a genuine input: the value is determined by the code
		 * of the callee.
		 */
		IMPRECISE_CALL_RESULT,

		/**
		 * Any other unknown value introduced by the semantics of the program.
		 */
		UNKNOWN;
	}

	private final Kind kind;

	private final CodeLocation location;

	private final Type type;

	private final Set<BinaryExpression> constraints;

	private final String description;

	/**
	 * Builds the source.
	 *
	 * @param kind        the kind of the source
	 * @param location    the location where the unknown value is introduced
	 * @param type        the type of the unknown value
	 * @param constraints the constraints that the unknown value satisfies (can
	 *                        be {@code null} or empty if the value is
	 *                        unconstrained)
	 * @param description a human-readable description of the source
	 */
	public InputSource(
			Kind kind,
			CodeLocation location,
			Type type,
			Set<BinaryExpression> constraints,
			String description) {
		Objects.requireNonNull(kind);
		Objects.requireNonNull(location);
		Objects.requireNonNull(type);
		Objects.requireNonNull(description);
		this.kind = kind;
		this.location = location;
		this.type = type;
		this.constraints = constraints == null || constraints.isEmpty()
				? Collections.emptySet()
				: Collections.unmodifiableSet(Set.copyOf(constraints));
		this.description = description;
	}

	/**
	 * Yields the kind of this source.
	 *
	 * @return the kind
	 */
	public Kind getKind() {
		return kind;
	}

	/**
	 * Yields the location where the unknown value is introduced.
	 *
	 * @return the location
	 */
	public CodeLocation getLocation() {
		return location;
	}

	/**
	 * Yields the type of the unknown value.
	 *
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Yields the constraints that the unknown value satisfies, expressed over
	 * the value itself.
	 *
	 * @return the constraints (empty if the value is unconstrained)
	 */
	public Set<BinaryExpression> getConstraints() {
		return constraints;
	}

	/**
	 * Yields a human-readable description of this source.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Builds a {@link ReproducingInput} for a concrete value produced by this
	 * source.
	 *
	 * @param context    the context where the source is evaluated (can be
	 *                       {@code null})
	 * @param occurrence the number of times the source has been evaluated, in
	 *                       the same context, before producing the value
	 * @param value      the textual representation of the value
	 *
	 * @return the input
	 */
	public ReproducingInput toReproducingInput(
			ScopeId<?> context,
			int occurrence,
			String value) {
		return new ReproducingInput(
				location,
				description,
				context == null ? null : context.toString(),
				occurrence,
				value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(constraints, description, kind, location, type);
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
		InputSource other = (InputSource) obj;
		return Objects.equals(constraints, other.constraints)
				&& Objects.equals(description, other.description)
				&& kind == other.kind
				&& Objects.equals(location, other.location)
				&& Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return description + " at " + location;
	}
}
