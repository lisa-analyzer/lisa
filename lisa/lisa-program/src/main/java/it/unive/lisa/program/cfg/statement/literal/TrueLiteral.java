package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.BooleanType;

/**
 * A {@link Literal} representing the {@code true} boolean value. Instances of
 * this literal have a {@link BooleanType} static type.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TrueLiteral
		extends
		Literal<Boolean> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 */
	public TrueLiteral(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, true, cfg.getDescriptor().getUnit().getProgram().getTypes().getBooleanType());
	}

}
