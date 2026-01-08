package it.unive.lisa.interprocedural.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.events.Event;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * An event signaling that the analysis computed the given result for a given
 * call.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class ComputedCallResult<A extends AbstractLattice<A>>
		extends
		Event
		implements
		InterproceduralEvent {

	private final CFGCall call;
	private final ScopeId<A> id;
	private final AnalysisState<A> entry;
	private final ExpressionSet[] params;
	private final AnalysisState<A> state;

	/**
	 * Builds the event.
	 * 
	 * @param call   the call whose state has been computed
	 * @param id     the scope id for the results of this call
	 * @param entry  the entry state at the call site
	 * @param params the actual parameters at the call site
	 * @param state  the computed state
	 */
	public ComputedCallResult(
			CFGCall call,
			ScopeId<A> id,
			AnalysisState<A> entry,
			ExpressionSet[] params,
			AnalysisState<A> state) {
		this.call = call;
		this.state = state;
		this.id = id;
		this.entry = entry;
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
	 * Yields the scope id for the results of this call.
	 * 
	 * @return the scope id
	 */
	public ScopeId<A> getId() {
		return id;
	}

	/**
	 * Yields the entry state at the call site.
	 * 
	 * @return the entry state
	 */
	public AnalysisState<A> getEntryState() {
		return entry;
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
