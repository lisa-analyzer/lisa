package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.type.common.UInt32;

/**
 * A 32-bit unsigned {@link Literal} representing a constant integral value.
 * Instances of this literal have a {@link UInt32} static type. Internally, the
 * constant is stored in a (signed) {@code int}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UInt32Literal extends Literal<Integer> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public UInt32Literal(CFG cfg, CodeLocation location, int value) {
		super(cfg, location, value, UInt32.INSTANCE);
	}
}
