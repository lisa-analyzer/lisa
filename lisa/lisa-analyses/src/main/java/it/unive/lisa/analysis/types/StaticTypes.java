package it.unive.lisa.analysis.types;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;

import java.util.Collections;
import java.util.Set;

/**
 * An {@link InferredValue} holding a set of {@link Type}s, representing the
 * inferred runtime types of an {@link Expression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StaticTypes implements BaseNonRelationalTypeDomain<StaticTypes> {

	private static final StaticTypes BOTTOM = new StaticTypes(null, null);

	private final Type type;

	private final TypeSystem types;

	/**
	 * Builds the inferred types. The object built through this constructor
	 * represents an empty set of types.
	 */
	public StaticTypes() {
		this(null, Untyped.INSTANCE);
	}

	/**
	 * Builds the inferred types, representing only the given {@link Type}.
	 * 
	 * @param types the type system knowing about the types of the program where
	 *                  this element is created
	 * @param type  the type to be included in the set of inferred types
	 */
	StaticTypes(TypeSystem types, Type type) {
		this.type = type;
		this.types = types;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * Caution: invoking this method on the top instance obtained through
	 * {@code new StaticTypes().top()} will return a {@code null} value.
	 */
	@Override
	public Set<Type> getRuntimeTypes() {
		if (this.isBottom())
			Collections.emptySet();
		return type.allInstances(types);
	}

	@Override
	public StaticTypes top() {
		return new StaticTypes(types, Untyped.INSTANCE);
	}

	@Override
	public boolean isTop() {
		return type == Untyped.INSTANCE;
	}

	@Override
	public StaticTypes bottom() {
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
	public StaticTypes evalIdentifier(Identifier id, TypeEnvironment<StaticTypes> environment,
			ProgramPoint pp) throws SemanticException {
		StaticTypes eval = BaseNonRelationalTypeDomain.super.evalIdentifier(id, environment, pp);
		if (!eval.isTop() && !eval.isBottom())
			return eval;
		return new StaticTypes(pp.getProgram().getTypes(), id.getStaticType());
	}

	@Override
	public StaticTypes evalPushAny(PushAny pushAny, ProgramPoint pp) {
		return new StaticTypes(pp.getProgram().getTypes(), pushAny.getStaticType());
	}

	@Override
	public StaticTypes evalNullConstant(ProgramPoint pp) {
		return new StaticTypes(pp.getProgram().getTypes(), NullType.INSTANCE);
	}

	@Override
	public StaticTypes evalNonNullConstant(Constant constant, ProgramPoint pp) {
		return new StaticTypes(pp.getProgram().getTypes(), constant.getStaticType());
	}

	@Override
	public StaticTypes eval(ValueExpression expression, TypeEnvironment<StaticTypes> environment, ProgramPoint pp)
			throws SemanticException {
		if (expression instanceof BinaryExpression) {
			TypeSystem types = pp.getProgram().getTypes();
			BinaryExpression binary = (BinaryExpression) expression;
			if (binary.getOperator() instanceof TypeCast || binary.getOperator() instanceof TypeConv) {
				StaticTypes left = null, right = null;
				try {
					left = eval((ValueExpression) binary.getLeft(), environment, pp);
					right = eval((ValueExpression) binary.getRight(), environment, pp);
				} catch (ClassCastException e) {
					throw new SemanticException(expression + " is not a value expression");
				}
				Set<Type> lelems = left.type.allInstances(types);
				Set<Type> relems = right.type.allInstances(types);
				Set<Type> inferred = binary.getOperator().typeInference(types, lelems, relems);
				if (inferred.isEmpty())
					return BOTTOM;
				return new StaticTypes(pp.getProgram().getTypes(), Type.commonSupertype(inferred, Untyped.INSTANCE));
			}
		}

		return new StaticTypes(pp.getProgram().getTypes(), expression.getStaticType());
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, StaticTypes left,
			StaticTypes right, ProgramPoint pp) throws SemanticException {
		TypeSystem types = pp.getProgram().getTypes();
		Set<Type> lelems = left.type.allInstances(types);
		Set<Type> relems = right.type.allInstances(types);
		return new InferredTypes().satisfiesBinaryExpression(operator, new InferredTypes(types, lelems),
				new InferredTypes(types, relems), pp);
	}

	@Override
	public StaticTypes lubAux(StaticTypes other) throws SemanticException {
		return new StaticTypes(types, type.commonSupertype(other.type));
	}

	@Override
	public boolean lessOrEqualAux(StaticTypes other) throws SemanticException {
		return type.canBeAssignedTo(other.type);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		StaticTypes other = (StaticTypes) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
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
