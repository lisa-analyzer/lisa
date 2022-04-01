package it.unive.lisa.analysis.types;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.SetRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link InferredValue} holding a set of {@link Type}s, representing the
 * inferred runtime types of an {@link Expression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InferredTypes extends BaseNonRelationalTypeDomain<InferredTypes> {

	private static final InferredTypes TOP = new InferredTypes(Caches.types().mkUniversalSet());

	private static final InferredTypes BOTTOM = new InferredTypes(Caches.types().mkEmptySet());

	private final ExternalSet<Type> elements;

	/**
	 * Builds the inferred types. The object built through this constructor
	 * represents an empty set of types.
	 */
	public InferredTypes() {
		this(Caches.types().mkUniversalSet());
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

	@Override
	public ExternalSet<Type> getRuntimeTypes() {
		return elements;
	}

	@Override
	public InferredTypes top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return super.isTop() || elements.equals(TOP.elements);
	}

	@Override
	public InferredTypes bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return super.isBottom() || elements.equals(BOTTOM.elements);
	}

	@Override
	public DomainRepresentation representation() {
		if (isTop())
			return new StringRepresentation("\"type\" : \"inferredType\", \"value\" : [" + Lattice.TOP_REPR + "]");

		if (isBottom())
			return Lattice.BOTTOM_REPR;

		return new StringRepresentation("\"type\" : \"inferredType\", \"value\" : [" + new SetRepresentation(elements, StringRepresentation::new) + "]");
	}

	@Override
	protected InferredTypes evalIdentifier(Identifier id, TypeEnvironment<InferredTypes> environment,
			ProgramPoint pp) throws SemanticException {
		InferredTypes eval = super.evalIdentifier(id, environment, pp);
		if (!eval.isTop() && !eval.isBottom())
			return eval;
		return new InferredTypes(id.getRuntimeTypes());
	}

	@Override
	protected InferredTypes evalPushAny(PushAny pushAny, ProgramPoint pp) {
		return new InferredTypes(pushAny.getRuntimeTypes());
	}

	@Override
	protected InferredTypes evalNullConstant(ProgramPoint pp) {
		return new InferredTypes(NullType.INSTANCE);
	}

	@Override
	protected InferredTypes evalNonNullConstant(Constant constant, ProgramPoint pp) {
		return new InferredTypes(constant.getStaticType());
	}

	@Override
	protected InferredTypes evalUnaryExpression(UnaryOperator operator, InferredTypes arg,
			ProgramPoint pp) {
		ExternalSet<Type> inferred = operator.typeInference(arg.elements);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(inferred);
	}

	@Override
	protected InferredTypes evalBinaryExpression(BinaryOperator operator, InferredTypes left,
			InferredTypes right, ProgramPoint pp) {
		ExternalSet<Type> inferred = operator.typeInference(left.elements, right.elements);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(inferred);
	}

	@Override
	protected InferredTypes evalTernaryExpression(TernaryOperator operator, InferredTypes left,
			InferredTypes middle, InferredTypes right, ProgramPoint pp) {
		ExternalSet<Type> inferred = operator.typeInference(left.elements, middle.elements, right.elements);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(inferred);
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, InferredTypes left,
			InferredTypes right, ProgramPoint pp) {
		if (operator == ComparisonEq.INSTANCE || operator == ComparisonNe.INSTANCE) {
			if (!left.elements.allMatch(Type::isTypeTokenType) || !right.elements.allMatch(Type::isTypeTokenType))
				// if there is at least one element that is not a type
				// token, than we cannot reason about it
				return Satisfiability.UNKNOWN;

			ExternalSet<Type> lfiltered = left.elements.filter(Type::isTypeTokenType);
			ExternalSet<Type> rfiltered = right.elements.filter(Type::isTypeTokenType);
			if (operator == ComparisonEq.INSTANCE) {
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
		} else if (operator == TypeCheck.INSTANCE) {
			if (evalBinaryExpression(TypeCast.INSTANCE, left, right, pp).isBottom())
				// no common types, the check will always fail
				return Satisfiability.NOT_SATISFIED;
			AtomicBoolean mightFail = new AtomicBoolean();
			ExternalSet<Type> set = Type.cast(left.elements, right.elements, mightFail);
			if (left.elements.equals(set) && !mightFail.get())
				// if all the types stayed in 'set' then the there is no
				// execution that reach the expression with a type that cannot
				// be casted, and thus this is a tautology
				return Satisfiability.SATISFIED;

			// sometimes yes, sometimes no
			return Satisfiability.UNKNOWN;
		}

		return Satisfiability.UNKNOWN;
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

	@Override
	protected InferredTypes evalTypeCast(BinaryExpression cast, InferredTypes left, InferredTypes right,
			ProgramPoint pp) {
		ExternalSet<Type> inferred = cast.getOperator().typeInference(left.elements, right.elements);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(inferred);
	}

	@Override
	protected InferredTypes evalTypeConv(BinaryExpression conv, InferredTypes left, InferredTypes right,
			ProgramPoint pp) {
		ExternalSet<Type> inferred = conv.getOperator().typeInference(left.elements, right.elements);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(inferred);
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
