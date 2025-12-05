package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.function.Predicate;

/**
 * A {@link CartesianCombination} of two {@link AbstractLattice}s. This class
 * takes care of propagating all lattice operations to the two components, and
 * provides a unified interface for working with the combined lattice. It does
 * not perform any kind of reduction between the two, that must instead be
 * provided by subclasses.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <C>  the type of the cartesian combination
 * @param <T1> the type of the first value lattice
 * @param <T2> the type of the second value lattice
 */
public abstract class AbstractLatticeProduct<C extends AbstractLatticeProduct<C, T1, T2>,
		T1 extends AbstractLattice<T1>,
		T2 extends AbstractLattice<T2>>
		extends
		CartesianCombination<C, T1, T2>
		implements
		AbstractLattice<C> {

	/**
	 * Creates a new cartesian combination of two value lattices.
	 * 
	 * @param first  the first value lattice
	 * @param second the second value lattice
	 */
	public AbstractLatticeProduct(
			T1 first,
			T2 second) {
		super(first, second);
	}

	@Override
	public C pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return mk(first.pushScope(token, pp), second.pushScope(token, pp));
	}

	@Override
	public C popScope(
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
	public C forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return mk(first.forgetIdentifier(id, pp), second.forgetIdentifier(id, pp));
	}

	@Override
	public C forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return mk(first.forgetIdentifiers(ids, pp), second.forgetIdentifiers(ids, pp));
	}

	@Override
	public C forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return mk(first.forgetIdentifiersIf(test, pp), second.forgetIdentifiersIf(test, pp));
	}

}