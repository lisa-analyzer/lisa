package it.unive.lisa.analysis;

import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.HistoryState;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import org.apache.commons.lang3.tuple.Pair;

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
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return new HistoryState<>(domain.assign(state.head(), id, expression, context, pp));
	}

	@Override
	public HistoryState<A> smallStepSemantics(
			HistoryState<A> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return new HistoryState<>(domain.smallStepSemantics(state.head(), expression, context, pp));
	}

	@Override
	public HistoryState<A> assume(
			HistoryState<A> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		return new HistoryState<>(domain.assume(state.head(), expression, context, src, dest));
	}

	@Override
	public Satisfiability satisfies(
			HistoryState<A> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return domain.satisfies(state.head(), expression, context, pp);
	}

	@Override
	public HistoryState<A> makeLattice() {
		return new HistoryState<>(domain.makeLattice());
	}

	@Override
	public HistoryState<A> onCallReturn(
			HistoryState<A> entryState,
			HistoryState<A> callres,
			ScopeId<?> context,
			ProgramPoint call)
			throws SemanticException {
		return new HistoryState<>(domain.onCallReturn(entryState.head(), callres.head(), context, call));
	}

	@Override
	public Pair<HistoryState<A>, HistoryState<A>> onExecutionToError(
			HistoryState<A> remaining,
			HistoryState<A> moved,
			Error error,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		Pair<A, A> res = domain.onExecutionToError(remaining.head(), moved.head(), error, context, pp);
		// these callbacks adjust the current state without performing a
		// semantic step, so the history is preserved
		return Pair.of(
				res.getLeft() == remaining.head() ? remaining : remaining.withHead(res.getLeft()),
				res.getRight() == moved.head() ? moved : moved.withHead(res.getRight()));
	}

	@Override
	public HistoryState<A> onErrorToExecution(
			HistoryState<A> state,
			ProtectedBlock protectedBlock,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		// this callback adjusts the current state without performing a
		// semantic step, so the history is preserved
		A head = domain.onErrorToExecution(state.head(), protectedBlock, context, pp);
		return head == state.head() ? state : state.withHead(head);
	}

	@Override
	public Pair<HistoryState<A>, HistoryState<A>> onExecutionToHalting(
			HistoryState<A> remaining,
			HistoryState<A> moved,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		Pair<A, A> res = domain.onExecutionToHalting(remaining.head(), moved.head(), context, pp);
		// these callbacks adjust the current state without performing a
		// semantic step, so the history is preserved
		return Pair.of(
				res.getLeft() == remaining.head() ? remaining : remaining.withHead(res.getLeft()),
				res.getRight() == moved.head() ? moved : moved.withHead(res.getRight()));
	}

	@Override
	public SemanticOracle makeOracle(
			HistoryState<A> state,
			ScopeId<?> context) {
		return domain.makeOracle(state.head(), context);
	}
}
