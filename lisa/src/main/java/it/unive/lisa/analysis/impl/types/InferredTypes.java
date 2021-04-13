package it.unive.lisa.analysis.impl.types;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.inference.BaseInferredValue;
import it.unive.lisa.analysis.inference.InferenceSystem;
import it.unive.lisa.analysis.inference.InferredValue;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.types.BoolType;
import it.unive.lisa.symbolic.types.IntType;
import it.unive.lisa.symbolic.types.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.Utils;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * An {@link InferredValue} holding a set of {@link Type}s, representing the
 * inferred runtime types of an {@link Expression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InferredTypes extends BaseInferredValue<InferredTypes> {

	private static final InferredTypes TOP = new InferredTypes(Caches.types().mkUniversalSet());

	private static final InferredTypes BOTTOM = new InferredTypes(Caches.types().mkEmptySet());

	private final ExternalSet<Type> elements;

	/**
	 * Builds the inferred types. The object built through this constructor
	 * represents an empty set of types.
	 */
	public InferredTypes() {
		this(Caches.types().mkEmptySet());
	}

	/**
	 * Builds the inferred types, representing only the given {@link Type}.
	 * 
	 * @param type the type to be included in the set of inferred types
	 */
	InferredTypes(Type type) {
		this(Caches.types().mkSingletonSet(type));
	}

	/**
	 * Builds the inferred types, representing only the given set of
	 * {@link Type}s.
	 * 
	 * @param types the types to be included in the set of inferred types
	 */
	InferredTypes(ExternalSet<Type> types) {
		this.elements = types;
	}

	/**
	 * Yields the {@link ExternalSet} containing the types held by this
	 * instance.
	 * 
	 * @return the set of types inside this instance
	 */
	public ExternalSet<Type> getRuntimeTypes() {
		return elements;
	}

	@Override
	public InferredTypes top() {
		return TOP;
	}

	@Override
	public InferredTypes bottom() {
		return BOTTOM;
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

		Set<Type> tmp = new TreeSet<>(
				(l, r) -> Utils.nullSafeCompare(true, l, r, (ll, rr) -> ll.toString().compareTo(rr.toString())));
		tmp.addAll(elements);
		return tmp.toString();
	}

	@Override
	protected InferredTypes evalIdentifier(Identifier id, InferenceSystem<InferredTypes> environment) {
		InferredTypes eval = super.evalIdentifier(id, environment);
		if (!eval.isTop() && !eval.isBottom())
			return eval;
		return new InferredTypes(id.getTypes());
	}

	@Override
	protected InferredTypes evalPushAny(PushAny pushAny) {
		return new InferredTypes(pushAny.getTypes());
	}

	@Override
	protected InferredTypes evalNullConstant(ProgramPoint pp) {
		return new InferredTypes(NullType.INSTANCE);
	}

	@Override
	protected InferredTypes evalNonNullConstant(Constant constant, ProgramPoint pp) {
		return new InferredTypes(constant.getDynamicType());
	}

	@Override
	protected InferredTypes evalUnaryExpression(UnaryOperator operator, InferredTypes arg, ProgramPoint pp) {
		switch (operator) {
		case LOGICAL_NOT:
			if (arg.elements.noneMatch(Type::isBooleanType))
				return bottom();
			return new InferredTypes(BoolType.INSTANCE);
		case NUMERIC_NEG:
			if (arg.elements.noneMatch(Type::isNumericType))
				return bottom();
			return new InferredTypes(arg.elements.filter(Type::isNumericType));
		case STRING_LENGTH:
			if (arg.elements.noneMatch(Type::isStringType))
				return bottom();
			return new InferredTypes(IntType.INSTANCE);
		case TYPEOF:
			return new InferredTypes(new TypeTokenType(arg.elements.copy()));
		default:
			return top();
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
			if (left.elements.noneMatch(Type::isStringType) || right.elements.noneMatch(Type::isStringType))
				return bottom();
			return new InferredTypes(StringType.INSTANCE);
		case STRING_INDEX_OF:
			if (left.elements.noneMatch(Type::isStringType) || right.elements.noneMatch(Type::isStringType))
				return bottom();
			return new InferredTypes(IntType.INSTANCE);
		case STRING_CONTAINS:
		case STRING_ENDS_WITH:
		case STRING_EQUALS:
		case STRING_STARTS_WITH:
			if (left.elements.noneMatch(Type::isStringType) || right.elements.noneMatch(Type::isStringType))
				return bottom();
			return new InferredTypes(BoolType.INSTANCE);
		case TYPE_CAST:
			return evalTypeCast(null, left, right);
		case TYPE_CONV:
			return evalTypeConv(null, left, right);
		case TYPE_CHECK:
			if (right.elements.noneMatch(Type::isTypeTokenType))
				return bottom();
			return new InferredTypes(BoolType.INSTANCE);
		default:
			return top();
		}
	}

	@Override
	protected InferredTypes evalTernaryExpression(TernaryOperator operator, InferredTypes left, InferredTypes middle,
			InferredTypes right, ProgramPoint pp) {
		switch (operator) {
		case STRING_SUBSTRING:
			if (left.elements.noneMatch(Type::isStringType)
					|| middle.elements.noneMatch(Type::isNumericType)
					|| middle.elements.filter(Type::isNumericType).noneMatch(t -> t.asNumericType().isIntegral())
					|| right.elements.noneMatch(Type::isNumericType)
					|| right.elements.filter(Type::isNumericType).noneMatch(t -> t.asNumericType().isIntegral()))
				return bottom();
			return new InferredTypes(StringType.INSTANCE);
		case STRING_REPLACE:
			if (left.elements.noneMatch(Type::isStringType) || middle.elements.noneMatch(Type::isStringType)
					|| right.elements.noneMatch(Type::isStringType))
				return bottom();
			return new InferredTypes(StringType.INSTANCE);
		default:
			return top();
		}
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
			ExternalSet<Type> set = cast(left.elements, right.elements);
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
		return true;
	}

	/**
	 * Simulates a cast operation, where an expression with possible runtime
	 * types {@code types} is being cast to one of the possible type tokens in
	 * {@code tokens}. All types in {@code tokens} that are not
	 * {@link TypeTokenType}s, according to {@link Type#isTypeTokenType()}, will
	 * be ignored. The returned set contains a subset of the types in
	 * {@code types}, keeping only the ones that can be assigned to one of the
	 * types represented by one of the tokens in {@code tokens}.
	 * 
	 * @param types  the types of the expression being casted
	 * @param tokens the tokens representing the operand of the cast
	 * 
	 * @return the set of possible types after the cast
	 */
	ExternalSet<Type> cast(ExternalSet<Type> types, ExternalSet<Type> tokens) {
		ExternalSet<Type> result = Caches.types().mkEmptySet();
		for (Type token : tokens.filter(Type::isTypeTokenType).multiTransform(t -> t.asTypeTokenType().getTypes()))
			for (Type t : types)
				if (t.canBeAssignedTo(token))
					result.add(t);

		return result;
	}

	/**
	 * Simulates a conversion operation, where an expression with possible
	 * runtime types {@code types} is being converted to one of the possible
	 * type tokens in {@code tokens}. All types in {@code tokens} that are not
	 * {@link TypeTokenType}s, according to {@link Type#isTypeTokenType()}, will
	 * be ignored. The returned set contains a subset of the types in
	 * {@code tokens}, keeping only the ones such that there exists at least one
	 * type in {@code types} that can be assigned to it.
	 * 
	 * @param types  the types of the expression being converted
	 * @param tokens the tokens representing the operand of the type conversion
	 * 
	 * @return the set of possible types after the type conversion
	 */
	ExternalSet<Type> convert(ExternalSet<Type> types, ExternalSet<Type> tokens) {
		ExternalSet<Type> result = Caches.types().mkEmptySet();
		for (Type token : tokens.filter(Type::isTypeTokenType).multiTransform(t -> t.asTypeTokenType().getTypes()))
			for (Type t : types)
				if (t.canBeAssignedTo(token))
					result.add(token);

		return result;
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
	ExternalSet<Type> commonNumericalType(ExternalSet<Type> left, ExternalSet<Type> right) {
		if (left.noneMatch(Type::isNumericType) && right.noneMatch(Type::isNumericType))
			// if none have numeric types in them,
			// we cannot really compute the
			return Caches.types().mkEmptySet();

		ExternalSet<Type> result = Caches.types().mkEmptySet();
		for (Type t1 : left.filter(type -> type.isNumericType() || type.isUntyped()))
			for (Type t2 : right.filter(type -> type.isNumericType() || type.isUntyped()))
				if (t1.isUntyped() && t2.isUntyped())
					result.add(t1);
				else if (t1.isUntyped())
					result.add(t2);
				else if (t2.isUntyped())
					result.add(t1);
				else
					result.add(t1.commonSupertype(t2));

		return result;
	}

	@Override
	protected InferredTypes evalTypeCast(BinaryExpression cast, InferredTypes left, InferredTypes right) {
		if (right.elements.noneMatch(Type::isTypeTokenType))
			return bottom();
		ExternalSet<Type> set = cast(left.elements, right.elements);
		if (set.isEmpty())
			return bottom();
		return new InferredTypes(set);
	}

	@Override
	protected InferredTypes evalTypeConv(BinaryExpression conv, InferredTypes left, InferredTypes right) {
		if (right.elements.noneMatch(Type::isTypeTokenType))
			return bottom();
		ExternalSet<Type> set = convert(left.elements, right.elements);
		if (set.isEmpty())
			return bottom();
		return new InferredTypes(set);
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		// Type analysis tracks information on any identifier
		return true;
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		// Type analysis can process any expression
		return true;
	}
}
