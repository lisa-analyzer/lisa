package it.unive.lisa.lattices.heap;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A monolithic heap lattice that consists of a single element
 * ({@link Monolith#SINGLETON}) and a bottom element ({@link Monolith#BOTTOM}).
 * The single element represents the whole heap, while the bottom element
 * represents erroneous states. All memory locations are abstracted to a single
 * heap location, which is the monolith.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Monolith
		implements
		HeapLattice<Monolith> {

	/**
	 * The singleton instance of the monolithic heap.
	 */
	public static final Monolith SINGLETON = new Monolith();

	/**
	 * The bottom element of the monolithic heap, which represents erroneous
	 * states.
	 */
	public static final Monolith BOTTOM = new Monolith();

	private static final StructuredRepresentation REPR = new StringRepresentation("monolith");

	private Monolith() {
	}

	@Override
	public boolean lessOrEqual(
			Monolith other)
			throws SemanticException {
		return this == BOTTOM || other == SINGLETON;
	}

	@Override
	public Monolith lub(
			Monolith other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public Monolith upchain(
			Monolith other)
			throws SemanticException {
		return this == BOTTOM && other == BOTTOM ? BOTTOM : SINGLETON;
	}

	@Override
	public Monolith glb(
			Monolith other)
			throws SemanticException {
		return this == SINGLETON && other == SINGLETON ? SINGLETON : BOTTOM;
	}

	@Override
	public Monolith downchain(
			Monolith other)
			throws SemanticException {
		return this == SINGLETON && other == SINGLETON ? SINGLETON : BOTTOM;
	}

	@Override
	public Monolith top() {
		return SINGLETON;
	}

	@Override
	public Monolith bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		return REPR;
	}

	@Override
	public Pair<Monolith, List<HeapReplacement>> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<HeapReplacement>> popScope(
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
	public Pair<Monolith, List<HeapReplacement>> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<HeapReplacement>> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<HeapReplacement>> forgetIdentifiersIf(
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

}
