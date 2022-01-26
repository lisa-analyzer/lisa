package it.unive.lisa.program.cfg.statement.call.assignment;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A strategy that passes the parameters in the same order as they are
 * specified.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class OrderPreservingAssigningStrategy implements ParameterAssigningStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final OrderPreservingAssigningStrategy INSTANCE = new OrderPreservingAssigningStrategy();

	private OrderPreservingAssigningStrategy() {
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> prepare(
					Call call,
					AnalysisState<A, H, V> callState,
					InterproceduralAnalysis<A, H, V> interprocedural,
					StatementStore<A, H, V> expressions,
					Parameter[] formals,
					ExpressionSet<SymbolicExpression>[] parameters)
					throws SemanticException {
		// prepare the state for the call: assign the value to each parameter
		AnalysisState<A, H, V> prepared = callState;
		for (int i = 0; i < formals.length; i++) {
			AnalysisState<A, H, V> temp = prepared.bottom();
			for (SymbolicExpression exp : parameters[i])
				temp = temp.lub(prepared.assign(formals[i].toSymbolicVariable(), exp, call));
			prepared = temp;
		}

		return prepared;
	}

}
