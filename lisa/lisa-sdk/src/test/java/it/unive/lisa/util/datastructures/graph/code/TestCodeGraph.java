package it.unive.lisa.util.datastructures.graph.code;

import it.unive.lisa.util.datastructures.graph.GraphVisitor;

public class TestCodeGraph extends CodeGraph<TestCodeGraph, TestCodeGraph.TestCodeNode, TestCodeGraph.TestCodeEdge> {

	protected TestCodeGraph() {
		super(new TestCodeEdge(null, null));
	}

	public static class TestCodeNode implements CodeNode<TestCodeGraph, TestCodeNode, TestCodeEdge> {

		private final int id;

		public TestCodeNode(
				int id) {
			this.id = id;
		}

		@Override
		public <V> boolean accept(
				GraphVisitor<TestCodeGraph, TestCodeNode, TestCodeEdge, V> visitor,
				V tool) {
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
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestCodeNode other = (TestCodeNode) obj;
			if (id != other.id)
				return false;
			return true;
		}

		@Override
		public int compareTo(
				TestCodeNode o) {
			return id - o.id;
		}
	}

	public static class TestCodeEdge implements CodeEdge<TestCodeGraph, TestCodeNode, TestCodeEdge> {

		private final TestCodeNode source, destination;

		public TestCodeEdge(
				TestCodeNode source,
				TestCodeNode destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		public TestCodeNode getSource() {
			return source;
		}

		@Override
		public TestCodeNode getDestination() {
			return destination;
		}

		@Override
		public <V> boolean accept(
				GraphVisitor<TestCodeGraph, TestCodeNode, TestCodeEdge, V> visitor,
				V tool) {
			return false;
		}

		@Override
		public boolean isUnconditional() {
			return true;
		}

		@Override
		public TestCodeEdge newInstance(
				TestCodeNode source,
				TestCodeNode destination) {
			return new TestCodeEdge(source, destination);
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
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestCodeEdge other = (TestCodeEdge) obj;
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

		@Override
		public int compareTo(
				TestCodeEdge o) {
			if (source.id - o.source.id != 0)
				return source.id - o.source.id;
			return destination.id - o.destination.id;
		}
	}
}