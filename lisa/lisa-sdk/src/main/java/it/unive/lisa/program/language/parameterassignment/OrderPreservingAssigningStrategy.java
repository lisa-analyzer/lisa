package it.unive.lisa.program.language.parameterassignment;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A strategy that passes the parameters in the same order as they are
 * specified.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class OrderPreservingAssigningStrategy
		implements
		ParameterAssigningStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final OrderPreservingAssigningStrategy INSTANCE = new OrderPreservingAssigningStrategy();

	private OrderPreservingAssigningStrategy() {
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> Pair<AnalysisState<A>, ExpressionSet[]> prepare(
					Call call,
					AnalysisState<A> callState,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions,
					Parameter[] formals,
					ExpressionSet[] parameters)
					throws SemanticException {
		// prepare the state for the call: assign the value to each parameter
		AnalysisState<A> prepared = callState;
		for (int i = 0; i < formals.length; i++) {
			AnalysisState<A> temp = prepared.bottom();
			for (SymbolicExpression exp : parameters[i])
				temp = temp
						.lub(interprocedural.getAnalysis().assign(prepared, formals[i].toSymbolicVariable(), exp,
								call));
			prepared = temp;
		}

		// we remove expressions from the stack
		prepared = new AnalysisState<>(prepared.getState(), new ExpressionSet(), prepared.getFixpointInformation());
		return Pair.of(prepared, parameters);
	}

}
