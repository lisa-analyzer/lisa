package it.unive.lisa.analysis.impl.types;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.inference.BaseInferredValue;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.types.BoolType;
import it.unive.lisa.symbolic.types.IntType;
import it.unive.lisa.symbolic.types.StringType;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * An {@link InferredValue} holding a set of {@link Type}s, representing the
 * inferred runtime types of an {@link Expression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InferredTypes extends BaseInferredValue<InferredTypes> {

	private static final InferredTypes TOP = new InferredTypes();

	private static final InferredTypes BOTTOM = new InferredTypes(Caches.types().mkEmptySet(), false, true);

	private final ExternalSet<Type> elements;

	private final boolean isTop, isBottom;

	/**
	 * Builds the inferred types. The object built through this constructor
	 * represents the top of the lattice.
	 */
	public InferredTypes() {
		this(Caches.types().mkUniversalSet(), true, false);
	}

	private InferredTypes(Type type) {
		this(Caches.types().mkSingletonSet(type), false, false);
	}

	private InferredTypes(ExternalSet<Type> types) {
		this(types, false, false);
	}

	private InferredTypes(ExternalSet<Type> types, boolean isTop, boolean isBottom) {
		this.elements = types;
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	/**
	 * Yields the {@link ExternalSet} containing the types held by this
	 * instance.
	 * 
	 * @return the set of types inside this instance
	 */
	public ExternalSet<Type> getRuntimeTypes() {
		return (ExternalSet<Type>) elements;
	}

	@Override
	public InferredTypes top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public InferredTypes bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return isBottom;
	}

	@Override
	public InferredTypes executionState() {
		return bottom();
	}

	@Override
	public String representation() {
		if (isTop())
			return Lattice.TOP_STRING;

		if (isBottom())
			return Lattice.BOTTOM_STRING;

		return elements.toString();
	}

	@Override
	protected InferredTypes evalNullConstant(ProgramPoint pp) {
		return new InferredTypes(Caches.types().mkSingletonSet(NullType.INSTANCE));
	}

	@Override
	protected InferredTypes evalNonNullConstant(Constant constant, ProgramPoint pp) {
		return new InferredTypes(Caches.types().mkSingletonSet(constant.getDynamicType()));
	}

	@Override
	protected InferredTypes evalUnaryExpression(UnaryOperator operator, InferredTypes arg, ProgramPoint pp) {
		switch (operator) {
		case LOGICAL_NOT:
			if (arg.elements.noneMatch(Type::isBooleanType))
				return bottom();
			return new InferredTypes(arg.elements.filter(Type::isBooleanType));
		case NUMERIC_NEG:
			if (arg.elements.noneMatch(Type::isNumericType))
				return bottom();
			return new InferredTypes(arg.elements.filter(Type::isNumericType));
		case STRING_LENGTH:
			if (arg.elements.noneMatch(Type::isStringType))
				return bottom();
			return new InferredTypes(IntType.INSTANCE);
		case TYPEOF:
			return new InferredTypes(new TypeTokenType(arg.elements));
		default:
			return bottom();
		}
	}

	@Override
	protected InferredTypes evalBinaryExpression(BinaryOperator operator, InferredTypes left, InferredTypes right,
			ProgramPoint pp) {
		switch (operator) {
		case COMPARISON_EQ:
		case COMPARISON_NE:
			return new InferredTypes(BoolType.INSTANCE);
		case COMPARISON_GE:
		case COMPARISON_GT:
		case COMPARISON_LE:
		case COMPARISON_LT:
			if (left.elements.noneMatch(Type::isNumericType) || right.elements.noneMatch(Type::isNumericType))
				return bottom();
			ExternalSet<Type> set = commonNumericalType(left.elements, right.elements);
			if (set.isEmpty())
				return bottom();
			return new InferredTypes(BoolType.INSTANCE);
		case LOGICAL_AND:
		case LOGICAL_OR:
			if (left.elements.noneMatch(Type::isBooleanType) || right.elements.noneMatch(Type::isBooleanType))
				return bottom();
			return new InferredTypes(BoolType.INSTANCE);
		case NUMERIC_ADD:
		case NUMERIC_DIV:
		case NUMERIC_MOD:
		case NUMERIC_MUL:
		case NUMERIC_SUB:
			if (left.elements.noneMatch(Type::isNumericType) || right.elements.noneMatch(Type::isNumericType))
				return bottom();
			set = commonNumericalType(left.elements, right.elements);
			if (set.isEmpty())
				return bottom();
			return new InferredTypes(set);
		case STRING_CONCAT:
		case STRING_CONTAINS:
		case STRING_ENDS_WITH:
		case STRING_EQUALS:
		case STRING_INDEX_OF:
		case STRING_STARTS_WITH:
			if (left.elements.noneMatch(Type::isStringType) || right.elements.noneMatch(Type::isStringType))
				return bottom();
			return new InferredTypes(StringType.INSTANCE);
		case TYPE_CAST:
			if (right.elements.noneMatch(Type::isTypeTokenType))
				return bottom();
			set = right.elements.filter(r -> left.elements.anyMatch(l -> l.canBeAssignedTo(r)));
			if (set.isEmpty())
				return bottom();
			return new InferredTypes(BoolType.INSTANCE);
		case TYPE_CHECK:
			if (right.elements.noneMatch(Type::isTypeTokenType))
				return bottom();
			return new InferredTypes(BoolType.INSTANCE);
		default:
			return bottom();
		}
	}

	@Override
	protected InferredTypes evalTernaryExpression(TernaryOperator operator, InferredTypes left, InferredTypes middle,
			InferredTypes right, ProgramPoint pp) {
		switch (operator) {
		case STRING_SUBSTRING:
			if (left.elements.noneMatch(Type::isStringType) || middle.elements.noneMatch(Type::isNumericType)
					|| right.elements.noneMatch(Type::isNumericType))
				return bottom();
			return new InferredTypes(StringType.INSTANCE);
		case STRING_REPLACE:
			if (left.elements.noneMatch(Type::isStringType) || middle.elements.noneMatch(Type::isStringType)
					|| right.elements.noneMatch(Type::isStringType))
				return bottom();
			return new InferredTypes(StringType.INSTANCE);
		default:
			return bottom();
		}
	}

	@Override
	protected Satisfiability satisfiesAbstractValue(InferredTypes value, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNullConstant(ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, InferredTypes arg, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, InferredTypes left,
			InferredTypes right, ProgramPoint pp) {
		switch (operator) {
		case COMPARISON_EQ:
		case COMPARISON_NE:
			if (!left.elements.allMatch(Type::isTypeTokenType) || !right.elements.allMatch(Type::isTypeTokenType))
				// if there is at least one element that is not a type
				// token, than we cannot reason about it
				return Satisfiability.UNKNOWN;

			if (operator == BinaryOperator.COMPARISON_EQ) {
				if (left.elements.size() == 1 && left.elements.equals(right.elements))
					// only one element, and it is the same
					return Satisfiability.SATISFIED;
				else if (!left.elements.intersects(right.elements))
					// no common elements, they cannot be equal
					return Satisfiability.NOT_SATISFIED;
				else
					// we don't know really
					return Satisfiability.UNKNOWN;
			} else {
				if (!left.elements.intersects(right.elements))
					// no common elements, they cannot be equal
					return Satisfiability.SATISFIED;
				else if (left.elements.size() == 1 && left.elements.equals(right.elements))
					// only one element, and it is the same
					return Satisfiability.NOT_SATISFIED;
				else
					// we don't know really
					return Satisfiability.UNKNOWN;
			}
		case TYPE_CHECK:
			if (evalBinaryExpression(BinaryOperator.TYPE_CAST, left, right, pp).isBottom())
				// no common types, the check will always fail
				return Satisfiability.NOT_SATISFIED;
			ExternalSet<Type> set = left.elements.filter(l -> right.elements.anyMatch(r -> l.canBeAssignedTo(r)));
			if (left.elements.equals(set))
				// if all the types stayed in 'set' then the there is no
				// execution that reach the expression with a type that cannot
				// be casted, and thus this is a tautology
				return Satisfiability.SATISFIED;

			// sometimes yes, sometimes no
			return Satisfiability.UNKNOWN;
		default:
			return Satisfiability.UNKNOWN;
		}
	}

	@Override
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, InferredTypes left,
			InferredTypes middle, InferredTypes right, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected InferredTypes lubAux(InferredTypes other) throws SemanticException {
		return new InferredTypes(elements.union(other.elements));
	}

	@Override
	protected InferredTypes wideningAux(InferredTypes other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(InferredTypes other) throws SemanticException {
		return other.elements.contains(elements);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
		result = prime * result + (isBottom ? 1231 : 1237);
		result = prime * result + (isTop ? 1231 : 1237);
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
		InferredTypes other = (InferredTypes) obj;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		if (isBottom != other.isBottom)
			return false;
		if (isTop != other.isTop)
			return false;
		return true;
	}

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
	private ExternalSet<Type> commonNumericalType(ExternalSet<Type> left, ExternalSet<Type> right) {
		if (left.noneMatch(Type::isNumericType) && right.noneMatch(Type::isNumericType))
			// if none have numeric types in them,
			// we cannot really compute the
			return Caches.types().mkEmptySet();

		ExternalSet<Type> result = Caches.types().mkEmptySet();
		for (Type t1 : left.filter(type -> type.isNumericType() || type.isUntyped()))
			for (Type t2 : right.filter(type -> type.isNumericType() || type.isUntyped()))
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
					return Caches.types().mkEmptySet();

		return result;
	}
}
