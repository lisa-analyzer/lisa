package it.unive.lisa.analysis;

import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.HistoryState;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A wrapper {@link AbstractDomain} that pairs an underlying domain {@code D}
 * with a {@link HistoryState}, recording the sequence of abstract lattice
 * elements that have been computed for a given program point across the
 * iterations of a fixpoint computation. This history can be used to inspect the
 * intermediate states computed during the iterates after the analysis
 * completes, instead of only being able to inspect the final invariant.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of {@link AbstractLattice} embedded in this state
 * @param <D> the type of {@link AbstractDomain} that produces instances of
 *                {@code A}
 */
public class HistoryDomain<
		D extends AbstractDomain<A>,
		A extends AbstractLattice<A>>
		implements
		AbstractDomain<HistoryState<A>> {

	private final D domain;

	/**
	 * Builds the history domain.
	 *
	 * @param domain the underlying domain
	 */
	public HistoryDomain(
			D domain) {
		this.domain = domain;
	}

	@Override
	public void setEventQueue(
			EventQueue queue) {
		domain.setEventQueue(queue);
	}

	@Override
	public HistoryState<A> assign(
			HistoryState<A> state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return new HistoryState<>(domain.assign(state.head(), id, expression, pp));
	}

	@Override
	public HistoryState<A> smallStepSemantics(
			HistoryState<A> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return new HistoryState<>(domain.smallStepSemantics(state.head(), expression, pp));
	}

	@Override
	public HistoryState<A> assume(
			HistoryState<A> state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		return new HistoryState<>(domain.assume(state.head(), expression, src, dest));
	}

	@Override
	public Satisfiability satisfies(
			HistoryState<A> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return domain.satisfies(state.head(), expression, pp);
	}

	@Override
	public HistoryState<A> makeLattice() {
		return new HistoryState<>(domain.makeLattice());
	}

	@Override
	public HistoryState<A> onCallReturn(
			HistoryState<A> entryState,
			HistoryState<A> callres,
			ProgramPoint call)
			throws SemanticException {
		return new HistoryState<>(domain.onCallReturn(entryState.head(), callres.head(), call));
	}

	@Override
	public SemanticOracle makeOracle(
			HistoryState<A> state) {
		return domain.makeOracle(state.head());
	}
}
