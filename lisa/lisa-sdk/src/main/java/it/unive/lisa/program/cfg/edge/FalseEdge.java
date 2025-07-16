package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;

/**
 * An edge connecting two statements, that is traversed when the condition
 * expressed in the source state does not hold. The abstract analysis state gets
 * modified by assuming that the statement where this edge originates does not
 * hold.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FalseEdge
		extends
		Edge {

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 */
	public FalseEdge(
			Statement source,
			Statement destination) {
		super(source, destination);
	}

	@Override
	public String toString() {
		return "[ " + getSource() + " ] -F-> [ " + getDestination() + " ]";
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> traverseForward(
					AnalysisState<A> state,
					Analysis<A, D> analysis)
					throws SemanticException {
		ExpressionSet exprs = state.getComputedExpressions();
		AnalysisState<A> result = state.bottom();
		for (SymbolicExpression expr : exprs) {
			UnaryExpression negated = new UnaryExpression(
					expr.getStaticType(),
					expr,
					LogicalNegation.INSTANCE,
					expr.getCodeLocation());
			result = result.lub(analysis.assume(state, negated, getSource(), getDestination()));
		}
		return result;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> traverseBackwards(
					AnalysisState<A> state,
					Analysis<A, D> analysis)
					throws SemanticException {
		return traverseForward(state, analysis);
	}

	@Override
	public boolean isUnconditional() {
		return false;
	}

	@Override
	public FalseEdge newInstance(
			Statement source,
			Statement destination) {
		return new FalseEdge(source, destination);
	}

}
