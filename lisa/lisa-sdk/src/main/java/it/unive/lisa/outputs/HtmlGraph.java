package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.SortedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableNodeDescription;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;

/**
 * A graph that can be dumped as an html page using javascript to visualize the
 * graphs. This graph effectively wraps a {@link DotGraph} instance to
 * provide visualization.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HtmlGraph extends VisualGraph {

	private final DotGraph graph;

	private final String description;

	private final SortedMap<Integer, Pair<String, SerializableNodeDescription>> descriptions;

	private final String descriptionLabel;

	private final boolean includeSubnodes;

	/**
	 * Builds the graph.
	 * 
	 * @param graph            the wrapped {@link DotGraph}
	 * @param includeSubnodes  whether or not sub-nodes should be part of the
	 *                             graph
	 * @param map              a map from a node id to its text and description
	 * @param description      the description of the graph, used as subtitle
	 *                             (can be {@code null})
	 * @param descriptionLabel the display name of the descriptions, used as
	 *                             label in the collapse/expand toggles
	 */
	public HtmlGraph(
			DotGraph graph,
			boolean includeSubnodes,
			SortedMap<Integer, Pair<String, SerializableNodeDescription>> map,
			String description,
			String descriptionLabel) {
		super();
		this.graph = graph;
		this.descriptions = map;
		this.description = description;
		this.descriptionLabel = descriptionLabel;
		this.includeSubnodes = includeSubnodes;
	}

    private String loadResourceTemplate(String path) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

	@Override
	public void dump(
			Writer writer)
			throws IOException {
		StringWriter graphWriter = new StringWriter();
		graph.dumpStripped(graphWriter);
		String graphText = graphWriter.toString();
		String graphTitle = "Graph: " + graph.getTitle();
		String graphDescription = "";
		if (StringUtils.isNotBlank(description))
			graphDescription = "ID: " + description;

		TemplateEngine templateEngine = new TemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(resolver);

        String file = includeSubnodes ? "html-graph/viewer-compound.html" : "html-graph/viewer.html";
        String htmlTemplate = loadResourceTemplate(file);

        Context context = new Context();
        context.setVariable("graphTitle", graphTitle);
        context.setVariable("graphDescription", graphDescription);
        context.setVariable("graphDescriptionLabel", descriptionLabel);
        context.setVariable("graphContent", graphText);
        StringBuilder descrs = new StringBuilder();
		for (Entry<Integer, Pair<String, SerializableNodeDescription>> d : descriptions.entrySet()) {
			String nodeName = nodeName(d.getKey());
			if (includeSubnodes || graph.graph.nodes().stream().filter(n -> n.name().toString().equals(nodeName)).findAny().isPresent()) {
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
        context.setVariable("graphDescriptions", descrs.toString().trim());
        
        
        String html = templateEngine.process(htmlTemplate, context);
        writer.write(html);
	}

	private static void populate(
			StringBuilder descrs,
			int depth,
			SerializableValue value) {
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

	private static boolean isStringLike(
			SerializableValue value) {
		return value instanceof SerializableString
				|| (value instanceof SerializableArray && ((SerializableArray) value).getElements().stream()
						.allMatch(SerializableString.class::isInstance));
	}
}
