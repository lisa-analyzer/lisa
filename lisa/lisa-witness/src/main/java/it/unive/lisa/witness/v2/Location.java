package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "file_name", "line", "column", "function" })
public final class Location {

	private final String fileName;

	private final int line;

	private final Integer column;

	private final String function;

	public Location(
			String fileName,
			int line) {
		this(fileName, line, null, null);
	}

	public Location(
			String fileName,
			int line,
			Integer column) {
		this(fileName, line, column, null);
	}

	@JsonCreator
	public Location(
			@JsonProperty(value = "file_name", required = true) String fileName,
			@JsonProperty(value = "line", required = true) int line,
			@JsonProperty("column") Integer column,
			@JsonProperty("function") String function) {
		this.fileName = Objects.requireNonNull(fileName, "fileName");
		if (line < 1)
			throw new InvalidWitnessException("The line must be at least 1: " + line);
		if (column != null && column < 1)
			throw new InvalidWitnessException("The column must be at least 1: " + column);
		this.line = line;
		this.column = column;
		this.function = function;
	}

	@JsonProperty("file_name")
	public String getFileName() {
		return fileName;
	}

	@JsonProperty("line")
	public int getLine() {
		return line;
	}

	@JsonProperty("column")
	public Integer getColumn() {
		return column;
	}

	@JsonProperty("function")
	public String getFunction() {
		return function;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileName, line, column, function);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Location other = (Location) obj;
		return line == other.line && fileName.equals(other.fileName) && Objects.equals(column, other.column)
				&& Objects.equals(function, other.function);
	}

	@Override
	public String toString() {
		return fileName + ":" + line + (column == null ? "" : ":" + column);
	}
}
