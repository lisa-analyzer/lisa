package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.util.collections.ExternalSet;

public interface BinaryNumericalOperation {
	public default ExternalSet<Type> commonNumericalType(SymbolicExpression left, SymbolicExpression right) {
		if (left.getTypes().noneMatch(Type::isNumericType) && right.getTypes().noneMatch(Type::isNumericType))
			// if none have numeric types in them, we cannot really compute the result
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
