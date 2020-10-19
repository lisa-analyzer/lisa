package it.unive.lisa.cfg.statement;

import it.unive.lisa.cfg.CFG;

/**
 * A statement that does nothing. Can be used for instrumenting branching
 * operations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NoOp extends Statement {

	/**
	 * Builds the no-op. The location where this no-op happens is unknown (i.e. no
	 * source file/line/column is available).
	 * 
	 * @param cfg the cfg that this statement belongs to
	 */
	public NoOp(CFG cfg) {
		this(cfg, null, -1, -1);
	}

	/**
	 * Builds the no-op, happening at the given
	 * location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param sourceFile the source file where this statement happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this statement happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source file.
	 *                   If unknown, use {@code -1}
	 */
	public NoOp(CFG cfg, String sourceFile, int line, int col) {
		super(cfg, sourceFile, line, col);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + getClass().getName().hashCode();
		return result;
	}

	@Override
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (getClass() != st.getClass())
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return "no-op";
	}
}
