package it.unive.lisa.witness.v2.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * The metadata of an entry of a witness. It holds the version of the witness
 * format, a unique identifier of the entry, the time the entry was created, the
 * tool that produced it and the {@link Task} it refers to. Only the format
 * version {@value #FORMAT_VERSION} is supported.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonPropertyOrder({ "format_version", "uuid", "creation_time", "producer", "task" })
public final class Metadata {

	/**
	 * The version of the witness format supported by this package.
	 */
	public static final String FORMAT_VERSION = "2.4";

	/**
	 * The unique identifier of the entry.
	 */
	private final UUID uuid;

	/**
	 * The version of the witness format of the entry.
	 */
	private final String formatVersion;

	/**
	 * The time the entry was created.
	 */
	private final Instant creationTime;

	/**
	 * The tool that produced the entry.
	 */
	private final Producer producer;

	/**
	 * The task the entry refers to.
	 */
	private final Task task;

	/**
	 * Builds the metadata.
	 * 
	 * @param formatVersion the version of the witness format, which must be
	 *                          {@value #FORMAT_VERSION}
	 * @param uuid          the unique identifier of the entry
	 * @param creationTime  the time the entry was created
	 * @param producer      the tool that produced the entry
	 * @param task          the task the entry refers to
	 * 
	 * @throws InvalidWitnessException if the format version is not supported
	 */
	public Metadata(
			String formatVersion,
			UUID uuid,
			Instant creationTime,
			Producer producer,
			Task task) {
		this.formatVersion = Objects.requireNonNull(formatVersion, "formatVersion");
		if (!FORMAT_VERSION.equals(formatVersion))
			throw new InvalidWitnessException(
					"Only format version " + FORMAT_VERSION + " is supported, not " + formatVersion);
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.creationTime = Objects.requireNonNull(creationTime, "creationTime");
		this.producer = Objects.requireNonNull(producer, "producer");
		this.task = Objects.requireNonNull(task, "task");
	}

	/**
	 * Builds the metadata read from a witness. The creation time is written as
	 * an ISO 8601 date and time, with any offset from UTC.
	 * 
	 * @param formatVersion the version of the witness format
	 * @param uuid          the unique identifier of the entry
	 * @param creationTime  the time the entry was created, as it is written in
	 *                          the witness
	 * @param producer      the tool that produced the entry
	 * @param task          the task the entry refers to
	 * 
	 * @return the metadata
	 * 
	 * @throws InvalidWitnessException if the creation time is not an ISO 8601
	 *                                     date and time, or if the format
	 *                                     version is not supported
	 */
	@JsonCreator
	private static Metadata read(
			@JsonProperty(value = "format_version", required = true) String formatVersion,
			@JsonProperty(value = "uuid", required = true) UUID uuid,
			@JsonProperty(value = "creation_time", required = true) String creationTime,
			@JsonProperty(value = "producer", required = true) Producer producer,
			@JsonProperty(value = "task", required = true) Task task) {
		Instant time;
		try {
			time = OffsetDateTime.parse(creationTime).toInstant();
		} catch (DateTimeParseException e) {
			throw new InvalidWitnessException("The creation time is not an ISO 8601 date and time: " + creationTime);
		}
		return new Metadata(formatVersion, uuid, time, producer, task);
	}

	/**
	 * Builds the metadata of a new entry, with the supported format version, a
	 * random identifier and the current time, truncated to seconds.
	 * 
	 * @param producer the tool producing the entry
	 * @param task     the task the entry refers to
	 * 
	 * @return the metadata
	 */
	public static Metadata create(
			Producer producer,
			Task task) {
		return new Metadata(FORMAT_VERSION, UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.SECONDS),
				producer, task);
	}

	/**
	 * Returns the version of the witness format of the entry.
	 * 
	 * @return the format version
	 */
	@JsonProperty("format_version")
	public String getFormatVersion() {
		return formatVersion;
	}

	/**
	 * Returns the unique identifier of the entry.
	 * 
	 * @return the identifier
	 */
	@JsonProperty("uuid")
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Returns the time the entry was created.
	 * 
	 * @return the creation time
	 */
	@JsonIgnore
	public Instant getCreationTime() {
		return creationTime;
	}

	/**
	 * Returns the time the entry was created, as an ISO 8601 date and time in
	 * UTC. This is how the time is written in a witness.
	 * 
	 * @return the creation time, as text
	 */
	@JsonProperty("creation_time")
	public String getCreationTimeText() {
		return DateTimeFormatter.ISO_INSTANT.format(creationTime);
	}

	/**
	 * Returns the tool that produced the entry.
	 * 
	 * @return the producer
	 */
	@JsonProperty("producer")
	public Producer getProducer() {
		return producer;
	}

	/**
	 * Returns the task the entry refers to.
	 * 
	 * @return the task
	 */
	@JsonProperty("task")
	public Task getTask() {
		return task;
	}

	@Override
	public int hashCode() {
		return Objects.hash(formatVersion, uuid, creationTime, producer, task);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Metadata other = (Metadata) obj;
		return formatVersion.equals(other.formatVersion) && uuid.equals(other.uuid)
				&& creationTime.equals(other.creationTime) && producer.equals(other.producer)
				&& task.equals(other.task);
	}
}
