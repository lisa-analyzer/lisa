package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.type.common.BoolType;

/**
 * A {@link Literal} representing the {@code true} boolean value. Instances of
 * this literal have a {@link BoolType} static type.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TrueLiteral extends Literal<Boolean> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link ImplementedCFG} where this literal lies
	 * @param location the location where this literal is defined
	 */
	public TrueLiteral(ImplementedCFG cfg, CodeLocation location) {
		super(cfg, location, true, BoolType.INSTANCE);
	}
}
