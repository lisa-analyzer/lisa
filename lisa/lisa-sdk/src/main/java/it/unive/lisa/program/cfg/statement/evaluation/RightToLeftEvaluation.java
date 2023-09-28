package it.unive.lisa.program.cfg.statement.evaluation;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * A right-to-left {@link EvaluationOrder}, evaluating expressions in reversed
 * order.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class RightToLeftEvaluation implements EvaluationOrder {

	/**
	 * The singleton instance of this class.
	 */
	public static final RightToLeftEvaluation INSTANCE = new RightToLeftEvaluation();

	private RightToLeftEvaluation() {
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> evaluate(
			Expression[] subExpressions,
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A> interprocedural,
			StatementStore<A> expressions,
			ExpressionSet[] computed)
			throws SemanticException {
		if (subExpressions.length == 0)
			return entryState;

		AnalysisState<A> postState = entryState;
		for (int i = computed.length - 1; i >= 0; i--) {
			AnalysisState<A> tmp = subExpressions[i].semantics(postState, interprocedural, expressions);
			expressions.put(subExpressions[i], tmp);
			computed[i] = tmp.getComputedExpressions();
			postState = tmp;
		}

		return postState;
	}

	@Override
	public int previous(
			int pos,
			int len) {
		return pos == len - 1 ? -1 : pos + 1;
	}

	@Override
	public int next(
			int pos,
			int len) {
		return pos - 1;
	}

	@Override
	public int first(
			int len) {
		return len - 1;
	}

	@Override
	public int last(
			int len) {
		return 0;
	}
}
