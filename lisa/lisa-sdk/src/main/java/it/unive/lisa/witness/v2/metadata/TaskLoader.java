package it.unive.lisa.witness.v2.metadata;

import it.unive.lisa.witness.v2.DataModel;
import it.unive.lisa.witness.v2.Language;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A loader of tasks from the files given to a verifier. Directories are
 * expanded into the source files of the language they contain. Each file is
 * listed once with the spelling of its path as it is given, and hashed with
 * SHA-256.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class TaskLoader {

	/**
	 * The extensions of the source files of each language.
	 */
	private static final Map<Language, List<String>> EXTENSIONS = Map.of(
			Language.C, List.of(".c", ".h", ".i"),
			Language.JAVA, List.of(".java"),
			Language.PYTHON, List.of(".py"));

	/**
	 * This class only has static methods, and it is never instantiated.
	 */
	private TaskLoader() {
	}

	/**
	 * Loads the task made of the given inputs and property file. A file given
	 * directly is always kept, while a directory is expanded into the source
	 * files of the language it contains, in alphabetical order. A file reached
	 * twice, for instance through a directory and directly, is listed once. The
	 * specification is the content of the property file, without leading and
	 * trailing blanks.
	 * 
	 * @param inputs       the files and directories given to the verifier
	 * @param propertyFile the file containing the specification
	 * @param language     the language of the program
	 * @param dataModel    the data model of the program, or {@code null} if the
	 *                         language has none
	 * 
	 * @return the task
	 * 
	 * @throws IOException if a file cannot be read
	 */
	public static Task load(
			List<Path> inputs,
			Path propertyFile,
			Language language,
			DataModel dataModel)
			throws IOException {
		List<Path> files = new ArrayList<>();
		for (Path input : inputs)
			files.addAll(sourceFiles(input, language));
		Map<String, String> hashes = new LinkedHashMap<>();
		Set<Path> seen = new HashSet<>();
		for (Path file : files)
			if (seen.add(file.toAbsolutePath().normalize()))
				hashes.put(file.toString(), sha256(file));
		String specification = Files.readString(propertyFile, StandardCharsets.UTF_8).strip();
		return new Task(new ArrayList<>(hashes.keySet()), hashes, specification, language, dataModel);
	}

	/**
	 * Returns the files of the given input. A file is yielded as it is, while a
	 * directory yields the source files of the language it contains, in
	 * alphabetical order.
	 * 
	 * @param input    a file or a directory
	 * @param language the language of the program
	 * 
	 * @return the files of the input
	 * 
	 * @throws IOException if the directory cannot be read
	 */
	private static List<Path> sourceFiles(
			Path input,
			Language language)
			throws IOException {
		if (!Files.isDirectory(input))
			return List.of(input);
		try (Stream<Path> walk = Files.walk(input)) {
			return walk.filter(Files::isRegularFile).filter(f -> isSource(f, language)).sorted()
					.collect(Collectors.toList());
		}
	}

	/**
	 * Returns whether the given file is a source file of the given language,
	 * according to its extension.
	 * 
	 * @param file     the file
	 * @param language the language
	 * 
	 * @return {@code true} if the file is a source file of the language
	 */
	private static boolean isSource(
			Path file,
			Language language) {
		String name = file.getFileName().toString();
		for (String extension : EXTENSIONS.get(language))
			if (name.endsWith(extension))
				return true;
		return false;
	}

	/**
	 * Returns the SHA-256 hash of the content of the given file.
	 * 
	 * @param file the file
	 * 
	 * @return the hash, as 64 lowercase hexadecimal digits
	 * 
	 * @throws IOException if the file cannot be read
	 */
	private static String sha256(
			Path file)
			throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
		StringBuilder hex = new StringBuilder();
		for (byte b : digest.digest(Files.readAllBytes(file)))
			hex.append(String.format("%02x", b));
		return hex.toString();
	}
}
