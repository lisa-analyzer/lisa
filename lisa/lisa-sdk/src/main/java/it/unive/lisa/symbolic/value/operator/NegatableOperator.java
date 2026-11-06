package it.unive.lisa.symbolic.value.operator;

import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;

/**
 * An {@link Operator} whose effect can be inverted, that is, for which an
 * opposite operator exists (e.g., a comparison or a logical operator).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface NegatableOperator
		extends
		Operator {

	/**
	 * Yields the opposite operator of this operator (e.g., {@link ComparisonLe}
	 * to {@link ComparisonGt}, {@link ComparisonLt} to {@link ComparisonGe}).
	 * 
	 * @return the opposite operator of this operator
	 */
	NegatableOperator opposite();

}
