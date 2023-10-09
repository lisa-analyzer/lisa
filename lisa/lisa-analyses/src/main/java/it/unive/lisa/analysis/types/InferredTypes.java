package it.unive.lisa.analysis.types;

import static org.apache.commons.collections4.CollectionUtils.intersection;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * An {@link InferredValue} holding a set of {@link Type}s, representing the
 * inferred runtime types of an {@link Expression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InferredTypes implements BaseNonRelationalTypeDomain<InferredTypes> {

	private static final InferredTypes BOTTOM = new InferredTypes(null, Collections.emptySet());

	private final Set<Type> elements;

	private final boolean isTop;

	/**
	 * Builds the inferred types. The object built through this constructor
	 * represents an empty set of types.
	 */
	public InferredTypes() {
		this(null, (Set<Type>) null);
	}

	/**
	 * Builds the inferred types, representing only the given {@link Type}.
	 * 
	 * @param typeSystem the type system knowing about the types of the program
	 *                       where this element is created
	 * @param type       the type to be included in the set of inferred types
	 */
	public InferredTypes(
			TypeSystem typeSystem,
			Type type) {
		this(typeSystem, Collections.singleton(type));
	}

	/**
	 * Builds the inferred types, representing only the given set of
	 * {@link Type}s.
	 * 
	 * @param typeSystem the type system knowing about the types of the program
	 *                       where this element is created
	 * @param types      the types to be included in the set of inferred types
	 */
	public InferredTypes(
			TypeSystem typeSystem,
			Set<Type> types) {
		this(typeSystem != null && types.equals(typeSystem.getTypes()), types);
	}

	/**
	 * Builds the inferred types, representing only the given set of
	 * {@link Type}s.
	 * 
	 * @param isTop whether or not the set of types represents all possible
	 *                  types
	 * @param types the types to be included in the set of inferred types
	 */
	public InferredTypes(
			boolean isTop,
			Set<Type> types) {
		this.elements = types;
		this.isTop = isTop;
	}

	@Override
	public Set<Type> getRuntimeTypes() {
		if (elements == null)
			Collections.emptySet();
		return elements;
	}

	@Override
	public InferredTypes top() {
		return new InferredTypes(true, null);
	}

	@Override
	public boolean isTop() {
		return BaseNonRelationalTypeDomain.super.isTop() || isTop;
	}

	@Override
	public InferredTypes bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return BaseNonRelationalTypeDomain.super.isBottom() || BOTTOM.elements.equals(elements);
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();

		if (isBottom())
			return Lattice.bottomRepresentation();

		return new SetRepresentation(elements, StringRepresentation::new);
	}

	@Override
	public InferredTypes evalIdentifier(
			Identifier id,
			TypeEnvironment<InferredTypes> environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		InferredTypes eval = BaseNonRelationalTypeDomain.super.evalIdentifier(id, environment, pp, oracle);
		if (!eval.isTop())
			return eval;
		TypeSystem types = pp.getProgram().getTypes();
		return new InferredTypes(types, id.getStaticType().allInstances(types));
	}

	@Override
	public InferredTypes evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TypeSystem types = pp.getProgram().getTypes();
		if (pushAny.getStaticType().isUntyped())
			return new InferredTypes(true, types.getTypes());
		return new InferredTypes(types, pushAny.getStaticType().allInstances(types));
	}

	@Override
	public InferredTypes evalPushInv(
			PushInv pushInv,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return bottom();
	}

	@Override
	public InferredTypes evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle) {
		return new InferredTypes(pp.getProgram().getTypes(), NullType.INSTANCE);
	}

	@Override
	public InferredTypes evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return new InferredTypes(pp.getProgram().getTypes(), constant.getStaticType());
	}

	@Override
	public InferredTypes evalUnaryExpression(
			UnaryOperator operator,
			InferredTypes arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> elems = arg.isTop() ? types.getTypes() : arg.elements;
		Set<Type> inferred = operator.typeInference(types, elems);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(types, inferred);
	}

	@Override
	public InferredTypes evalBinaryExpression(
			BinaryOperator operator,
			InferredTypes left,
			InferredTypes right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.isTop() ? types.getTypes() : left.elements;
		Set<Type> relems = right.isTop() ? types.getTypes() : right.elements;
		Set<Type> inferred = operator.typeInference(types, lelems, relems);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(types, inferred);
	}

	@Override
	public InferredTypes evalTernaryExpression(
			TernaryOperator operator,
			InferredTypes left,
			InferredTypes middle,
			InferredTypes right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.isTop() ? types.getTypes() : left.elements;
		Set<Type> melems = middle.isTop() ? types.getTypes() : middle.elements;
		Set<Type> relems = right.isTop() ? types.getTypes() : right.elements;
		Set<Type> inferred = operator.typeInference(types, lelems, melems, relems);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(types, inferred);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryOperator operator,
			InferredTypes left,
			InferredTypes right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.isTop() ? types.getTypes() : left.elements;
		Set<Type> relems = right.isTop() ? types.getTypes() : right.elements;
		if (operator == ComparisonEq.INSTANCE || operator == ComparisonNe.INSTANCE) {
			Set<Type> lfiltered = lelems.stream().filter(Type::isTypeTokenType).collect(Collectors.toSet());
			Set<Type> rfiltered = relems.stream().filter(Type::isTypeTokenType).collect(Collectors.toSet());

			if (lelems.size() != lfiltered.size() || relems.size() != rfiltered.size())
				// if there is at least one element that is not a type
				// token, than we cannot reason about it
				return Satisfiability.UNKNOWN;

			if (operator == ComparisonEq.INSTANCE) {
				if (lelems.size() == 1 && lelems.equals(relems))
					// only one element, and it is the same
					return Satisfiability.SATISFIED;
				else if (intersection(lelems, relems).isEmpty()
						&& !typeTokensIntersect(lfiltered, rfiltered))
					// no common elements, they cannot be equal
					return Satisfiability.NOT_SATISFIED;
				else
					// we don't know really
					return Satisfiability.UNKNOWN;
			} else {
				if (intersection(lelems, relems).isEmpty() && !typeTokensIntersect(lfiltered, rfiltered))
					// no common elements, they cannot be equal
					return Satisfiability.SATISFIED;
				else if (lelems.size() == 1 && lelems.equals(relems))
					// only one element, and it is the same
					return Satisfiability.NOT_SATISFIED;
				else
					// we don't know really
					return Satisfiability.UNKNOWN;
			}
		} else if (operator == TypeCheck.INSTANCE) {
			if (evalBinaryExpression(TypeCast.INSTANCE, left, right, pp, oracle).isBottom())
				// no common types, the check will always fail
				return Satisfiability.NOT_SATISFIED;
			AtomicBoolean mightFail = new AtomicBoolean();
			Set<Type> set = types.cast(lelems, relems, mightFail);
			if (lelems.equals(set) && !mightFail.get())
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
	static boolean typeTokensIntersect(
			Set<Type> lfiltered,
			Set<Type> rfiltered) {
		for (Type l : lfiltered)
			for (Type r : rfiltered)
				if (!intersection(l.asTypeTokenType().getTypes(), r.asTypeTokenType().getTypes())
						.isEmpty())
					return true;

		return false;
	}

	@Override
	public InferredTypes lubAux(
			InferredTypes other)
			throws SemanticException {
		Set<Type> lub = new HashSet<>(elements);
		lub.addAll(other.elements);
		return new InferredTypes(null, lub);
	}

	@Override
	public boolean lessOrEqualAux(
			InferredTypes other)
			throws SemanticException {
		return other.elements.containsAll(elements);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
		result = prime * result + (isTop ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
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
		if (isTop != other.isTop)
			return false;
		return true;
	}

	@Override
	public InferredTypes evalTypeCast(
			BinaryExpression cast,
			InferredTypes left,
			InferredTypes right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.isTop() ? types.getTypes() : left.elements;
		Set<Type> relems = right.isTop() ? types.getTypes() : right.elements;
		Set<Type> inferred = cast.getOperator().typeInference(types, lelems, relems);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(types, inferred);
	}

	@Override
	public InferredTypes evalTypeConv(
			BinaryExpression conv,
			InferredTypes left,
			InferredTypes right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.isTop() ? types.getTypes() : left.elements;
		Set<Type> relems = right.isTop() ? types.getTypes() : right.elements;
		Set<Type> inferred = conv.getOperator().typeInference(types, lelems, relems);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(types, inferred);
	}
}
