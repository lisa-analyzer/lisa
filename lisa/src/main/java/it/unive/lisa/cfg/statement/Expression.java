package it.unive.lisa.cfg.statement;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;

/**
 * An expression that is part of a statement of the program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Expression extends Statement {

	/**
	 * The static type of this expression
	 */
	private final Type type;
	
	/**
	 * Builds an untyped expression happening at the given source location,
	 * that is its type is Untyped.
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
		this(cfg, sourceFile, line, col, Untyped.INSTANCE);
	}
	
	/**
	 * Builds a typed expression happening at the given source location.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param type		 the type of this expression
	 */
	protected Expression(CFG cfg, String sourceFile, int line, int col, Type type) {
		super(cfg, sourceFile, line, col);
		this.type = type;
	}
	
	/**
	 * Yields the type of this expression.
	 * 
	 * @return the type of this expression
	 */
	public Type getType() {
		return type;
	}
}
