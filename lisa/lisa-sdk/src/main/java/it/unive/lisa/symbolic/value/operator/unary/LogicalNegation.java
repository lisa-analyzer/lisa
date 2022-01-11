package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.LogicalOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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

	private LogicalNegation() {
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
	public ExternalSet<Type> typeInference(ExternalSet<Type> argument) {
		if (argument.noneMatch(Type::isBooleanType))
			return Caches.types().mkEmptySet();
		return Caches.types().mkSingletonSet(BoolType.INSTANCE);
	}
}
