package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Literal;
import it.unive.lisa.test.imp.types.IntType;

/**
 * An IMP {@link Literal} representing a constant integer value. Instances of
 * this literal have a {@link IntType} static type.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPIntLiteral extends Literal {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg        the {@link CFG} where this literal lies
	 * @param sourceFile the source file name where this literal is defined
	 * @param line       the line number where this literal is defined
	 * @param col        the column where this literal is defined
	 * @param value      the constant value represented by this literal
	 */
	public IMPIntLiteral(CFG cfg, String sourceFile, int line, int col, int value) {
		super(cfg, sourceFile, line, col, value, IntType.INSTANCE);
	}
}
