package it.unive.lisa.analysis.impl.types;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.cfg.type.NullType;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;
import it.unive.lisa.symbolic.types.BoolType;
import it.unive.lisa.symbolic.types.IntType;
import it.unive.lisa.symbolic.types.StringType;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.util.collections.ExternalSet;
import it.unive.lisa.util.collections.ExternalSetCache;

/**
 * A type inference that collects the set of possible {@link Type}s of
 * {@link Identifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeInference extends BaseNonRelationalValueDomain<TypeInference> {

	private static final ExternalSetCache<Type> TYPES_CACHE = Caches.types();

	private static final TypeInference TOP = new TypeInference(Untyped.INSTANCE, true, false);

	private static final TypeInference BOTTOM = new TypeInference(Untyped.INSTANCE, false, true);

	private final ExternalSet<Type> types;

	private final boolean isTop, isBottom;

	/**
	 * Builds a type inference object that corresponds to the top element.
	 */
	public TypeInference() {
		this(Untyped.INSTANCE, true, false);
	}

	private TypeInference(Type type) {
		this(type, false, false);
	}

	private TypeInference(ExternalSet<Type> types) {
		this(types, false, false);
	}

	private TypeInference(Type type, boolean isTop, boolean isBottom) {
		this(TYPES_CACHE.mkSingletonSet(type), isBottom, isTop);
	}

	private TypeInference(ExternalSet<Type> types, boolean isTop, boolean isBottom) {
		this.types = types.copy();
		this.isBottom = isBottom;
		this.isTop = isTop;
	}

	@Override
	public TypeInference top() {
		return TOP;
	}

	@Override
	public TypeInference bottom() {
		return BOTTOM;
	}

	@Override
	protected TypeInference lubAux(TypeInference other) throws SemanticException {
		return new TypeInference(types.union(other.types));
	}

	@Override
	protected TypeInference wideningAux(TypeInference other) throws SemanticException {
		return lubAux(other); // TODO not sure this is fine for languages like
								// javascript
	}

	@Override
	protected boolean lessOrEqualAux(TypeInference other) throws SemanticException {
		return other.types.contains(types);
	}

	@Override
	protected TypeInference evalNullConstant() {
		return new TypeInference(NullType.INSTANCE);
	}

	@Override
	protected TypeInference evalNonNullConstant(Constant constant) {
		return new TypeInference(constant.getTypes());
	}

	@Override
	protected TypeInference evalTypeConversion(Type type, TypeInference arg) {
		if (arg.types.noneMatch(t -> t.canBeAssignedTo(type)))
			return bottom();
		return new TypeInference(arg.types.filter(t -> t.canBeAssignedTo(type)));
	}

	@Override
	protected TypeInference evalUnaryExpression(UnaryOperator operator, TypeInference arg) {
		switch (operator) {
		case LOGICAL_NOT:
			if (arg.types.noneMatch(Type::isBooleanType))
				return bottom();
			return new TypeInference(BoolType.INSTANCE);
		case NUMERIC_NEG:
			if (arg.types.noneMatch(Type::isNumericType))
				return bottom();
			return new TypeInference(arg.types.filter(Type::isNumericType));
		case STRING_LENGTH:
			if (arg.types.noneMatch(Type::isStringType))
				return bottom();
			return new TypeInference(IntType.INSTANCE);
		}
		return top();
	}

	@Override
	protected TypeInference evalBinaryExpression(BinaryOperator operator, TypeInference left, TypeInference right) {
		switch (operator) {
		case COMPARISON_EQ:
		case COMPARISON_NE:
			return new TypeInference(BoolType.INSTANCE);
		case COMPARISON_GE:
		case COMPARISON_GT:
		case COMPARISON_LE:
		case COMPARISON_LT:
			if (left.types.noneMatch(Type::isNumericType) || right.types.noneMatch(Type::isNumericType))
				return bottom();
			return new TypeInference(BoolType.INSTANCE);
		case LOGICAL_AND:
		case LOGICAL_OR:
			if (left.types.noneMatch(Type::isBooleanType) || right.types.noneMatch(Type::isBooleanType))
				return bottom();
			return new TypeInference(BoolType.INSTANCE);
		case NUMERIC_ADD:
		case NUMERIC_DIV:
		case NUMERIC_MOD:
		case NUMERIC_MUL:
		case NUMERIC_SUB:
			if (left.types.noneMatch(Type::isNumericType) || right.types.noneMatch(Type::isNumericType))
				return bottom();
			ExternalSet<Type> result = TYPES_CACHE.mkEmptySet();
			for (Type t1 : left.types.filter(type -> type.isNumericType()))
				for (Type t2 : right.types.filter(type -> type.isNumericType()))
					if (t1.canBeAssignedTo(t2))
						result.add(t2);
					else if (t2.canBeAssignedTo(t1))
						result.add(t1);
					else
						return bottom();
			return new TypeInference(result);
		case STRING_CONCAT:
			if (left.types.noneMatch(Type::isStringType) || right.types.noneMatch(Type::isStringType))
				return bottom();
			return new TypeInference(StringType.INSTANCE);
		case STRING_CONTAINS:
		case STRING_ENDS_WITH:
		case STRING_EQUALS:
		case STRING_STARTS_WITH:
			if (left.types.noneMatch(Type::isStringType) || right.types.noneMatch(Type::isStringType))
				return bottom();
			return new TypeInference(BoolType.INSTANCE);
		case STRING_INDEX_OF:
			if (left.types.noneMatch(Type::isStringType) || right.types.noneMatch(Type::isStringType))
				return bottom();
			return new TypeInference(IntType.INSTANCE);
		}
		return top();
	}

	@Override
	protected TypeInference evalTernaryExpression(TernaryOperator operator, TypeInference left, TypeInference middle,
			TypeInference right) {
		switch (operator) {
		case STRING_REPLACE:
		case STRING_SUBSTRING:
			if (left.types.noneMatch(Type::isStringType) || middle.types.noneMatch(Type::isStringType)
					|| right.types.noneMatch(Type::isStringType))
				return bottom();
			return new TypeInference(StringType.INSTANCE);
		}
		return top();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isBottom ? 1231 : 1237);
		result = prime * result + (isTop ? 1231 : 1237);
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeInference other = (TypeInference) obj;
		if (isBottom != other.isBottom)
			return false;
		if (isTop != other.isTop)
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

	@Override
	public String representation() {
		if (isTop())
			return "TOP";

		if (isBottom())
			return "BOTTOM";

		return types.toString();
	}

	@Override
	public String toString() {
		return representation();
	}
}
