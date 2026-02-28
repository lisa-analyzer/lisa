package it.unive.lisa.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.ReportingTool;
import it.unive.lisa.analysis.events.AnalysisEvent;
import it.unive.lisa.analysis.events.DomainEvent;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.EventListener;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.interprocedural.events.InterproceduralEvent;
import it.unive.lisa.listeners.TracingListener.TraceLevel;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import it.unive.lisa.program.cfg.fixpoints.events.FixpointEvent;
import it.unive.lisa.util.collections.workset.LIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * An event listener that traces {@link StartEvent}s and {@link EndEvent}s to
 * build a flame graph-style html page.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FlameGraphListener
		implements
		EventListener {

	private static class TraceElement {
		private final String text;
		private final long start_ns;
		private long end_ns;
		private final List<TraceElement> children;

		public TraceElement(
				String text,
				long start_ns) {
			this.text = text;
			this.start_ns = start_ns;
			this.end_ns = 0;
			this.children = new LinkedList<>();
		}

		@Override
		public String toString() {
			return text + " [" + start_ns + ", " + end_ns + "]";
		}

		private StructuredRepresentation toSerializable() {
			Map<String, StructuredRepresentation> fields = Map.of(
					"text",
					new StringRepresentation(text),
					"start_ns",
					new StringRepresentation(start_ns),
					"end_ns",
					new StringRepresentation(end_ns),
					"children",
					new ListRepresentation(children.stream()
							.map(TraceElement::toSerializable)
							.collect(Collectors.toList())));
			return new ObjectRepresentation(fields);
		}
	}

	/**
	 * The name of the flame graph file.
	 */
	public static final String FLAME_FNAME = "flamegraph.html";

	private final TraceLevel level;

	/**
	 * Builds the listener.
	 * 
	 * @param level the level of tracing
	 */
	public FlameGraphListener(
			TraceLevel level) {
		this.level = level;
	}

	private TraceElement root;
	private long firstStartTime;
	private WorkingSet<TraceElement> running;

	@Override
	public void beforeExecution(
			ReportingTool tool) {
		running = new LIFOWorkingSet<>();
		root = null;
		firstStartTime = 0;
	}

	private String loadResourceTemplate(
			String path)
			throws IOException {
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
				Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
			scanner.useDelimiter("\\A");
			return scanner.hasNext() ? scanner.next() : "";
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

		SerializableValue val = root.toSerializable().toSerializableValue();
		StringWriter dump = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, System.getProperty("lisa.json.indent") != null);

		TemplateEngine templateEngine = new TemplateEngine();
		StringTemplateResolver resolver = new StringTemplateResolver();
		resolver.setTemplateMode(TemplateMode.HTML);
		templateEngine.setTemplateResolver(resolver);
		Context context = new Context();

		try {
			mapper.writeValue(dump, val);
			context.setVariable("graphContent", dump.toString());
			String htmlTemplate = loadResourceTemplate("html-files/flamegraph.html");
			String html = templateEngine.process(htmlTemplate, context);
			tool.getFileManager().mkHtmlFile("flamegraph", writer -> writer.write(html));
			tool.getFileManager().usedHtmlViewer();
		} catch (IOException e) {
			throw new AnalysisSetupException("Cannot serialize flame graph", e);
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

		long time_ns = event.getTimestamp() - firstStartTime;
		if (event instanceof StartEvent) {
			TraceElement started;
			if (root == null) {
				started = new TraceElement(((StartEvent) event).getTarget(), 0);
				firstStartTime = event.getTimestamp();
				root = started;
			} else {
				started = new TraceElement(((StartEvent) event).getTarget(), time_ns);
				running.peek().children.add(started);
			}
			running.push(started);
		} else if (event instanceof EndEvent) {
			TraceElement ended = running.pop();
			ended.end_ns = time_ns;
			if (!ended.text.equals(((EndEvent) event).getTarget())) {
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
	}

}
