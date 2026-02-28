package it.unive.lisa.program.cfg.statement.evaluation;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.fixpoints.events.StatementSemanticsEnd;
import it.unive.lisa.program.cfg.fixpoints.events.StatementSemanticsStart;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * A left-to-right {@link EvaluationOrder}, evaluating expressions in the given
 * order.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LeftToRightEvaluation
		implements
		EvaluationOrder {

	/**
	 * The singleton instance of this class.
	 */
	public static final LeftToRightEvaluation INSTANCE = new LeftToRightEvaluation();

	private LeftToRightEvaluation() {
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> evaluate(
			Expression[] subExpressions,
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions,
			ExpressionSet[] computed)
			throws SemanticException {
		if (subExpressions.length == 0)
			return entryState;

		AnalysisState<A> postState = entryState;
		for (int i = 0; i < computed.length; i++) {
			Expression node = subExpressions[i];

			if (interprocedural.getEventQueue() != null)
				interprocedural.getEventQueue().post(new StatementSemanticsStart<>(node, postState));

			AnalysisState<A> tmp = node.forwardSemantics(postState, interprocedural, expressions);

			if (interprocedural.getEventQueue() != null)
				interprocedural.getEventQueue().post(new StatementSemanticsEnd<>(node, postState, tmp));

			expressions.put(node, tmp);
			computed[i] = tmp.getExecutionExpressions();
			postState = tmp;
		}

		return postState;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> bwdEvaluate(
			Expression[] subExpressions,
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions,
			ExpressionSet[] computed)
			throws SemanticException {
		if (subExpressions.length == 0)
			return entryState;

		AnalysisState<A> postState = entryState;
		for (int i = computed.length - 1; i >= 0; i--) {
			Expression node = subExpressions[i];

			if (interprocedural.getEventQueue() != null)
				interprocedural.getEventQueue().post(new StatementSemanticsStart<>(node, postState));

			AnalysisState<A> tmp = node.backwardSemantics(postState, interprocedural, expressions);

			if (interprocedural.getEventQueue() != null)
				interprocedural.getEventQueue().post(new StatementSemanticsEnd<>(node, postState, tmp));

			expressions.put(node, tmp);
			computed[i] = tmp.getExecutionExpressions();
			postState = tmp;
		}

		return postState;
	}

	@Override
	public int previous(
			int pos,
			int len) {
		return pos - 1;
	}

	@Override
	public int next(
			int pos,
			int len) {
		return pos == len - 1 ? -1 : pos + 1;
	}

	@Override
	public int first(
			int len) {
		return 0;
	}

	@Override
	public int last(
			int len) {
		return len - 1;
	}

}
