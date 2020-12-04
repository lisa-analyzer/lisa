package it.unive.lisa.checks.warnings;

import org.apache.commons.lang3.StringUtils;

/**
 * A warning reported by LiSA on the program under analysis. This warning is
 * tied to a location, i.e. it might have information about source file, line
 * number and column. This does not mean that it will always have them, since
 * CFGs and statements might have been built without that information.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class WarningWithLocation extends Warning {

	/**
	 * The source file where this warning is reported. If it is unknown, this
	 * field might contain {@code null}.
	 */
	private final String sourceFile;

	/**
	 * The line where this warning is reported in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int line;

	/**
	 * The column where this warning is reported in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int col;

	/**
	 * Builds the warning.
	 * 
	 * @param sourceFile the source file where this warning is reported. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this warning is reported in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this warning is reported in the source
	 *                       file. If unknown, use {@code -1}
	 * @param message    the message of this warning
	 */
	public WarningWithLocation(String sourceFile, int line, int col, String message) {
		super(message);
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
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
	 * Yields the location where this warning was reported.
	 * 
	 * @return the location of this warning
	 */
	public final String getLocation() {
		return "'" + String.valueOf(sourceFile) + "':" + line + ":" + col;
	}

	/**
	 * Yields the location where this warning was reported, surrounded by square
	 * brackets.
	 * 
	 * @return the location of this warning surrounded by brackets
	 */
	public final String getLocationWithBrackets() {
		return "[" + getLocation() + "]";
	}

	@Override
	public int compareTo(Warning o) {
		if (!(o instanceof WarningWithLocation))
			return super.compareTo(o);

		WarningWithLocation other = (WarningWithLocation) o;
		int cmp;

		if ((cmp = StringUtils.compare(sourceFile, other.sourceFile)) != 0)
			return cmp;

		if ((cmp = Integer.compare(line, other.line)) != 0)
			return cmp;

		if ((cmp = Integer.compare(col, other.col)) != 0)
			return cmp;

		return super.compareTo(other);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + col;
		result = prime * result + line;
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		WarningWithLocation other = (WarningWithLocation) obj;
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
	public abstract String toString();
}
