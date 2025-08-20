package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.LogicalOperator;
import it.unive.lisa.type.BooleanType;

/**
 * Given two expressions that both evaluate to Boolean values, a
 * {@link BinaryExpression} using this operator computes the logical conjunction
 * of those values, without short-circuiting.<br>
 * <br>
 * First argument expression type: {@link BooleanType}<br>
 * Second argument expression type: {@link BooleanType}<br>
 * Computed expression type: {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LogicalAnd
		extends
		LogicalOperation {

	/**
	 * The singleton instance of this class.
	 */
	public static final LogicalAnd INSTANCE = new LogicalAnd();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected LogicalAnd() {
	}

	@Override
	public String toString() {
		return "&&";
	}

	@Override
	public LogicalOperator opposite() {
		return LogicalOr.INSTANCE;
	}

}
