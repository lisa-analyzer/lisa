package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.List;
import java.util.function.Predicate;

/**
 * A no-op heap domain that represents a top or bottom state without any actual
 * heap information. This is useful in analyses where heap information is not
 * relevant or when a placeholder is needed. Note that this domain never
 * produces substitutions, and rewrite operations will always return the input
 * expression wrapped in an {@link ExpressionSet}.
 */
public class NoOpHeap implements HeapDomain<NoOpHeap> {

	/**
	 * The top element of this domain, representing the default state.
	 */
	public static final NoOpHeap TOP = new NoOpHeap();

	/**
	 * The bottom element of this domain, representing an erroneous or undefined
	 * state.
	 */
	public static final NoOpHeap BOTTOM = new NoOpHeap();

	private NoOpHeap() {
		// private constructor to prevent instantiation
	}

	@Override
	public boolean lessOrEqual(
			NoOpHeap other)
			throws SemanticException {
		return isBottom() || other.isTop();
	}

	@Override
	public NoOpHeap lub(
			NoOpHeap other)
			throws SemanticException {
		return isBottom() && other.isBottom() ? BOTTOM : TOP;
	}

	@Override
	public NoOpHeap glb(
			NoOpHeap other)
			throws SemanticException {
		return isBottom() || other.isBottom() ? BOTTOM : TOP;
	}

	@Override
	public NoOpHeap top() {
		return TOP;
	}

	@Override
	public NoOpHeap bottom() {
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
	public NoOpHeap assign(
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpHeap smallStepSemantics(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpHeap assume(
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public ExpressionSet rewrite(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ExpressionSet(expression);
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return List.of();
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return false;
	}

	@Override
	public NoOpHeap forgetIdentifier(
			Identifier id)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpHeap forgetIdentifiers(Iterable<Identifier> ids) throws SemanticException {
		return this;
	}

	@Override
	public NoOpHeap forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpHeap pushScope(
			ScopeToken token)
			throws SemanticException {
		return this;
	}

	@Override
	public NoOpHeap popScope(
			ScopeToken token)
			throws SemanticException {
		return this;
	}

	@Override
	public Satisfiability alias(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability isReachableFrom(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}
}
