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
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;

/**
 * Returns an expression to the caller CFG, terminating the execution of the CFG
 * where this statement lies. For terminating CFGs that do not return any value,
 * use {@link Ret}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Return
		extends
		UnaryStatement
		implements
		MetaVariableCreator {

	/**
	 * Builds the return, returning {@code expression} to the caller CFG,
	 * happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param location   the location where this statement is defined within the
	 *                       program
	 * @param expression the expression to return
	 */
	public Return(
			CFG cfg,
			CodeLocation location,
			Expression expression) {
		super(cfg, location, "return", expression);
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
	public final Identifier getMetaVariable() {
		Expression e = getSubExpression();
		String name = "ret_value@" + getCFG().getDescriptor().getName();
		Variable var = new Variable(e.getStaticType(), name, getLocation());
		return var;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Identifier meta = getMetaVariable();
		return interprocedural.getAnalysis().assign(state, meta, expr, this);
	}

}
