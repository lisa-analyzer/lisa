package it.unive.lisa.interprocedural.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.Event;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * An event signaling that the analysis has computed the state for a given call.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class ComputedCallState<A extends AbstractLattice<A>>
		extends
		Event
		implements
		InterproceduralEvent {

	private final CFGCall call;
	private final AnalysisState<A> state;
	private final ExpressionSet[] params;

	/**
	 * Builds the event.
	 * 
	 * @param call   the call whose state has been computed
	 * @param state  the computed state
	 * @param params the actual parameters at the call site
	 */
	public ComputedCallState(
			CFGCall call,
			AnalysisState<A> state,
			ExpressionSet[] params) {
		this.call = call;
		this.state = state;
		this.params = params;
	}

	/**
	 * Yields the call whose state has been computed.
	 * 
	 * @return the call
	 */
	public CFGCall getCall() {
		return call;
	}

	/**
	 * Yields the computed state.
	 * 
	 * @return the state
	 */
	public AnalysisState<A> getState() {
		return state;
	}

	/**
	 * Yields the actual parameters at the call site.
	 * 
	 * @return the parameters
	 */
	public ExpressionSet[] getParams() {
		return params;
	}

}
