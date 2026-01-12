package it.unive.lisa.listeners;

import java.io.BufferedWriter;
import java.io.IOException;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.ReportingTool;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.EventListener;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.interprocedural.events.InterproceduralEvent;
import it.unive.lisa.logging.TimeFormat;
import it.unive.lisa.util.collections.workset.LIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;

public class TracingListener
		implements
		EventListener {

	/**
	 * The name of the trace file.
	 */
	public static final String TRACE_FNAME = "trace.txt";

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
		if (!(event instanceof InterproceduralEvent))
			return;

		try {
			if (event instanceof StartEvent) {
				running.push(event);
				writer.write("| ".repeat(indent));
				writer.write("> ");
				writer.write(((StartEvent) event).getTarget());
				writer.newLine();
				indent++;
				if (++writes % 100 == 0)
					writer.flush();
			} else if (event instanceof EndEvent) {
				indent--;
				Event started = running.pop();
				writer.write("| ".repeat(indent));
				writer.write("L [completed in "
						+ TimeFormat.UP_TO_HOURS.format(event.getTimestamp() - started.getTimestamp())
						+ "]");
				writer.newLine();
				if (++writes % 100 == 0)
					writer.flush();
			}
		} catch (IOException e) {
			onError(event, e, tool);
		}
	}

}

