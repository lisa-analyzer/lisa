package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An edge connecting two statements, that is traversed when the condition
 * expressed in the source state does not hold. The abstract analysis state gets
 * modified by assuming that the statement where this edge originates does not
 * hold.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FalseEdge extends Edge {

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
	public <A extends AbstractState<A>> AnalysisState<A> traverseForward(
			AnalysisState<A> state)
			throws SemanticException {
		ExpressionSet exprs = state.getComputedExpressions();
		AnalysisState<A> result = state.bottom();
		for (SymbolicExpression expr : exprs) {
			AnalysisState<A> falseState = state.split(expr, getSource(), getDestination()).getRight();
			result = result.lub(falseState.assume(expr, getSource(), getDestination()));
		}
		return result;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> traverseBackwards(
			AnalysisState<A> state)
			throws SemanticException {
		return traverseForward(state);
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
