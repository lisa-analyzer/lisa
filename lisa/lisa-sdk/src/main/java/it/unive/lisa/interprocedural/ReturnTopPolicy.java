package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.Variable;

/**
 * An {@link OpenCallPolicy}, where the post state is exactly the entry state,
 * with the only difference of having a synthetic variable named
 * {@value OpenCallPolicy#RETURNED_VARIABLE_NAME} assigned to top <i>only</i> if
 * the call returns a value. This variable, that is also stored as computed
 * expression, represent the unknown result of the call, if any.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReturnTopPolicy implements OpenCallPolicy {

	/**
	 * The singleton instance of this class.
	 */
	public static final ReturnTopPolicy INSTANCE = new ReturnTopPolicy();

	private ReturnTopPolicy() {
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> apply(
					OpenCall call,
					AnalysisState<A, H, V> entryState,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException {

		if (call.getStaticType().isVoidType())
			return entryState.smallStepSemantics(new Skip(call.getLocation()), call);
		else {
			PushAny pushany = new PushAny(call.getStaticType(), call.getLocation());
			pushany.setRuntimeTypes(call.getRuntimeTypes());
			Variable var = new Variable(call.getStaticType(), RETURNED_VARIABLE_NAME, call.getLocation());
			var.setRuntimeTypes(call.getRuntimeTypes());
			return entryState.assign(var, pushany, call);
		}
	}

}
