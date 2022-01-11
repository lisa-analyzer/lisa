package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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

	private ComparisonNe() {
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
	public ExternalSet<Type> typeInference(ExternalSet<Type> left, ExternalSet<Type> right) {
		return Caches.types().mkSingletonSet(BoolType.INSTANCE);
	}
}