package it.unive.lisa.cfg.expression;

import org.apache.commons.lang3.StringUtils;

/**
 * A statement of the program to analyze.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Statement implements Comparable<Statement> {

	/**
	 * The source file where this statement happens. If it is unknown, this field
	 * might contain {@code null}.
	 */
	private final String sourceFile;

	/**
	 * The line where this statement happens in the source file. If it is unknown,
	 * this field might contain {@code -1}.
	 */
	private final int line;

	/**
	 * The column where this statement happens in the source file. If it is unknown,
	 * this field might contain {@code -1}.
	 */
	private final int col;

	/**
	 * Builds a statement happening at an unknown location.
	 */
	protected Statement() {
		this(null, -1, -1);
	}

	/**
	 * Builds a statement happening at the given source location.
	 * 
	 * @param sourceFile the source file where this statement happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this statement happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source file.
	 *                   If unknown, use {@code -1}
	 */
	protected Statement(String sourceFile, int line, int col) {
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
	}

	/**
	 * Yields the source file name where this statement happens. This method returns
	 * {@code null} if the source file is unknown.
	 * 
	 * @return the source file, or {@code null}
	 */
	public String getSourceFile() {
		return sourceFile;
	}

	/**
	 * Yields the line number where this statement happens in the source file. This
	 * method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the line number, or {@code -1}
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Yields the column where this statement happens in the source file. This
	 * method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the column, or {@code -1}
	 */
	public int getCol() {
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
		Statement other = (Statement) obj;
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

	/**
	 * Compares two statements in terms of order of appearance in the program,
	 * comparing source files first, followed by lines and columns at last. <br/>
	 * <br/>
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Statement o) {
		int cmp;

		if ((cmp = StringUtils.compare(sourceFile, o.sourceFile)) != 0)
			return cmp;

		if ((cmp = Integer.compare(line, o.line)) != 0)
			return cmp;

		return Integer.compare(col, o.col);
	}

	@Override
	public abstract String toString();
}
