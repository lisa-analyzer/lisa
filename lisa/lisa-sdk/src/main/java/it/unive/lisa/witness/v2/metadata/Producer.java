package it.unive.lisa.witness.v2.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import java.util.Objects;

/**
 * The tool that produced an entry of a witness. The name and the version are
 * required. The configuration, the command line and a free description are
 * optional.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "name", "version", "configuration", "command_line", "description" })
public final class Producer {

	/**
	 * The name of the tool.
	 */
	private final String name;

	/**
	 * The version of the tool.
	 */
	private final String version;

	/**
	 * The configuration the tool ran with, or {@code null} if it is not given.
	 */
	private final String configuration;

	/**
	 * The command line the tool ran with, or {@code null} if it is not given.
	 */
	private final String commandLine;

	/**
	 * Any other information about the tool, or {@code null} if it is not given.
	 */
	private final String description;

	/**
	 * Builds a producer with a name and a version only.
	 * 
	 * @param name    the name of the tool
	 * @param version the version of the tool
	 * 
	 * @throws InvalidWitnessException if the name or the version is blank
	 */
	public Producer(
			String name,
			String version) {
		this(name, version, null, null, null);
	}

	/**
	 * Builds a producer.
	 * 
	 * @param name          the name of the tool
	 * @param version       the version of the tool
	 * @param configuration the configuration the tool ran with, or {@code null}
	 *                          if it is not given
	 * @param commandLine   the command line the tool ran with, or {@code null}
	 *                          if it is not given
	 * @param description   any other information about the tool, or
	 *                          {@code null} if it is not given
	 * 
	 * @throws InvalidWitnessException if the name or the version is blank
	 */
	@JsonCreator
	public Producer(
			@JsonProperty(value = "name", required = true) String name,
			@JsonProperty(value = "version", required = true) String version,
			@JsonProperty("configuration") String configuration,
			@JsonProperty("command_line") String commandLine,
			@JsonProperty("description") String description) {
		this.name = Objects.requireNonNull(name, "name");
		this.version = Objects.requireNonNull(version, "version");
		if (name.isBlank())
			throw new InvalidWitnessException("The producer needs a name");
		if (version.isBlank())
			throw new InvalidWitnessException("The producer " + name + " needs a version");
		this.configuration = configuration;
		this.commandLine = commandLine;
		this.description = description;
	}

	/**
	 * Builds a producer whose version is the {@code Implementation-Version} of
	 * the manifest of the jar containing the given class.
	 * 
	 * @param name   the name of the tool
	 * @param anchor a class of the tool
	 * 
	 * @return the producer
	 * 
	 * @throws IllegalStateException if the manifest has no
	 *                                   {@code Implementation-Version}, for
	 *                                   instance when the tool does not run
	 *                                   from a jar
	 */
	public static Producer of(
			String name,
			Class<?> anchor) {
		Package pkg = anchor.getPackage();
		String version = pkg == null ? null : pkg.getImplementationVersion();
		if (version == null)
			throw new IllegalStateException("No Implementation-Version in the manifest of " + anchor.getName()
					+ ": the version of the producer " + name + " is required");
		return new Producer(name, version);
	}

	/**
	 * Returns the name of the tool.
	 * 
	 * @return the name
	 */
	@JsonProperty("name")
	public String getName() {
		return name;
	}

	/**
	 * Returns the version of the tool.
	 * 
	 * @return the version
	 */
	@JsonProperty("version")
	public String getVersion() {
		return version;
	}

	/**
	 * Returns the configuration the tool ran with.
	 * 
	 * @return the configuration, or {@code null} if it is not given
	 */
	@JsonProperty("configuration")
	public String getConfiguration() {
		return configuration;
	}

	/**
	 * Returns the command line the tool ran with.
	 * 
	 * @return the command line, or {@code null} if it is not given
	 */
	@JsonProperty("command_line")
	public String getCommandLine() {
		return commandLine;
	}

	/**
	 * Returns any other information about the tool.
	 * 
	 * @return the description, or {@code null} if it is not given
	 */
	@JsonProperty("description")
	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, version, configuration, commandLine, description);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Producer other = (Producer) obj;
		return name.equals(other.name) && version.equals(other.version)
				&& Objects.equals(configuration, other.configuration)
				&& Objects.equals(commandLine, other.commandLine) && Objects.equals(description, other.description);
	}

	@Override
	public String toString() {
		return name + " " + version;
	}
}
