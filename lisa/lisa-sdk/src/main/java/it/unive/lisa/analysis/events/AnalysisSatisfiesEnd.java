package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the end of a satisfiability test of a symbolic expression
 * during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisSatisfiesEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		EndEvent {

	private final AnalysisState<A> state;
	private final Satisfiability result;
	private final SymbolicExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param state      the analysis state before the computation
	 * @param result     the satisfiability result
	 * @param expression the symbolic expression being tested
	 */
	public AnalysisSatisfiesEnd(
			AnalysisState<A> state,
			Satisfiability result,
			SymbolicExpression expression) {
		this.state = state;
		this.result = result;
		this.expression = expression;
	}

	/**
	 * Yields the analysis state before the computation.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getState() {
		return state;
	}

	/**
	 * Yields the satisfiability result.
	 * 
	 * @return the result
	 */
	public Satisfiability getResult() {
		return result;
	}

	/**
	 * Yields the symbolic expression being assumed.
	 * 
	 * @return the symbolic expression
	 */
	public SymbolicExpression getExpression() {
		return expression;
	}

	@Override
	public String getTarget() {
		return "Analysis: Satisfies of " + expression;
	}

}
