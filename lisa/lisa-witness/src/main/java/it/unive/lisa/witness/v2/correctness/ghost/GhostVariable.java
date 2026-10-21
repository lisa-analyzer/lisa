package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

/**
 * A ghost variable, declared by the ghost instrumentation of a correctness
 * witness and used by its invariants.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonPropertyOrder({ "name", "type", "scope", "initial" })
public final class GhostVariable {

	/**
	 * The name of the variable.
	 */
	private final String name;

	/**
	 * The C type of the variable.
	 */
	private final String type;

	/**
	 * The scope of the variable.
	 */
	private final GhostScope scope;

	/**
	 * The initial value of the variable.
	 */
	private final Expression initial;

	/**
	 * Builds the variable.
	 * 
	 * @param name    the name of the variable
	 * @param type    the C type of the variable
	 * @param scope   the scope of the variable
	 * @param initial the initial value of the variable
	 */
	@JsonCreator
	public GhostVariable(
			@JsonProperty(value = "name", required = true) String name,
			@JsonProperty(value = "type", required = true) String type,
			@JsonProperty(value = "scope", required = true) GhostScope scope,
			@JsonProperty(value = "initial", required = true) Expression initial) {
		this.name = Objects.requireNonNull(name, "name");
		this.type = Objects.requireNonNull(type, "type");
		this.scope = Objects.requireNonNull(scope, "scope");
		this.initial = Objects.requireNonNull(initial, "initial");
	}

	/**
	 * Returns the name of the variable.
	 * 
	 * @return the name
	 */
	@JsonProperty("name")
	public String getName() {
		return name;
	}

	/**
	 * Returns the C type of the variable.
	 * 
	 * @return the type
	 */
	@JsonProperty("type")
	public String getType() {
		return type;
	}

	/**
	 * Returns the scope of the variable.
	 * 
	 * @return the scope
	 */
	@JsonProperty("scope")
	public GhostScope getScope() {
		return scope;
	}

	/**
	 * Returns the initial value of the variable.
	 * 
	 * @return the initial value
	 */
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
