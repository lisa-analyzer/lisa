package it.unive.lisa.program.cfg.statement.evaluation;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;

public class RightToLeftEvaluation implements EvaluationOrder {

	public static final RightToLeftEvaluation INSTANCE = new RightToLeftEvaluation();

	private RightToLeftEvaluation() {
	}

	@Override
	public <A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>> void evaluate(
			Expression[] subExpressions,
			AnalysisState<A, H, V> entryState,
			InterproceduralAnalysis<A, H, V> interprocedural,
			StatementStore<A, H, V> expressions,
			ExpressionSet<SymbolicExpression>[] computed,
			AnalysisState<A, H, V>[] subStates)
			throws SemanticException {
		AnalysisState<A, H, V> preState = entryState;
		for (int i = computed.length; i >= 0; i--) {
			preState = subStates[i] = subExpressions[i].semantics(preState, interprocedural, expressions);
			expressions.put(subExpressions[i], subStates[i]);
			computed[i] = subStates[i].getComputedExpressions();
		}
	}
}
