package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.type.TypeValue;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A {@link TypeLattice} and {@link TypeValue} with just one non-bottom value.
 * This is useful in analyses where type information is not relevant or when a
 * placeholder is needed. Note that this lattice always models {@link Untyped}
 * as the only possible runtime type.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SingleTypeLattice
		implements
		TypeLattice<SingleTypeLattice>,
		TypeValue<SingleTypeLattice> {

	/**
	 * The singleton instance of this lattice, which is the only non-bottom
	 * value.
	 */
	public static final SingleTypeLattice SINGLETON = new SingleTypeLattice();

	/**
	 * The bottom instance of this lattice, which is the only non-top value.
	 */
	public static final SingleTypeLattice BOTTOM = new SingleTypeLattice();

	private SingleTypeLattice() {
	}

	@Override
	public boolean lessOrEqual(
			SingleTypeLattice other)
			throws SemanticException {
		return this == BOTTOM || other == SINGLETON;
	}

	@Override
	public SingleTypeLattice lub(
			SingleTypeLattice other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public SingleTypeLattice glb(
			SingleTypeLattice other)
			throws SemanticException {
		return this == SINGLETON && other == SINGLETON ? SINGLETON : BOTTOM;
	}

	@Override
	public SingleTypeLattice top() {
		return SINGLETON;
	}

	@Override
	public SingleTypeLattice bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		return Lattice.topRepresentation();
	}

	@Override
	public SingleTypeLattice pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SingleTypeLattice popScope(
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
	public SingleTypeLattice forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SingleTypeLattice forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SingleTypeLattice forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SingleTypeLattice store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return this;
	}

	@Override
	public Set<Type> getRuntimeTypes() {
		return Set.of(Untyped.INSTANCE);
	}

}
