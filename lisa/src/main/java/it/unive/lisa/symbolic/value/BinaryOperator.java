package it.unive.lisa.symbolic.value;

import it.unive.lisa.cfg.type.BooleanType;
import it.unive.lisa.cfg.type.NumericType;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A binary operator that can be applied to a pair of
 * {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum BinaryOperator implements Operator {
	// TODO the semantics of the operators should be clearly stated
	/**
	 * A subtraction between two numerical values of type {@link NumericType}.
	 */
	NUMERIC_SUB("-"),

	/**
	 * An addition between two numerical values of type {@link NumericType}.
	 */
	NUMERIC_ADD("+"),

	/**
	 * A division between two numerical values of type {@link NumericType}.
	 */
	NUMERIC_DIV("/"),

	/**
	 * A multiplication between two numerical values of type {@link NumericType}.
	 */
	NUMERIC_MUL("*"),

	/**
	 * A remainder computation between two numerical values of type
	 * {@link NumericType}.
	 */
	NUMERIC_MOD("%"),

	/**
	 * The logical or between two values of type {@link BooleanType}
	 */
	LOGICAL_OR("||"),

	/**
	 * The logical and between two values of type {@link BooleanType}
	 */
	LOGICAL_AND("&&"),

	/**
	 * An operator that tests if two values are different (different type or
	 * different value)
	 */
	COMPARISON_NE("!="),

	/**
	 * An operator that tests if two values are exactly equals (same type and same
	 * value)
	 */
	COMPARISON_EQ("=="),

	/**
	 * A comparison that tests if two numerical values of type {@link NumericType}
	 * are in relation through the "greater or equal than" relation
	 */
	COMPARISON_GE(">="),

	/**
	 * A comparison that tests if two numerical values of type {@link NumericType}
	 * are in relation through the "greater than" relation
	 */
	COMPARISON_GT(">"),

	/**
	 * A comparison that tests if two numerical values of type {@link NumericType}
	 * are in relation through the "less or equal than" relation
	 */
	COMPARISON_LE("<="),

	/**
	 * A comparison that tests if two numerical values of type {@link NumericType}
	 * are in relation through the "less than" relation
	 */
	COMPARISON_LT("<");

	private final String representation;

	private BinaryOperator(String representation) {
		this.representation = representation;
	}

	@Override
	public String getStringRepresentation() {
		return representation;
	}

	@Override
	public String toString() {
		return getStringRepresentation();
	}
}
