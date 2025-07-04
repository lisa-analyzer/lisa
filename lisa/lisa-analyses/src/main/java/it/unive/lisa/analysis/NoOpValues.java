package it.unive.lisa.analysis;

import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.function.Predicate;

/**
 * A no-op value domain that represents a top or bottom state without any actual
 * value information. This is useful in analyses where value information is not
 * relevant or when a placeholder is needed.
 */
public class NoOpValues implements ValueDomain<NoOpValues> {

	/**
	 * The top element of this domain, representing the default state.
	 */
	public static final NoOpValues TOP = new NoOpValues();

	/**
	 * The bottom element of this domain, representing an erroneous or undefined
	 * state.
	 */
	public static final NoOpValues BOTTOM = new NoOpValues();

	private NoOpValues() {
		// private constructor to prevent instantiation
	}

	@Override
	public boolean lessOrEqual(
			NoOpValues other)
			throws SemanticException {
		return isBottom() || other.isTop();
	}

	@Override
	public NoOpValues lub(
			NoOpValues other)
			throws SemanticException {
		return isBottom() && other.isBottom() ? BOTTOM : TOP;
	}

	@Override
	public NoOpValues glb(
			NoOpValues other)
			throws SemanticException {
		return isBottom() || other.isBottom() ? BOTTOM : TOP;
	}

	@Override
	public NoOpValues top() {
		return TOP;
	}

	@Override
	public NoOpValues bottom() {
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
	public NoOpValues assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpValues smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpValues assume(
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
	public NoOpValues forgetIdentifier(
			Identifier id)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpValues forgetIdentifiers(Iterable<Identifier> ids) throws SemanticException {
		return this;
	}

	@Override
	public NoOpValues forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpValues pushScope(
			ScopeToken token)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpValues popScope(
			ScopeToken token)
			throws SemanticException {
		return this;
	}
}
