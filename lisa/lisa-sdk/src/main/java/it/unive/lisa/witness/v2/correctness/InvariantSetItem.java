package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unive.lisa.witness.v2.Entry;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Location;
import it.unive.lisa.witness.v2.metadata.Task;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * An abstract item of an invariant set. Its implementations are
 * {@link Invariant} and {@link FunctionContract}. Every item has a location, a
 * format and optional {@link Label}s. Its format must be allowed for the
 * language of the task, and the deprecated {@link Format#ACSL_EXPRESSION} is
 * never allowed.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
		@JsonSubTypes.Type(value = Invariant.class, name = "invariant"),
		@JsonSubTypes.Type(value = FunctionContract.class, name = "contract")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class InvariantSetItem {

	/**
	 * The location of the item.
	 */
	private final Location location;

	/**
	 * The format of the expressions of the item.
	 */
	private final Format format;

	/**
	 * The labels of the item, which can be empty.
	 */
	private final Set<Label> labels;

	/**
	 * Builds the item.
	 * 
	 * @param location the location of the item
	 * @param format   the format of the expressions of the item
	 * @param labels   the labels of the item, or {@code null} if it has none
	 * 
	 * @throws InvalidWitnessException if a label is {@code null}
	 */
	protected InvariantSetItem(
			Location location,
			Format format,
			Set<Label> labels) {
		this.location = Objects.requireNonNull(location, "location");
		this.format = Objects.requireNonNull(format, "format");
		if (labels != null && labels.stream().anyMatch(Objects::isNull))
			throw new InvalidWitnessException("A label cannot be null");
		this.labels = labels == null || labels.isEmpty() ? Collections.emptySet()
				: Collections.unmodifiableSet(EnumSet.copyOf(labels));
	}

	/**
	 * Returns the location of the item.
	 * 
	 * @return the location
	 */
	@JsonProperty("location")
	public Location getLocation() {
		return location;
	}

	/**
	 * Returns the format of the expressions of the item.
	 * 
	 * @return the format
	 */
	@JsonProperty("format")
	public Format getFormat() {
		return format;
	}

	/**
	 * Returns the labels of the item.
	 * 
	 * @return the labels, which can be empty and cannot be modified
	 */
	@JsonProperty("labels")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public Set<Label> getLabels() {
		return labels;
	}

	/**
	 * Returns whether the item is labelled as inductive.
	 * 
	 * @return {@code true} if the item has the label {@link Label#INDUCTIVE}
	 */
	@JsonIgnore
	public boolean isInductive() {
		return labels.contains(Label.INDUCTIVE);
	}

	/**
	 * Checks the item against the given task. Its location must be in an input
	 * file of the task, its format must be allowed for the language of the
	 * task, and the rules of each kind of item are checked by
	 * {@link #checkContent(Task)}.
	 * 
	 * @param task the task of the item
	 * 
	 * @throws InvalidWitnessException if the item breaks a rule of the format
	 *                                     for the task
	 */
	@SuppressWarnings("deprecation")
	public final void check(
			Task task) {
		Entry.checkLocation(task, location);
		task.getLanguage().checkAllows(format, this);
		if (format == Format.ACSL_EXPRESSION)
			throw new InvalidWitnessException("acsl_expression is only allowed in function_return waypoints: " + this);
		checkContent(task);
	}

	/**
	 * Checks the rules of this kind of item against the given task.
	 * 
	 * @param task the task of the item
	 * 
	 * @throws InvalidWitnessException if the item breaks a rule of the format
	 *                                     for the task
	 */
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
