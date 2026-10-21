package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Location;
import it.unive.lisa.witness.v2.metadata.Task;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
		@JsonSubTypes.Type(value = Invariant.class, name = "invariant"),
		@JsonSubTypes.Type(value = FunctionContract.class, name = "contract")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class InvariantSetItem {

	private final Location location;

	private final Format format;

	private final Set<Label> labels;

	protected InvariantSetItem(
			Location location,
			Format format,
			Set<Label> labels) {
		this.location = Objects.requireNonNull(location, "location");
		this.format = Objects.requireNonNull(format, "format");
		this.labels = labels == null || labels.isEmpty() ? Collections.emptySet()
				: Collections.unmodifiableSet(EnumSet.copyOf(labels));
	}

	@JsonProperty("location")
	public Location getLocation() {
		return location;
	}

	@JsonProperty("format")
	public Format getFormat() {
		return format;
	}

	@JsonProperty("labels")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public Set<Label> getLabels() {
		return labels;
	}

	@JsonIgnore
	public boolean isInductive() {
		return labels.contains(Label.INDUCTIVE);
	}

	@SuppressWarnings("deprecation")
	public final void check(
			Task task) {
		task.getLanguage().checkAllows(format, this);
		if (format == Format.ACSL_EXPRESSION)
			throw new InvalidWitnessException("acsl_expression is only allowed in function_return waypoints: " + this);
		checkContent(task);
	}

	protected abstract void checkContent(
			Task task);

	@Override
	public int hashCode() {
		return Objects.hash(location, format, labels);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		InvariantSetItem other = (InvariantSetItem) obj;
		return location.equals(other.location) && format == other.format && labels.equals(other.labels);
	}
}
