package it.unive.lisa.outputs;

import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableNodeDescription;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * A graph that can be dumped as an html page using javascript to visualize the
 * graphs. This graph effectively wraps a {@link DotGraph} instance to provide
 * visualization.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HtmlGraph
		extends
		VisualGraph {

	private final SerializableGraph graph;

	private final String description;

	private final SortedMap<Integer, Pair<String, SerializableNodeDescription>> descriptions;

	private final String descriptionLabel;

	private final boolean includeSubnodes;

	/**
	 * Builds the graph.
	 * 
	 * @param g                the graph to dump
	 * @param includeSubnodes  whether or not sub-nodes should be part of the
	 *                             graph
	 * @param map              a map from a node id to its text and description
	 * @param description      the description of the graph, used as subtitle
	 *                             (can be {@code null})
	 * @param descriptionLabel the display name of the descriptions, used as
	 *                             label in the collapse/expand toggles
	 */
	public HtmlGraph(
			SerializableGraph g,
			boolean includeSubnodes,
			SortedMap<Integer, Pair<String, SerializableNodeDescription>> map,
			String description,
			String descriptionLabel) {
		super();
		this.graph = g;
		this.descriptions = map;
		this.description = description;
		this.descriptionLabel = descriptionLabel;
		this.includeSubnodes = includeSubnodes;
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
	public void dump(
			Writer writer)
			throws IOException {
		StringWriter graphWriter = new StringWriter();
		DotGraph dot = graph.toDot();
		dot.dumpStripped(graphWriter);
		String graphText = graphWriter.toString();
		String graphTitle = "Graph: " + dot.getTitle();
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

		Map<Integer, List<Integer>> inners = new HashMap<>();
		Set<Integer> topLevel = graph.getNodes().stream().map(n -> n.getId()).collect(Collectors.toSet());
		graph.getNodes().forEach(n -> {
			inners.put(n.getId(), n.getSubNodes());
			topLevel.removeAll(n.getSubNodes());
		});

		StringBuilder descrs = new StringBuilder();
		for (Entry<Integer, Pair<String, SerializableNodeDescription>> d : descriptions.entrySet()) {
			String nodeName = nodeName(d.getKey());
			boolean topLevelNode = topLevel.contains(d.getKey());
			if (topLevelNode)
				if (includeSubnodes)
					nodeDescriptionWithSubnodes(descrs, d, nodeName, inners, 4);
				else
					singleNodeDescription(descrs, d, nodeName, 4);
		}
		context.setVariable("graphDescriptions", descrs.toString().trim());

		String html = templateEngine.process(htmlTemplate, context);
		writer.write(html);
	}

	private void singleNodeDescription(
			StringBuilder descrs,
			Entry<Integer, Pair<String, SerializableNodeDescription>> d,
			String nodeName,
			int tabs) {
		descrs.append("\t".repeat(tabs))
				.append("<div id=\"header-")
				.append(nodeName)
				.append("\" class=\"header-info header-hidden\">\n");
		descrs.append("\t".repeat(tabs + 1))
				.append("<div class=\"description-title-wrapper\"><span class=\"description-title\">")
				.append(StringUtils.capitalize(descriptionLabel))
				.append(" for ")
				.append("</span><span class=\"description-title-text\">")
				.append(d.getValue().getLeft())
				.append("</span></div>\n");
		populate(descrs, tabs + 1, d.getValue().getRight().getDescription());
		descrs.append("\t".repeat(tabs)).append("</div>\n");
	}

	private void nodeDescriptionWithSubnodes(
			StringBuilder descrs,
			Entry<Integer, Pair<String, SerializableNodeDescription>> entry,
			String nodeName,
			Map<Integer, List<Integer>> inners,
			int tabs) {
		descrs.append("\t".repeat(tabs))
				.append("<div id=\"header-")
				.append(nodeName)
				.append("\" class=\"header-info header-hidden\">\n");
		singleSubNodeAccordion(descrs, true, entry.getKey(), entry.getValue(), inners, tabs + 1);
		descrs.append("\t".repeat(tabs)).append("</div>\n");
	}

	private void singleSubNodeAccordion(
			StringBuilder descrs,
			boolean active,
			Integer id,
			Pair<String, SerializableNodeDescription> d,
			Map<Integer, List<Integer>> inners,
			int tabs) {
		descrs.append("\t".repeat(tabs))
				.append("<button class=\"accordion")
				.append(active ? " active" : "")
				.append("\"><span class=\"description-title-text\">")
				.append(d.getLeft())
				.append("</span></button>\n");
		descrs.append("\t".repeat(tabs))
				.append("<div class=\"header-panel\"")
				.append(active ? " style=\"display: block\"" : "")
				.append(">");
		populate(descrs, tabs + 1, d.getRight().getDescription());
		List<Integer> subs = inners.getOrDefault(id, Collections.emptyList());
		if (!subs.isEmpty()) {
			descrs.append("\t".repeat(tabs)).append("<div class=\"description-nest\">\n");
			for (Integer i : subs)
				singleSubNodeAccordion(descrs, false, i, descriptions.get(i), inners, tabs + 1);
			descrs.append("\t".repeat(tabs)).append("</div>\n");
		}
		descrs.append("\t".repeat(tabs)).append("</div>\n");
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
					descrs.append("\t".repeat(depth)).append("<div class=\"description-nest\">\n");
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
				|| (value instanceof SerializableArray
						&& ((SerializableArray) value).getElements()
								.stream()
								.allMatch(SerializableString.class::isInstance));
	}

}
