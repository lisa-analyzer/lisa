package it.unive.lisa.symbolic.value.operator;

import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.BooleanType;

/**
 * A logical operation on operand(s) of type {@link BooleanType}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface NegatableOperator extends Operator {

	/**
	 * Yields the opposite operator of this operator (e.g., {@link ComparisonLe}
	 * to {@link ComparisonGt}, {@link ComparisonLt} to {@link ComparisonGe}).
	 * 
	 * @return the opposite operator of this operator
	 */
	NegatableOperator opposite();

}
