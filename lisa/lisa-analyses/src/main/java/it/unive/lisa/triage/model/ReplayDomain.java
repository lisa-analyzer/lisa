package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import java.util.Collection;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An abstract domain that replays the execution of a program, tracking the
 * symbolic store and the underlying abstract domain. The replay domain is
 * parameterized by the type of the underlying abstract domain and the type of
 * the abstract lattice it produces.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of {@link AbstractLattice} embedded in this state
 * @param <D> the type of {@link AbstractDomain} that produces instances of
 *                {@code A}
 */
public class ReplayDomain<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		AbstractDomain<ReplayState<A>> {

	private final D domain;

	private final FreshValueFactory factory;

	private final int maxCases;

	private final boolean restricts;

	/**
	 * Builds the domain, restricting each operation to the cases computing its
	 * operands.
	 *
	 * @param domain   the wrapped domain
	 * @param factory  the factory of fresh values used by the stores
	 * @param maxCases the maximum number of cases of each store
	 */
	public ReplayDomain(
			D domain,
			FreshValueFactory factory,
			int maxCases) {
		this(domain, factory, maxCases, true);
	}

	/**
	 * Builds the domain.
	 *
	 * @param domain    the wrapped domain
	 * @param factory   the factory of fresh values used by the stores
	 * @param maxCases  the maximum number of cases of each store
	 * @param restricts whether each operation is restricted to the cases of
	 *                      the store computing its operands (see
	 *                      {@link SymbolicStore#restrictTo(SymbolicExpression, ProgramPoint)}):
	 *                      without it, the stores do not record the
	 *                      expressions computed by their cases, and every case
	 *                      is paired with every computed expression
	 */
	public ReplayDomain(
			D domain,
			FreshValueFactory factory,
			int maxCases,
			boolean restricts) {
		this.domain = domain;
		this.factory = factory;
		this.maxCases = maxCases;
		this.restricts = restricts;
	}

	// the cases of the store that might have computed the given expression
	// (all of them, if the replay does not restrict them)
	private SymbolicStore restricted(
			SymbolicStore store,
			SymbolicExpression expression,
			ProgramPoint pp) {
		return restricts ? store.restrictTo(expression, pp) : store;
	}

	// the store where the given point computed the given expression (that is
	// not recorded, if the replay does not restrict the cases)
	private SymbolicStore computing(
			SymbolicStore store,
			SymbolicExpression expression,
			ProgramPoint pp) {
		return restricts ? store.computing(expression, pp) : store;
	}

	/**
	 * Yields the wrapped domain.
	 *
	 * @return the domain
	 */
	public D getDomain() {
		return domain;
	}

	/**
	 * Lifts the given abstract state to a {@link ReplayState} whose store is
	 * the identity transition, that is, the starting point of a replay.
	 *
	 * @param state the abstract state
	 *
	 * @return the lifted state
	 */
	public ReplayState<A> lift(
			A state) {
		return new ReplayState<>(state, SymbolicStore.identity(factory, maxCases));
	}

	@Override
	public void setEventQueue(
			EventQueue queue) {
		domain.setEventQueue(queue);
	}

	@Override
	public ReplayState<A> assign(
			ReplayState<A> state,
			Identifier id,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		A inner = domain.assign(state.getInner(), id, expression, context, pp);
		if (inner.isBottom())
			return state.withInner(inner);
		// only the cases that computed the expression assign it (see
		// SymbolicStore#restrictTo), and the point computed the identifier
		return new ReplayState<>(inner,
				computing(restricted(state.getStore(), expression, pp).assign(id, expression, pp), id, pp));
	}

