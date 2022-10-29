package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.common.UInt16Type;

/**
 * A 16-bit unsigned {@link Literal} representing a constant integral value.
 * Instances of this literal have a {@link UInt16Type} static type. Internally,
 * the constant is stored in a (signed) {@code short}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UInt16Literal extends Literal<Short> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public UInt16Literal(CFG cfg, CodeLocation location, short value) {
		super(cfg, location, value, UInt16Type.INSTANCE);
	}
}
