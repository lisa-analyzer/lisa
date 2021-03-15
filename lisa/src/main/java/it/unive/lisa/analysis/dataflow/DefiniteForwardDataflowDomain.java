package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.InverseSetLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * A {@link DataflowDomain} for <b>forward</b> and <b>definite</b> dataflow
 * analysis. Being definite means that this domain is an instance of
 * {@link InverseSetLattice}, i.e., is a set whose join operation is the set
 * intersection.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of {@link DataflowElement} contained in this domain
 */
public class DefiniteForwardDataflowDomain<E extends DataflowElement<DefiniteForwardDataflowDomain<E>, E>>
		extends InverseSetLattice<DefiniteForwardDataflowDomain<E>, E>
		implements DataflowDomain<DefiniteForwardDataflowDomain<E>, E> {

	private final boolean isTop;

	private final E domain;

	/**
	 * Builds an empty domain.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to perform <i>kill</i> and <i>gen</i> operations
	 */
	public DefiniteForwardDataflowDomain(E domain) {
		this(domain, new HashSet<>(), true);
	}

	private DefiniteForwardDataflowDomain(E domain, Set<E> elements, boolean isTop) {
		super(elements);
		this.domain = domain;
		this.isTop = isTop;
	}

	@Override
	public DefiniteForwardDataflowDomain<E> assign(Identifier id, ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		Set<E> updated = new HashSet<>();
		for (E generated : domain.gen(id, expression, pp, this))
			updated.add(generated);
		DefiniteForwardDataflowDomain<E> gen = new DefiniteForwardDataflowDomain<E>(domain, updated, false);

		return gen.forgetIdentifiers(domain.kill(id, expression, pp, this));
	}

	@Override
	public DefiniteForwardDataflowDomain<E> smallStepSemantics(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public DefiniteForwardDataflowDomain<E> assume(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		// TODO could be refined
		return this;
	}

	@Override
	public DefiniteForwardDataflowDomain<E> forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop())
			return this;

		Collection<E> toRemove = new LinkedList<>();
		for (E e : elements)
			if (e.getIdentifier().equals(id))
				toRemove.add(e);

		if (toRemove.isEmpty())
			return this;
		Set<E> updated = new HashSet<>(elements);
		updated.removeAll(toRemove);
		return new DefiniteForwardDataflowDomain<E>(domain, updated, false);
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		// TODO could be refined
		return Satisfiability.UNKNOWN;
	}

	@Override
	public String representation() {
		return elements.toString();
	}

	@Override
	public DefiniteForwardDataflowDomain<E> top() {
		return new DefiniteForwardDataflowDomain<>(domain, new HashSet<>(), true);
	}

	@Override
	public boolean isTop() {
		return elements.isEmpty() && isTop;
	}

	@Override
	public DefiniteForwardDataflowDomain<E> bottom() {
		return new DefiniteForwardDataflowDomain<>(domain, new HashSet<>(), false);
	}

	@Override
	public boolean isBottom() {
		return elements.isEmpty() && !isTop;
	}

	@Override
	protected DefiniteForwardDataflowDomain<E> mk(Set<E> set) {
		return new DefiniteForwardDataflowDomain<>(domain, set, false);
	}

	@Override
	public Collection<E> getDataflowElements() {
		return elements;
	}
}
