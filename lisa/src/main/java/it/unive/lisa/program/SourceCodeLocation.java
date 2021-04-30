package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CodeLocation;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A location of an element in the source code represented by the path to the
 * source code, the line and the column where the element appears.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * @author <a href="mailto:vincenzo.arceri@unive.it">VincenzoArceri</a>
 */
public class SourceCodeLocation implements CodeLocation {

	/**
	 * The source file where this code element happens. If it is unknown, this
	 * field might contain {@code null}.
	 */
	private final String sourceFile;

	/**
	 * The line where this code element happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int line;

	/**
	 * The column where this code element happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int col;

	/**
	 * Builds the code element.
	 * 
	 * @param sourceFile the source file where this code element happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this code element happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this code element happens in the
	 *                       source file. If unknown, use {@code -1}
	 */
	public SourceCodeLocation(String sourceFile, int line, int col) {
		Objects.requireNonNull(sourceFile, "The source file cannot be null");
		if (line == -1)
			throw new IllegalArgumentException("Line number cannot be negative");
		if (col == -1)
			throw new IllegalArgumentException("Column number cannot be negative");
		this.sourceFile = FilenameUtils.separatorsToUnix(sourceFile);
		this.line = line;
		this.col = col;
	}

	@Override
	public String getCodeLocation() {
		return "'" + sourceFile + "':" + line + ":" + col;
	}

	/**
	 * Yields the source file name where this code element happens. This method
	 * returns the string {@code "null"} if the source file is unknown.
	 * 
	 * @return the source file, or {@code null}
	 */
	public final String getSourceFile() {
		return String.valueOf(sourceFile);
	}

	/**
	 * Yields the line number where this code element happens in the source
	 * file. This method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the line number, or {@code -1}
	 */
	public final int getLine() {
		return line;
	}

	/**
	 * Yields the column where this code element happens in the source file.
	 * This method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the column, or {@code -1}
	 */
	public final int getCol() {
		return col;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + col;
		result = prime * result + line;
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "'" + sourceFile + "':" + line + ":" + col;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SourceCodeLocation other = (SourceCodeLocation) obj;
		if (col != other.col)
			return false;
		if (line != other.line)
			return false;
		if (sourceFile == null) {
			if (other.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(other.sourceFile))
			return false;
		return true;
	}

	@Override
	public int compareTo(CodeLocation other) {
		if (!(other instanceof SourceCodeLocation))
			return -1;

		SourceCodeLocation o = (SourceCodeLocation) other;

		int cmp;

		if ((cmp = StringUtils.compare(getSourceFile(), o.getSourceFile())) != 0)
			return cmp;

		if ((cmp = Integer.compare(getLine(), o.getLine())) != 0)
			return cmp;

		return Integer.compare(getCol(), o.getCol());
	}
}
