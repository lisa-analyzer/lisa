package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.type.Int64Type;

/**
 * A 64-bit signed {@link Literal} representing a constant integral value.
 * Instances of this literal have a {@link Int64Type} static type. Internally,
 * the constant is stored in a {@code long}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Int64Literal
		extends
		Literal<Long> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public Int64Literal(
			CFG cfg,
			CodeLocation location,
			long value) {
		super(cfg, location, value, Int64Type.INSTANCE);
	}

}
