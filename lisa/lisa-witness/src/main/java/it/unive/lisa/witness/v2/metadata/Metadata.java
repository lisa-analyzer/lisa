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

@JsonPropertyOrder({ "format_version", "uuid", "creation_time", "producer", "task" })
public final class Metadata {

	public static final String FORMAT_VERSION = "2.4";

	private final UUID uuid;

	private final String formatVersion;

	private final Instant creationTime;

	private final Producer producer;

	private final Task task;

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

	public static Metadata create(
			Producer producer,
			Task task) {
		return new Metadata(FORMAT_VERSION, UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.SECONDS),
				producer, task);
	}

	@JsonProperty("format_version")
	public String getFormatVersion() {
		return formatVersion;
	}

	@JsonProperty("uuid")
	public UUID getUuid() {
		return uuid;
	}

	@JsonIgnore
	public Instant getCreationTime() {
		return creationTime;
	}

	@JsonProperty("creation_time")
	public String getCreationTimeText() {
		return DateTimeFormatter.ISO_INSTANT.format(creationTime);
	}

	@JsonProperty("producer")
	public Producer getProducer() {
		return producer;
	}

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
