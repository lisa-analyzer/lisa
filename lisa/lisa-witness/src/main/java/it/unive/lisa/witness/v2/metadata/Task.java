package it.unive.lisa.witness.v2.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.DataModel;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The verification task an entry of a witness refers to. A task is made of the
 * input files of the program, written exactly as they were given to the
 * verifier, with the SHA-256 hash of each of them, the specification to verify
 * and the language of the program. C tasks also have a {@link DataModel}.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "input_files", "input_file_hashes", "specification", "data_model", "language" })
public final class Task {

	/**
	 * The text of a SHA-256 hash, made of 64 hexadecimal digits.
	 */
	private static final Pattern SHA_256 = Pattern.compile("[0-9a-fA-F]{64}");

	/**
	 * The input files of the program, as they were given to the verifier.
	 */
	private final List<String> inputFiles;

	/**
	 * The SHA-256 hash of each input file.
	 */
	private final Map<String, String> inputFileHashes;

	/**
	 * The specification to verify.
	 */
	private final String specification;

	/**
	 * The language of the program.
	 */
	private final Language language;

	/**
	 * The data model of the program, or {@code null} if the language has none.
	 */
	private final DataModel dataModel;

	/**
	 * Builds the task.
	 * 
	 * @param inputFiles      the input files of the program, as they were given
	 *                            to the verifier
	 * @param inputFileHashes the SHA-256 hash of each input file
	 * @param specification   the specification to verify
	 * @param language        the language of the program
	 * @param dataModel       the data model of the program, or {@code null} if
	 *                            the language has none
	 * 
	 * @throws InvalidWitnessException if there is no input file, if a file is
	 *                                     listed twice, if a file has no hash
	 *                                     or an invalid one, or if the data
	 *                                     model is missing for C or given for
	 *                                     another language
	 */
	@JsonCreator
	public Task(
			@JsonProperty(value = "input_files", required = true) List<String> inputFiles,
			@JsonProperty(value = "input_file_hashes", required = true) Map<String, String> inputFileHashes,
			@JsonProperty(value = "specification", required = true) String specification,
			@JsonProperty(value = "language", required = true) Language language,
			@JsonProperty("data_model") DataModel dataModel) {
		this.inputFiles = List.copyOf(Objects.requireNonNull(inputFiles, "inputFiles"));
		this.inputFileHashes = Collections.unmodifiableMap(
				new LinkedHashMap<>(Objects.requireNonNull(inputFileHashes, "inputFileHashes")));
		this.specification = Objects.requireNonNull(specification, "specification");
		this.language = Objects.requireNonNull(language, "language");
		this.dataModel = dataModel;
		if (language.requiresDataModel() && dataModel == null)
			throw new InvalidWitnessException("A data model is required for " + language);
		if (!language.requiresDataModel() && dataModel != null)
			throw new InvalidWitnessException("A data model is not allowed for " + language);
		if (this.inputFiles.isEmpty())
			throw new InvalidWitnessException("A task needs at least one input file");
		// two spellings of one relative path are the same file,
		// while an absolute and a relative path are not compared,
		// since the working directory of the producer is unknown
		// (TaskLoader compares them)
		Set<Path> distinct = new HashSet<>();
		for (String file : this.inputFiles)
			if (!distinct.add(Path.of(file).normalize()))
				throw new InvalidWitnessException("An input file is listed more than once: " + file);
		if (!this.inputFileHashes.keySet().equals(new HashSet<>(this.inputFiles)))
			throw new InvalidWitnessException("Every input file needs exactly one hash");
		for (Map.Entry<String, String> hash : this.inputFileHashes.entrySet())
			if (!SHA_256.matcher(hash.getValue()).matches())
				throw new InvalidWitnessException(
						"The hash of " + hash.getKey() + " is not a SHA-256 hash: " + hash.getValue());
	}

	/**
	 * Returns the input files of the program, as they were given to the
	 * verifier.
	 * 
	 * @return the input files, which cannot be modified
	 */
	@JsonProperty("input_files")
	public List<String> getInputFiles() {
		return inputFiles;
	}

	/**
	 * Returns the SHA-256 hash of each input file.
	 * 
	 * @return the hashes, which cannot be modified
	 */
	@JsonProperty("input_file_hashes")
	public Map<String, String> getInputFileHashes() {
		return inputFileHashes;
	}

	/**
	 * Returns the specification to verify.
	 * 
	 * @return the specification
	 */
	@JsonProperty("specification")
	public String getSpecification() {
		return specification;
	}

	/**
	 * Returns the data model of the program.
	 * 
	 * @return the data model, or {@code null} if the language has none
	 */
	@JsonProperty("data_model")
	public DataModel getDataModel() {
		return dataModel;
	}

	/**
	 * Returns the language of the program.
	 * 
	 * @return the language
	 */
	@JsonProperty("language")
	public Language getLanguage() {
		return language;
	}

	@Override
	public int hashCode() {
		return Objects.hash(inputFiles, inputFileHashes, specification, language, dataModel);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Task other = (Task) obj;
		return inputFiles.equals(other.inputFiles) && inputFileHashes.equals(other.inputFileHashes)
				&& specification.equals(other.specification) && language == other.language
				&& dataModel == other.dataModel;
	}

	@Override
	public String toString() {
		return language + " task " + inputFiles + ", " + specification;
	}
}
