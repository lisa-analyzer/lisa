package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonRedundantPowerset.NonRelationalNonRedundantPowerset;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.NonRedundantIntervalSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An analysis computing finite non redundant powersets of {@link IntInterval}s,
 * approximating integer values as a non redundant set of intervals.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NonRedundantIntervals
		extends
		NonRelationalNonRedundantPowerset<NonRedundantIntervalSet, IntInterval> {

	/**
	 * Builds a new non redundant intervals analysis.
	 */
	public NonRedundantIntervals() {
		super(new Interval(), new NonRedundantIntervalSet());
	}

	@Override
	public ValueEnvironment<NonRedundantIntervalSet> assumeBinaryExpression(
			ValueEnvironment<NonRedundantIntervalSet> state,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;

		Identifier id;
		NonRedundantIntervalSet eval;
		boolean rightIsExpr;
		BinaryOperator operator = expression.getOperator();
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			if (!canProcess(right, src, oracle))
				// the expression does not have a numerical value, we do not
				// assume anything on it
				return state;
			eval = eval(state, right, src, oracle);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			if (!canProcess(left, src, oracle))
				// the expression does not have a numerical value, we do not
				// assume anything on it
				return state;
			eval = eval(state, left, src, oracle);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return state;

		NonRedundantIntervalSet starting = state.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return state.bottom();
		if (eval.isTop())
			// we do not know anything about the expression, so we cannot assume
			// anything on the identifier
			return state;

		SortedSet<IntInterval> newSet = new TreeSet<>();

		for (IntInterval startingInterval : starting.elements)
			for (IntInterval interval : eval.elements) {
				IntInterval update = Interval.updateValue(operator, rightIsExpr, startingInterval, interval);

				if (update == null)
					// no update, keep the original interval
					newSet.add(startingInterval);
				else if (update.isBottom())
					// update is bottom, so the value is not propagated
					continue;
				else
					// update is not bottom, so add it to the new set
					newSet.add(update);
			}

		NonRedundantIntervalSet intervals = new NonRedundantIntervalSet(newSet).removeRedundancy().removeOverlapping();
		if (intervals.isBottom())
			return state.bottom();
		else
			return state.putState(id, intervals);
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<NonRedundantIntervalSet> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Collections.emptySet();
		if (state.isBottom())
			return null;

		if ((e instanceof UnaryExpression && ((UnaryExpression) e).getOperator() == LogicalNegation.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalAnd.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalOr.INSTANCE)) {
			Satisfiability sat = satisfies(state, e, pp, oracle);
			if (sat == Satisfiability.SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
			else if (sat == Satisfiability.NOT_SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
			else if (sat == Satisfiability.UNKNOWN)
				return Collections.emptySet();
			else
				return null;
		}

		if (e instanceof UnaryExpression) {
			UnaryOperator operator = ((UnaryExpression) e).getOperator();
			if (operator == NumericToString.INSTANCE) {
				ValueExpression arg = (ValueExpression) ((UnaryExpression) e).getExpression();
				NonRedundantIntervalSet value = eval(state, arg, pp, oracle);
				if (value.isTop() || value.elements.size() != 1 || !value.elements.iterator().next().isSingleton())
					return Collections.emptySet();
				if (value.isBottom())
					return null;
				return ValueDomain.makeEqConstraint(
						pp.getProgram().getTypes().getStringType(),
						value.elements.iterator().next().getLow().toString(),
						e,
						pp);
			}
		}

		if (e instanceof BinaryExpression) {
			BinaryOperator operator = ((BinaryExpression) e).getOperator();
			if (operator == ComparisonEq.INSTANCE
					|| operator == ComparisonNe.INSTANCE
					|| operator == ComparisonLe.INSTANCE
					|| operator == ComparisonLt.INSTANCE
					|| operator == ComparisonGe.INSTANCE
					|| operator == ComparisonGt.INSTANCE) {
				Satisfiability sat = satisfies(state, e, pp, oracle);
				if (sat == Satisfiability.SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
				else if (sat == Satisfiability.NOT_SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
				else if (sat == Satisfiability.UNKNOWN)
					return Collections.emptySet();
				else
					return null;
			}
		}

		NonRedundantIntervalSet value = eval(state, e, pp, oracle);
		if (value.isBottom())
			return null;
		if (value.isTop())
			return Collections.emptySet();
		Integer lower = null, upper = null;
		try {
			for (IntInterval intv : value.elements) {
				if (!intv.lowIsMinusInfinity() && (lower == null || lower > intv.getLow().toInt()))
					lower = intv.getLow().toInt();
				if (!intv.highIsPlusInfinity() && (upper == null || upper < intv.getHigh().toInt()))
					upper = intv.getHigh().toInt();
			}
		} catch (MathNumberConversionException e1) {
			// both accesses are guarded by checks for infinity, so this should
			// never happen
			throw new SemanticException("Unable to extract interval bounds", e1);
		}

		if (upper == null && lower == null)
			return Collections.emptySet();
		return ValueDomain.makeRangeConstraints(pp.getProgram().getTypes().getIntegerType(), lower, upper, e, pp);
	}

}
