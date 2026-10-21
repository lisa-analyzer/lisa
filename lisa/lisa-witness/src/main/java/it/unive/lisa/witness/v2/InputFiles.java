package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.metadata.Task;
import java.nio.file.Path;

// the name of a file in the input files of a task, written exactly as the
// task writes it; relative names are resolved against the working directory
// and symbolic links are not followed, as in TaskLoader
final class InputFiles {

	private InputFiles() {
	}

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
