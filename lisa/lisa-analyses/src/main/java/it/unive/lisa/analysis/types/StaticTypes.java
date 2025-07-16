package it.unive.lisa.analysis.types;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.type.BaseNonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.type.NonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.type.TypeValue;
import it.unive.lisa.analysis.types.InferredTypes.TypeSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.SetUtils;

/**
 * A {@link NonRelationalTypeDomain} that tracks the static type of variables,
 * and that computes expression types using their static type. Typing
 * information is thus deemed to be the set of all subtypes of the tracked type.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StaticTypes
		implements
		BaseNonRelationalTypeDomain<StaticTypes.Supertype> {

	@Override
	public Supertype evalIdentifier(
			Identifier id,
			TypeEnvironment<Supertype> environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Supertype eval = BaseNonRelationalTypeDomain.super.evalIdentifier(id, environment, pp, oracle);
		if (!eval.isTop() && !eval.isBottom())
			return eval;
		return new Supertype(pp.getProgram().getTypes(), id.getStaticType());
	}

	@Override
	public Supertype evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return new Supertype(pp.getProgram().getTypes(), pushAny.getStaticType());
	}

	@Override
	public Supertype evalPushInv(
			PushInv pushInv,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new Supertype(pp.getProgram().getTypes(), pushInv.getStaticType());
	}

	@Override
	public Supertype evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle) {
		return new Supertype(pp.getProgram().getTypes(), NullType.INSTANCE);
	}

	@Override
	public Supertype evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return new Supertype(pp.getProgram().getTypes(), constant.getStaticType());
	}

	@Override
	public Supertype eval(
			TypeEnvironment<Supertype> environment,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof BinaryExpression) {
			TypeSystem types = pp.getProgram().getTypes();
			BinaryExpression binary = (BinaryExpression) expression;
			if (binary.getOperator() instanceof TypeCast || binary.getOperator() instanceof TypeConv) {
				Supertype left = null, right = null;
				try {
					left = eval(environment, (ValueExpression) binary.getLeft(), pp, oracle);
					right = eval(environment, (ValueExpression) binary.getRight(), pp, oracle);
				} catch (ClassCastException e) {
					throw new SemanticException(
							expression + " is not a value expression");
				}
				Set<Type> lelems = left.type.allInstances(types);
				Set<Type> relems = right.type.allInstances(types);
				Set<Type> inferred = binary.getOperator().typeInference(types, lelems, relems);
				if (inferred.isEmpty())
					return Supertype.BOTTOM;
				return new Supertype(pp.getProgram().getTypes(), Type.commonSupertype(inferred, Untyped.INSTANCE));
			}
		}

		return new Supertype(pp.getProgram().getTypes(), expression.getStaticType());
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			Supertype left,
			Supertype right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.type.allInstances(types);
		Set<Type> relems = right.type.allInstances(types);
		return new InferredTypes()
				.satisfiesBinaryExpression(expression, new TypeSet(types, lelems), new TypeSet(types, relems), pp,
						oracle);
	}

	@Override
	public TypeEnvironment<Supertype> assumeUnaryExpression(
			TypeEnvironment<Supertype> environment,
			UnaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
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
		Supertype eval;
		ValueExpression left = (ValueExpression) ((BinaryExpression) expression.getExpression()).getLeft();
		ValueExpression right = (ValueExpression) ((BinaryExpression) expression.getExpression()).getRight();
		if (left instanceof Identifier) {
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
		} else if (right instanceof Identifier) {
			eval = eval(environment, left, src, oracle);
			id = (Identifier) right;
		} else
			return environment;

		TypeSystem types = src.getProgram().getTypes();
		Set<Type> elems = eval.type.allInstances(types);
		if (elems.stream().anyMatch(Type::isTypeTokenType))
			// if there is no type token in the evaluation,
			// this is not a type condition and we cannot
			// assume anything
			return environment;

		// these are all types compatible with the type tokens
		Set<Type> okTypes = elems
				.stream()
				.filter(Type::isTypeTokenType)
				.map(Type::asTypeTokenType)
				.map(TypeTokenType::getTypes)
				.flatMap(Set::stream)
				.flatMap(t -> t.allInstances(types).stream())
				.collect(Collectors.toSet());

		Supertype starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();
		Supertype update = null;

		// we keep only the ones that can be casted
		Type sup = Type.commonSupertype(SetUtils.difference(starting.type.allInstances(types), okTypes), null);
		if (sup == null)
			update = Supertype.BOTTOM;
		else if (sup == Untyped.INSTANCE)
			update = top();
		else
			update = new Supertype(types, sup);

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}

	@Override
	public TypeEnvironment<Supertype> assumeBinaryExpression(
			TypeEnvironment<Supertype> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		Identifier id;
		Supertype eval;
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
		} else if (right instanceof Identifier) {
			eval = eval(environment, left, src, oracle);
			id = (Identifier) right;
		} else
			return environment;

		TypeSystem types = src.getProgram().getTypes();
		Set<Type> elems = eval.type.allInstances(types);
		// these are all types compatible with the type tokens
		Set<Type> filtered = elems
				.stream()
				.filter(Type::isTypeTokenType)
				.map(Type::asTypeTokenType)
				.map(TypeTokenType::getTypes)
				.flatMap(Set::stream)
				.flatMap(t -> t.allInstances(types).stream())
				.collect(Collectors.toSet());
		if (filtered.isEmpty())
			// if there is no type token in the evaluation,
			// this is not a type condition and we cannot
			// assume anything
			return environment;

		BinaryOperator operator = expression.getOperator();
		Supertype starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();
		Supertype update = null;

		if (operator == ComparisonEq.INSTANCE)
			// eval.type is (or at least should be) exact, while starting.type
			// can be an overapproximation. the equality *might* hold whenever
			// eval.type can be assigned to starting.type (ie it is a subtype
			// of it). the best we can do while preserving soundness
			// is set the type to eval.type
			update = eval.type.canBeAssignedTo(starting.type) ? eval : bottom();
		else if (operator == TypeCheck.INSTANCE) {
			// we keep only the ones that can be casted
			Type sup = Type.commonSupertype(SetUtils.intersection(starting.type.allInstances(types), filtered), null);
			if (sup == null)
				update = Supertype.BOTTOM;
			else if (sup == Untyped.INSTANCE)
				update = top();
			else
				update = new Supertype(types, sup);
		}

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}

	@Override
	public Supertype top() {
		return new Supertype().top();
	}

	@Override
	public Supertype bottom() {
		return Supertype.BOTTOM;
	}

	/**
	 * A lattice structure tracking the common supertype of a set of
	 * {@link Type}s. The set of types represented by values of this class
	 * correspond to all possible instances of the common supertype.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class Supertype
			implements
			TypeValue<Supertype>,
			BaseLattice<Supertype> {

		private static final Supertype BOTTOM = new Supertype(null, null);

		private final Type type;

		private final TypeSystem types;

		/**
		 * Builds the inferred types. The object built through this constructor
		 * represents an empty set of types.
		 */
		public Supertype() {
			this(null, Untyped.INSTANCE);
		}

		/**
		 * Builds the inferred types, representing only the given {@link Type}.
		 * 
		 * @param types the type system knowing about the types of the program
		 *                  where this element is created
		 * @param type  the type to be included in the set of inferred types
		 */
		public Supertype(
				TypeSystem types,
				Type type) {
			this.type = type;
			this.types = types;
		}

		@Override
		public Set<Type> getRuntimeTypes() {
			if (this.isBottom())
				Collections.emptySet();
			return type.allInstances(types);
		}

		@Override
		public Supertype top() {
			return new Supertype(types, Untyped.INSTANCE);
		}

		@Override
		public boolean isTop() {
			return type == Untyped.INSTANCE;
		}

		@Override
		public Supertype bottom() {
			return BOTTOM;
		}

		@Override
		public StructuredRepresentation representation() {
			if (isTop())
				return Lattice.topRepresentation();

			if (isBottom())
				return Lattice.bottomRepresentation();

			return new StringRepresentation(type.toString());
		}

		@Override
		public Supertype lubAux(
				Supertype other)
				throws SemanticException {
			return new Supertype(types, type.commonSupertype(other.type));
		}

		@Override
		public boolean lessOrEqualAux(
				Supertype other)
				throws SemanticException {
			return type.canBeAssignedTo(other.type);
		}

		@Override
		public Supertype glbAux(
				Supertype other)
				throws SemanticException {
			Type sup = Type
					.commonSupertype(
							SetUtils.intersection(type.allInstances(types), other.type.allInstances(types)),
							null);
			if (sup == null)
				return BOTTOM;
			if (sup == Untyped.INSTANCE)
				return top();
			return new Supertype(types, sup);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			Supertype other = (Supertype) obj;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

	}

}
