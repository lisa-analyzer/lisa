package it.unive.lisa.cfg.statement;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.type.NullType;

/**
 * A literal representing the {@code null} constant.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NullLiteral extends Literal {

	private static final Object NULL_CONST = new Object();

	/**
	 * Builds the null literal. The location where this literal happens is unknown
	 * (i.e. no source file/line/column is available).
	 * The type of a null literal is {@link NullType}.
	 * 
	 * @param cfg the cfg that this expression belongs to
	 */
	public NullLiteral(CFG cfg) {
		super(cfg, NULL_CONST, NullType.INSTANCE);
	}

	/**
	 * Builds the null literal, happening at the given location in the program.
	 * The type of a null literal is {@link NullType}.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 */
	public NullLiteral(CFG cfg, String sourceFile, int line, int col) {
		super(cfg, sourceFile, line, col, NULL_CONST, NullType.INSTANCE);
	}

	@Override
	public String toString() {
		return "null";
	}
}
