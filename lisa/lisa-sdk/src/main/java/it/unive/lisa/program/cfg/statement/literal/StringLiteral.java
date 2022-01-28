package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.type.common.StringType;

/**
 * A {@link Literal} representing a constant string value. Instances of this
 * literal have a {@link StringType} static type.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringLiteral extends Literal<String> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link ImplementedCFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public StringLiteral(ImplementedCFG cfg, CodeLocation location, String value) {
		super(cfg, location, value, StringType.INSTANCE);
	}
}
