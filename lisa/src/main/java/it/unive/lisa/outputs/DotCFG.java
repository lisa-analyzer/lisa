package it.unive.lisa.outputs;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Statement;
import java.io.Reader;
import java.util.function.Function;
import org.graphstream.graph.implementations.MultiGraph;

/**
 * An {@link DotGraph} built from a {@link CFG}. Instances of this class can be
 * created through {@link #fromCFG(CFG, Function)}, or read from a file through
 * {@link DotGraph#readDot(Reader)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class DotCFG extends DotGraph<Statement, Edge, CFG> {

	private DotCFG() {
		super(new CFGLegend().graph);
	}

	/**
	 * Builds a {@link DotCFG} from a {@link CFG}. The label of a node
	 * representing a statement {@code node} will be composed by joining
	 * {@code node.toString()} ( {@link Statement#toString()}) with
	 * {@code labelGenerator.apply(node)} ({@link Function#apply(Object)})
	 * through a new line.
	 * 
	 * @param source         the source to export into dot format
	 * @param labelGenerator the function used to generate extra labels
	 * 
	 * @return the exported graph built starting from the source
	 */
	public static DotCFG fromCFG(CFG source, Function<Statement, String> labelGenerator) {
		DotCFG graph = new DotCFG();

		for (Statement node : source.getEntrypoints())
			graph.addNode(node, true, node.stopsExecution(), labelGenerator);

		for (Statement node : source.getNodes())
			if (!source.getEntrypoints().contains(node))
				graph.addNode(node, false, node.stopsExecution(), labelGenerator);

		for (Statement src : source.getNodes())
			for (Statement dest : source.followersOf(src)) {
				Edge edge = source.getEdgeConnecting(src, dest);
				if (edge instanceof TrueEdge)
					graph.addEdge(edge, COLOR_BLUE, CONDITIONAL_EDGE_STYLE);
				else if (edge instanceof FalseEdge)
					graph.addEdge(edge, COLOR_RED, CONDITIONAL_EDGE_STYLE);
				else
					graph.addEdge(edge, COLOR_BLACK, null);
			}

		return graph;
	}

	private static class CFGLegend {
		private final org.graphstream.graph.Graph graph;

		private CFGLegend() {
			graph = new MultiGraph("legend");
			org.graphstream.graph.Node l = graph.addNode("legend");
			StringBuilder builder = new StringBuilder();
			builder.append("<");
			builder.append("<table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" cellborder=\"0\">");
			builder.append("<tr><td align=\"right\">node border&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(NORMAL_NODE_COLOR);
			builder.append("\">");
			builder.append(NORMAL_NODE_COLOR);
			builder.append("</font>, single</td></tr>");
			builder.append("<tr><td align=\"right\">entrypoint border&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(SPECIAL_NODE_COLOR);
			builder.append("\">");
			builder.append(SPECIAL_NODE_COLOR);
			builder.append("</font>, single</td></tr>");
			builder.append("<tr><td align=\"right\">exitpoint border&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(SPECIAL_NODE_COLOR);
			builder.append("\">");
			builder.append(SPECIAL_NODE_COLOR);
			builder.append("</font>, double</td></tr>");
			builder.append("<tr><td align=\"right\">sequential edge&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(COLOR_BLACK);
			builder.append("\">");
			builder.append(COLOR_BLACK);
			builder.append("</font>, solid</td></tr>");
			builder.append("<tr><td align=\"right\">true edge&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(COLOR_BLUE);
			builder.append("\">");
			builder.append(COLOR_BLUE);
			builder.append("</font>, ");
			builder.append(CONDITIONAL_EDGE_STYLE);
			builder.append("</td></tr>");
			builder.append("<tr><td align=\"right\">false edge&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(COLOR_RED);
			builder.append("\">");
			builder.append(COLOR_RED);
			builder.append("</font>, ");
			builder.append(CONDITIONAL_EDGE_STYLE);
			builder.append("</td></tr>");
			builder.append("</table>");
			builder.append(">");
			l.setAttribute("label", builder.toString());
		}
	}
}
