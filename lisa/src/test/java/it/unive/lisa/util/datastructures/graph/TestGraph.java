package it.unive.lisa.util.datastructures.graph;

import it.unive.lisa.outputs.DotGraph;
import java.util.function.Function;

public class TestGraph extends Graph<TestGraph, TestGraph.TestNode, TestGraph.TestEdge> {

	@Override
	protected DotGraph<TestNode, TestEdge, TestGraph> toDot(Function<TestNode, String> labelGenerator) {
		return null;
	}

	public static class TestNode implements Node<TestNode, TestEdge, TestGraph> {

		private final int id;
		
		public TestNode(int id) {
			this.id = id;
		}

		@Override
		public int setOffset(int offset) {
			return offset;
		}

		@Override
		public <V> boolean accept(GraphVisitor<TestGraph, TestNode, TestEdge, V> visitor, V tool) {
			return false;
		}

		@Override
		public String toString() {
			return String.valueOf(id);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestNode other = (TestNode) obj;
			if (id != other.id)
				return false;
			return true;
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((destination == null) ? 0 : destination.hashCode());
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestEdge other = (TestEdge) obj;
			if (destination == null) {
				if (other.destination != null)
					return false;
			} else if (!destination.equals(other.destination))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}
	}
}