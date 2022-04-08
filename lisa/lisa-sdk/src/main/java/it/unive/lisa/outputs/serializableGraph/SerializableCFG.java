package it.unive.lisa.outputs.serializableGraph;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryStatement;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

public class SerializableCFG {

	public static SerializableGraph fromCFG(CFG source) {
		return fromCFG(source, null);
	}

	public static SerializableGraph fromCFG(CFG source, Function<Statement, SerializableValue> descriptionGenerator) {
		SerializableGraph graph = new SerializableGraph();

		graph.setName(source.getDescriptor().getFullSignatureWithParNames());
		if (source instanceof CFGWithAnalysisResults<?, ?, ?, ?>)
			graph.setDescription(((CFGWithAnalysisResults<?, ?, ?, ?>) source).getId());

		for (Statement node : source.getNodes()) {
			Map<Statement, List<Statement>> inners = new IdentityHashMap<>();
			node.accept(new InnerNodeExtractor(), inners);
			for (Statement inner : inners.keySet())
				addNode(graph, inner, inners.getOrDefault(inner, Collections.emptyList()), descriptionGenerator);
			addNode(graph, node, inners.getOrDefault(node, Collections.emptyList()), descriptionGenerator);
		}

		for (Statement src : source.getNodes())
			for (Statement dest : source.followersOf(src))
				for (Edge edge : source.getEdgesConnecting(src, dest))
					graph.addEdge(
							new SerializableEdge(src.getOffset(), dest.getOffset(), edge.getClass().getSimpleName()));

		return graph;
	}

	private static void addNode(SerializableGraph graph, Statement node, List<Statement> inners,
			Function<Statement, SerializableValue> descriptionGenerator) {
		List<Integer> innerIds = inners.stream().map(st -> st.getOffset()).collect(Collectors.toList());
		SerializableNode n = new SerializableNode(node.getOffset(), innerIds, node.toString());
		graph.addNode(n);
		if (descriptionGenerator != null) {
			SerializableValue value = descriptionGenerator.apply(node);
			if (value != null)
				graph.addNodeDescription(new SerializableNodeDescription(node.getOffset(), value));
		}
	}

	private static class InnerNodeExtractor
			implements GraphVisitor<CFG, Statement, Edge, Map<Statement, List<Statement>>> {

		@Override
		public boolean visit(Map<Statement, List<Statement>> tool, CFG graph) {
			return false;
		}

		@Override
		public boolean visit(Map<Statement, List<Statement>> tool, CFG graph, Statement node) {
			List<Statement> inners = tool.computeIfAbsent(node, st -> new LinkedList<>());
			if (node instanceof UnaryStatement)
				// TODO should we have an nary statement? or an interface that
				// marks statements with sub-statements?
				inners.add(((UnaryStatement) node).getExpression());
			else if (node instanceof NaryExpression)
				inners.addAll(Arrays.asList(((NaryExpression) node).getSubExpressions()));
			return true;
		}

		@Override
		public boolean visit(Map<Statement, List<Statement>> tool, CFG graph, Edge edge) {
			return false;
		}
	}
}
