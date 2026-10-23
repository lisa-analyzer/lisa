package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import java.nio.file.Path;
import java.util.Objects;

/**
 * An abstract builder of witnesses for a given task. Its implementations are
 * {@link ViolationWitnessBuilder} and {@link CorrectnessWitnessBuilder}. Each
 * call adds a part of the witness, and it is checked when it is made. A builder
 * makes one witness, and it is not thread-safe.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public abstract class WitnessBuilder {

	/**
	 * The task of the witness being built.
	 */
	protected final Task task;

	/**
	 * The tool producing the witness being built.
	 */
	protected final Producer producer;

	/**
	 * Builds the builder.
	 * 
	 * @param task     the task of the witness
	 * @param producer the tool producing the witness
	 */
	protected WitnessBuilder(
			Task task,
			Producer producer) {
		this.task = Objects.requireNonNull(task, "task");
		this.producer = Objects.requireNonNull(producer, "producer");
	}

	/**
	 * Returns a location in the given file, without column and function. The
	 * file can be given with any spelling of its path, and the location uses
	 * its name exactly as it is written in the input files of the task.
	 * 
	 * @param file the file of the location, one of the input files of the task
	 * @param line the line, starting from 1
	 * 
	 * @return the location
	 * 
	 * @throws InvalidWitnessException if the file is not an input file of the
	 *                                     task, or if the line or the column is
	 *                                     less than 1
	 */
	public Location location(
			Path file,
			int line) {
		return new Location(InputFiles.nameOf(task, file), line, null, null);
	}

	/**
	 * Returns a location in the given file, without function. The file can be
	 * given with any spelling of its path, and the location uses its name
	 * exactly as it is written in the input files of the task.
	 * 
	 * @param file   the file of the location, one of the input files of the
	 *                   task
	 * @param line   the line, starting from 1
	 * @param column the column, starting from 1
	 * 
	 * @return the location
	 * 
	 * @throws InvalidWitnessException if the file is not an input file of the
	 *                                     task, or if the line or the column is
	 *                                     less than 1
	 */
	public Location location(
			Path file,
			int line,
			int column) {
		return new Location(InputFiles.nameOf(task, file), line, column, null);
	}

	/**
	 * Returns a location in the given file. The file can be given with any
	 * spelling of its path, and the location uses its name exactly as it is
	 * written in the input files of the task.
	 * 
	 * @param file     the file of the location, one of the input files of the
	 *                     task
	 * @param line     the line, starting from 1
	 * @param column   the column, starting from 1
	 * @param function the function containing the location
	 * 
	 * @return the location
	 * 
	 * @throws InvalidWitnessException if the file is not an input file of the
	 *                                     task, or if the line or the column is
	 *                                     less than 1
	 */
	public Location location(
			Path file,
			int line,
			int column,
			String function) {
		return new Location(InputFiles.nameOf(task, file), line, column, function);
	}
}
