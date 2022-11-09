package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.LogicalOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * Given an expression that evaluates to a Boolean value, a
 * {@link UnaryExpression} using this operator computes the logical negation of
 * the expression: if it evaluates to {@code true}, it is transformed to
 * {@code false}, and vice versa.<br>
 * <br>
 * Argument expression type: {@link BooleanType}<br>
 * Computed expression type: {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LogicalNegation implements LogicalOperator, UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final LogicalNegation INSTANCE = new LogicalNegation();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected LogicalNegation() {
	}

	@Override
	public LogicalNegation opposite() {
		return this;
	}

	@Override
	public String toString() {
		return "!";
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> argument) {
		if (argument.stream().noneMatch(Type::isBooleanType))
			return Collections.emptySet();
		return Collections.singleton(types.getBooleanType());
	}
}
