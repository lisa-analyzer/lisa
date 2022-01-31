package it.unive.lisa.analysis.types;

import java.util.HashSet;

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
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An {@link InferredValue} holding a set of {@link Type}s, representing the
 * inferred runtime types of an {@link Expression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StaticTypes extends BaseNonRelationalTypeDomain<StaticTypes> {

	private static final StaticTypes TOP = new StaticTypes(Untyped.INSTANCE);

	private static final StaticTypes BOTTOM = new StaticTypes(null);

	private final Type type;

	/**
	 * Builds the inferred types. The object built through this constructor
	 * represents an empty set of types.
	 */
	public StaticTypes() {
		this(Untyped.INSTANCE);
	}

	/**
	 * Builds the inferred types, representing only the given {@link Type}.
	 * 
	 * @param type the type to be included in the set of inferred types
	 */
	StaticTypes(Type type) {
		this.type = type;
	}

	@Override
	public ExternalSet<Type> getRuntimeTypes() {
		if (this.isBottom())
			Caches.types().mkEmptySet();
		return Caches.types().mkSet(type.allInstances());
	}

	@Override
	public StaticTypes top() {
		return TOP;
	}

	@Override
	public StaticTypes bottom() {
		return BOTTOM;
	}

	@Override
	public DomainRepresentation representation() {
		if (isTop())
			return Lattice.TOP_REPR;

		if (isBottom())
			return Lattice.BOTTOM_REPR;

		return new SetRepresentation(new HashSet<>(type.allInstances()), StringRepresentation::new);
	}

	@Override
	protected StaticTypes evalIdentifier(Identifier id, TypeEnvironment<StaticTypes> environment,
			ProgramPoint pp) throws SemanticException {
		StaticTypes eval = super.evalIdentifier(id, environment, pp);
		if (!eval.isTop() && !eval.isBottom())
			return eval;
		return new StaticTypes(id.getStaticType());
	}

	@Override
	protected StaticTypes evalPushAny(PushAny pushAny, ProgramPoint pp) {
		return new StaticTypes(pushAny.getStaticType());
	}

	@Override
	protected StaticTypes evalNullConstant(ProgramPoint pp) {
		return new StaticTypes(NullType.INSTANCE);
	}

	@Override
	protected StaticTypes evalNonNullConstant(Constant constant, ProgramPoint pp) {
		return new StaticTypes(constant.getDynamicType());
	}

	@Override
	public StaticTypes eval(ValueExpression expression, TypeEnvironment<StaticTypes> environment, ProgramPoint pp)
			throws SemanticException {
		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			if (binary.getOperator() instanceof TypeCast || binary.getOperator() instanceof TypeConv) {
				StaticTypes left = null, right = null;
				try {
					left = eval((ValueExpression) binary.getLeft(), environment, pp);
					right = eval((ValueExpression) binary.getRight(), environment, pp);
				} catch (ClassCastException e) {
					throw new SemanticException(expression + " is not a value expression");
				}
				ExternalSet<Type> lelems = Caches.types().mkSet(left.type.allInstances());
				ExternalSet<Type> relems = Caches.types().mkSet(right.type.allInstances());
				ExternalSet<Type> inferred = binary.getOperator().typeInference(lelems, relems);
				if (inferred.isEmpty())
					return BOTTOM;
				return new StaticTypes(inferred.reduce(inferred.first(), (result, t) -> result.commonSupertype(t)));
			}
		}

		return new StaticTypes(expression.getStaticType());
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, StaticTypes left,
			StaticTypes right, ProgramPoint pp) throws SemanticException {
		ExternalSet<Type> lelems = Caches.types().mkSet(left.type.allInstances());
		ExternalSet<Type> relems = Caches.types().mkSet(right.type.allInstances());
		return new InferredTypes().satisfiesBinaryExpression(operator, new InferredTypes(lelems),
				new InferredTypes(relems), pp);
	}

	@Override
	protected StaticTypes lubAux(StaticTypes other) throws SemanticException {
		return new StaticTypes(type.commonSupertype(other.type));
	}

	@Override
	protected StaticTypes wideningAux(StaticTypes other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(StaticTypes other) throws SemanticException {
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
