package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

/**
 * A position in one of the input files of a task. The file name must be written
 * exactly as in the input files of the task. Lines and columns start from 1.
 * The column is optional, and when it is missing the location refers to the
 * first matching position of the line. The function is optional too, and names
 * the function containing the location.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "file_name", "line", "column", "function" })
public final class Location {

	/**
	 * The name of the file of this location.
	 */
	private final String fileName;

	/**
	 * The line of this location, starting from 1.
	 */
	private final int line;

	/**
	 * The column of this location, starting from 1, or {@code null} if it has
	 * none.
	 */
	private final Integer column;

	/**
	 * The function containing this location, or {@code null} if it is not
	 * given.
	 */
	private final String function;

	/**
	 * Builds a location without column and function.
	 * 
	 * @param fileName the name of the file, as written in the input files of
	 *                     the task
	 * @param line     the line, starting from 1
	 * 
	 * @throws InvalidWitnessException if the line is less than 1
	 */
	public Location(
			String fileName,
			int line) {
		this(fileName, line, null, null);
	}

	/**
	 * Builds a location without function.
	 * 
	 * @param fileName the name of the file, as written in the input files of
	 *                     the task
	 * @param line     the line, starting from 1
	 * @param column   the column, starting from 1, or {@code null} if it is not
	 *                     given
	 * 
	 * @throws InvalidWitnessException if the line or the column is less than 1
	 */
	public Location(
			String fileName,
			int line,
			Integer column) {
		this(fileName, line, column, null);
	}

	/**
	 * Builds a location.
	 * 
	 * @param fileName the name of the file, as written in the input files of
	 *                     the task
	 * @param line     the line, starting from 1
	 * @param column   the column, starting from 1, or {@code null} if it is not
	 *                     given
	 * @param function the function containing the location, or {@code null} if
	 *                     it is not given
	 * 
	 * @throws InvalidWitnessException if the line or the column is less than 1
	 */
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

	/**
	 * Returns the name of the file of this location.
	 * 
	 * @return the file name
	 */
	@JsonProperty("file_name")
	public String getFileName() {
		return fileName;
	}

	/**
	 * Returns the line of this location, starting from 1.
	 * 
	 * @return the line
	 */
	@JsonProperty("line")
	public int getLine() {
		return line;
	}

	/**
	 * Returns the column of this location, starting from 1.
	 * 
	 * @return the column, or {@code null} if it is not given
	 */
	@JsonProperty("column")
	public Integer getColumn() {
		return column;
	}

	/**
	 * Returns the function containing this location.
	 * 
	 * @return the function, or {@code null} if it is not given
	 */
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
		return fileName + ":" + line + (column == null ? "" : ":" + column)
				+ (function == null ? "" : " (" + function + ")");
	}
}
