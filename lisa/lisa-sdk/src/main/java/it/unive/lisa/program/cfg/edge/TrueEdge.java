package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An edge connecting two statements, that is traversed when the condition
 * expressed in the source state holds. The abstract analysis state gets
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
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> traverse(
					AnalysisState<A, H, V> sourceState) throws SemanticException {
		ExpressionSet<SymbolicExpression> exprs = sourceState.getComputedExpressions();
		AnalysisState<A, H, V> result = sourceState.bottom();
		for (SymbolicExpression expr : exprs)
			result = result.lub(sourceState.assume(expr, getSource()));
		return result;
	}

	@Override
	public boolean canBeSimplified() {
		return false;
	}

	@Override
	public TrueEdge newInstance(Statement source, Statement destination) {
		return new TrueEdge(source, destination);
	}
}
