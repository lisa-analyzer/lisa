package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Literal;
import it.unive.lisa.test.imp.types.BoolType;

/**
 * The IMP {@link Literal} representing the {@code true} boolean value.
 * Instances of this literal have a {@link BoolType} static type.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPTrueLiteral extends Literal {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg        the {@link CFG} where this literal lies
	 * @param sourceFile the source file name where this literal is defined
	 * @param line       the line number where this literal is defined
	 * @param col        the column where this literal is defined
	 */
	public IMPTrueLiteral(CFG cfg, String sourceFile, int line, int col) {
		super(cfg, sourceFile, line, col, true, BoolType.INSTANCE);
	}
}
