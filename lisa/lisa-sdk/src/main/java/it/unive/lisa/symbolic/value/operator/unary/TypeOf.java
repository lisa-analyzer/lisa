package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.TypeOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * Given any expression, a {@link UnaryExpression} using this operator computes
 * the type(s) of that expression.<br>
 * <br>
 * Argument expression type: any {@link Type}<br>
 * Computed expression type: {@link TypeTokenType} containing all types of
 * argument
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeOf implements TypeOperator, UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final TypeOf INSTANCE = new TypeOf();

	private TypeOf() {
	}

	@Override
	public String toString() {
		return "typeof";
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> argument) {
		return Caches.types().mkSingletonSet(new TypeTokenType(argument.copy()));
	}
}
