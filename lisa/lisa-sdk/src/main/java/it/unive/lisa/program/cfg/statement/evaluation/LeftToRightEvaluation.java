package it.unive.lisa.program.cfg.statement.evaluation;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
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
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> evaluate(
					Expression[] subExpressions,
					AnalysisState<A, H, V, T> entryState,
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					StatementStore<A, H, V, T> expressions,
					ExpressionSet<SymbolicExpression>[] computed)
					throws SemanticException {
		if (subExpressions.length == 0)
			return entryState;

		AnalysisState<A, H, V, T> preState = entryState;
		for (int i = 0; i < computed.length; i++) {
			preState = subExpressions[i].semantics(preState, interprocedural, expressions);
			expressions.put(subExpressions[i], preState);
			computed[i] = preState.getComputedExpressions();
		}

		return preState;
	}
}
