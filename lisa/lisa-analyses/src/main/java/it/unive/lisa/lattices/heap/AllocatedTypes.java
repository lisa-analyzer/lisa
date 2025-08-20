package it.unive.lisa.lattices.heap;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A heap lattice that contains the types of the heap locations. These are
 * modelled as a set of types that have been allocated in the heap.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AllocatedTypes
		extends
		SetLattice<AllocatedTypes, String>
		implements
		HeapLattice<AllocatedTypes> {

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
	public Pair<AllocatedTypes, List<HeapReplacement>> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<AllocatedTypes, List<HeapReplacement>> popScope(
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
	public Pair<AllocatedTypes, List<HeapReplacement>> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<AllocatedTypes, List<HeapReplacement>> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(this, Collections.emptyList());
	}

	@Override
	public Pair<AllocatedTypes, List<HeapReplacement>> forgetIdentifiersIf(
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
	public List<HeapReplacement> expand(
			HeapReplacement base)
			throws SemanticException {
		return List.of(base);
	}

}
