package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.common.BoolType;

/**
 * A {@link Literal} representing the {@code false} boolean value. Instances of
 * this literal have a {@link BoolType} static type.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FalseLiteral extends Literal<Boolean> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 */
	public FalseLiteral(CFG cfg, CodeLocation location) {
		super(cfg, location, false, BoolType.INSTANCE);
	}

}
