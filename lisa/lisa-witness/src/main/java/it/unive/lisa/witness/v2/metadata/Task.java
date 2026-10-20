package it.unive.lisa.witness.v2.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.DataModel;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "input_files", "input_file_hashes", "specification", "data_model", "language" })
public final class Task {

	private final List<String> inputFiles;

	private final Map<String, String> inputFileHashes;

	private final String specification;

	private final Language language;

	private final DataModel dataModel;

	public Task(
			List<String> inputFiles,
			Map<String, String> inputFileHashes,
			String specification,
			Language language,
			DataModel dataModel) {
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
		if (!this.inputFileHashes.keySet().equals(new HashSet<>(this.inputFiles)))
			throw new InvalidWitnessException("Every input file needs exactly one hash");
	}

	@JsonProperty("input_files")
	public List<String> getInputFiles() {
		return inputFiles;
	}

	@JsonProperty("input_file_hashes")
	public Map<String, String> getInputFileHashes() {
		return inputFileHashes;
	}

	@JsonProperty("specification")
	public String getSpecification() {
		return specification;
	}

	@JsonProperty("data_model")
	public DataModel getDataModel() {
		return dataModel;
	}

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
