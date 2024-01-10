package it.unive.lisa.analysis.numeric;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * The pentagons abstract domain, a weakly relational numerical abstract domain. This abstract
 * domain captures properties of the form of x &disin; [a, b] &and; x &lt; y. It is more precise than the
 * well known interval domain, but it is less precise than the octagon domain.
 * It is implemented as a {@link ValueDomain}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class Pentagon implements ValueDomain<Pentagon>, BaseLattice<Pentagon> {

	/**
	 * The interval environment.
	 */
	private final ValueEnvironment<Interval> intervals;

	/**
	 * The upper bounds environment.
	 */
	private final ValueEnvironment<UpperBounds> upperBounds;

	/**
	 * Builds the pentagons.
	 */
	public Pentagon() {
		this.intervals = new ValueEnvironment<>(new Interval()).top();
		this.upperBounds = new ValueEnvironment<>(new UpperBounds(true)).top();
	}

	/**
	 * Builds the pentagons.
	 * 
	 * @param intervals the interval environment
	 * @param upperBounds the upper bounds environment
	 */
	public Pentagon(ValueEnvironment<Interval> intervals, ValueEnvironment<UpperBounds> upperBounds) {
		this.intervals = intervals;
		this.upperBounds = upperBounds;
	}

	@Override
	public Pentagon assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		ValueEnvironment<UpperBounds> newBounds = upperBounds.assign(id, expression, pp, oracle);
		ValueEnvironment<Interval> newIntervals = intervals.assign(id, expression, pp, oracle);

		// we add the semantics for assignments here as we have access to the whole assignment
		if (expression instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression) expression;
			BinaryOperator op = be.getOperator();
			if (op instanceof SubtractionOperator) {
				if (be.getLeft() instanceof Identifier) {
					Identifier x = (Identifier) be.getLeft();

					if (be.getRight() instanceof Constant) 
						// r = x - c
						newBounds = newBounds.putState(id, upperBounds.getState(x).add(x));
					else if (be.getRight() instanceof Identifier) {
						// r = x - y
						Identifier y = (Identifier) be.getRight();

						if (newBounds.getState(y).contains(x))
							newIntervals = newIntervals.putState(id, newIntervals.getState(id).glb(new Interval(MathNumber.ONE, MathNumber.PLUS_INFINITY)));
					}
				}
			} else if (op instanceof RemainderOperator && be.getRight() instanceof Identifier) {
				// r = u % d
				Identifier d = (Identifier) be.getRight();
				MathNumber low = intervals.getState(d).interval.getLow();
				if (low.isPositive()  || low.isZero())
					newBounds = newBounds.putState(id, new UpperBounds(Collections.singleton(d)));
				else
					newBounds = newBounds.putState(id, new UpperBounds().top());
			}
		}


		return new Pentagon(
				newIntervals,
				newBounds).closure();
	}

	@Override
	public Pentagon smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		return new Pentagon(
				intervals.smallStepSemantics(expression, pp, oracle),
				upperBounds.smallStepSemantics(expression, pp, oracle));
	}

	@Override
	public Pentagon assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle)
			throws SemanticException {
		return new Pentagon(
				intervals.assume(expression, src, dest, oracle),
				upperBounds.assume(expression, src, dest, oracle));
	}

	@Override
	public Pentagon forgetIdentifier(Identifier id) throws SemanticException {
		return new Pentagon(
				intervals.forgetIdentifier(id),
				upperBounds.forgetIdentifier(id));
	}

	@Override
	public Pentagon forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		return new Pentagon(
				intervals.forgetIdentifiersIf(test),
				upperBounds.forgetIdentifiersIf(test));
	}


	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		return intervals.satisfies(expression, pp, oracle).glb(upperBounds.satisfies(expression, pp, oracle));
	}

	@Override
	public Pentagon pushScope(ScopeToken token) throws SemanticException {
		return new Pentagon(intervals.pushScope(token), upperBounds.pushScope(token));
	}

	@Override
	public Pentagon popScope(ScopeToken token) throws SemanticException {
		return new Pentagon(intervals.popScope(token), upperBounds.popScope(token));
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		if (isBottom())
			return Lattice.bottomRepresentation();
		Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
		for (Identifier id : CollectionUtils.union(intervals.getKeys(), upperBounds.getKeys()))
			mapping.put(new StringRepresentation(id),
					new StringRepresentation(intervals.getState(id).toString() + ", " +
							upperBounds.getState(id).representation()));
		return new MapRepresentation(mapping);
	}

	@Override
	public Pentagon top() {
		return new Pentagon(intervals.top(), upperBounds.top());
	}

	@Override
	public boolean isTop() {
		return intervals.isTop() && upperBounds.isTop();
	}

	@Override
	public Pentagon bottom() {
		return new Pentagon(intervals.bottom(), upperBounds.bottom());
	}

	@Override
	public boolean isBottom() {
		return intervals.isBottom() && upperBounds.isBottom();
	}

	private Pentagon closure() throws SemanticException {
		ValueEnvironment<UpperBounds> newBounds = new ValueEnvironment<UpperBounds>(upperBounds.lattice, upperBounds.getMap());

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
						newBounds.getState(id1).glb(new UpperBounds(closure)));

		}

		return new Pentagon(intervals, newBounds);
	}

	@Override
	public Pentagon lubAux(Pentagon other) throws SemanticException {
		ValueEnvironment<UpperBounds> newBounds = upperBounds.lub(other.upperBounds);
		for (Entry<Identifier, UpperBounds> entry : upperBounds) {
			Set<Identifier> closure = new HashSet<>();
			for (Identifier bound : entry.getValue())
				if (other.intervals.getState(entry.getKey()).interval.getHigh()
						.compareTo(other.intervals.getState(bound).interval.getLow()) < 0)
					closure.add(bound);
			if (!closure.isEmpty())
				// glb is the union
				newBounds = newBounds.putState(entry.getKey(),
						newBounds.getState(entry.getKey()).glb(new UpperBounds(closure)));
		}

		for (Entry<Identifier, UpperBounds> entry : other.upperBounds) {
			Set<Identifier> closure = new HashSet<>();
			for (Identifier bound : entry.getValue())
				if (intervals.getState(entry.getKey()).interval.getHigh()
						.compareTo(intervals.getState(bound).interval.getLow()) < 0)
					closure.add(bound);
			if (!closure.isEmpty())
				// glb is the union
				newBounds = newBounds.putState(entry.getKey(),
						newBounds.getState(entry.getKey()).glb(new UpperBounds(closure)));
		}

		return new Pentagon(intervals.lub(other.intervals), newBounds);
	}

	@Override
	public Pentagon wideningAux(Pentagon other) throws SemanticException {
		return new Pentagon(intervals.widening(other.intervals), upperBounds.widening(other.upperBounds));
	}

	@Override
	public boolean lessOrEqualAux(Pentagon other) throws SemanticException {
		if (!intervals.lessOrEqual(other.intervals))
			return false;
		for (Entry<Identifier, UpperBounds> entry : other.upperBounds)
			for (Identifier bound : entry.getValue())
				if (!(upperBounds.getState(entry.getKey()).contains(bound)
						|| intervals.getState(entry.getKey()).interval.getHigh()
						.compareTo(intervals.getState(bound).interval.getLow()) < 0))
					return false;

		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(intervals, upperBounds);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pentagon other = (Pentagon) obj;
		return Objects.equals(intervals, other.intervals) && Objects.equals(upperBounds, other.upperBounds);
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public boolean knowsIdentifier(Identifier id) {
		return intervals.knowsIdentifier(id) || upperBounds.knowsIdentifier(id);
	}
}