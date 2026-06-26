package it.unive.lisa.lattices;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.analysis.memory.MemoryLattice;
import it.unive.lisa.analysis.nonrelational.memory.MemoryValue;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link MemoryLattice} and {@link MemoryValue} with just one non-bottom
 * value. This is useful in analyses where memory information is not relevant or
 * when a placeholder is needed. Note that this lattice never produces
 * substitutions.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SingleMemoryLattice
		implements
		MemoryLattice<SingleMemoryLattice>,
		MemoryValue<SingleMemoryLattice> {

	/**
	 * The singleton instance of this lattice, which is the only non-bottom
	 * value.
	 */
	public static final SingleMemoryLattice SINGLETON = new SingleMemoryLattice();

	/**
	 * The bottom instance of this lattice, which is the only non-top value.
	 */
	public static final SingleMemoryLattice BOTTOM = new SingleMemoryLattice();

	private SingleMemoryLattice() {
	}

	@Override
	public boolean lessOrEqual(
			SingleMemoryLattice other)
			throws SemanticException {
		return this == BOTTOM || other == SINGLETON;
	}

	@Override
	public SingleMemoryLattice lub(
			SingleMemoryLattice other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public SingleMemoryLattice upchain(
			SingleMemoryLattice other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public SingleMemoryLattice downchain(
			SingleMemoryLattice other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public SingleMemoryLattice glb(
			SingleMemoryLattice other)
			throws SemanticException {
		return this == SINGLETON && other == SINGLETON ? SINGLETON : BOTTOM;
	}

	@Override
	public SingleMemoryLattice top() {
		return SINGLETON;
	}

	@Override
	public SingleMemoryLattice bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		return Lattice.topRepresentation();
	}

	@Override
	public Pair<SingleMemoryLattice, List<MemoryReplacement>> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<SingleMemoryLattice, List<MemoryReplacement>> popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return false;
	}

	@Override
	public Pair<SingleMemoryLattice, List<MemoryReplacement>> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<SingleMemoryLattice, List<MemoryReplacement>> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<SingleMemoryLattice, List<MemoryReplacement>> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public List<MemoryReplacement> expand(
			MemoryReplacement base)
			throws SemanticException {
		return List.of(base);
	}

	@Override
	public <F extends FunctionalLattice<F, Identifier, SingleMemoryLattice>> Collection<Identifier> reachableOnlyFrom(
			F state,
			Collection<Identifier> ids)
			throws SemanticException {
		return ids;
	}

}
