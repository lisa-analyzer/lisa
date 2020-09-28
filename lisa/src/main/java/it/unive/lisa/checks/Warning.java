package it.unive.lisa.checks;

import org.apache.commons.lang3.StringUtils;

/**
 * A warning reported by LiSA on the program under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Warning implements Comparable<Warning> {

	/**
	 * The source file where this warning is reported. If it is unknown, this field
	 * might contain {@code null}.
	 */
	private final String sourceFile;

	/**
	 * The line where this warning is reported in the source file. If it is unknown,
	 * this field might contain {@code -1}.
	 */
	private final int line;

	/**
	 * The column where this warning is reported in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int col;

	/**
	 * The message of this warning
	 */
	private final String message;

	/**
	 * Builds the warning.
	 * 
	 * @param sourceFile the source file where this warning is reported. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this warning is reported in the
	 *                   source file. If unknown, use {@code -1}
	 * @param col        the column where this warning is reported in the source
	 *                   file. If unknown, use {@code -1}
	 * @param message    the message of this warning
	 */
	public Warning(String sourceFile, int line, int col, String message) {
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
		this.message = message;
	}

	/**
	 * Yields the source file name where this warning is reported. This method
	 * returns {@code null} if the source file is unknown.
	 * 
	 * @return the source file, or {@code null}
	 */
	public final String getSourceFile() {
		return sourceFile;
	}

	/**
	 * Yields the line number where this warning is reported in the source file.
	 * This method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the line number, or {@code -1}
	 */
	public final int getLine() {
		return line;
	}

	/**
	 * Yields the column where this warning is reported in the source file. This
	 * method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the column, or {@code -1}
	 */
	public final int getCol() {
		return col;
	}

	/**
	 * Yields the message of this warning.
	 * 
	 * @return the message of this warning
	 */
	public final String getMessage() {
		return message;
	}
	
	/**
	 * Yields the location where this warning was reported.
	 * 
	 * @return the location of this warning
	 */
	public final String getLocation() {
		return "'" + String.valueOf(sourceFile) + "':" + line + ":" + col;
	}

	@Override
	public int compareTo(Warning o) {
		int cmp;

		if ((cmp = StringUtils.compare(sourceFile, o.sourceFile)) != 0)
			return cmp;

		if ((cmp = Integer.compare(line, o.line)) != 0)
			return cmp;

		if ((cmp = Integer.compare(col, o.col)) != 0)
			return cmp;

		return StringUtils.compare(message, o.message);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + col;
		result = prime * result + line;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Warning other = (Warning) obj;
		if (col != other.col)
			return false;
		if (line != other.line)
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (sourceFile == null) {
			if (other.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(other.sourceFile))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return "[" + getLocation() + "] " + message;
	}
}
