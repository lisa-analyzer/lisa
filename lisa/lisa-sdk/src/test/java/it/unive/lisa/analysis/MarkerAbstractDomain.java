package it.unive.lisa.analysis;

import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A minimal, hand-rolled {@link AbstractDomain} over
 * {@link MarkerAbstractLattice} used by tests in this package to exercise
 * {@link Analysis} without resorting to a mocking framework.
 * {@link #assign(MarkerAbstractLattice, Identifier, SymbolicExpression, ProgramPoint)}
 * actually records that the assigned identifier is now known, so that tests can
 * observe the effect of an assignment on the wrapped state; {@link #assume} is
 * unsatisfiable exactly when the given expression is a {@link Skip} (an
 * arbitrary but deterministic convention used only by these tests), letting
 * tests control whether {@link Analysis#assume} yields bottom or not.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MarkerAbstractDomain
		implements
		AbstractDomain<MarkerAbstractLattice> {

	@Override
	public MarkerAbstractLattice assign(
			MarkerAbstractLattice state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp) {
		Set<Identifier> known = new HashSet<>(state.known);
		known.add(id);
		return new MarkerAbstractLattice(known);
	}

	@Override
	public MarkerAbstractLattice smallStepSemantics(
			MarkerAbstractLattice state,
			SymbolicExpression expression,
			ProgramPoint pp) {
		return state;
	}

	@Override
	public MarkerAbstractLattice assume(
			MarkerAbstractLattice state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest) {
		return expression instanceof Skip ? state.bottom() : state;
	}

	@Override
	public SemanticOracle makeOracle(
			MarkerAbstractLattice state) {
		return new MarkerOracle();
	}

	@Override
	public MarkerAbstractLattice makeLattice() {
		return new MarkerAbstractLattice();
	}

	@Override
	public MarkerAbstractLattice onCallReturn(
			MarkerAbstractLattice entryState,
			MarkerAbstractLattice callres,
			ProgramPoint call) {
		return callres;
	}

	@Override
	public void setEventQueue(
			EventQueue queue) {
	}

	/**
	 * A minimal, hand-rolled {@link SemanticOracle} that answers every query
	 * with the least informative but well-formed answer.
	 */
	public static class MarkerOracle
			implements
			SemanticOracle {

		@Override
		public EventQueue getEventQueue() {
			return null;
		}

		@Override
		public Set<Type> getRuntimeTypesOf(
				SymbolicExpression e,
				ProgramPoint pp) {
			return Collections.singleton(Untyped.INSTANCE);
		}

		@Override
		public Type getDynamicTypeOf(
				SymbolicExpression e,
				ProgramPoint pp) {
			return Untyped.INSTANCE;
		}

		@Override
		public ExpressionSet rewrite(
				SymbolicExpression expression,
				ProgramPoint pp) {
			// identifiers are already in their simplest form; anything else
			// (e.g. an access to a heap structure) is rewritten to a fixed,
			// well-known identifier, letting tests exercise the "rewriting
			// produced an identifier" path of Analysis#assign
			if (expression instanceof Identifier)
				return new ExpressionSet(expression);
			return new ExpressionSet(new Variable(Untyped.INSTANCE, "rewritten", pp.getLocation()));
		}

		@Override
		public ExpressionSet rewrite(
				ExpressionSet expressions,
				ProgramPoint pp) {
			return expressions;
		}

		@Override
		public Satisfiability alias(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp) {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public ExpressionSet reachableFrom(
				SymbolicExpression e,
				ProgramPoint pp) {
			return new ExpressionSet(e);
		}

		@Override
		public Satisfiability isReachableFrom(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp) {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public Satisfiability areMutuallyReachable(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp) {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public boolean hasWholeValueAnlysis() {
			return false;
		}

		@Override
		public Set<BinaryExpression> constraints(
				ValueDomain<?> requesting,
				ValueExpression e,
				ProgramPoint pp) {
			return Collections.emptySet();
		}

	}

}
