package it.unive.lisa.cfg.statement;

import it.unive.lisa.cfg.CFG;

/**
 * An expression that is part of a statement of the program.
 * 
 * @author @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Expression extends Statement {

	/**
	 * Builds an expression happening at the given source location.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 */
	protected Expression(CFG cfg, String sourceFile, int line, int col) {
		super(cfg, sourceFile, line, col);
	}
}
