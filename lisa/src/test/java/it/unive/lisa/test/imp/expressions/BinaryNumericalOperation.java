package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.cfg.statement.BinaryNativeCall;
import it.unive.lisa.cfg.type.NumericType;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * An interface for {@link BinaryNativeCall}s whose operands' types are
 * {@link NumericType}s. This interface provides a default method for evaluating
 * the runtime types of the expression's result.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface BinaryNumericalOperation {

	/**
	 * Computes the {@link ExternalSet} of runtime {@link Type}s of a
	 * {@link SymbolicExpression} over {@link NumericType} expressions. The
	 * resulting set of types is computed as follows:
	 * <ul>
	 * <li>if both arguments have no numeric types among their possible types,
	 * then a singleton set containing {@link Untyped#INSTANCE} is returned</li>
	 * <li>for each pair {@code <t1, t2>} where {@code t1} is a type of
	 * {@code left} and {@code t2} is a type of {@code right}:
	 * <ul>
	 * <li>if {@code t1} is {@link Untyped}, then {@link Untyped#INSTANCE} is
	 * added to the set</li>
	 * <li>if {@code t2} is {@link Untyped}, then {@link Untyped#INSTANCE} is
	 * added to the set</li>
	 * <li>if {@code t1} can be assigned to {@code t2}, then {@code t2} is added
	 * to the set</li>
	 * <li>if {@code t2} can be assigned to {@code t1}, then {@code t1} is added
	 * to the set</li>
	 * <li>if none of the above conditions hold (that is usually a symptom of a
	 * type error), a singleton set containing {@link Untyped#INSTANCE} is
	 * immediately returned</li>
	 * </ul>
	 * </li>
	 * <li>if the set of possible types is not empty, it is returned as-is,
	 * otherwise a singleton set containing {@link Untyped#INSTANCE} is
	 * returned</li>
	 * </ul>
	 * 
	 * @param left  the left-hand side of the operation
	 * @param right the right-hand side of the operation
	 * 
	 * @return the set of possible runtime types
	 */
	public default ExternalSet<Type> commonNumericalType(SymbolicExpression left, SymbolicExpression right) {
		if (left.getTypes().noneMatch(Type::isNumericType) && right.getTypes().noneMatch(Type::isNumericType))
			// if none have numeric types in them, we cannot really compute the
			// result
			return Caches.types().mkSingletonSet(Untyped.INSTANCE);

		ExternalSet<Type> result = Caches.types().mkEmptySet();
		for (Type t1 : left.getTypes().filter(type -> type.isNumericType() || type.isUntyped()))
			for (Type t2 : right.getTypes().filter(type -> type.isNumericType() || type.isUntyped()))
				if (t1.isUntyped() && t2.isUntyped())
					// we do not really consider this case,
					// it will fall back into the last corner case before return
					continue;
				else if (t1.isUntyped())
					result.add(t2);
				else if (t2.isUntyped())
					result.add(t1);
				else if (t1.canBeAssignedTo(t2))
					result.add(t2);
				else if (t2.canBeAssignedTo(t1))
					result.add(t1);
				else
					return Caches.types().mkSingletonSet(Untyped.INSTANCE);
		if (result.isEmpty())
			result.add(Untyped.INSTANCE);
		return result;
	}
}
