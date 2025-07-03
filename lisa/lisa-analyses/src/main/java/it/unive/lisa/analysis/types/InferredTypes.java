package it.unive.lisa.analysis.types;

import static org.apache.commons.collections4.SetUtils.difference;
import static org.apache.commons.collections4.SetUtils.intersection;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.ReferenceType;
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
 * A {@link NonRelationalTypeDomain} holding a set of {@link Type}s,
 * representing the inferred runtime types of an {@link Expression}.
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
		TypeSystem types = pp.getProgram().getTypes();
		if (id instanceof HeapLocation && ((HeapLocation) id).isAllocation())
			// if this is a heap location that is being allocated,
			// its types are exactly the static type
			return new InferredTypes(types, id.getStaticType());
		if (id instanceof MemoryPointer) {
			// if this is a memory pointer, its types are all the types
			// that can be pointed to by the static type of the pointer
			MemoryPointer mp = (MemoryPointer) id;
			InferredTypes inner = evalIdentifier(mp.getReferencedLocation(), environment, pp, oracle);
			if (inner.isTop())
				return new InferredTypes(types, id.getStaticType().allInstances(types));
			if (inner.isBottom())
				return BOTTOM;
			return new InferredTypes(types,
					inner.elements.stream().map(t -> new ReferenceType(t)).collect(Collectors.toSet()));
		}
		InferredTypes eval = BaseNonRelationalTypeDomain.super.evalIdentifier(id, environment, pp, oracle);
		if (!eval.isTop())
			return eval;
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
			UnaryExpression expression,
			InferredTypes arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> elems = arg.isTop() ? types.getTypes() : arg.elements;
		Set<Type> inferred = expression.getOperator().typeInference(types, elems);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(types, inferred);
	}

	@Override
	public InferredTypes evalBinaryExpression(
			BinaryExpression expression,
			InferredTypes left,
			InferredTypes right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.isTop() ? types.getTypes() : left.elements;
		Set<Type> relems = right.isTop() ? types.getTypes() : right.elements;
		Set<Type> inferred = expression.getOperator().typeInference(types, lelems, relems);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(types, inferred);
	}

	@Override
	public InferredTypes evalTernaryExpression(
			TernaryExpression expression,
			InferredTypes left,
			InferredTypes middle,
			InferredTypes right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.isTop() ? types.getTypes() : left.elements;
		Set<Type> melems = middle.isTop() ? types.getTypes() : middle.elements;
		Set<Type> relems = right.isTop() ? types.getTypes() : right.elements;
		Set<Type> inferred = expression.getOperator().typeInference(types, lelems, melems, relems);
		if (inferred.isEmpty())
			return BOTTOM;
		return new InferredTypes(types, inferred);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			InferredTypes left,
			InferredTypes right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.isTop() ? types.getTypes() : left.elements;
		Set<Type> relems = right.isTop() ? types.getTypes() : right.elements;
		BinaryOperator operator = expression.getOperator();
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
			if (evalBinaryExpression(expression.withOperator(TypeCast.INSTANCE), left, right, pp, oracle).isBottom())
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

	@Override
	public TypeEnvironment<InferredTypes> assumeUnaryExpression(
			TypeEnvironment<InferredTypes> environment,
			UnaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(expression, environment, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		// we only support the negated type check
		if (!(expression.getOperator() instanceof LogicalNegation)
				|| !(expression.getExpression() instanceof BinaryExpression)
				|| !(((BinaryExpression) expression.getExpression()).getOperator() instanceof TypeCheck))
			return environment;

		Identifier id;
		InferredTypes eval;
		ValueExpression left = (ValueExpression) ((BinaryExpression) expression.getExpression()).getLeft();
		ValueExpression right = (ValueExpression) ((BinaryExpression) expression.getExpression()).getRight();
		if (left instanceof Identifier) {
			eval = eval(right, environment, src, oracle);
			id = (Identifier) left;
		} else if (right instanceof Identifier) {
			eval = eval(left, environment, src, oracle);
			id = (Identifier) right;
		} else
			return environment;

		TypeSystem types = src.getProgram().getTypes();
		Set<Type> elems = eval.isTop() ? types.getTypes() : eval.elements;
		if (elems.stream().anyMatch(Type::isTypeTokenType))
			// if there is no type token in the evaluation,
			// this is not a type condition and we cannot
			// assume anything
			return environment;

		// these are all types compatible with the type tokens
		Set<Type> okTypes = elems.stream()
				.filter(Type::isTypeTokenType)
				.map(Type::asTypeTokenType)
				.map(TypeTokenType::getTypes)
				.flatMap(Set::stream)
				.flatMap(t -> t.allInstances(types).stream())
				.collect(Collectors.toSet());

		InferredTypes starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();
		// we keep only the ones that can be casted
		InferredTypes update = new InferredTypes(types, difference(starting.elements, okTypes));
		if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}

	@Override
	public TypeEnvironment<InferredTypes> assumeBinaryExpression(
			TypeEnvironment<InferredTypes> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(expression, environment, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		Identifier id;
		InferredTypes eval;
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			eval = eval(right, environment, src, oracle);
			id = (Identifier) left;
		} else if (right instanceof Identifier) {
			eval = eval(left, environment, src, oracle);
			id = (Identifier) right;
		} else
			return environment;

		TypeSystem types = src.getProgram().getTypes();
		Set<Type> elems = eval.isTop() ? types.getTypes() : eval.elements;
		Set<Type> tokens = elems.stream().filter(Type::isTypeTokenType).collect(Collectors.toSet());
		if (tokens.isEmpty())
			// if there is no type token in the evaluation,
			// this is not a type condition and we cannot
			// assume anything
			return environment;

		Set<Type> exactTypes = tokens.stream()
				.flatMap(t -> t.asTypeTokenType().getTypes().stream())
				.collect(Collectors.toSet());

		BinaryOperator operator = expression.getOperator();
		InferredTypes starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();
		InferredTypes update = null;

		if (operator == ComparisonEq.INSTANCE)
			// we keep only the types allowed by the type tokens
			// that were already there
			update = new InferredTypes(types, intersection(starting.elements, exactTypes));
		else if (operator == ComparisonNe.INSTANCE)
			// we keep only the types that are not allowed by the type tokens
			update = new InferredTypes(types, difference(starting.elements, exactTypes));
		else if (operator == TypeCheck.INSTANCE)
			// we keep only the ones that can be casted
			update = new InferredTypes(types, types.cast(starting.elements, tokens));

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
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
	public InferredTypes glbAux(
			InferredTypes other)
			throws SemanticException {
		Set<Type> lub = new HashSet<>(elements);
		lub.retainAll(other.elements);
		return new InferredTypes(null, lub);
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
