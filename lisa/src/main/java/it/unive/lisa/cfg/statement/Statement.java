package it.unive.lisa.cfg.statement;

import it.unive.lisa.cfg.CFG;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A statement of the program to analyze.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Statement implements Comparable<Statement> {

	/**
	 * The cfg containing this statement.
	 */
	private final CFG cfg;

	/**
	 * The source file where this statement happens. If it is unknown, this
	 * field might contain {@code null}.
	 */
	private final String sourceFile;

	/**
	 * The line where this statement happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int line;

	/**
	 * The column where this statement happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int col;

	/**
	 * Builds a statement happening at the given source location.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param sourceFile the source file where this statement happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this statement happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source
	 *                       file. If unknown, use {@code -1}
	 */
	protected Statement(CFG cfg, String sourceFile, int line, int col) {
		Objects.requireNonNull(cfg, "Containing CFG cannot be null");
		this.cfg = cfg;
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
	}

	/**
	 * Yields the CFG that this statement belongs to.
	 * 
	 * @return the containing CFG
	 */
	public final CFG getCFG() {
		return cfg;
	}

	/**
	 * Yields the source file name where this statement happens. This method
	 * returns {@code null} if the source file is unknown.
	 * 
	 * @return the source file, or {@code null}
	 */
	public final String getSourceFile() {
		return sourceFile;
	}

	/**
	 * Yields the line number where this statement happens in the source file.
	 * This method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the line number, or {@code -1}
	 */
	public final int getLine() {
		return line;
	}

	/**
	 * Yields the column where this statement happens in the source file. This
	 * method returns {@code -1} if the line number is unknown.
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

	/**
	 * All statements use reference equality for equality checks, to allow
	 * different statement with the same content but placed in different part of
	 * the cfg to being not equal if there are no debug information available.
	 * For checking if two statements are effectively equal (that is, they are
	 * different object with the same structure) use
	 * {@link #isEqualTo(Statement)}. <br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public final boolean equals(Object obj) {
		return this == obj;
	}

	/**
	 * Checks if this statement is effectively equal to the given one, that is,
	 * if they have the same structure while potentially being different
	 * instances.
	 * 
	 * @param st the other statement
	 * 
	 * @return {@code true} if this statement and the given one are effectively
	 *             equals
	 */
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (st == null)
			return false;
		if (getClass() != st.getClass())
			return false;
		if (col != st.col)
			return false;
		if (line != st.line)
			return false;
		if (sourceFile == null) {
			if (st.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(st.sourceFile))
			return false;
		return true;
	}

	/**
	 * Compares two statements in terms of order of appearance in the program,
	 * comparing source files first, followed by lines and columns at last. <br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public final int compareTo(Statement o) {
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
