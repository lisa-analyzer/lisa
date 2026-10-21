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

public final class TaskLoader {

	private static final Map<Language, List<String>> EXTENSIONS = Map.of(
			Language.C, List.of(".c", ".h", ".i"),
			Language.JAVA, List.of(".java"),
			Language.PYTHON, List.of(".py"));

	private TaskLoader() {
	}

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

	// a file given directly is always kept; in a directory, only the source
	// files of the language
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

	private static boolean isSource(
			Path file,
			Language language) {
		String name = file.getFileName().toString();
		for (String extension : EXTENSIONS.get(language))
			if (name.endsWith(extension))
				return true;
		return false;
	}

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
