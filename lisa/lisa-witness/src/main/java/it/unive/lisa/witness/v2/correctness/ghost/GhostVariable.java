package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

@JsonPropertyOrder({ "name", "type", "scope", "initial" })
public final class GhostVariable {

	private final String name;

	private final String type;

	private final GhostScope scope;

	private final Expression initial;

	public GhostVariable(
			String name,
			String type,
			GhostScope scope,
			Expression initial) {
		this.name = Objects.requireNonNull(name, "name");
		this.type = Objects.requireNonNull(type, "type");
		this.scope = Objects.requireNonNull(scope, "scope");
		this.initial = Objects.requireNonNull(initial, "initial");
	}

	@JsonProperty("name")
	public String getName() {
		return name;
	}

	@JsonProperty("type")
	public String getType() {
		return type;
	}

	@JsonProperty("scope")
	public GhostScope getScope() {
		return scope;
	}

	@JsonProperty("initial")
	public Expression getInitial() {
		return initial;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, scope, initial);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		GhostVariable other = (GhostVariable) obj;
		return name.equals(other.name) && type.equals(other.type) && scope == other.scope
				&& initial.equals(other.initial);
	}

	@Override
	public String toString() {
		return type + " " + name + " = " + initial.getValue();
	}
}
