package it.unive.lisa.analysis;

import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A no-op type domain that represents a top or bottom state without any actual
 * type information. This is useful in analyses where type information is not
 * relevant or when a placeholder is needed. Note that this domain cannot
 * produce typing information:
 * {@link #getRuntimeTypesOf(SymbolicExpression, ProgramPoint, SemanticOracle)}
 * always returns all possible types, and
 * {@link #getDynamicTypeOf(SymbolicExpression, ProgramPoint, SemanticOracle)}
 * always returns {@link Untyped#INSTANCE}.
 */
public class NoOpTypes implements TypeDomain<NoOpTypes> {

	/**
	 * The top element of this domain, representing the default state.
	 */
	public static final NoOpTypes TOP = new NoOpTypes();

	/**
	 * The bottom element of this domain, representing an erroneous or undefined
	 * state.
	 */
	public static final NoOpTypes BOTTOM = new NoOpTypes();

	private NoOpTypes() {
		// private constructor to prevent instantiation
	}

	@Override
	public boolean lessOrEqual(
			NoOpTypes other)
			throws SemanticException {
		return isBottom() || other.isTop();
	}

	@Override
	public NoOpTypes lub(
			NoOpTypes other)
			throws SemanticException {
		return isBottom() && other.isBottom() ? BOTTOM : TOP;
	}

	@Override
	public NoOpTypes glb(
			NoOpTypes other)
			throws SemanticException {
		return isBottom() || other.isBottom() ? BOTTOM : TOP;
	}

	@Override
	public NoOpTypes top() {
		return TOP;
	}

	@Override
	public NoOpTypes bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		else
			return new StringRepresentation("-");
	}

	@Override
	public NoOpTypes assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpTypes smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpTypes assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return false;
	}

	@Override
	public NoOpTypes forgetIdentifier(
			Identifier id)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpTypes forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpTypes pushScope(
			ScopeToken token)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpTypes popScope(
			ScopeToken token)
			throws SemanticException {
		return this;
	}

	@Override
	public Set<Type> getRuntimeTypesOf(
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return pp.getProgram().getTypes().getTypes();
	}

	@Override
	public Type getDynamicTypeOf(
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Untyped.INSTANCE;
	}
}
