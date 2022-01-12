package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.Variable;

/**
 * A worst-case {@link OpenCallPolicy}, where the whole analysis state becomes
 * top and all information is lost. The return value, if any, is stored in a
 * variable named {@value OpenCallPolicy#RETURNED_VARIABLE_NAME}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class WorstCasePolicy implements OpenCallPolicy {

	/**
	 * The singleton instance of this class.
	 */
	public static final WorstCasePolicy INSTANCE = new WorstCasePolicy();

	private WorstCasePolicy() {
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> apply(
					OpenCall call,
					AnalysisState<A, H, V> entryState,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException {
		AnalysisState<A, H, V> poststate = entryState.top();

		if (call.getStaticType().isVoidType())
			return poststate.smallStepSemantics(new Skip(call.getLocation()), call);
		else
			return poststate.smallStepSemantics(
					new Variable(call.getRuntimeTypes(), RETURNED_VARIABLE_NAME, call.getLocation()), call);
	}

}