	@Override
	public ReplayState<A> smallStepSemantics(
			ReplayState<A> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		A inner = domain.smallStepSemantics(state.getInner(), expression, context, pp);
		if (inner.isBottom())
			return state.withInner(inner);
		// only the cases that computed the operands of the expression compute
		// it (see SymbolicStore#restrictTo), and they all compute it (also
		// reads of identifiers: as the computed expressions are recognized by
		// identity, another read of the same identifier is not mistaken for
		// this one)
		SymbolicStore store = computing(restricted(state.getStore(), expression, pp), expression, pp);
		if (expression instanceof MemoryAllocation)
			// the allocation yields a new reference
			return new ReplayState<>(inner, store.allocate((MemoryAllocation) expression, pp));
		if (expression instanceof PushAny)
			// the unknown value is introduced here (e.g., as the result of a
			// call to code that is not part of the program)
			return new ReplayState<>(inner, store.introduce((PushAny) expression, pp));
		return new ReplayState<>(inner, store);
	}

	@Override
	public ReplayState<A> assume(
			ReplayState<A> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		A inner = domain.assume(state.getInner(), expression, context, src, dest);
		if (inner.isBottom())
			return state.withInner(inner);
		// only the cases that computed the condition assume it (see
		// SymbolicStore#restrictTo)
		return new ReplayState<>(inner, restricted(state.getStore(), expression, src).assume(expression, src));
	}

	@Override
	public ReplayState<A> restrictTo(
			ReplayState<A> state,
			ProgramPoint pp,
			SymbolicExpression computed) {
		// only the cases where the point computed the expression (see
		// SymbolicStore#restrictTo)
		SymbolicStore store = restricts ? state.getStore().restrictTo(pp, computed) : state.getStore();
		return store == state.getStore() ? state : new ReplayState<>(state.getInner(), store);
	}

	@Override
	public boolean restrictsExactly(
			ReplayState<A> state,
			ProgramPoint pp,
			SymbolicExpression computed) {
		// without restricting, the cases are all kept
		return restricts && state.getStore().restrictsExactly(pp, computed);
	}

	@Override
	public Satisfiability satisfies(
			ReplayState<A> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return domain.satisfies(state.getInner(), expression, context, pp);
	}

	@Override
	public ReplayState<A> makeLattice() {
		return lift(domain.makeLattice());
	}

	@Override
	public ReplayState<A> onCallReturn(
			ReplayState<A> entryState,
			ReplayState<A> callres,
			ScopeId<?> context,
			ProgramPoint call)
			throws SemanticException {
		return callres.withInner(domain.onCallReturn(entryState.getInner(), callres.getInner(), context, call));
	}

	@Override
	public Pair<ReplayState<A>, ReplayState<A>> onExecutionToError(
			ReplayState<A> remaining,
			ReplayState<A> moved,
			Error error,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		Pair<A, A> res = domain.onExecutionToError(remaining.getInner(), moved.getInner(), error, context, pp);
		return Pair.of(remaining.withInner(res.getLeft()), moved.withInner(res.getRight()));
	}

	@Override
	public ReplayState<A> onErrorToExecution(
			ReplayState<A> state,
			ProtectedBlock protectedBlock,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return state.withInner(domain.onErrorToExecution(state.getInner(), protectedBlock, context, pp));
	}

	@Override
	public Pair<ReplayState<A>, ReplayState<A>> onExecutionToHalting(
			ReplayState<A> remaining,
			ReplayState<A> moved,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		Pair<A, A> res = domain.onExecutionToHalting(remaining.getInner(), moved.getInner(), context, pp);
		return Pair.of(remaining.withInner(res.getLeft()), moved.withInner(res.getRight()));
	}

	@Override
	public SemanticOracle makeOracle(
			ReplayState<A> state,
			ScopeId<?> context) {
		return domain.makeOracle(state.getInner(), context);
	}

	@Override
	public <T> Collection<T> getAllDomainInstances(
			Class<T> domain) {
		Collection<T> result = AbstractDomain.super.getAllDomainInstances(domain);
		result.addAll(this.domain.getAllDomainInstances(domain));
		return result;
	}

}
