package it.unive.lisa.program;

import org.apache.commons.lang3.StringUtils;

/**
 * Common superclass for code elements that share information about the program
 * point where they are defined.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class CodeElement implements Comparable<CodeElement> {

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
	protected CodeElement(String sourceFile, int line, int col) {
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
	}

	/**
	 * Yields the source file name where this code element happens. This method
	 * returns {@code null} if the source file is unknown.
	 * 
	 * @return the source file, or {@code null}
	 */
	public final String getSourceFile() {
		return sourceFile;
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CodeElement other = (CodeElement) obj;
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
	public int compareTo(CodeElement o) {
		int cmp;

		if ((cmp = StringUtils.compare(getSourceFile(), o.getSourceFile())) != 0)
			return cmp;

		if ((cmp = Integer.compare(getLine(), o.getLine())) != 0)
			return cmp;

		return Integer.compare(getCol(), o.getCol());
	}
}
