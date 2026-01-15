package it.unive.lisa.lattices;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.nonrelational.heap.HeapValue;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link HeapLattice} and {@link HeapValue} with just one non-bottom value.
 * This is useful in analyses where heap information is not relevant or when a
 * placeholder is needed. Note that this lattice never produces substitutions.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SingleHeapLattice
		implements
		HeapLattice<SingleHeapLattice>,
		HeapValue<SingleHeapLattice> {

	/**
	 * The singleton instance of this lattice, which is the only non-bottom
	 * value.
	 */
	public static final SingleHeapLattice SINGLETON = new SingleHeapLattice();

	/**
	 * The bottom instance of this lattice, which is the only non-top value.
	 */
	public static final SingleHeapLattice BOTTOM = new SingleHeapLattice();

	private SingleHeapLattice() {
	}

	@Override
	public boolean lessOrEqual(
			SingleHeapLattice other)
			throws SemanticException {
		return this == BOTTOM || other == SINGLETON;
	}

	@Override
	public SingleHeapLattice lub(
			SingleHeapLattice other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public SingleHeapLattice upchain(
			SingleHeapLattice other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public SingleHeapLattice downchain(
			SingleHeapLattice other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public SingleHeapLattice glb(
			SingleHeapLattice other)
			throws SemanticException {
		return this == SINGLETON && other == SINGLETON ? SINGLETON : BOTTOM;
	}

	@Override
	public SingleHeapLattice top() {
		return SINGLETON;
	}

	@Override
	public SingleHeapLattice bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		return Lattice.topRepresentation();
	}

	@Override
	public Pair<SingleHeapLattice, List<HeapReplacement>> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<SingleHeapLattice, List<HeapReplacement>> popScope(
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
	public Pair<SingleHeapLattice, List<HeapReplacement>> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<SingleHeapLattice, List<HeapReplacement>> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<SingleHeapLattice, List<HeapReplacement>> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public List<HeapReplacement> expand(
			HeapReplacement base)
			throws SemanticException {
		return List.of(base);
	}

	@Override
	public <F extends FunctionalLattice<F, Identifier, SingleHeapLattice>> Collection<Identifier> reachableOnlyFrom(
			F state,
			Collection<Identifier> ids)
			throws SemanticException {
		return ids;
	}

}
