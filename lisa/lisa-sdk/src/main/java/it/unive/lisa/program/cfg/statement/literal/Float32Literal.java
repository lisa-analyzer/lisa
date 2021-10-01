package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.common.Float32;
import it.unive.lisa.type.common.Int32;

/**
 * A 32-bit signed {@link Literal} representing a constant non-integral value.
 * Instances of this literal have a {@link Float32} static type. Internally, the
 * constant is stored in a {@code float}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Float32Literal extends Literal<Float> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public Float32Literal(CFG cfg, CodeLocation location, float value) {
		super(cfg, location, value, Int32.INSTANCE);
	}
}
