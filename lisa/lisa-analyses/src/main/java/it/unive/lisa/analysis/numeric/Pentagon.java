package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
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
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Implementation of the pentagons analysis of
 * <a href="https://doi.org/10.1016/j.scico.2009.04.004">this paper</a>.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Pentagon
		implements
		ValueDomain<Pentagon>,
		BaseLattice<Pentagon> {

	private final UpperBounds upperbounds;
	private final ValueEnvironment<Interval> intervals;

	/**
	 * Builds a new pentagon instance.
	 */
	public Pentagon() {
		this(new UpperBounds().top(), new ValueEnvironment<>(new Interval()).top());
	}

	/**
	 * Builds a new pentagon instance.
	 * 
	 * @param upperbounds the client upper bounds instance
	 * @param intervals   the client intervals instance
	 */
	public Pentagon(
			UpperBounds upperbounds,
			ValueEnvironment<Interval> intervals) {
		this.upperbounds = upperbounds;
		this.intervals = intervals;
	}

	@Override
	public Pentagon top() {
		return new Pentagon(upperbounds.top(), intervals.top());
	}

	@Override
	public boolean isTop() {
		return upperbounds.isTop() && intervals.isTop();
	}

	@Override
	public Pentagon bottom() {
		return new Pentagon(upperbounds.bottom(), intervals.bottom());
	}

	@Override
	public boolean isBottom() {
		return upperbounds.isBottom() && intervals.isBottom();
	}

	@Override
	public boolean lessOrEqualAux(
			Pentagon other)
			throws SemanticException {
		if (!this.intervals.lessOrEqual(other.intervals))
			return false;

		for (Entry<Identifier, UpperBounds.IdSet> entry : other.upperbounds)
			for (Identifier bound : entry.getValue()) {
				if (this.upperbounds.getState(entry.getKey()).contains(bound))
					continue;

				Interval state = this.intervals.getState(entry.getKey());
				Interval boundState = this.intervals.getState(bound);
				if (state.isBottom()
						|| boundState.isTop()
						|| state.interval.getHigh().compareTo(boundState.interval.getLow()) < 0)
					continue;

				return false;
			}

		return true;
	}

	@Override
	public Pentagon lubAux(
			Pentagon other)
			throws SemanticException {
		ValueEnvironment<Interval> newIntervals = this.intervals.lub(other.intervals);

		// lub performs the intersection between the two
		// this effectively builds s'
		UpperBounds newBounds = upperbounds.lub(other.upperbounds);

		// the following builds s''
		for (Identifier x : upperbounds.getKeys()) {
			UpperBounds.IdSet closure = newBounds.getState(x);

			Interval b2_x = other.intervals.getState(x);
			if (!b2_x.isBottom()) {
				for (Identifier y : upperbounds.getState(x)) {
					Interval b2_y = other.intervals.getState(y);
					if (!b2_y.isBottom() && b2_x.interval.getHigh().compareTo(b2_y.interval.getLow()) < 0) {
						closure = closure.add(y);
					}
				}
			}

			newBounds = newBounds.putState(x, closure);
		}

		// the following builds s'''
		for (Identifier x : other.upperbounds.getKeys()) {
			UpperBounds.IdSet closure = newBounds.getState(x);

			Interval b1_x = intervals.getState(x);
			if (!b1_x.isBottom())
				for (Identifier y : other.upperbounds.getState(x)) {
					Interval b1_y = intervals.getState(y);
					if (!b1_y.isBottom() && b1_x.interval.getHigh().compareTo(b1_y.interval.getLow()) < 0)
						closure = closure.add(y);
				}

			newBounds = newBounds.putState(x, closure);
		}

		return new Pentagon(newBounds, newIntervals);
	}

	@Override
	public Pentagon wideningAux(
			Pentagon other)
			throws SemanticException {
		return new Pentagon(
				upperbounds.wideningAux(other.upperbounds),
				intervals.widening(other.intervals));
	}

	@Override
	public Pentagon assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UpperBounds newBounds = upperbounds.assign(id, expression, pp, oracle);
		ValueEnvironment<Interval> newIntervals = intervals.assign(id, expression, pp, oracle);

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
							newIntervals = newIntervals.putState(id, newIntervals.getState(id)
									.glb(new Interval(MathNumber.ONE, MathNumber.PLUS_INFINITY)));
						}
						Interval intv = intervals.getState(y);
						if (!intv.isBottom() && intv.interval.getLow().compareTo(MathNumber.ZERO) > 0)
							newBounds = upperbounds.putState(id, upperbounds.getState(x).add(x));
						else
							newBounds = upperbounds.putState(id, upperbounds.lattice.top());
					}
				}
			} else if (op instanceof RemainderOperator && be.getRight() instanceof Identifier) {
				// r = u % d
				Identifier d = (Identifier) be.getRight();
				MathNumber low = intervals.getState(d).interval.getLow();
				if (low.isPositive() || low.isZero())
					newBounds = newBounds.putState(id, new IdSet(Collections.singleton(d)));
				else
					newBounds = newBounds.putState(id, upperbounds.lattice.top());
			}

		}

		return new Pentagon(newBounds, newIntervals).closure();
	}

	private Pentagon closure() throws SemanticException {
		UpperBounds newBounds = new UpperBounds(upperbounds.lattice, upperbounds.getMap());

		for (Identifier id1 : intervals.getKeys()) {
			Set<Identifier> closure = new HashSet<>();
			for (Identifier id2 : intervals.getKeys())
				if (!id1.equals(id2))
					if (intervals.getState(id1).interval.getHigh()
							.compareTo(intervals.getState(id2).interval.getLow()) < 0)
						closure.add(id2);
			if (!closure.isEmpty())
				// glb is the union
				newBounds = newBounds.putState(id1,
						newBounds.getState(id1).glb(new UpperBounds.IdSet(closure)));

		}

		return new Pentagon(newBounds, intervals);
	}

	@Override
	public Pentagon smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new Pentagon(
				upperbounds.smallStepSemantics(expression, pp, oracle),
				intervals.smallStepSemantics(expression, pp, oracle));
	}

	@Override
	public Pentagon assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return new Pentagon(
				upperbounds.assume(expression, src, dest, oracle),
				intervals.assume(expression, src, dest, oracle));
	}

	@Override
	public Pentagon forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return new Pentagon(
				upperbounds.forgetIdentifier(id, pp),
				intervals.forgetIdentifier(id, pp));
	}

	@Override
	public Pentagon forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return new Pentagon(
				upperbounds.forgetIdentifiers(ids, pp),
				intervals.forgetIdentifiers(ids, pp));
	}

	@Override
	public Pentagon forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return new Pentagon(
				upperbounds.forgetIdentifiersIf(test, pp),
				intervals.forgetIdentifiersIf(test, pp));
	}

	@Override
	public Satisfiability satisfies(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return intervals.satisfies(expression, pp, oracle).glb(upperbounds.satisfies(expression, pp, oracle));
	}

	@Override
	public Pentagon pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new Pentagon(upperbounds.pushScope(token, pp), intervals.pushScope(token, pp));
	}

	@Override
	public Pentagon popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new Pentagon(upperbounds.popScope(token, pp), intervals.popScope(token, pp));
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		if (isBottom())
			return Lattice.bottomRepresentation();
		Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
		for (Identifier id : CollectionUtils.union(intervals.getKeys(), upperbounds.getKeys()))
			mapping.put(new StringRepresentation(id),
					new StringRepresentation(intervals.getState(id).toString() + ", " +
							upperbounds.getState(id).representation()));
		return new MapRepresentation(mapping);
	}

	@Override
	public int hashCode() {
		return Objects.hash(intervals, upperbounds);
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
		Pentagon other = (Pentagon) obj;
		return Objects.equals(intervals, other.intervals) && Objects.equals(upperbounds, other.upperbounds);
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return intervals.knowsIdentifier(id) || upperbounds.knowsIdentifier(id);
	}
}