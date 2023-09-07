package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Skip;

/**
 * A statement that raises an error, stopping the execution of the current CFG
 * and propagating the error to among the call chain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Throw extends UnaryStatement {

	/**
	 * Builds the throw, raising {@code expression} as error, happening at the
	 * given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param location   the location where the expression is defined within the
	 *                       program
	 * @param expression the expression to raise as error
	 */
	public Throw(CFG cfg, CodeLocation location, Expression expression) {
		super(cfg, location, "throw", expression);
	}

	@Override
	public boolean stopsExecution() {
		return true;
	}

	@Override
	public boolean throwsError() {
		return true;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> unarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		// only temporary
		return state.smallStepSemantics(new Skip(getLocation()), this);
	}
}
