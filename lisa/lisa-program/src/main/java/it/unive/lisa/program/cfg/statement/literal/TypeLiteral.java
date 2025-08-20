package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import java.util.Collections;

/**
 * A {@link Literal} representing a type of the program. Instances of this
 * literal have a {@link TypeTokenType} static type containing the given type.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeLiteral
		extends
		Literal<Type> {

	/**
	 * Builds the literal.
	 * 
	 * @param cfg      the {@link CFG} where this literal lies
	 * @param location the location where this literal is defined
	 * @param value    the constant value represented by this literal
	 */
	public TypeLiteral(
			CFG cfg,
			CodeLocation location,
			Type value) {
		super(cfg, location, value, new TypeTokenType(Collections.singleton(value)));
	}

}
