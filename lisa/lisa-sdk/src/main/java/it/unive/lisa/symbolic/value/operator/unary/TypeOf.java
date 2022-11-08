package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.TypeOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected TypeOf() {
	}

	@Override
	public String toString() {
		return "typeof";
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> argument) {
		return Collections.singleton(new TypeTokenType(new HashSet<>(argument)));
	}
}
