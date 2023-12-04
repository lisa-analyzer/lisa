package it.unive.lisa.outputs.serializableGraph;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.NaryStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * Utility class to build {@link SerializableGraph}s from {@link CFG}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SerializableCFG {

	private SerializableCFG() {
	}

	/**
	 * Builds a {@link SerializableGraph} starting from the given {@link CFG},
	 * with no extra descriptions for the statements.
	 * 
	 * @param source the source cfg
	 *
	 * @return the serializable version of that cfg
	 */
	public static SerializableGraph fromCFG(
			CFG source) {
		return fromCFG(source, null);
	}

	/**
	 * Builds a {@link SerializableGraph} starting from the given {@link CFG},
	 * using the given function to generate extra descriptions for each
	 * statement.
	 * 
	 * @param source               the source cfg
	 * @param descriptionGenerator the function that can generate descriptions
	 *                                 from statements
	 *
	 * @return the serializable version of that cfg
	 */
	public static SerializableGraph fromCFG(
			CFG source,
			BiFunction<CFG, Statement, SerializableValue> descriptionGenerator) {
		String name = source.getDescriptor().getFullSignatureWithParNames();
		String desc;
		if (source instanceof AnalyzedCFG<?> && !((AnalyzedCFG<?>) source).getId().isStartingId())
			desc = ((AnalyzedCFG<?>) source).getId().toString();
		else
			desc = null;

		SortedSet<SerializableNode> nodes = new TreeSet<>();
		SortedSet<SerializableNodeDescription> descrs = new TreeSet<>();
		SortedSet<SerializableEdge> edges = new TreeSet<>();

		OffsetGenerator gen = new OffsetGenerator();
		source.accept(gen, null);

		for (Statement node : source.getNodes())
			process(source, nodes, descrs, node, descriptionGenerator, gen.result);

		for (Edge edge : source.getEdges())
			edges.add(new SerializableEdge(
					gen.result.get(edge.getSource()).getLeft(),
					gen.result.get(edge.getDestination()).getLeft(),
					edge.getClass().getSimpleName()));

		return new SerializableGraph(name, desc, nodes, edges, descrs);
	}

	private static void process(
			CFG source,
			SortedSet<SerializableNode> nodes,
			SortedSet<SerializableNodeDescription> descrs,
			Statement node,
			BiFunction<CFG, Statement, SerializableValue> descriptionGenerator,
			Map<Statement, Pair<Integer, List<Statement>>> mapping) {
		Pair<Integer, List<Statement>> p = mapping.get(node);
		for (Statement inner : p.getRight())
			process(source, nodes, descrs, inner, descriptionGenerator, mapping);
		List<Integer> innerIds = p.getValue().stream().map(st -> mapping.get(st).getKey()).collect(Collectors.toList());
		addNode(source, nodes, descrs, node, p.getKey(), innerIds, descriptionGenerator);
	}

	private static void addNode(
			CFG source,
			SortedSet<SerializableNode> nodes,
			SortedSet<SerializableNodeDescription> descrs,
			Statement node,
			Integer offset,
			List<Integer> inners,
			BiFunction<CFG, Statement, SerializableValue> descriptionGenerator) {
		SerializableNode n = new SerializableNode(offset, inners, node.toString());
		nodes.add(n);
		if (descriptionGenerator != null) {
			SerializableValue value = descriptionGenerator.apply(source, node);
			if (value != null)
				descrs.add(new SerializableNodeDescription(offset, value));
		}
	}

	private static class OffsetGenerator
			implements
			GraphVisitor<CFG, Statement, Edge, Void> {

		private int offset = 0;
		private Map<Statement, Pair<Integer, List<Statement>>> result = new IdentityHashMap<>();

		@Override
		public boolean visit(
				Void tool,
				CFG graph,
				Statement node) {
			List<Statement> inners = new LinkedList<>();
			if (node instanceof NaryStatement)
				inners.addAll(Arrays.asList(((NaryStatement) node).getSubExpressions()));
			else if (node instanceof NaryExpression)
				inners.addAll(Arrays.asList(((NaryExpression) node).getSubExpressions()));
			result.put(node, Pair.of(offset++, inners));
			return true;
		}
	}
}
