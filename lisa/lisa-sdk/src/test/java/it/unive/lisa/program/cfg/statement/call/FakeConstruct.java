package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A minimal {@link PluggableStatement}, only used so that a {@link NativeCFG}
 * can be built in tests without ever actually invoking its rewrite machinery.
 */
public class FakeConstruct
		extends
		NaryExpression
		implements
		PluggableStatement {

	public FakeConstruct(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "fake", parameters);
	}

	public static FakeConstruct build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new FakeConstruct(cfg, location, parameters);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		return state;
	}

}
