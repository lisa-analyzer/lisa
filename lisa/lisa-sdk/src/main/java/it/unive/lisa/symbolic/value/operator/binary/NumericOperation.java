package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

/**
 * A common implementation for classes implementing {@link BinaryOperator},
 * providing a {@link #typeInference(TypeSystem, Set, Set)} implementation that
 * delegates to {@link NumericType#commonNumericalType(Set, Set)}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class NumericOperation
		implements
		BinaryOperator {

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		// commonNumericalType already yields an empty set whenever neither
		// side has a numeric type; an additional per-side numeric-only check
		// here would incorrectly discard the case where one side is entirely
		// Untyped (unknown, but possibly numeric) and the other is numeric,
		// which commonNumericalType is explicitly designed to pair together
		return NumericType.commonNumericalType(left, right);
	}

}
