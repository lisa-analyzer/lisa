package it.unive.lisa.analysis.types;

import java.util.concurrent.atomic.AtomicBoolean;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.inference.BaseInferredValue;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.SetRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.types.BoolType;
import it.unive.lisa.symbolic.types.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.common.Int32;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An {@link InferredValue} holding a set of {@link Type}s, representing the
 * inferred runtime types of an {@link Expression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InferredTypes extends BaseInferredValue<InferredTypes> {

	private static final InferredTypes TOP = new InferredTypes(Caches.types().mkUniversalSet());

	private static final InferredTypes BOTTOM = new InferredTypes(Caches.types().mkEmptySet());

	private static final InferredPair<InferredTypes> BOTTOM_PAIR = new InferredPair<>(BOTTOM, BOTTOM, BOTTOM);

	private static final InferredPair<InferredTypes> TOP_PAIR = new InferredPair<>(TOP, TOP, TOP);

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
	public DomainRepresentation representation() {
		if (isTop())
			return Lattice.TOP_REPR;

		if (isBottom())
			return Lattice.BOTTOM_REPR;

		return new SetRepresentation(elements, StringRepresentation::new);
	}

	private InferredPair<InferredTypes> mk(InferredTypes types) {
		return new InferredPair<>(this, types, BOTTOM);
	}

	@Override
	protected InferredPair<InferredTypes> evalIdentifier(Identifier id, InferenceSystem<InferredTypes> environment,
			ProgramPoint pp) throws SemanticException {
		InferredPair<InferredTypes> eval = super.evalIdentifier(id, environment, pp);
		if (!eval.getInferred().isTop() && !eval.getInferred().isBottom())
			return eval;
		return mk(new InferredTypes(id.getTypes()));
	}

	@Override
	protected InferredPair<InferredTypes> evalPushAny(PushAny pushAny, InferredTypes state, ProgramPoint pp) {
		return mk(new InferredTypes(pushAny.getTypes()));
	}

	@Override
	protected InferredPair<InferredTypes> evalNullConstant(InferredTypes state, ProgramPoint pp) {
		return mk(new InferredTypes(NullType.INSTANCE));
	}

	@Override
	protected InferredPair<InferredTypes> evalNonNullConstant(Constant constant, InferredTypes state, ProgramPoint pp) {
		return mk(new InferredTypes(constant.getDynamicType()));
	}

	@Override
	protected InferredPair<InferredTypes> evalUnaryExpression(UnaryOperator operator, InferredTypes arg,
			InferredTypes state, ProgramPoint pp) {
		switch (operator) {
		case LOGICAL_NOT:
			if (arg.elements.noneMatch(Type::isBooleanType))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(BoolType.INSTANCE));
		case NUMERIC_NEG:
			if (arg.elements.noneMatch(Type::isNumericType))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(arg.elements.filter(Type::isNumericType)));
		case STRING_LENGTH:
			if (arg.elements.noneMatch(Type::isStringType))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(Int32.INSTANCE));
		case TYPEOF:
			return mk(new InferredTypes(new TypeTokenType(arg.elements.copy())));
		default:
			return TOP_PAIR;
		}
	}

	@Override
	protected InferredPair<InferredTypes> evalBinaryExpression(BinaryOperator operator, InferredTypes left,
			InferredTypes right, InferredTypes state,
			ProgramPoint pp) {
		switch (operator) {
		case COMPARISON_EQ:
		case COMPARISON_NE:
			return mk(new InferredTypes(BoolType.INSTANCE));
		case COMPARISON_GE:
		case COMPARISON_GT:
		case COMPARISON_LE:
		case COMPARISON_LT:
			if (left.elements.noneMatch(Type::isNumericType) || right.elements.noneMatch(Type::isNumericType))
				return BOTTOM_PAIR;
			ExternalSet<Type> set = commonNumericalType(left.elements, right.elements);
			if (set.isEmpty())
				return BOTTOM_PAIR;
			return mk(new InferredTypes(BoolType.INSTANCE));
		case LOGICAL_AND:
		case LOGICAL_OR:
			if (left.elements.noneMatch(Type::isBooleanType) || right.elements.noneMatch(Type::isBooleanType))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(BoolType.INSTANCE));
		case NUMERIC_ADD:
		case NUMERIC_DIV:
		case NUMERIC_MOD:
		case NUMERIC_MUL:
		case NUMERIC_SUB:
			if (left.elements.noneMatch(Type::isNumericType) || right.elements.noneMatch(Type::isNumericType))
				return BOTTOM_PAIR;
			set = commonNumericalType(left.elements, right.elements);
			if (set.isEmpty())
				return BOTTOM_PAIR;
			return mk(new InferredTypes(set));
		case STRING_CONCAT:
			if (left.elements.noneMatch(Type::isStringType) || right.elements.noneMatch(Type::isStringType))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(StringType.INSTANCE));
		case STRING_INDEX_OF:
			if (left.elements.noneMatch(Type::isStringType) || right.elements.noneMatch(Type::isStringType))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(Int32.INSTANCE));
		case STRING_CONTAINS:
		case STRING_ENDS_WITH:
		case STRING_EQUALS:
		case STRING_STARTS_WITH:
			if (left.elements.noneMatch(Type::isStringType) || right.elements.noneMatch(Type::isStringType))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(BoolType.INSTANCE));
		case TYPE_CAST:
			return evalTypeCast(null, left, right, state, pp);
		case TYPE_CONV:
			return evalTypeConv(null, left, right, state, pp);
		case TYPE_CHECK:
			if (right.elements.noneMatch(Type::isTypeTokenType))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(BoolType.INSTANCE));
		default:
			return TOP_PAIR;
		}
	}

	@Override
	protected InferredPair<InferredTypes> evalTernaryExpression(TernaryOperator operator, InferredTypes left,
			InferredTypes middle,
			InferredTypes right, InferredTypes state, ProgramPoint pp) {
		switch (operator) {
		case STRING_SUBSTRING:
			if (left.elements.noneMatch(Type::isStringType)
					|| middle.elements.noneMatch(Type::isNumericType)
					|| middle.elements.filter(Type::isNumericType).noneMatch(t -> t.asNumericType().isIntegral())
					|| right.elements.noneMatch(Type::isNumericType)
					|| right.elements.filter(Type::isNumericType).noneMatch(t -> t.asNumericType().isIntegral()))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(StringType.INSTANCE));
		case STRING_REPLACE:
			if (left.elements.noneMatch(Type::isStringType) || middle.elements.noneMatch(Type::isStringType)
					|| right.elements.noneMatch(Type::isStringType))
				return BOTTOM_PAIR;
			return mk(new InferredTypes(StringType.INSTANCE));
		default:
			return TOP_PAIR;
		}
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, InferredTypes left,
			InferredTypes right, InferredTypes state, ProgramPoint pp) {
		switch (operator) {
		case COMPARISON_EQ:
		case COMPARISON_NE:
			if (!left.elements.allMatch(Type::isTypeTokenType) || !right.elements.allMatch(Type::isTypeTokenType))
				// if there is at least one element that is not a type
				// token, than we cannot reason about it
				return Satisfiability.UNKNOWN;

			ExternalSet<Type> lfiltered = left.elements.filter(Type::isTypeTokenType);
			ExternalSet<Type> rfiltered = right.elements.filter(Type::isTypeTokenType);
			if (operator == BinaryOperator.COMPARISON_EQ) {
				if (left.elements.size() == 1 && left.elements.equals(right.elements))
					// only one element, and it is the same
					return Satisfiability.SATISFIED;
				else if (!left.elements.intersects(right.elements) && !typeTokensIntersect(lfiltered, rfiltered))
					// no common elements, they cannot be equal
					return Satisfiability.NOT_SATISFIED;
				else
					// we don't know really
					return Satisfiability.UNKNOWN;
			} else {
				if (!left.elements.intersects(right.elements) && !typeTokensIntersect(lfiltered, rfiltered))
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
			if (evalBinaryExpression(BinaryOperator.TYPE_CAST, left, right, state, pp).isBottom())
				// no common types, the check will always fail
				return Satisfiability.NOT_SATISFIED;
			AtomicBoolean mightFail = new AtomicBoolean();
			ExternalSet<Type> set = cast(left.elements, right.elements, mightFail);
			if (left.elements.equals(set) && !mightFail.get())
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

	/**
	 * Checks whether or not the two given set of type tokens intersects,
	 * meaning that there exists at least one type token {@code t1} from
	 * {@code lfiltered} and one type token {@code t2} from {@code rfiltered}
	 * such that {@code t1.getTypes().intersects(t2.getTypes())}.<br>
	 * <br>
	 * Note that all types in both sets received as parameters are assumed to be
	 * {@link TypeTokenType}s, hence no type check is performed before
	 * converting them.
	 * 
	 * @param lfiltered the first set of type tokens
	 * @param rfiltered the second set of type tokens
	 * 
	 * @return {@code true} if the sets of tokens intersect
	 * 
	 * @throws NullPointerException if one of the types is not a
	 *                                  {@link TypeTokenType} (this is due to
	 *                                  the conversion)
	 */
	static boolean typeTokensIntersect(ExternalSet<Type> lfiltered, ExternalSet<Type> rfiltered) {
		for (Type l : lfiltered)
			for (Type r : rfiltered)
				if (l.asTypeTokenType().getTypes().intersects(r.asTypeTokenType().getTypes()))
					return true;

		return false;
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
	 * @param types     the types of the expression being casted
	 * @param tokens    the tokens representing the operand of the cast
	 * @param mightFail a reference to the boolean to set if this cast might
	 *                      fail (e.g., casting an [int, string] to an int will
	 *                      yield a set containing int and will set the boolean
	 *                      to true)
	 * 
	 * @return the set of possible types after the cast
	 */
	ExternalSet<Type> cast(ExternalSet<Type> types, ExternalSet<Type> tokens, AtomicBoolean mightFail) {
		if (mightFail != null)
			mightFail.set(false);

		ExternalSet<Type> result = Caches.types().mkEmptySet();
		for (Type token : tokens.filter(Type::isTypeTokenType).multiTransform(t -> t.asTypeTokenType().getTypes()))
			for (Type t : types)
				if (t.canBeAssignedTo(token))
					result.add(t);
				else if (mightFail != null)
					mightFail.set(true);

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
	protected InferredPair<InferredTypes> evalTypeCast(BinaryExpression cast, InferredTypes left, InferredTypes right,
			InferredTypes state, ProgramPoint pp) {
		if (right.elements.noneMatch(Type::isTypeTokenType))
			return BOTTOM_PAIR;
		ExternalSet<Type> set = cast(left.elements, right.elements, null);
		if (set.isEmpty())
			return BOTTOM_PAIR;
		return mk(new InferredTypes(set));
	}

	@Override
	protected InferredPair<InferredTypes> evalTypeConv(BinaryExpression conv, InferredTypes left, InferredTypes right,
			InferredTypes state, ProgramPoint pp) {
		if (right.elements.noneMatch(Type::isTypeTokenType))
			return BOTTOM_PAIR;
		ExternalSet<Type> set = convert(left.elements, right.elements);
		if (set.isEmpty())
			return BOTTOM_PAIR;
		return mk(new InferredTypes(set));
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		return !(id instanceof MemoryPointer);
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		// Type analysis can process any expression
		return true;
	}
}
