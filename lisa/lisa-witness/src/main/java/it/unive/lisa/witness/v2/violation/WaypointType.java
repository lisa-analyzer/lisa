package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonValue;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;

public enum WaypointType {

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
		}

	};

	private final String name;

	private WaypointType(
			String name) {
		this.name = name;
	}

	abstract void check(
			Action action,
			Constraint constraint);

	public abstract Constraint constraint(
			String value,
			Language language);

	private static String required(
			String value) {
		if (value == null)
			throw new InvalidWitnessException("This waypoint type needs a value");
		return value;
	}

	private static Constraint none(
			String value) {
		if (value != null)
			throw new InvalidWitnessException("This waypoint type has no value: " + value);
		return null;
	}

	@JsonValue
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
