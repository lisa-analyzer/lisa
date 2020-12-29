package it.unive.lisa.util.datastructures.graph;

/**
 * A visitor of a {@link Graph}. Instances of this interface provide callbacks
 * to invoke when visiting the various components of a graph.<br>
 * <br>
 * The visiting starts by visiting the graph itself through
 * {@link #visit(Object, Graph)}, and then by visiting {@link Node}s and
 * {@link Edge}s independently, in no particular order, through
 * {@link #visit(Object, Graph, Node)} and {@link #visit(Object, Graph, Edge)}
 * respectively.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of {@link Graph} that this visitor visits
 * @param <N> the type of {@link Node}s in the visited graph
 * @param <E> the type of {@link Edge}s in the visited graph
 * @param <V> the type of auxiliary tool that the callbacks provided by this
 *                visitor can use while visiting the graph
 */
public interface GraphVisitor<G extends Graph<G, N, E>, N extends Node<N, E, G>, E extends Edge<N, E, G>, V> {

	/**
	 * Visits a graph, inspecting its structure and signature, but
	 * <b>without</b> accessing its contents.
	 * 
	 * @param tool  the auxiliary tool that this visitor can use
	 * @param graph the graph being visited
	 * 
	 * @return whether or not the visiting should stop when this call returns
	 */
	boolean visit(V tool, G graph);

	/**
	 * Visits a node of the given graph.
	 * 
	 * @param tool  the auxiliary tool that this visitor can use
	 * @param graph the graph where the visited node belongs
	 * @param node  the node being visited
	 * 
	 * @return whether or not the visiting should stop when this call returns
	 */
	boolean visit(V tool, G graph, N node);

	/**
	 * Visits an edge of the given graph.
	 * 
	 * @param tool  the auxiliary tool that this visitor can use
	 * @param graph the graph where the visited edge belongs
	 * @param edge  the edge being visited
	 * 
	 * @return whether or not the visiting should stop when this call returns
	 */
	boolean visit(V tool, G graph, E edge);
}
