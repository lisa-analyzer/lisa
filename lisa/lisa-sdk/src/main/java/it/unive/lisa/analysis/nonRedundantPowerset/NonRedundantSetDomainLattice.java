package it.unive.lisa.analysis.nonRedundantPowerset;

import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * A {@link NonRedundantSetLattice} that is also a {@link DomainLattice}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <S> the type of the concrete non redundant set lattice
 * @param <L> the type of the underlying lattice whose elements are contained in
 *                the non redundant set lattice
 */
public abstract class NonRedundantSetDomainLattice<S extends NonRedundantSetDomainLattice<S, L>,
		L extends ValueLattice<L>> extends NonRedundantSetLattice<S, L> implements DomainLattice<S, S> {

	/**
	 * Creates a new non redundant set domain lattice with the given elements
	 * and singleton.
	 * 
	 * @param elements  the elements of this lattice
	 * @param singleton the singleton element of this lattice
	 */
	protected NonRedundantSetDomainLattice(
			Set<L> elements,
			L singleton) {
		super(elements, singleton);
	}

	@Override
	public S forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		Set<L> newElements = new TreeSet<>();
		for (L elem : this.elements)
			newElements.add(elem.forgetIdentifier(id, pp));
		return mk(newElements).removeRedundancy();
	}

	@Override
	public S forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		Set<L> newElements = new TreeSet<>();
		for (L elem : this.elements)
			newElements.add(elem.forgetIdentifiers(ids, pp));
		return mk(newElements).removeRedundancy();
	}

	@Override
	public S forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		Set<L> newElements = new TreeSet<>();
		for (L elem : this.elements)
			newElements.add(elem.forgetIdentifiersIf(test, pp));
		return mk(newElements).removeRedundancy();
	}

	@Override
	public S pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		Set<L> newElements = new TreeSet<>();
		for (L elem : this.elements)
			newElements.add(elem.pushScope(token, pp));
		return mk(newElements).removeRedundancy();
	}

	@Override
	public S popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		Set<L> newElements = new TreeSet<>();
		for (L elem : this.elements)
			newElements.add(elem.popScope(token, pp));
		return mk(newElements).removeRedundancy();
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		for (L elem : this.elements)
			if (elem.knowsIdentifier(id))
				return true;
		return false;
	}

}
