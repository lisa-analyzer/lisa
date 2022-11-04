package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.common.UInt64Type;

/**
 * A 64-bit unsigned {@link Literal} representing a constant integral value.
 * Instances of this literal have a {@link UInt64Type} static type. Internally,
 * the constant is stored in a (signed) {@code long}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UInt64Literal extends Literal<Long> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public UInt64Literal(CFG cfg, CodeLocation location, long value) {
		super(cfg, location, value, UInt64Type.INSTANCE);
	}
}
