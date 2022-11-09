package it.unive.lisa.outputs;

import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableNodeDescription;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.SortedMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

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

	private final SortedMap<Integer, Pair<String, SerializableNodeDescription>> descriptions;

	private final String descriptionLabel;

	private final boolean includeSubnodes;

	/**
	 * Builds the graph.
	 * 
	 * @param graph            the wrapped {@link GraphmlGraph}
	 * @param includeSubnodes  whether or not sub-nodes should be part of the
	 *                             graph
	 * @param map              a map from a node id to its text and description
	 * @param description      the description of the graph, used as subtitle
	 *                             (can be {@code null})
	 * @param descriptionLabel the display name of the descriptions, used as
	 *                             label in the collapse/expand toggles
	 */
	public HtmlGraph(GraphmlGraph graph, boolean includeSubnodes,
			SortedMap<Integer, Pair<String, SerializableNodeDescription>> map,
			String description, String descriptionLabel) {
		super();
		this.graph = graph;
		this.descriptions = map;
		this.description = description;
		this.descriptionLabel = descriptionLabel;
		this.includeSubnodes = includeSubnodes;
	}

	@Override
	public void dump(Writer writer) throws IOException {
		StringWriter graphWriter = new StringWriter();
		graph.dump(graphWriter, false);
		String graphText = graphWriter.toString()
				.replace("&apos;", "'")
				.replace("'", "\\'")
				.replace("\n", "\\n");
		String graphTitle = graph.getTitle();
		String graphDescription = "";
		if (StringUtils.isNotBlank(description))
			graphDescription = description;

		String file = includeSubnodes ? "html-graph/viewer-compound.html" : "html-graph/viewer.html";
		try (InputStream viewer = getClass().getClassLoader().getResourceAsStream(file)) {
			String viewerCode = IOUtils.toString(viewer, StandardCharsets.UTF_8);
			viewerCode = viewerCode.replace("$$$GRAPH_TITLE$$$", graphTitle);
			viewerCode = viewerCode.replace("$$$GRAPH_DESCRIPTION$$$", graphDescription);
			viewerCode = viewerCode.replace("$$$GRAPH_DESCRIPTION_LABEL$$$", descriptionLabel);
			viewerCode = viewerCode.replace("$$$GRAPH_CONTENT$$$", graphText);

			StringBuilder descrs = new StringBuilder();
			for (Entry<Integer, Pair<String, SerializableNodeDescription>> d : descriptions.entrySet()) {
				String nodeName = nodeName(d.getKey());
				if (includeSubnodes || graph.graph.getNode(nodeName) != null) {
					descrs.append("\t\t\t\t<div id=\"header-")
							.append(nodeName)
							.append("\" class=\"header-hidden\">\n");
					descrs.append(
							"\t\t\t\t\t<div class=\"description-title-wrapper\"><span class=\"description-title\">")
							.append(StringUtils.capitalize(descriptionLabel))
							.append(" for ")
							.append("</span><span class=\"description-title-text\">")
							.append(d.getValue().getLeft())
							.append("</span></div>\n");
					populate(descrs, 5, d.getValue().getRight().getDescription());
					descrs.append("\t\t\t\t</div>\n");
				}
			}

			viewerCode = viewerCode.replace("$$$GRAPH_DESCRIPTIONS$$$", descrs.toString().trim());

			writer.write(viewerCode);
		}
	}

	private static void populate(StringBuilder descrs, int depth, SerializableValue value) {
		if (value instanceof SerializableString) {
			descrs.append(value.toString());
		} else if (value instanceof SerializableArray) {
			SerializableArray array = (SerializableArray) value;
			if (array.getElements().stream().allMatch(SerializableString.class::isInstance)) {
				descrs.append("[");
				for (int i = 0; i < array.getElements().size(); i++) {
					if (i != 0)
						descrs.append(", ");
					populate(descrs, depth + 1, array.getElements().get(i));
				}
				descrs.append("]");
			} else {
				for (int i = 0; i < array.getElements().size(); i++) {
					descrs.append("\t".repeat(depth))
							.append("<span class=\"description-header\">Element ")
							.append(i)
							.append(":</span><br/>\n");
					descrs.append("\t".repeat(depth))
							.append("<div class=\"description-nest\">\n");
					populate(descrs, depth + 1, array.getElements().get(i));
					descrs.append("\t".repeat(depth)).append("</div>\n");
				}
			}
		} else if (value instanceof SerializableObject) {
			SerializableObject object = (SerializableObject) value;
			for (Entry<String, SerializableValue> field : object.getFields().entrySet()) {
				descrs.append("\t".repeat(depth))
						.append("<span class=\"description-header\">")
						.append(field.getKey())
						.append(": </span>");
				if (isStringLike(field.getValue())) {
					populate(descrs, depth + 1, field.getValue());
					descrs.append("<br/>\n");
				} else {
					descrs.append("<br/>\n");
					descrs.append("\t".repeat(depth)).append("<div class=\"description-nest\">\n");
					populate(descrs, depth + 1, field.getValue());
					descrs.append("\t".repeat(depth)).append("</div>\n");
				}
			}
		} else
			throw new IllegalArgumentException("Unknown value type: " + value.getClass().getName());
	}

	private static boolean isStringLike(SerializableValue value) {
		return value instanceof SerializableString
				|| (value instanceof SerializableArray && ((SerializableArray) value).getElements().stream()
						.allMatch(SerializableString.class::isInstance));
	}
}
