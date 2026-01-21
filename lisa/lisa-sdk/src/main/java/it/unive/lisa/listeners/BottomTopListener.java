package it.unive.lisa.listeners;

import it.unive.lisa.ReportingTool;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.events.EvaluationEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.EventListener;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * An event listener that traces bottom and top elements generated during the
 * analysis starting from states that are not bottom or top.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BottomTopListener
		implements
		EventListener {

	private final boolean traceTop;

	/**
	 * Builds the listener that does not trace top elements.
	 */
	public BottomTopListener() {
		this(false);
	}

	/**
	 * Builds the listener.
	 * 
	 * @param traceTop whether to trace top elements as well
	 */
	public BottomTopListener(
			boolean traceTop) {
		this.traceTop = traceTop;
	}

	@Override
	public void onEvent(
			Event event,
			ReportingTool tool) {
		if (!(event instanceof EvaluationEvent<?, ?>))
			return;
		EvaluationEvent<?, ?> eval = (EvaluationEvent<?, ?>) event;
		if (!(eval.getProgramPoint() instanceof Statement))
			return;
		Lattice<?> state = eval.getPreState();
		Lattice<?> result = eval.getPostState();
		Statement pp = (Statement) eval.getProgramPoint();
		if (result.isBottom() && !state.isBottom())
			tool.noticeOn(pp, "Analysis produced "
					+ Lattice.BOTTOM_STRING
					+ " from non-"
					+ Lattice.BOTTOM_STRING
					+ " state");
		if (traceTop && result.isTop() && !state.isTop())
			tool.noticeOn(pp, "Analysis produced "
					+ Lattice.TOP_STRING
					+ " from non-"
					+ Lattice.TOP_STRING
					+ " state");
	}
}
