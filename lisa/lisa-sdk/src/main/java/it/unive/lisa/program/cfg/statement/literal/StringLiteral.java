package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.StringType;

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
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public StringLiteral(CFG cfg, CodeLocation location, String value) {
		super(cfg, location, value, cfg.getDescriptor().getUnit().getProgram().getTypes().getStringType());
	}

	@Override
	public String toString() {
		return "\"" + super.toString() + "\"";
	}
}
