package it.unive.lisa.lattices.memory;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.analysis.memory.MemoryLattice;
import it.unive.lisa.lattices.SetLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A memory lattice that contains the types of the memory locations. These are
 * modelled as a set of types that have been allocated in the memory.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AllocatedTypes
		extends
		SetLattice<AllocatedTypes, String>
		implements
		MemoryLattice<AllocatedTypes> {

	/**
	 * Builds an empty set of types.
	 */
	public AllocatedTypes() {
		super(Collections.emptySet(), true);
	}

	/**
	 * Builds a new set of types containing the given elements.
	 * 
	 * @param elements the elements to put in this set
	 */
	public AllocatedTypes(
			Set<String> elements) {
		super(elements, true);
	}

	private AllocatedTypes(
			Set<String> elements,
			boolean isTop) {
		super(elements, isTop);
	}

	@Override
	public AllocatedTypes top() {
		return new AllocatedTypes(Collections.emptySet(), true);
	}

	@Override
	public AllocatedTypes bottom() {
		return new AllocatedTypes(Collections.emptySet(), false);
	}

	@Override
	public Pair<AllocatedTypes, List<MemoryReplacement>> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<AllocatedTypes, List<MemoryReplacement>> popScope(
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
	public Pair<AllocatedTypes, List<MemoryReplacement>> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<AllocatedTypes, List<MemoryReplacement>> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<AllocatedTypes, List<MemoryReplacement>> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public AllocatedTypes mk(
			Set<String> set) {
		return new AllocatedTypes(set);
	}

	@Override
	public List<MemoryReplacement> expand(
			MemoryReplacement base)
			throws SemanticException {
		return List.of(base);
	}

}
