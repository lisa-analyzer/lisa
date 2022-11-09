package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * Given two expressions, a {@link BinaryExpression} using this operator checks
 * if the values those expressions compute to are different. This operator
 * corresponds to the logical negation of {@link ComparisonEq}.<br>
 * <br>
 * First argument expression type: any {@link Type}<br>
 * Second argument expression type: any {@link Type}<br>
 * Computed expression type: {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ComparisonNe implements ComparisonOperator, BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final ComparisonNe INSTANCE = new ComparisonNe();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected ComparisonNe() {
	}

	@Override
	public String toString() {
		return "!=";
	}

	@Override
	public ComparisonOperator opposite() {
		return ComparisonEq.INSTANCE;
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		return Collections.singleton(types.getBooleanType());
	}
}