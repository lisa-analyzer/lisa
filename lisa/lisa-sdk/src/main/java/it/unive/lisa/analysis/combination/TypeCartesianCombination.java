package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.function.Predicate;

/**
 * A {@link CartesianCombination} of two {@link TypeLattice}s. This class takes
 * care of propagating all lattice operations to the two components, and
 * provides a unified interface for working with the combined lattice. It does
 * not perform any kind of reduction between the two, that must instead be
 * provided by subclasses.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <C>  the type of the cartesian combination
 * @param <T1> the type of the first type lattice
 * @param <T2> the type of the second type lattice
 */
public abstract class TypeCartesianCombination<C extends TypeCartesianCombination<C, T1, T2>,
		T1 extends TypeLattice<T1>,
		T2 extends TypeLattice<T2>> extends CartesianCombination<C, T1, T2> implements TypeLattice<C> {

	/**
	 * Creates a new cartesian combination of two type lattices.
	 * 
	 * @param first  the first type lattice
	 * @param second the second type lattice
	 */
	public TypeCartesianCombination(
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
