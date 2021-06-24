package it.unive.lisa.util.datastructures.graph;

import java.util.function.Function;

import it.unive.lisa.outputs.DotGraph;

public class TestGraph extends Graph<TestGraph, TestGraph.TestNode, TestGraph.TestEdge> {

	@Override
	protected DotGraph<TestNode, TestEdge, TestGraph> toDot(Function<TestNode, String> labelGenerator) {
		return null;
	}

	public static class TestNode implements Node<TestNode, TestEdge, TestGraph> {

		private int offset = -1;

		@Override
		public int setOffset(int offset) {
			return this.offset = offset;
		}

		@Override
		public <V> boolean accept(GraphVisitor<TestGraph, TestNode, TestEdge, V> visitor, V tool) {
			return false;
		}

		@Override
		public String toString() {
			return String.valueOf(offset);
		}
	}

	public static class TestEdge implements Edge<TestNode, TestEdge, TestGraph> {

		private final TestNode source, destination;

		public TestEdge(TestNode source, TestNode destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		public TestNode getSource() {
			return source;
		}

		@Override
		public TestNode getDestination() {
			return destination;
		}

		@Override
		public boolean canBeSimplified() {
			return false;
		}

		@Override
		public TestEdge newInstance(TestNode source, TestNode destination) {
			return new TestEdge(source, destination);
		}

		@Override
		public <V> boolean accept(GraphVisitor<TestGraph, TestNode, TestEdge, V> visitor, V tool) {
			return false;
		}

		@Override
		public String toString() {
			return source + "->" + destination;
		}
	}
}