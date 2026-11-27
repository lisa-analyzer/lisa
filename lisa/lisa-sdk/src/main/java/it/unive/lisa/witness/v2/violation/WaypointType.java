package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonValue;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;

/**
 * The types of the waypoints of a violation witness. Each type has its own
 * rules on the action and the constraint of its waypoints.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum WaypointType {

	/**
	 * A point where an expression holds. Its constraint is the expression, in
	 * the plain format of the language.
	 */
	ASSUMPTION("assumption") {

		@Override
		public Constraint constraint(
				String value,
				Language language) {
			return Constraint.of(required(value), language.getExpressionFormat());
		}

		@Override
		void check(
				Action action,
				Constraint constraint) {
			if (constraint == null || constraint.getFormat() == null || constraint.getFormat().isExtended())
				throw new InvalidWitnessException(
						"An assumption waypoint needs a constraint with a non-extended format");
		}

	},

	/**
	 * The point where the specification is violated. It is always followed, and
	 * it has no constraint.
	 */
	TARGET("target") {

		@Override
		public Constraint constraint(
				String value,
				Language language) {
			return none(value);
		}

		@Override
		void check(
				Action action,
				Constraint constraint) {
			if (action != Action.FOLLOW)
				throw new InvalidWitnessException("A target waypoint must have action follow");
			if (constraint != null)
				throw new InvalidWitnessException("A target waypoint has no constraint");
		}

	},

	/**
	 * The entry into the function called at a location. It has no constraint.
	 */
	FUNCTION_ENTER("function_enter") {

		@Override
		public Constraint constraint(
				String value,
				Language language) {
			return none(value);
		}

		@Override
		void check(
				Action action,
				Constraint constraint) {
			if (constraint != null)
				throw new InvalidWitnessException("A function_enter waypoint has no constraint");
		}

	},

	/**
	 * The return from the function called at a location. Its constraint is on
	 * the returned value, in an extended format, such as {@code \result == 0}.
	 */
	FUNCTION_RETURN("function_return") {

		@Override
		public Constraint constraint(
				String value,
				Language language) {
			return Constraint.of(required(value), language.getExtendedExpressionFormat());
		}

		@Override
		void check(
				Action action,
				Constraint constraint) {
			if (constraint == null || constraint.getFormat() == null || !constraint.getFormat().isExtended())
				throw new InvalidWitnessException(
						"A function_return waypoint needs a constraint with an extended format (ext_*)");
		}

	},

	/**
	 * The choice of a branch of a condition. Its constraint is {@code true} or
	 * {@code false}, without format.
	 */
	BRANCHING("branching") {

		@Override
		public Constraint constraint(
				String value,
				Language language) {
			return Constraint.withoutFormat(required(value));
		}

		@Override
		void check(
				Action action,
				Constraint constraint) {
			if (constraint == null || constraint.getFormat() != null)
				throw new InvalidWitnessException("A branching waypoint needs a constraint without a format");
			if (!constraint.getValue().equals("true") && !constraint.getValue().equals("false"))
				throw new InvalidWitnessException(
						"The constraint of a branching waypoint is true or false, not " + constraint.getValue());
		}

	};

	/**
	 * The name of this type, as it is written in a witness.
	 */
	private final String name;

	/**
	 * Builds the type.
	 * 
	 * @param name the name of the type, as it is written in a witness
	 */
	private WaypointType(
			String name) {
		this.name = name;
	}

	/**
	 * Checks the action and the constraint of a waypoint of this type.
	 * 
	 * @param action     the action of the waypoint
	 * @param constraint the constraint of the waypoint, or {@code null} if it
	 *                       has none
	 * 
	 * @throws InvalidWitnessException if the action or the constraint are not
	 *                                     allowed for this type
	 */
	abstract void check(
			Action action,
			Constraint constraint);

	/**
	 * Builds the constraint of a waypoint of this type from the given value, in
	 * the format this type requires for the given language.
	 * 
	 * @param value    the value of the constraint, or {@code null} for the
	 *                     types without constraint
	 * @param language the language of the program
	 * 
	 * @return the constraint, or {@code null} for the types without constraint
	 * 
	 * @throws InvalidWitnessException if a value is missing for a type that
	 *                                     needs it, or given for a type that
	 *                                     has none
	 */
	public abstract Constraint constraint(
			String value,
			Language language);

	/**
	 * Returns the given value, which must be given.
	 * 
	 * @param value the value
	 * 
	 * @return the value
	 * 
	 * @throws InvalidWitnessException if the value is {@code null}
	 */
	private static String required(
			String value) {
		if (value == null)
			throw new InvalidWitnessException("This waypoint type needs a value");
		return value;
	}

	/**
	 * Returns no constraint, after checking that no value is given.
	 * 
	 * @param value the value, which must be {@code null}
	 * 
	 * @return {@code null}
	 * 
	 * @throws InvalidWitnessException if a value is given
	 */
	private static Constraint none(
			String value) {
		if (value != null)
			throw new InvalidWitnessException("This waypoint type has no value: " + value);
		return null;
	}

	/**
	 * Returns the name of this type, as it is written in a witness.
	 * 
	 * @return the name of this type
	 */
	@JsonValue
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
