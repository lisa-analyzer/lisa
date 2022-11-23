package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.type.Int32Type;

/**
 * A 32-bit signed {@link Literal} representing a constant integral value.
 * Instances of this literal have a {@link Int32Type} static type. Internally,
 * the constant is stored in a {@code int}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Int32Literal extends Literal<Integer> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public Int32Literal(CFG cfg, CodeLocation location, int value) {
		super(cfg, location, value, Int32Type.INSTANCE);
	}
}
