package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonValue;
import it.unive.lisa.witness.v2.InvalidWitnessException;

public enum WaypointType {

	ASSUMPTION("assumption") {

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
		void check(
				Action action,
				Constraint constraint) {
			if (constraint != null)
				throw new InvalidWitnessException("A function_enter waypoint has no constraint");
		}

	},

	FUNCTION_RETURN("function_return") {

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

	@JsonValue
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
