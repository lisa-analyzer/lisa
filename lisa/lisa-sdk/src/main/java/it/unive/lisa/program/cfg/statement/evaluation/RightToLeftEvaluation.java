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
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import java.util.HashSet;
import java.util.Set;

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
	@SuppressWarnings("unchecked")
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

		AnalysisState<A, H, V, T> postState = entryState;
		for (int i = computed.length - 1; i >= 0; i--) {
			AnalysisState<A, H, V, T> tmp = subExpressions[i].semantics(postState, interprocedural, expressions);
			expressions.put(subExpressions[i], tmp);
			computed[i] = tmp.getComputedExpressions();
			T typedom = (T) tmp.getDomainInstance(TypeDomain.class);
			Set<Type> types = new HashSet<>();
			for (SymbolicExpression e : tmp.rewrite(computed[i], subExpressions[i]))
				types.addAll(typedom.getRuntimeTypesOf((ValueExpression) e, subExpressions[i]));
			computed[i].forEach(e -> e.setRuntimeTypes(types));
			postState = tmp;
		}

		return postState;
	}

	@Override
	public int previous(int pos, int len) {
		return pos == len - 1 ? -1 : pos + 1;
	}

	@Override
	public int next(int pos, int len) {
		return pos - 1;
	}

	@Override
	public int first(int len) {
		return len - 1;
	}

	@Override
	public int last(int len) {
		return 0;
	}
}
