package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.ValueCartesianCombination;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.UpperBounds.IdSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Implementation of the pentagons analysis of
 * <a href="https://doi.org/10.1016/j.scico.2009.04.004">this paper</a>.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Pentagon
		implements
		ValueDomain<Pentagon.Reduction> {

	/**
	 * The lattice structure of the pentagons analysis, which is a cartesian
	 * product of two lattices: a map from identifiers to {@link IntInterval}s,
	 * and a map from identifiers to {@link UpperBounds.IdSet}s (representing
	 * upper bounds information). This class also implements the necessary
	 * reductions to refine the bounds based on the intervals.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class Reduction
			extends
			ValueCartesianCombination<Reduction, ValueEnvironment<IntInterval>, ValueEnvironment<UpperBounds.IdSet>> {

		/**
		 * Builds a new reduced product between intervals and upper bounds.
		 */
		public Reduction() {
			super(
					new ValueEnvironment<>(IntInterval.TOP),
					new ValueEnvironment<>(new UpperBounds.IdSet(Collections.emptySet())));
		}

		private Reduction(
				ValueEnvironment<IntInterval> first,
				ValueEnvironment<IdSet> second) {
			super(first, second);
		}

		@Override
		public Reduction mk(
				ValueEnvironment<IntInterval> first,
				ValueEnvironment<IdSet> second) {
			return new Reduction(first, second);
		}

		@Override
		public boolean lessOrEqualAux(
				Reduction other)
				throws SemanticException {
			if (!first.lessOrEqual(other.first))
				return false;

			for (Entry<Identifier, UpperBounds.IdSet> entry : other.second)
				for (Identifier bound : entry.getValue()) {
					if (second.getState(entry.getKey()).contains(bound))
						continue;

					IntInterval state = first.getState(entry.getKey());
					IntInterval boundState = first.getState(bound);
					if (state.isBottom() || boundState.isTop() || state.getHigh().compareTo(boundState.getLow()) < 0)
						continue;

					return false;
				}

			return true;
		}

		@Override
		public Reduction lubAux(
				Reduction other)
				throws SemanticException {
			ValueEnvironment<IntInterval> newIntervals = first.lub(other.first);

			// lub performs the intersection between the two
			// this effectively builds s'
			ValueEnvironment<IdSet> newBounds = second.lub(other.second);

			// the following builds s''
			for (Identifier x : second.getKeys()) {
				UpperBounds.IdSet closure = newBounds.getState(x);

				IntInterval b2_x = other.first.getState(x);
				if (!b2_x.isBottom()) {
					for (Identifier y : second.getState(x)) {
						IntInterval b2_y = other.first.getState(y);
						if (!b2_y.isBottom() && b2_x.getHigh().compareTo(b2_y.getLow()) < 0) {
							closure = closure.add(y);
						}
					}
				}

				newBounds = newBounds.putState(x, closure);
			}

			// the following builds s'''
			for (Identifier x : other.second.getKeys()) {
				UpperBounds.IdSet closure = newBounds.getState(x);

				IntInterval b1_x = first.getState(x);
				if (!b1_x.isBottom())
					for (Identifier y : other.second.getState(x)) {
						IntInterval b1_y = first.getState(y);
						if (!b1_y.isBottom() && b1_x.getHigh().compareTo(b1_y.getLow()) < 0)
							closure = closure.add(y);
					}

				newBounds = newBounds.putState(x, closure);
			}

			return new Reduction(newIntervals, newBounds);
		}

		private Reduction closure()
				throws SemanticException {
			ValueEnvironment<IdSet> newBounds = new ValueEnvironment<>(second.lattice, second.getMap());

			for (Identifier id1 : first.getKeys()) {
				Set<Identifier> closure = new HashSet<>();
				for (Identifier id2 : first.getKeys())
					if (!id1.equals(id2))
						if (first.getState(id1).getHigh().compareTo(first.getState(id2).getLow()) < 0)
							closure.add(id2);
				if (!closure.isEmpty())
					// glb is the union
					newBounds = newBounds.putState(id1, newBounds.getState(id1).glb(new IdSet(closure)));

			}

			return new Reduction(first, newBounds);
		}

		@Override
		public Reduction store(
				Identifier target,
				Identifier source)
				throws SemanticException {
			return new Pentagon.Reduction(first.store(target, source), second.store(target, source));
		}

		@Override
		public StructuredRepresentation representation() {
			if (isTop())
				return Lattice.topRepresentation();
			if (isBottom())
				return Lattice.bottomRepresentation();
			Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
			for (Identifier id : CollectionUtils.union(first.getKeys(), second.getKeys()))
				mapping
						.put(
								new StringRepresentation(id),
								new StringRepresentation(
										first.getState(id).toString() + ", " + second.getState(id).representation()));
			return new MapRepresentation(mapping);
		}

	}

	private final UpperBounds upperbounds = new UpperBounds();

	private final Interval intervals = new Interval();

	@Override
	public Reduction makeLattice() {
		return new Reduction(intervals.makeLattice(), upperbounds.makeLattice());
	}

	@Override
	public Reduction assign(
			Reduction state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ValueEnvironment<IntInterval> newIntervals = intervals.assign(state.first, id, expression, pp, oracle);
		ValueEnvironment<IdSet> newBounds = upperbounds.assign(state.second, id, expression, pp, oracle);

		if (expression instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression) expression;
			BinaryOperator op = be.getOperator();

			if (op instanceof SubtractionOperator) {
				if (be.getLeft() instanceof Identifier) {
					Identifier x = (Identifier) be.getLeft();

					if (be.getRight() instanceof Identifier) {
						// r = x - y
						Identifier y = (Identifier) be.getRight();
						if (newBounds.getState(y).contains(x)) {
							newIntervals = newIntervals
									.putState(
											id,
											newIntervals
													.getState(id)
													.glb(new IntInterval(MathNumber.ONE, MathNumber.PLUS_INFINITY)));
						}
						IntInterval intv = state.first.getState(y);
						if (!intv.isBottom() && intv.getLow().compareTo(MathNumber.ZERO) > 0)
							newBounds = state.second.putState(id, state.second.getState(x).add(x));
						else
							newBounds = state.second.putState(id, new IdSet(Collections.emptySet(), true).top());
					}
				}
			} else if (op instanceof RemainderOperator && be.getRight() instanceof Identifier) {
				// r = u % d
				Identifier d = (Identifier) be.getRight();
				MathNumber low = state.first.getState(d).getLow();
				if (low.isPositive() || low.isZero())
					newBounds = newBounds.putState(id, new IdSet(Collections.singleton(d)));
				else
					newBounds = newBounds.putState(id, new IdSet(Collections.emptySet(), true).top());
			}

		}

		return new Reduction(newIntervals, newBounds).closure();
	}

	@Override
	public Reduction smallStepSemantics(
			Reduction state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new Reduction(
				intervals.smallStepSemantics(state.first, expression, pp, oracle),
				upperbounds.smallStepSemantics(state.second, expression, pp, oracle));
	}

	@Override
	public Reduction assume(
			Reduction state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		return new Reduction(
				intervals.assume(state.first, expression, src, dest, oracle),
				upperbounds.assume(state.second, expression, src, dest, oracle));
	}

	@Override
	public Satisfiability satisfies(
			Reduction state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return intervals
				.satisfies(state.first, expression, pp, oracle)
				.glb(upperbounds.satisfies(state.second, expression, pp, oracle));
	}

}
