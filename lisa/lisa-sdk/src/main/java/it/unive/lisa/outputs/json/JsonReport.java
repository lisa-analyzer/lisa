package it.unive.lisa.outputs.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unive.lisa.LiSAReport;
import it.unive.lisa.LiSARunInfo;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A report of an executed analysis that can be dumped in json format, and that
 * can be read from a json file.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JsonReport {

	private final Set<JsonMessage> warnings;

	private final Set<JsonMessage> notices;

	private final Set<String> files;

	private final Map<String, String> info;

	private final Map<String, String> configuration;

	private static class NonEmptyFilter {

		@Override
		public boolean equals(
				Object obj) {
			if (obj instanceof SerializableObject) {
				SerializableObject map = (SerializableObject) obj;
				return map.getFields().isEmpty();
			}
			return obj == null;
		}

	}

	@JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = NonEmptyFilter.class)
	private final SerializableObject additionalInfo;

	/**
	 * Builds an empty report.
	 */
	public JsonReport() {
		this(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Map.of(), Map.of(), Map.of());
	}

	/**
	 * Builds the report, starting from the given one.
	 * 
	 * @param report the original report
	 */
	public JsonReport(
			LiSAReport report) {
		this(
				report.getWarnings(),
				report.getNotices(),
				report.getCreatedFiles(),
				report.getRunInfo().toPropertyBag(),
				report.getConfiguration().toPropertyBag(),
				report.getAdditionalInfo());
	}

	private JsonReport(
			Collection<Message> warnings,
			Collection<Message> notices,
			Collection<String> files,
			Map<String, String> info,
			Map<String, String> configuration,
			Map<String, StructuredRepresentation> additionalInfo) {
		this.files = new TreeSet<>(files);
		this.info = info;
		this.configuration = configuration;
		this.warnings = new TreeSet<>();
		for (Message warn : warnings)
			this.warnings.add(new JsonMessage(warn));
		this.notices = new TreeSet<>();
		for (Message notice : notices)
			this.notices.add(new JsonMessage(notice));

		ObjectRepresentation obj = new ObjectRepresentation(additionalInfo);
		this.additionalInfo = obj.toSerializableValue();
	}

	/**
	 * Yields the collection of warning {@link JsonMessage}s contained into this
	 * report.
	 * 
	 * @return the collection of warnings
	 */
	public Collection<JsonMessage> getWarnings() {
		return warnings;
	}

	/**
	 * Yields the collection of notice {@link JsonMessage}s contained into this
	 * report.
	 * 
	 * @return the collection of notices
	 */
	public Collection<JsonMessage> getNotices() {
		return notices;
	}

	/**
	 * Yields the collection of file names contained into this report. These
	 * represents the names of the files that have been created during the
	 * analysis, and should be interpreted as paths relative to the workdir of
	 * the analysis (or the folder containing this report, if this was read from
	 * a file).
	 * 
	 * @return the collection of file names
	 */
	public Collection<String> getFiles() {
		return files;
	}

	/**
	 * Yields the configuration of the analysis, in the form of a property bag.
	 * This corresponds to the object returned by
	 * {@link LiSAConfiguration#toPropertyBag()}.
	 * 
	 * @return the configuration
	 */
	public Map<String, String> getConfiguration() {
		return configuration;
	}

	/**
	 * Yields the information about the analysis ran, in the form of a property
	 * bag. This corresponds to the object returned by
	 * {@link LiSARunInfo#toPropertyBag()}.
	 * 
	 * @return the configuration
	 */
	public Map<String, String> getInfo() {
		return info;
	}

	/**
	 * Yields the additional information about the analysis, in the form of a
	 * property bag. This corresponds to the object returned by
	 * {@link LiSAReport#getAdditionalInfo()}.
	 * 
	 * @return the additional information
	 */
	public SerializableObject getAdditionalInfo() {
		return additionalInfo;
	}

	/**
	 * Dumps this report to the given {@link Writer} instance, serializing it as
	 * a json object.
	 * 
	 * @param writer the writer to write to
	 * 
	 * @throws IOException if some I/O error happens while writing to the writer
	 */
	public void dump(
			Writer writer)
			throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		mapper.writeValue(writer, this);
	}

	/**
	 * Reads a {@link JsonReport} from a {@link Reader} instance, deserializing
	 * it as a json object.
	 * 
	 * @param reader the reader to read from
	 * 
	 * @return the read report
	 * 
	 * @throws IOException if some I/O error happens while reading from the
	 *                         reader
	 */
	public static JsonReport read(
			Reader reader)
			throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(reader, JsonReport.class);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((files == null) ? 0 : files.hashCode());
		result = prime * result + ((warnings == null) ? 0 : warnings.hashCode());
		result = prime * result + ((notices == null) ? 0 : notices.hashCode());
		result = prime * result + ((info == null) ? 0 : info.hashCode());
		result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
		result = prime * result + ((additionalInfo == null) ? 0 : additionalInfo.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonReport other = (JsonReport) obj;
		if (files == null) {
			if (other.files != null)
				return false;
		} else if (!files.equals(other.files))
			return false;
		if (warnings == null) {
			if (other.warnings != null)
				return false;
		} else if (!warnings.equals(other.warnings))
			return false;
		if (notices == null) {
			if (other.notices != null)
				return false;
		} else if (!notices.equals(other.notices))
			return false;
		if (info == null) {
			if (other.info != null)
				return false;
		} else if (!info.equals(other.info))
			return false;
		if (configuration == null) {
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		if (additionalInfo == null) {
			if (other.additionalInfo != null)
				return false;
		} else if (!additionalInfo.equals(other.additionalInfo))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JsonReport [warnings="
				+ warnings
				+ ", notices="
				+ notices
				+ ", files="
				+ files
				+ ", info="
				+ info
				+ ", configuration="
				+ configuration
				+ ", additionalInfo="
				+ additionalInfo
				+ "]";
	}

	/**
	 * A message that is ready to dump into a {@link JsonReport}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class JsonMessage
			implements
			Comparable<JsonMessage> {

		private String message;

		/**
		 * Builds an empty message with no message.
		 */
		public JsonMessage() {
			this.message = null;
		}

		/**
		 * Builds the message, cloning the information from the given
		 * {@link Message}.
		 * 
		 * @param message the message to clone
		 */
		public JsonMessage(
				Message message) {
			this.message = message.toString();
		}

		/**
		 * Yields the message of this message.
		 * 
		 * @return the message
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * Sets the message of this message.
		 * 
		 * @param message the message
		 */
		public void setMessage(
				String message) {
			this.message = message;
		}

		@Override
		public String toString() {
			return getMessage();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((message == null) ? 0 : message.hashCode());
			return result;
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			JsonMessage other = (JsonMessage) obj;
			if (message == null) {
				if (other.message != null)
					return false;
			} else if (!message.equals(other.message))
				return false;
			return true;
		}

		@Override
		public int compareTo(
				JsonMessage o) {
			return message.compareTo(o.message);
		}

	}

}
