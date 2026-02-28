package it.unive.lisa.interprocedural.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;

/**
 * An event signaling the end of the fixpoint computation for a given CFG in the
 * interprocedural analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class CFGFixpointEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		InterproceduralEvent,
		EndEvent {

	private final CFG cfg;
	private final ScopeId<A> id;
	private final AnalysisState<A> entryState;
	private final AnalyzedCFG<A> result;

	/**
	 * Builds the event.
	 * 
	 * @param cfg        the cfg whose fixpoint is starting
	 * @param id         the scope id of the analysis being performed
	 * @param entryState the entry state for the fixpoint
	 * @param result     the result of the fixpoint computation
	 */
	public CFGFixpointEnd(
			CFG cfg,
			ScopeId<A> id,
			AnalysisState<A> entryState,
			AnalyzedCFG<A> result) {
		this.cfg = cfg;
		this.id = id;
		this.entryState = entryState;
		this.result = result;
	}

	/**
	 * Yields the cfg whose fixpoint is starting.
	 * 
	 * @return the cfg
	 */
	public CFG getCfg() {
		return cfg;
	}

	/**
	 * Yields the scope id of the analysis being performed.
	 * 
	 * @return the scope id
	 */
	public ScopeId<A> getId() {
		return id;
	}

	/**
	 * Yields the entry state for the fixpoint.
	 * 
	 * @return the entry state
	 */
	public AnalysisState<A> getEntryState() {
		return entryState;
	}

	/**
	 * Yields the result of the fixpoint computation.
	 * 
	 * @return the result
	 */
	public AnalyzedCFG<A> getResult() {
		return result;
	}

	@Override
	public String getTarget() {
		return "Fixpoint of " + cfg.getDescriptor().getFullSignature();
	}
}
