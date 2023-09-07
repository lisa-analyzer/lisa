package it.unive.lisa.program.cfg.statement.evaluation;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A left-to-right {@link EvaluationOrder}, evaluating expressions in the given
 * order.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LeftToRightEvaluation implements EvaluationOrder {

	/**
	 * The singleton instance of this class.
	 */
	public static final LeftToRightEvaluation INSTANCE = new LeftToRightEvaluation();

	private LeftToRightEvaluation() {
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> evaluate(
			Expression[] subExpressions,
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A> interprocedural,
			StatementStore<A> expressions,
			ExpressionSet<SymbolicExpression>[] computed)
			throws SemanticException {
		if (subExpressions.length == 0)
			return entryState;

		AnalysisState<A> postState = entryState;
		for (int i = 0; i < computed.length; i++) {
			AnalysisState<A> tmp = subExpressions[i].semantics(postState, interprocedural, expressions);
			expressions.put(subExpressions[i], tmp);
			computed[i] = tmp.getComputedExpressions();
			for (SymbolicExpression e : computed[i])
				e.setRuntimeTypes(tmp.getState().getRuntimeTypesOf(e, subExpressions[i]));
			postState = tmp;
		}

		return postState;
	}

	@Override
	public int previous(int pos, int len) {
		return pos - 1;
	}

	@Override
	public int next(int pos, int len) {
		return pos == len - 1 ? -1 : pos + 1;
	}

	@Override
	public int first(int len) {
		return 0;
	}

	@Override
	public int last(int len) {
		return len - 1;
	}
}
