package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.LogicalOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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
public class LogicalAnd implements LogicalOperator, BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final LogicalAnd INSTANCE = new LogicalAnd();

	private LogicalAnd() {
	}

	@Override
	public String toString() {
		return "&&";
	}

	@Override
	public LogicalOperator opposite() {
		return LogicalOr.INSTANCE;
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> left, ExternalSet<Type> right) {
		if (left.noneMatch(Type::isBooleanType) || right.noneMatch(Type::isBooleanType))
			return Caches.types().mkEmptySet();
		return Caches.types().mkSingletonSet(BoolType.INSTANCE);
	}
}
