package it.unive.lisa.witness.v2.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "name", "version", "configuration", "command_line", "description" })
public final class Producer {

	private final String name;

	private final String version;

	private final String configuration;

	private final String commandLine;

	private final String description;

	public Producer(
			String name,
			String version) {
		this(name, version, null, null, null);
	}

	public Producer(
			String name,
			String version,
			String configuration,
			String commandLine,
			String description) {
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

	public static Producer of(
			String name,
			Class<?> anchor) {
		Package pkg = anchor.getPackage();
		String version = pkg == null ? null : pkg.getImplementationVersion();
		if (version == null)
			throw new InvalidWitnessException("No Implementation-Version in the manifest of " + anchor.getName()
					+ ": the version of the producer " + name + " is required");
		return new Producer(name, version);
	}

	@JsonProperty("name")
	public String getName() {
		return name;
	}

	@JsonProperty("version")
	public String getVersion() {
		return version;
	}

	@JsonProperty("configuration")
	public String getConfiguration() {
		return configuration;
	}

	@JsonProperty("command_line")
	public String getCommandLine() {
		return commandLine;
	}

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
