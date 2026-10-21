package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import java.nio.file.Path;
import java.util.Objects;

// a builder makes one witness and is not thread-safe
public abstract class WitnessBuilder {

	protected final Task task;

	protected final Producer producer;

	protected WitnessBuilder(
			Task task,
			Producer producer) {
		this.task = Objects.requireNonNull(task, "task");
		this.producer = Objects.requireNonNull(producer, "producer");
	}

	public Location location(
			Path file,
			int line) {
		return new Location(InputFiles.nameOf(task, file), line, null, null);
	}

	public Location location(
			Path file,
			int line,
			int column) {
		return new Location(InputFiles.nameOf(task, file), line, column, null);
	}

	public Location location(
			Path file,
			int line,
			int column,
			String function) {
		return new Location(InputFiles.nameOf(task, file), line, column, function);
	}
}
