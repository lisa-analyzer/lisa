package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class HtmlGraph extends GraphStreamWrapper {

	private final GraphmlGraph graph;
	
	private final String description;
	
	/**
	 * Builds a graph.
	 */
	public HtmlGraph(GraphmlGraph graph, String description) {
		super();
		this.graph = graph;
		this.description = description;
	}

	@Override
	public void dump(Writer writer) throws IOException {
		StringWriter graphWriter = new StringWriter();
		graph.dump(graphWriter, false);
		String graphText = graphWriter.toString();
		String graphTitle = graph.getTitle();
		
		try (InputStream viewer = getClass().getClassLoader().getResourceAsStream("html-graph/viewer.html")) {
			String viewerCode = IOUtils.toString(viewer, StandardCharsets.UTF_8);
			viewerCode = viewerCode.replace("$$$GRAPH_TITLE$$$", graphTitle);
			viewerCode = viewerCode.replace("$$$GRAPH_DESCRIPTION$$$", StringUtils.isNotBlank(description) ? description : "none");
			viewerCode = viewerCode.replace("$$$GRAPH_CONTENT$$$", graphText);
			writer.write(viewerCode);
		}
	}
}
