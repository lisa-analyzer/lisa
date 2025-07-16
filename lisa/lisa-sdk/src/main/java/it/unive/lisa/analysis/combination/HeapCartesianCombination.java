package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link CartesianCombination} of two {@link HeapLattice}s. This class takes
 * care of propagating all lattice operations to the two components, and
 * provides a unified interface for working with the combined lattice. It does
 * not perform any kind of reduction between the two, that must instead be
 * provided by subclasses. Substitutions produced by this lattice are
 * combinations of the substitutions produced by the two components.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <C>  the type of the cartesian combination
 * @param <T1> the type of the first heap lattice
 * @param <T2> the type of the second heap lattice
 */
public abstract class HeapCartesianCombination<C extends HeapCartesianCombination<C, T1, T2>,
		T1 extends HeapLattice<T1>,
		T2 extends HeapLattice<T2>>
		extends
		CartesianCombination<C, T1, T2>
		implements
		HeapLattice<C> {

	/**
	 * Creates a new cartesian combination of two heap lattices.
	 * 
	 * @param first  the first heap lattice
	 * @param second the second heap lattice
	 */
	public HeapCartesianCombination(
			T1 first,
			T2 second) {
		super(first, second);
	}

	/**
	 * Creates a new cartesian combination of two heap lattices, combining the
	 * substitutions.
	 * 
	 * @param first  the first heap lattice and substitutions
	 * @param second the second heap lattice and substitutions
	 * 
	 * @return the new cartesian combination and substitutions
	 */
	protected abstract Pair<C, List<HeapReplacement>> mk(
			Pair<T1, List<HeapReplacement>> first,
			Pair<T2, List<HeapReplacement>> second);

	@Override
	public Pair<C, List<HeapReplacement>> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return mk(first.pushScope(token, pp), second.pushScope(token, pp));
	}

	@Override
	public Pair<C, List<HeapReplacement>> popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return mk(first.popScope(token, pp), second.popScope(token, pp));
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return first.knowsIdentifier(id) || second.knowsIdentifier(id);
	}

	@Override
	public Pair<C, List<HeapReplacement>> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return mk(first.forgetIdentifier(id, pp), second.forgetIdentifier(id, pp));
	}

	@Override
	public Pair<C, List<HeapReplacement>> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return mk(first.forgetIdentifiers(ids, pp), second.forgetIdentifiers(ids, pp));
	}

	@Override
	public Pair<C, List<HeapReplacement>> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return mk(first.forgetIdentifiersIf(test, pp), second.forgetIdentifiersIf(test, pp));
	}

}
