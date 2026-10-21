package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.metadata.Task;
import java.nio.file.Path;

/**
 * The names of the input files of a task. A file is found by comparing absolute
 * and normalized paths, and its name is yielded exactly as the task writes it.
 * Relative names are resolved against the working directory, and symbolic links
 * are not followed, as in {@link TaskLoader}.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
final class InputFiles {

	/**
	 * This class only has static methods, and it is never instantiated.
	 */
	private InputFiles() {
	}

	/**
	 * Returns the name of the given file, as it is written in the input files
	 * of the given task.
	 * 
	 * @param task the task
	 * @param file the file, with any spelling of its path
	 * 
	 * @return the name of the file in the input files of the task
	 * 
	 * @throws InvalidWitnessException if the file is not an input file of the
	 *                                     task
	 */
	static String nameOf(
			Task task,
			Path file) {
		Path wanted = file.toAbsolutePath().normalize();
		for (String input : task.getInputFiles())
			if (Path.of(input).toAbsolutePath().normalize().equals(wanted))
				return input;
		throw new InvalidWitnessException(file + " is not an input file of the task: " + task.getInputFiles());
	}

}
