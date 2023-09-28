package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.NullConstant;
import it.unive.lisa.type.NullType;

/**
 * A literal representing the {@code null} constant.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NullLiteral extends Literal<Object> {

	/**
	 * Builds the null literal, happening at the given location in the program.
	 * The type of a null literal is {@link NullType}.
	 * 
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location where the expression is defined within the
	 *                     program
	 */
	public NullLiteral(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, null, NullType.INSTANCE);
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> semantics(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		return entryState.smallStepSemantics(new NullConstant(getLocation()), this);
	}
}
