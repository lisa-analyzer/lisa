package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
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
public class Throw
		extends
		UnaryStatement
		implements
		YieldsValue {

	/**
	 * Builds the throw, raising {@code expression} as error, happening at the
	 * given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param location   the location where the expression is defined within the
	 *                       program
	 * @param expression the expression to raise as error
	 */
	public Throw(
			CFG cfg,
			CodeLocation location,
			Expression expression) {
		super(cfg, location, "throw", expression);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0; // no extra fields to compare
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
	public Expression yieldedValue() {
		return getSubExpression();
	}

	@Override
	public Statement withValue(Expression value) {
		return new Throw(getCFG(), getLocation(), value);
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		// only temporary
		return interprocedural.getAnalysis().smallStepSemantics(state, new Skip(getLocation()), this);
	}

}
