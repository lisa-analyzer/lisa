package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.SortedSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

/**
 * A graph that can be dumped as an html page using javascript to visualize the
 * graphs. This graph effectively wraps a {@link GraphmlGraph} instance to
 * provide visualization.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HtmlGraph extends GraphStreamWrapper {

	private final GraphmlGraph graph;

	private final String description;

	private final String descriptionLabel;

	private final String displayKey;

	private final SortedSet<String> displayClasses;

	/**
	 * Builds the graph.
	 * 
	 * @param graph            the wrapped {@link GraphmlGraph}
	 * @param description      the description of the graph, used as subtitle
	 *                             (can be {@code null})
	 * @param descriptionLabel the display name of the descriptions, used as
	 *                             label in the collapse/expand toggles
	 * @param displayKey       the name of the attribute that has to be searched
	 *                             for classifying nodes into categories that
	 *                             can be hidden
	 * @param displayClasses   the possible values for {@code displayKey}
	 */
	public HtmlGraph(GraphmlGraph graph, String description, String descriptionLabel, String displayKey,
			SortedSet<String> displayClasses) {
		super();
		this.graph = graph;
		this.description = description;
		this.descriptionLabel = descriptionLabel;
		this.displayKey = displayKey;
		this.displayClasses = displayClasses;
	}

	@Override
	public void dump(Writer writer) throws IOException {
		StringWriter graphWriter = new StringWriter();
		graph.dump(graphWriter, false);
		String graphText = graphWriter.toString().replace("&apos;", "'").replace("'", "\\'");
		String graphTitle = graph.getTitle();
		String graphDescription = "";
		if (StringUtils.isNotBlank(description))
			graphDescription = description;

		try (InputStream viewer = getClass().getClassLoader().getResourceAsStream("html-graph/viewer.html")) {
			String viewerCode = IOUtils.toString(viewer, StandardCharsets.UTF_8);
			viewerCode = viewerCode.replace("$$$GRAPH_TITLE$$$", graphTitle);
			viewerCode = viewerCode.replace("$$$GRAPH_DESCRIPTION$$$", graphDescription);
			viewerCode = viewerCode.replace("$$$GRAPH_DESCRIPTION_LABEL$$$", descriptionLabel);
			viewerCode = viewerCode.replace("$$$GRAPH_CONTENT$$$", graphText);

			StringBuilder switches = new StringBuilder();
			StringBuilder listeners = new StringBuilder();
			for (String displayClass : displayClasses) {
				String display = StringUtils.join(
						StringUtils.splitByCharacterTypeCamelCase(
								CaseUtils.toCamelCase(displayClass, true, '_')),
						' ');
				switches.append("\t\t\t\t<span><input type=\"checkbox\" id=\"show")
						.append(displayClass)
						.append("\" checked/>&nbsp;&nbsp;<label for=\"show")
						.append(displayClass)
						.append("\"><b>Show ")
						.append(display)
						.append("</b></label></span>\n");
				listeners.append("\t\tdocument.getElementById(\"show")
						.append(displayClass)
						.append("\").addEventListener(\"change\", function () {\n")
						.append("\t\t\ttoggleNodesVisibility(this.checked, '")
						.append(displayKey)
						.append("', '")
						.append(displayClass)
						.append("');\n")
						.append("\t\t});\n");
			}

			viewerCode = viewerCode.replace("$$$GRAPH_DISPLAY_SWITCHES$$$", switches.toString().trim());
			viewerCode = viewerCode.replace("$$$GRAPH_DISPLAY_LISTENERS$$$", listeners.toString().trim());

			writer.write(viewerCode);
		}
	}
}
