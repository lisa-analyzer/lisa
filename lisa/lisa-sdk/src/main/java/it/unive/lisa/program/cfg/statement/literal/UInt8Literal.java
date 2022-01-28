package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.type.common.UInt8;

/**
 * An 8-bit unsigned {@link Literal} representing a constant integral value.
 * Instances of this literal have a {@link UInt8} static type. Internally, the
 * constant is stored in a (signed) {@code byte}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UInt8Literal extends Literal<Byte> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link ImplementedCFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public UInt8Literal(ImplementedCFG cfg, CodeLocation location, byte value) {
		super(cfg, location, value, UInt8.INSTANCE);
	}
}
