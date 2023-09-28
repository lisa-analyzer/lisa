package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;

/**
 * A worst-case {@link OpenCallPolicy}, where the whole analysis state becomes
 * top and all information is lost. The return value, if any, is stored in the
 * call's meta variable.
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
	public <A extends AbstractState<A>> AnalysisState<A> apply(
			OpenCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] params)
			throws SemanticException {
		AnalysisState<A> poststate = entryState.top();

		if (call.getStaticType().isVoidType())
			return poststate.smallStepSemantics(new Skip(call.getLocation()), call);
		else {
			Identifier var = call.getMetaVariable();
			return poststate.smallStepSemantics(var, call);
		}
	}

}
