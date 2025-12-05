package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * Given two expressions, a {@link BinaryExpression} using this operator
 * computes an integer by comparing the expressions, assuming they define a
 * natural ordering: if they are equal the result is 0; if the value of the
 * first expression "preceeds" (or "is smaller") than the value of the second
 * expression, the result is a negative number; otherwise, the result is a
 * positive number.<br>
 * <br>
 * First argument expression type: any {@link Type}<br>
 * Second argument expression type: any {@link Type}<br>
 * Computed expression type: {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ValueComparison
		implements
		BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final ValueComparison INSTANCE = new ValueComparison();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected ValueComparison() {
	}

	@Override
	public String toString() {
		return "compareTo";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		return Collections.singleton(types.getIntegerType());
	}

}
