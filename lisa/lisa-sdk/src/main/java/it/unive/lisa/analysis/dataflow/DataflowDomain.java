package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.SetRepresentation;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * A dataflow domain that collects instances of {@link DataflowElement}. A
 * dataflow domain is a value domain that is represented as a set of elements,
 * that can be retrieved through {@link #getDataflowElements()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <D> the concrete type of {@link DataflowDomain}
 * @param <E> the concrete type of {@link DataflowElement} contained in this
 *                domain
 */
public abstract class DataflowDomain<D extends DataflowDomain<D, E>, E extends DataflowElement<D, E>>
		extends BaseLattice<D> implements ValueDomain<D> {

	private final boolean isTop;

	private final boolean isBottom;

	private final Set<E> elements;

	/**
	 * The underlying domain.
	 */
	public final E domain;

	/**
	 * Builds the domain.
	 * 
	 * @param domain   a singleton instance to be used during semantic
	 *                     operations to perform <i>kill</i> and <i>gen</i>
	 *                     operations
	 * @param elements the set of elements contained in this domain
	 * @param isTop    whether or not this domain is the top of the lattice
	 * @param isBottom whether or not this domain is the bottom of the lattice
	 */
	public DataflowDomain(E domain, Set<E> elements, boolean isTop, boolean isBottom) {
		this.elements = elements;
		this.domain = domain;
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	/**
	 * Utility for creating a concrete instance of {@link DataflowDomain} given
	 * its core fields.
	 * 
	 * @param domain   the underlying domain
	 * @param elements the elements contained in the instance to be created
	 * @param isTop    whether the created domain is the top element of the
	 *                     lattice
	 * @param isBottom whether the created domain is the bottom element of the
	 *                     lattice
	 * 
	 * @return the concrete instance of domain
	 */
	public abstract D mk(E domain, Set<E> elements, boolean isTop, boolean isBottom);

	@Override
	@SuppressWarnings("unchecked")
	public D assign(Identifier id, ValueExpression expression, ProgramPoint pp) throws SemanticException {
		// if id cannot be tracked by the underlying lattice,
		// or if the expression cannot be processed, return this
		return update(() -> !domain.tracksIdentifiers(id) || !domain.canProcess(expression),
				() -> domain.gen(id, expression, pp, (D) this),
				() -> domain.kill(id, expression, pp, (D) this));
	}

	@Override
	@SuppressWarnings("unchecked")
	public D smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		// if expression cannot be processed, return this
		return update(() -> !domain.canProcess(expression),
				() -> domain.gen(expression, pp, (D) this),
				() -> domain.kill(expression, pp, (D) this));
	}

	private interface SemanticElementsSupplier<E> {
		Collection<E> get() throws SemanticException;
	}

	@SuppressWarnings("unchecked")
	private D update(BooleanSupplier guard, SemanticElementsSupplier<E> gen, SemanticElementsSupplier<E> kill)
			throws SemanticException {
		if (isBottom())
			return (D) this;

		if (guard.getAsBoolean())
			return (D) this;

		Set<E> updated = new HashSet<>(getDataflowElements());
		for (E killed : kill.get())
			updated.remove(killed);
		for (E generated : gen.get())
			updated.add(generated);

		return mk(domain, updated, false, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public D assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		// TODO could be refined
		return (D) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop())
			return (D) this;

		Collection<E> toRemove = new LinkedList<>();
		for (E e : elements)
			if (e.getInvolvedIdentifiers().contains(id))
				toRemove.add(e);

		if (toRemove.isEmpty())
			return (D) this;

		Set<E> updated = new HashSet<>(elements);
		updated.removeAll(toRemove);
		return mk(domain, updated, false, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public D forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		if (isTop())
			return (D) this;

		Collection<E> toRemove = new LinkedList<>();
		for (E e : elements)
			if (e.getInvolvedIdentifiers().stream().anyMatch(test::test))
				toRemove.add(e);

		if (toRemove.isEmpty())
			return (D) this;

		Set<E> updated = new HashSet<>(elements);
		updated.removeAll(toRemove);
		return mk(domain, updated, false, false);
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		// TODO could be refined
		return Satisfiability.UNKNOWN;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
		result = prime * result + (isBottom ? 1231 : 1237);
		result = prime * result + (isTop ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataflowDomain<?, ?> other = (DataflowDomain<?, ?>) obj;
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		if (isBottom != other.isBottom)
			return false;
		if (isTop != other.isTop)
			return false;
		return true;
	}

	@Override
	public DomainRepresentation representation() {
		return new SetRepresentation(elements, DataflowElement::representation);
	}

	@Override
	public D top() {
		return mk(domain, new HashSet<>(), true, false);
	}

	@Override
	public boolean isTop() {
		return elements.isEmpty() && isTop;
	}

	@Override
	public D bottom() {
		return mk(domain, new HashSet<>(), false, true);
	}

	@Override
	public boolean isBottom() {
		return elements.isEmpty() && isBottom;
	}

	/**
	 * Yields the {@link DataflowElement}s contained in this domain instance.
	 * 
	 * @return the elements
	 */
	public final Set<E> getDataflowElements() {
		return elements;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D pushScope(ScopeToken scope) throws SemanticException {
		if (isTop() || isBottom())
			return (D) this;

		Set<E> result = new HashSet<>();
		E pushed;
		for (E element : this.elements)
			if ((pushed = element.pushScope(scope)) != null)
				result.add(pushed);

		return mk(domain, result, false, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public D popScope(ScopeToken scope) throws SemanticException {
		if (isTop() || isBottom())
			return (D) this;

		Set<E> result = new HashSet<>();
		E popped;
		for (E element : this.elements)
			if ((popped = element.popScope(scope)) != null)
				result.add(popped);

		return mk(domain, result, false, false);
	}

	@Override
	public final String toString() {
		return representation().toString();
	}
}
