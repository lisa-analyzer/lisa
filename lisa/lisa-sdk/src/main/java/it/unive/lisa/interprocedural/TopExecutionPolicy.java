package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;

/**
 * An {@link OpenCallPolicy} where the whole execution state becomes top and all
 * information is lost. The return value, if any, is stored in the call's meta
 * variable. No errors are assumed to be thrown.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TopExecutionPolicy
		implements
		OpenCallPolicy {

	/**
	 * The singleton instance of this class.
	 */
	public static final TopExecutionPolicy INSTANCE = new TopExecutionPolicy();

	private TopExecutionPolicy() {
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> apply(
			OpenCall call,
			AnalysisState<A> entryState,
			Analysis<A, D> analysis,
			ExpressionSet[] params)
			throws SemanticException {
		AnalysisState<A> poststate = entryState.topExecution();

		if (call.getStaticType().isVoidType())
			return analysis.smallStepSemantics(poststate, new Skip(call.getLocation()), call);
		else {
			Identifier var = call.getMetaVariable();
			return analysis.smallStepSemantics(poststate, var, call);
		}
	}

}
