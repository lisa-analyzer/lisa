package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.common.Float64Type;

/**
 * A 64-bit signed {@link Literal} representing a constant non-integral value.
 * Instances of this literal have a {@link Float64Type} static type. Internally,
 * the constant is stored in a {@code double}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Float64Literal extends Literal<Double> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public Float64Literal(CFG cfg, CodeLocation location, double value) {
		super(cfg, location, value, Float64Type.INSTANCE);
	}
}
