package it.unive.lisa.lattices.memory;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.analysis.memory.MemoryLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A monolithic memory lattice that consists of a single element
 * ({@link Monolith#SINGLETON}) and a bottom element ({@link Monolith#BOTTOM}).
 * The single element represents the whole memory, while the bottom element
 * represents erroneous states. All memory locations are abstracted to a single
 * memory location, which is the monolith.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Monolith
		implements
		MemoryLattice<Monolith> {

	/**
	 * The singleton instance of the monolithic memory.
	 */
	public static final Monolith SINGLETON = new Monolith();

	/**
	 * The bottom element of the monolithic memory, which represents erroneous
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
	public Pair<Monolith, List<MemoryReplacement>> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<MemoryReplacement>> popScope(
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
	public Pair<Monolith, List<MemoryReplacement>> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<MemoryReplacement>> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<Monolith, List<MemoryReplacement>> forgetIdentifiersIf(
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

}
