package it.unive.lisa.listeners;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.ReportingTool;
import it.unive.lisa.analysis.events.AnalysisEvent;
import it.unive.lisa.analysis.events.DomainEvent;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.EventListener;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.interprocedural.events.InterproceduralEvent;
import it.unive.lisa.program.cfg.fixpoints.events.FixpointEvent;
import it.unive.lisa.util.collections.workset.LIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * An event listener that traces {@link StartEvent}s and {@link EndEvent}s to a
 * trace file, constructing a timeline of how the analysis performed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TracingListener
		implements
		EventListener {

	/**
	 * Levels of tracing supported.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public enum TraceLevel {

		/**
		 * Trace {@link InterproceduralEvent}s only.
		 */
		INTERPROCEDURAL,

		/**
		 * Trace everything admitted by {@link #INTERPROCEDURAL}, as well as
		 * {@link FixpointEvent}s.
		 */
		FIXPOINT,

		/**
		 * Trace everything admitted by {@link #FIXPOINT}, as well as
		 * {@link AnalysisEvent}s.
		 */
		ANALYSIS,

		/**
		 * Trace everything admitted by {@link #ANALYSIS}, as well as
		 * {@link DomainEvent}s.
		 */
		DOMAIN,

		/**
		 * Trace all start and end events.
		 */
		ALL;
	}

	/**
	 * The name of the trace file.
	 */
	public static final String TRACE_FNAME = "trace.txt";

	private final TraceLevel level;

	/**
	 * Builds the listener.
	 * 
	 * @param level the level of tracing
	 */
	public TracingListener(
			TraceLevel level) {
		this.level = level;
	}

	private BufferedWriter writer;
	private int indent;
	private int writes;
	private WorkingSet<Event> running;

	@Override
	public void beforeExecution(
			ReportingTool tool) {
		indent = 0;
		writes = 0;
		running = new LIFOWorkingSet<>();
		try {
			this.writer = tool.getFileManager().mkOutputWriter(TRACE_FNAME);
		} catch (IOException e) {
			throw new AnalysisSetupException("Cannot create trace file", e);
		}
	}

	@Override
	public void afterExecution(
			ReportingTool tool) {
		if (!running.isEmpty()) {
			int count = running.size();
			String missing = "";
			while (!running.isEmpty()) {
				StartEvent pop = (StartEvent) running.pop();
				missing += "\n - (" + pop.getClass().getSimpleName() + ") " + pop.getTarget();
			}
			tool.notice(count + " events not closed: " + missing);
		}

		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new AnalysisSetupException("Cannot close trace file", e);
		}
	}

	@Override
	public void onEvent(
			Event event,
			ReportingTool tool) {
		if (!(event instanceof StartEvent || event instanceof EndEvent))
			return;

		boolean pass = false;
		switch (level) {
		case ALL:
			pass = true;
			break;
		case DOMAIN:
			if (event instanceof DomainEvent)
				pass = true;
		case ANALYSIS:
			if (event instanceof AnalysisEvent)
				pass = true;
		case FIXPOINT:
			if (event instanceof FixpointEvent)
				pass = true;
		case INTERPROCEDURAL:
			if (event instanceof InterproceduralEvent)
				pass = true;
		default:
			break;
		}
		if (!pass)
			return;

		try {
			if (event instanceof StartEvent) {
				running.push(event);
				writer.write("  ".repeat(indent));
				writer.write(((StartEvent) event).getTarget());
				writer.newLine();
				indent++;
				if (++writes % 100 == 0)
					writer.flush();
			} else if (event instanceof EndEvent) {
				indent--;
				Event ended = running.pop();
				if (!((StartEvent) ended).getTarget().equals(((EndEvent) event).getTarget())) {
					tool.notice("Unmatched end event: expected "
							+ ((StartEvent) ended).getTarget()
							+ " (of type "
							+ ended.getClass().getSimpleName()
							+ ") but found "
							+ ((EndEvent) event).getTarget()
							+ " (of type "
							+ event.getClass().getSimpleName()
							+ ")");
				}
			}
		} catch (IOException e) {
			onError(event, e, tool);
		}
	}

}
