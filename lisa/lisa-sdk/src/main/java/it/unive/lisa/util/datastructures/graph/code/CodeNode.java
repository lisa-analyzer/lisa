package it.unive.lisa.util.datastructures.graph.code;

import it.unive.lisa.util.datastructures.graph.Node;

public interface CodeNode<G extends CodeGraph<G, N, E>, N extends CodeNode<G, N, E>, E extends CodeEdge<G, N, E>>
		extends Node<G, N, E>, Comparable<N> {

	/**
	 * Sets the offset of this node to the given value, and then proceeds by
	 * setting the one of its nested nodes (if any) to subsequent values. The
	 * last offset used is returned.
	 * 
	 * @param offset the offset to set
	 * 
	 * @return the last offset used while setting the offsets of nested nodes
	 */
	int setOffset(int offset);

	/**
	 * Yields the offset of this node relative to its containing graph.
	 * 
	 * @return the offset
	 */
	int getOffset();
}
