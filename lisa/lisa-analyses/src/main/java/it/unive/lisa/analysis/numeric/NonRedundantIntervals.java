package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonRedundantPowerset.NonRelationalNonRedundantPowerset;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.numeric.NonRedundantIntervalSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
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
			eval = eval(state, right, src, oracle);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(state, left, src, oracle);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return state;

		NonRedundantIntervalSet starting = state.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return state.bottom();

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

}
