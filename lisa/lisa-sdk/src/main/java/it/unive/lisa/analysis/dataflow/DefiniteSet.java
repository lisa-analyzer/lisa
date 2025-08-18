package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A {@link DataflowDomainLattice} that represents a set of definite
 * {@link DataflowElement}s. Here, definite means that (i) the partial order is
 * superset inclusion, (ii) the lub operation is the set intersection, (iii) the
 * top element is the empty set, (iv) the bottom element is a set containing all
 * the elements. In fact, this class extends {@link InverseSetLattice}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the concrete type of {@link DataflowElement} contained in this
 *                lattice
 */
public class DefiniteSet<E extends DataflowElement<E>> extends InverseSetLattice<DefiniteSet<E>, E>
		implements
		DataflowDomainLattice<DefiniteSet<E>, E> {

	/**
	 * Builds the top element of this lattice.
	 */
	public DefiniteSet() {
		super(Set.of(), true);
	}

	/**
	 * Builds a {@link DefiniteSet} containing the given element.
	 * 
	 * @param element the element to be contained in this set
	 */
	public DefiniteSet(
			E element) {
		super(Set.of(element), true);
	}

	/**
	 * Builds a {@link DefiniteSet} containing the given elements.
	 * 
	 * @param elements the elements to be contained in this set
	 */
	public DefiniteSet(
			Set<E> elements) {
		super(elements, true);
	}

	/**
	 * Builds a {@link DefiniteSet} that is either the top or bottom element.
	 * 
	 * @param isTop whether this set is the top element or the bottom element
	 */
	public DefiniteSet(
			boolean isTop) {
		super(Set.of(), isTop);
	}

	private DefiniteSet(
			Set<E> elements,
			boolean isTop) {
		super(elements, isTop);
	}

	@Override
	public DefiniteSet<E> top() {
		return new DefiniteSet<>();
	}

	@Override
	public DefiniteSet<E> bottom() {
		return new DefiniteSet<>(false);
	}

	@Override
	public DefiniteSet<E> mk(
			Set<E> set) {
		return new DefiniteSet<>(set);
	}

	@Override
	public StructuredRepresentation representation() {
		return new SetRepresentation(elements, DataflowElement::representation);
	}

	@Override
	public DefiniteSet<E> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		Set<E> newElements = new HashSet<>();
		for (E element : elements) {
			E pushed = element.pushScope(token, pp);
			if (pushed != null)
				newElements.add(pushed);
		}

		return new DefiniteSet<>(newElements, isTop);
	}

	@Override
	public DefiniteSet<E> popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		Set<E> newElements = new HashSet<>();
		for (E element : elements) {
			E popped = element.popScope(token, pp);
			if (popped != null)
				newElements.add(popped);
		}

		return new DefiniteSet<>(newElements, isTop);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return elements.stream().anyMatch(e -> e.getInvolvedIdentifiers().contains(id));
	}

	@Override
	public DefiniteSet<E> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		Collection<E> toRemove = new LinkedList<>();
		for (E e : elements)
			if (e.getInvolvedIdentifiers().contains(id))
				toRemove.add(e);

		if (toRemove.isEmpty())
			return this;

		Set<E> updated = new HashSet<>(elements);
		updated.removeAll(toRemove);

		return new DefiniteSet<>(updated, isTop);
	}

	@Override
	public DefiniteSet<E> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		Collection<E> toRemove = new LinkedList<>();
		outer: for (E e : elements)
			for (Identifier id : ids)
				if (e.getInvolvedIdentifiers().contains(id)) {
					toRemove.add(e);
					continue outer;
				}

		if (toRemove.isEmpty())
			return this;

		Set<E> updated = new HashSet<>(elements);
		updated.removeAll(toRemove);

		return new DefiniteSet<>(updated, isTop);
	}

	@Override
	public DefiniteSet<E> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		Collection<E> toRemove = new LinkedList<>();
		for (E e : elements)
			if (e.getInvolvedIdentifiers().stream().anyMatch(test::test))
				toRemove.add(e);

		if (toRemove.isEmpty())
			return this;

		Set<E> updated = new HashSet<>(elements);
		updated.removeAll(toRemove);

		return new DefiniteSet<>(updated, isTop);
	}

	@Override
	public Set<E> getDataflowElements() {
		return elements;
	}

	@Override
	public DefiniteSet<E> update(
			Set<E> killed,
			Set<E> generated) {
		Set<E> updated = new HashSet<>(elements);
		updated.removeAll(killed);
		updated.addAll(generated);
		return new DefiniteSet<>(updated, isTop);
	}

	@Override
	public DefiniteSet<E> store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		Set<E> elements = new HashSet<>();
		for (E e : elements) {
			if (e.getInvolvedIdentifiers().contains(source))
				elements.add(e.replaceIdentifier(source, target));
			elements.add(e);
		}

		return new DefiniteSet<>(elements, isTop);
	}

}
