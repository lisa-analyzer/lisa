package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A worst-case {@link OpenCallPolicy}, where the whole analysis state becomes
 * top and all information is lost. The return value, if any, is stored in the
 * call's meta variable. All possible errors are assumed to be thrown.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class WorstCasePolicy
		implements
		OpenCallPolicy {

	/**
	 * The singleton instance of this class.
	 */
	public static final WorstCasePolicy INSTANCE = new WorstCasePolicy();

	private WorstCasePolicy() {
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> apply(
			OpenCall call,
			AnalysisState<A> entryState,
			Analysis<A, D> analysis,
			ExpressionSet[] params)
			throws SemanticException {
		AnalysisState<A> poststate = entryState.topExecution();

		AnalysisState<A> result;
		if (call.getStaticType().isVoidType())
			result = analysis.smallStepSemantics(poststate, new Skip(call.getLocation()), call);
		else {
			Identifier var = call.getMetaVariable();
			result = analysis.smallStepSemantics(poststate, var, call);
		}

		Map<Error, ProgramState<A>> errors = new HashMap<>();
		Map<Type, Set<Statement>> smashedErrors = new HashMap<>();
		ProgramState<A> top = poststate.getExecution();
		for (Type t : call.getProgram().getTypes().getTypes())
			if (t.isErrorType())
				if (analysis.shouldSmashError != null && analysis.shouldSmashError.test(t))
					smashedErrors.put(t, Set.of(call));
				else
					errors.put(new Error(t, call), top);

		return result.addErrors(errors).addSmashedErrors(smashedErrors, top);
	}

}
