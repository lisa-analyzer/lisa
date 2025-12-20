package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.function.Predicate;

/**
 * A {@link ValueLattice} with just one non-bottom value. This is useful in
 * analyses where value information is not relevant or when a placeholder is
 * needed.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SingleValueLattice
		implements
		ValueLattice<SingleValueLattice> {

	/**
	 * The singleton instance of this lattice, which is the only non-bottom
	 * value.
	 */
	public static final SingleValueLattice SINGLETON = new SingleValueLattice();

	/**
	 * The bottom instance of this lattice, which is the only non-top value.
	 */
	public static final SingleValueLattice BOTTOM = new SingleValueLattice();

	private SingleValueLattice() {
	}

	@Override
	public boolean lessOrEqual(
			SingleValueLattice other)
			throws SemanticException {
		return this == BOTTOM || other == SINGLETON;
	}

	@Override
	public SingleValueLattice lub(
			SingleValueLattice other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public SingleValueLattice chain(
			SingleValueLattice other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public SingleValueLattice glb(
			SingleValueLattice other)
			throws SemanticException {
		return this == SINGLETON && other == SINGLETON ? SINGLETON : BOTTOM;
	}

	@Override
	public SingleValueLattice top() {
		return SINGLETON;
	}

	@Override
	public SingleValueLattice bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		return Lattice.topRepresentation();
	}

	@Override
	public SingleValueLattice pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SingleValueLattice popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return false;
	}

	@Override
	public SingleValueLattice forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SingleValueLattice forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SingleValueLattice forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SingleValueLattice store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return this;
	}

}
