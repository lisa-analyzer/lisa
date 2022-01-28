package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.type.common.Int8;

/**
 * An 8-bit signed {@link Literal} representing a constant integral value.
 * Instances of this literal have a {@link Int8} static type. Internally, the
 * constant is stored in a {@code byte}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Int8Literal extends Literal<Byte> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link ImplementedCFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public Int8Literal(ImplementedCFG cfg, CodeLocation location, byte value) {
		super(cfg, location, value, Int8.INSTANCE);
	}
}
