package it.unive.lisa.util.datastructures.graph;

public interface GraphVisitor<G extends Graph<G, N, E>, N extends Node<N, E, G>, E extends Edge<N, E, G>, V> {

	boolean visit(V tool, G graph);
	
	boolean visit(V tool, G graph, N node);

	boolean visit(V tool, G graph, E edge);
}
