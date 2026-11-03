package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The lattice of {@link ReplayDomain}: an abstract state of the analysis,
 * paired with a {@link SymbolicStore} recording the symbolic transition
 * performed since the beginning of the replay. The abstract state is
 * responsible for all the semantic queries (satisfiability, runtime types, call
 * resolution, heap rewriting), exactly as during the analysis, while the store
 * only records.<br>
 * <br>
 * A state is bottom if its abstract state is, and top only if both its abstract
 * state and its store are. The lattice operations other than the least upper
 * bound (and the ones derived from it) are never used during a replay: they are
 * delegated to the abstract states, keeping the store of {@code this}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of the abstract states
 */
public class ReplayState<A extends AbstractLattice<A>>
		implements
		AbstractLattice<ReplayState<A>>,
		BaseLattice<ReplayState<A>> {

	private final A inner;

	private final SymbolicStore store;

	/**
	 * Builds the state.
	 *
	 * @param inner the abstract state
	 * @param store the symbolic store
	 */
	public ReplayState(
			A inner,
			SymbolicStore store) {
		this.inner = inner;
		this.store = store;
	}

	/**
	 * Yields the abstract state of this state.
	 *
	 * @return the abstract state
	 */
	public A getInner() {
		return inner;
	}

	/**
	 * Yields the symbolic store of this state.
	 *
	 * @return the store
	 */
	public SymbolicStore getStore() {
		return store;
	}

	/**
	 * Yields a copy of this state with the given abstract state.
	 *
	 * @param inner the abstract state
	 *
	 * @return the copy
	 */
	public ReplayState<A> withInner(
			A inner) {
		return inner == this.inner ? this : new ReplayState<>(inner, store);
	}

	/**
	 * Yields a copy of this state with the given store.
	 *
	 * @param store the store
	 *
	 * @return the copy
	 */
	public ReplayState<A> withStore(
			SymbolicStore store) {
		return store == this.store ? this : new ReplayState<>(inner, store);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return inner.knowsIdentifier(id);
	}

	@Override
	public ReplayState<A> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return new ReplayState<>(inner.forgetIdentifier(id, pp), store.forget(List.of(id)));
	}

	@Override
	public ReplayState<A> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return new ReplayState<>(inner.forgetIdentifiersIf(test, pp), store.forgetIf(test));
	}

	@Override
	public ReplayState<A> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return new ReplayState<>(inner.forgetIdentifiers(ids, pp), store.forget(ids));
	}

	@Override
	public ReplayState<A> pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new ReplayState<>(inner.pushScope(token, pp), store.scope(token, true));
	}

	@Override
	public ReplayState<A> popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new ReplayState<>(inner.popScope(token, pp), store.scope(token, false));
	}

	@Override
	public ReplayState<A> top() {
		return new ReplayState<>(inner.top(), store.top());
	}

	@Override
	public boolean isTop() {
		return inner.isTop() && store.isTop();
	}

	@Override
	public ReplayState<A> bottom() {
		return new ReplayState<>(inner.bottom(), store.identity());
	}

	@Override
	public boolean isBottom() {
		return inner.isBottom();
	}

	@Override
	public boolean lessOrEqualAux(
			ReplayState<A> other)
			throws SemanticException {
		return inner.lessOrEqual(other.inner) && store.lessOrEqual(other.store);
	}

	@Override
	public ReplayState<A> lubAux(
			ReplayState<A> other)
			throws SemanticException {
		return new ReplayState<>(inner.lub(other.inner), store.lub(other.store));
	}

	@Override
	public ReplayState<A> upchainAux(
			ReplayState<A> other)
			throws SemanticException {
		return new ReplayState<>(inner.upchain(other.inner), store.lub(other.store));
	}

	@Override
	public ReplayState<A> wideningAux(
			ReplayState<A> other)
			throws SemanticException {
		return new ReplayState<>(inner.widening(other.inner), store.lub(other.store));
	}

	@Override
	public ReplayState<A> glbAux(
			ReplayState<A> other)
			throws SemanticException {
		return new ReplayState<>(inner.glb(other.inner), store);
	}

	@Override
	public ReplayState<A> downchainAux(
			ReplayState<A> other)
			throws SemanticException {
		return new ReplayState<>(inner.downchain(other.inner), store);
	}

	@Override
	public ReplayState<A> narrowingAux(
			ReplayState<A> other)
			throws SemanticException {
		return new ReplayState<>(inner.narrowing(other.inner), store);
	}

	@Override
	public ReplayState<A> withTopMemory() {
		return new ReplayState<>(inner.withTopMemory(), store.heapHavoc());
	}

	@Override
	public ReplayState<A> withTopValues() {
		return new ReplayState<>(inner.withTopValues(), store.havocAll());
	}

	@Override
	public ReplayState<A> withTopTypes() {
		return new ReplayState<>(inner.withTopTypes(), store);
	}

	@Override
	public <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
			Class<D> domain) {
		Collection<D> result = AbstractLattice.super.getAllLatticeInstances(domain);
		result.addAll(inner.getAllLatticeInstances(domain));
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(inner, store);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReplayState<?> other = (ReplayState<?>) obj;
		return Objects.equals(inner, other.inner) && Objects.equals(store, other.store);
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		return new ListRepresentation(inner.representation(), new StringRepresentation(store.toString()));
	}
}
