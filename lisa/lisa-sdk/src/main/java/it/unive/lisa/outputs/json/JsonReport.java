package it.unive.lisa.outputs.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unive.lisa.LiSAReport;
import it.unive.lisa.LiSARunInfo;
import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.outputs.messages.MessageWithLocation;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

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
	 * A message that is ready to dump into a {@link JsonReport}. Messages are
	 * structured, containing the location they have been reported on (if any),
	 * the signature of the cfg they are related to (if any), a textual
	 * representation of the target they have been reported on (if any, and if
	 * it is not a cfg), the context they have been reported on and its hash (if
	 * any), the kind of the target, the text of the message, whether the
	 * message is definite, refuted, or possible, the inputs reproducing the reported
	 * behavior (if any), and the explanation of the outcome of its verification
	 * (if any). The explanation is informative only: it is not part of the
	 * identity of the message, and it is ignored when comparing messages.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@JsonPropertyOrder({ "location", "cfg", "text", "context", "contextHash", "targetKind", "message", "definite",
			"refuted", "reproducingInputs", "explanation" })
	public static class JsonMessage
			implements
			Comparable<JsonMessage> {

		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String location;

		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String cfg;

		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String text;

		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String context;

		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String contextHash;

		private String targetKind;

		private String message;

		private boolean definite;

		@JsonInclude(JsonInclude.Include.NON_DEFAULT)
		private boolean refuted;

		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		private List<JsonReproducingInput> reproducingInputs;

		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String explanation;

		/**
		 * Builds an empty possible message, with no location, cfg, target,
		 * text, context, context hash, reproducing inputs, and explanation.
		 */
		public JsonMessage() {
			this.location = null;
			this.cfg = null;
			this.text = null;
			this.context = null;
			this.contextHash = null;
			this.targetKind = null;
			this.message = null;
			this.definite = false;
			this.refuted = false;
			this.reproducingInputs = new ArrayList<>();
			this.explanation = null;
		}

		/**
		 * Builds a message starting from the given one.
		 * 
		 * @param message the message to represent
		 */
		public JsonMessage(
				Message message) {
			this.location = message instanceof MessageWithLocation
					? ((MessageWithLocation) message).getLocation().toString()
					: null;
			this.cfg = message.getCFGSignature();
			this.text = message.getTargetText();
			this.context = message.getContext() == null ? null : message.getContext().toString();
			this.contextHash = message.getContextHash();
			this.targetKind = message.getTag();
			this.message = message.getMessage();
			this.definite = message.isDefinite();
			this.refuted = message.isRefuted();
			this.reproducingInputs = new ArrayList<>();
			for (ReproducingInput input : message.getReproducingInputs())
				this.reproducingInputs.add(new JsonReproducingInput(input));
			this.explanation = message.getExplanation();
		}

		/**
		 * Yields the location this message has been reported on, if any.
		 * 
		 * @return the location, or {@code null}
		 */
		public String getLocation() {
			return location;
		}

		/**
		 * Sets the location this message has been reported on.
		 * 
		 * @param location the location (can be {@code null})
		 */
		public void setLocation(
				String location) {
			this.location = location;
		}

		/**
		 * Yields the signature of the cfg this message is related to, if any.
		 *
		 * @return the signature of the cfg, or {@code null}
		 */
		public String getCfg() {
			return cfg;
		}

		/**
		 * Sets the signature of the cfg this message is related to.
		 *
		 * @param cfg the signature of the cfg (can be {@code null})
		 */
		public void setCfg(
				String cfg) {
			this.cfg = cfg;
		}

		/**
		 * Yields the textual representation of the target this message has been
		 * reported on, if any.
		 *
		 * @return the textual representation of the target, or {@code null}
		 */
		public String getText() {
			return text;
		}

		/**
		 * Sets the textual representation of the target this message has been
		 * reported on.
		 *
		 * @param text the textual representation of the target (can be
		 *                 {@code null})
		 */
		public void setText(
				String text) {
			this.text = text;
		}

		/**
		 * Yields the textual representation of the context this message has
		 * been reported on, if any.
		 *
		 * @return the context, or {@code null}
		 */
		public String getContext() {
			return context;
		}

		/**
		 * Sets the textual representation of the context this message has been
		 * reported on.
		 *
		 * @param context the context (can be {@code null})
		 */
		public void setContext(
				String context) {
			this.context = context;
		}

		/**
		 * Yields the hash of the context this message has been reported on, if
		 * any.
		 *
		 * @return the hash of the context, or {@code null}
		 */
		public String getContextHash() {
			return contextHash;
		}

		/**
		 * Sets the hash of the context this message has been reported on.
		 *
		 * @param contextHash the hash of the context (can be {@code null})
		 */
		public void setContextHash(
				String contextHash) {
			this.contextHash = contextHash;
		}

		/**
		 * Yields the kind of target this message has been reported on(see
		 * {@link Message#getTag()}).
		 * 
		 * @return the kind of target
		 */
		public String getTargetKind() {
			return targetKind;
		}

		/**
		 * Sets the kind of target this message has been reported on.
		 * 
		 * @param targetKind the kind of target
		 */
		public void setTargetKind(
				String targetKind) {
			this.targetKind = targetKind;
		}

		/**
		 * Yields the text of this message.
		 * 
		 * @return the text
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * Sets the text of this message.
		 * 
		 * @param message the text
		 */
		public void setMessage(
				String message) {
			this.message = message;
		}

		/**
		 * Yields whether or not this message is definite.
		 * 
		 * @return {@code true} if this message is definite, {@code false} if it
		 *             is possible
		 */
		public boolean isDefinite() {
			return definite;
		}

		/**
		 * Sets whether or not this message is definite.
		 * 
		 * @param definite {@code true} if this message is definite,
		 *                     {@code false} if it is possible
		 */
		public void setDefinite(
				boolean definite) {
			this.definite = definite;
		}

		/**
		 * Yields whether or not this message is refuted, that is, if the
		 * reported behavior happens in no execution of the program.
		 * 
		 * @return {@code true} if this message is refuted
		 */
		public boolean isRefuted() {
			return refuted;
		}

		/**
		 * Sets whether or not this message is refuted.
		 * 
		 * @param refuted {@code true} if this message is refuted
		 */
		public void setRefuted(
				boolean refuted) {
			this.refuted = refuted;
		}

		/**
		 * Yields the inputs reproducing the behavior reported by this message.
		 * 
		 * @return the inputs (empty if none are known)
		 */
		public List<JsonReproducingInput> getReproducingInputs() {
			return reproducingInputs;
		}

		/**
		 * Sets the inputs reproducing the behavior reported by this message.
		 * 
		 * @param reproducingInputs the inputs (can be {@code null} if none are
		 *                              known)
		 */
		public void setReproducingInputs(
				List<JsonReproducingInput> reproducingInputs) {
			this.reproducingInputs = reproducingInputs == null ? new ArrayList<>() : reproducingInputs;
		}

		/**
		 * Yields the explanation of the outcome of the verification of this
		 * message, if any.
		 * 
		 * @return the explanation, or {@code null}
		 */
		public String getExplanation() {
			return explanation;
		}

		/**
		 * Sets the explanation of the outcome of the verification of this
		 * message.
		 * 
		 * @param explanation the explanation (can be {@code null} if there is
		 *                        none)
		 */
		public void setExplanation(
				String explanation) {
			this.explanation = explanation;
		}

		@Override
		public String toString() {
			StringBuilder res = new StringBuilder();
			if (location != null)
				res.append("[").append(location).append("] ");
			if (cfg != null)
				res.append("on '").append(cfg).append("' ");
			if (text != null)
				res.append("at '").append(text).append("' ");
			if (context != null)
				res.append("in context ").append(context).append(" ");
			if (targetKind != null)
				res.append("[").append(targetKind).append("] ");
			if (definite)
				res.append("[DEFINITE] ");
			if (refuted)
				res.append("[REFUTED] ");
			res.append(message);
			if (!reproducingInputs.isEmpty())
				res.append(" ").append(reproducingInputs);
			return res.toString();
		}

		@Override
		public int hashCode() {
			return Objects.hash(cfg, context, contextHash, definite, refuted, location, message, reproducingInputs,
					targetKind, text);
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
			return definite == other.definite
					&& refuted == other.refuted
					&& Objects.equals(location, other.location)
					&& Objects.equals(cfg, other.cfg)
					&& Objects.equals(text, other.text)
					&& Objects.equals(context, other.context)
					&& Objects.equals(contextHash, other.contextHash)
					&& Objects.equals(message, other.message)
					&& Objects.equals(reproducingInputs, other.reproducingInputs)
					&& Objects.equals(targetKind, other.targetKind);
		}

		@Override
		public int compareTo(
				JsonMessage o) {
			int cmp;
			if ((cmp = StringUtils.compare(location, o.location)) != 0)
				return cmp;
			if ((cmp = StringUtils.compare(cfg, o.cfg)) != 0)
				return cmp;
			if ((cmp = StringUtils.compare(text, o.text)) != 0)
				return cmp;
			if ((cmp = StringUtils.compare(context, o.context)) != 0)
				return cmp;
			if ((cmp = StringUtils.compare(contextHash, o.contextHash)) != 0)
				return cmp;
			if ((cmp = StringUtils.compare(targetKind, o.targetKind)) != 0)
				return cmp;
			if ((cmp = StringUtils.compare(message, o.message)) != 0)
				return cmp;
			if ((cmp = Boolean.compare(definite, o.definite)) != 0)
				return cmp;
			if ((cmp = Boolean.compare(refuted, o.refuted)) != 0)
				return cmp;
			if ((cmp = Integer.compare(reproducingInputs.size(), o.reproducingInputs.size())) != 0)
				return cmp;
			for (int i = 0; i < reproducingInputs.size(); i++)
				if ((cmp = reproducingInputs.get(i).compareTo(o.reproducingInputs.get(i))) != 0)
					return cmp;
			return 0;
		}

	}

	/**
	 * A {@link ReproducingInput} that is ready to dump into a
	 * {@link JsonReport}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@JsonPropertyOrder({ "location", "source", "context", "occurrence", "value" })
	public static class JsonReproducingInput
			implements
			Comparable<JsonReproducingInput> {

		private String location;

		private String source;

		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String context;

		private int occurrence;

		private String value;

		/**
		 * Builds an empty input.
		 */
		public JsonReproducingInput() {
			this.location = null;
			this.source = null;
			this.context = null;
			this.occurrence = 0;
			this.value = null;
		}

		/**
		 * Builds an input starting from the given one.
		 * 
		 * @param input the input to represent
		 */
		public JsonReproducingInput(
				ReproducingInput input) {
			this.location = input.getLocation().toString();
			this.source = input.getSource();
			this.context = input.getContext();
			this.occurrence = input.getOccurrence();
			this.value = input.getValue();
		}

		/**
		 * Yields the location of the source of nondeterminism.
		 * 
		 * @return the location
		 */
		public String getLocation() {
			return location;
		}

		/**
		 * Sets the location of the source of nondeterminism.
		 * 
		 * @param location the location
		 */
		public void setLocation(
				String location) {
			this.location = location;
		}

		/**
		 * Yields the description of the source of nondeterminism.
		 * 
		 * @return the description
		 */
		public String getSource() {
			return source;
		}

		/**
		 * Sets the description of the source of nondeterminism.
		 * 
		 * @param source the description
		 */
		public void setSource(
				String source) {
			this.source = source;
		}

		/**
		 * Yields the context where the source is evaluated, if any.
		 * 
		 * @return the context, or {@code null}
		 */
		public String getContext() {
			return context;
		}

		/**
		 * Sets the context where the source is evaluated.
		 * 
		 * @param context the context (can be {@code null})
		 */
		public void setContext(
				String context) {
			this.context = context;
		}

		/**
		 * Yields the number of times the source has been evaluated before
		 * producing this value.
		 * 
		 * @return the occurrence
		 */
		public int getOccurrence() {
			return occurrence;
		}

		/**
		 * Sets the number of times the source has been evaluated before
		 * producing this value.
		 * 
		 * @param occurrence the occurrence
		 */
		public void setOccurrence(
				int occurrence) {
			this.occurrence = occurrence;
		}

		/**
		 * Yields the textual representation of the value.
		 * 
		 * @return the value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Sets the textual representation of the value.
		 * 
		 * @param value the value
		 */
		public void setValue(
				String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return source
					+ " at "
					+ location
					+ (context == null ? "" : " in " + context)
					+ (occurrence == 0 ? "" : " (occurrence " + occurrence + ")")
					+ " = "
					+ value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(context, location, occurrence, source, value);
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
			JsonReproducingInput other = (JsonReproducingInput) obj;
			return Objects.equals(context, other.context)
					&& Objects.equals(location, other.location)
					&& occurrence == other.occurrence
					&& Objects.equals(source, other.source)
					&& Objects.equals(value, other.value);
		}

		@Override
		public int compareTo(
				JsonReproducingInput o) {
			int cmp;
			if ((cmp = StringUtils.compare(location, o.location)) != 0)
				return cmp;
			if ((cmp = StringUtils.compare(source, o.source)) != 0)
				return cmp;
			if ((cmp = StringUtils.compare(context, o.context)) != 0)
				return cmp;
			if ((cmp = Integer.compare(occurrence, o.occurrence)) != 0)
				return cmp;
			return StringUtils.compare(value, o.value);
		}

	}
}
