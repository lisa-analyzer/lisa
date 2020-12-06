package it.unive.lisa.cfg.edge;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Collection;

/**
 * A sequential edge connecting two statements. The abstract analysis state gets
 * modified by assuming that the statement where this edge originates does hold.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TrueEdge extends Edge {

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 */
	public TrueEdge(Statement source, Statement destination) {
		super(source, destination);
	}

	@Override
	public String toString() {
		return "[ " + getSource() + " ] -T-> [ " + getDestination() + " ]";
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> traverse(
			AnalysisState<H, V> sourceState) throws SemanticException {
		Collection<SymbolicExpression> exprs = sourceState.getComputedExpressions();
		AnalysisState<H, V> result = null;
		for (SymbolicExpression expr : exprs) {
			AnalysisState<H, V> tmp = sourceState.assume(expr);
			if (result == null)
				result = tmp;
			else
				result = result.lub(tmp);
		}
		return result;
	}
}
