package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.EvaluationEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the end of an assumption of a symbolic expression during
 * the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisAssumeEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		EndEvent,
		EvaluationEvent<AnalysisState<A>, AnalysisState<A>> {

	private final ProgramPoint pp;
	private final AnalysisState<A> state;
	private final AnalysisState<A> result;
	private final SymbolicExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param pp         the program point where the assumption happens
	 * @param state      the analysis state before the computation
	 * @param result     the analysis state after the computation
	 * @param expression the symbolic expression being assumed
	 */
	public AnalysisAssumeEnd(
			ProgramPoint pp,
			AnalysisState<A> state,
			AnalysisState<A> result,
			SymbolicExpression expression) {
		this.pp = pp;
		this.state = state;
		this.result = result;
		this.expression = expression;
	}

	@Override
	public ProgramPoint getProgramPoint() {
		return pp;
	}

	@Override
	public AnalysisState<A> getPreState() {
		return state;
	}

	@Override
	public AnalysisState<A> getPostState() {
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
		return "Analysis: Assume of " + expression;
	}

}
