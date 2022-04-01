package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.graphstream.graph.Graph;
import org.graphstream.stream.file.FileSinkBase;

public class FileSinkJSON extends FileSinkBase {
	protected PrintWriter out;

	@Override
	protected void outputHeader() throws IOException {
	}

	@Override
	protected void outputEndOfFile() throws IOException {
	}

	@Override
	protected void exportGraph(Graph g) {
		out = (PrintWriter) output;
		AtomicBoolean firstNode = new AtomicBoolean(true);
		AtomicBoolean firstEdge = new AtomicBoolean(true);

		out.printf("{\n\t\"nodes\":[\n");

		g.nodes().forEach(node -> {
			AtomicBoolean firstAttribute = new AtomicBoolean(true);
			String nodeId = node.getId();
			out.printf(firstNode.get() ? "\t{ \"id\" : \"%s\", \n" : ",\n\t{ \"id\" : \"%s\", \n", nodeId);

			if (firstNode.get())
				firstNode.set(false);

			node.attributeKeys().forEach(key -> {
				Object value = node.getAttribute(key);
				if (value instanceof String) {
					value = ((String) value).replace("\"", "\\\"");
				}
				out.printf(firstAttribute.get() ? "\t\t\"%s\" : \"%s\"" : ",\n\t\t\"%s\" : \"%s\"", key, value);

				if (firstAttribute.get())
					firstAttribute.set(false);
			});

			out.printf("\n\t}");
		});

		out.printf("\n\t],\n\t\"edges\" : [\n");

		g.edges().forEach(edge -> {
			AtomicBoolean firstAttribute = new AtomicBoolean(true);
			String edgeId = edge.getId();

			out.printf(firstEdge.get() ? "\t{ \"id\" : \"%s\", \n" : ",\n\t{ \"id\" : \"%s\", \n", edgeId);
			if (firstEdge.get())
				firstEdge.set(false);

			out.printf("\t\t\"start\" : \"%s\",\n", edge.getNode0());
			out.printf("\t\t\"end\" : \"%s\",\n", edge.getNode1());

			edge.attributeKeys().forEach(key -> {
				Object value = edge.getAttribute(key);
				out.printf(firstAttribute.get() ? "\t\t\"%s\" : \"%s\"" : ",\n\t\t\"%s\" : \"%s\"", key, value);

				if (firstAttribute.get())
					firstAttribute.set(false);
			});

			out.printf("\n\t}");
		});

		out.printf("\n\t]\n}");
	}

	public String toString(Graph g) {
		StringBuilder out = new StringBuilder();
		AtomicBoolean firstNode = new AtomicBoolean(true);
		AtomicBoolean firstEdge = new AtomicBoolean(true);

		out.append("{\n\t\"nodes\":[\n");

		g.nodes().forEach(node -> {
			AtomicBoolean firstAttribute = new AtomicBoolean(true);
			String nodeId = node.getId();
			out.append(String.format(firstNode.get() ? "\t{ \"id\" : \"%s\", \n" : ",\n\t{ \"id\" : \"%s\", \n",
					nodeId.toString()));

			if (firstNode.get())
				firstNode.set(false);

			node.attributeKeys().forEach(key -> {
				Object value = node.getAttribute(key);
				if (value instanceof String) {
					value = ((String) value).replace("\"", "\\\"");
				}
				out.append(String.format(firstAttribute.get() ? "\t\t\"%s\" : \"%s\"" : ",\n\t\t\"%s\" : \"%s\"",
						key.toString(), value.toString()));

				if (firstAttribute.get())
					firstAttribute.set(false);
			});

			out.append("\n\t}");
		});

		out.append("\n\t],\n\t\"edges\" : [\n");

		g.edges().forEach(edge -> {
			AtomicBoolean firstAttribute = new AtomicBoolean(true);
			String edgeId = edge.getId();

			out.append(String.format(firstEdge.get() ? "\t{ \"id\" : \"%s\", \n" : ",\n\t{ \"id\" : \"%s\", \n",
					edgeId.toString()));
			if (firstEdge.get())
				firstEdge.set(false);

			out.append(String.format("\t\t\"start\" : \"%s\",\n", edge.getNode0().toString()));
			out.append(String.format("\t\t\"end\" : \"%s\",\n", edge.getNode1().toString()));

			edge.attributeKeys().forEach(key -> {
				Object value = edge.getAttribute(key);
				out.append(String.format(firstAttribute.get() ? "\t\t\"%s\" : \"%s\"" : ",\n\t\t\"%s\" : \"%s\"",
						key.toString(), value.toString()));

				if (firstAttribute.get())
					firstAttribute.set(false);
			});

			out.append("\n\t}");
		});

		out.append("\n\t]\n}");
		return out.toString();
	}

	public void edgeAttributeAdded(String sourceId, long timeId, String edgeId, String attribute, Object value) {
		throw new UnsupportedOperationException();
	}

	public void edgeAttributeChanged(String sourceId, long timeId, String edgeId, String attribute, Object oldValue,
			Object newValue) {
		throw new UnsupportedOperationException();
	}

	public void edgeAttributeRemoved(String sourceId, long timeId, String edgeId, String attribute) {
		throw new UnsupportedOperationException();
	}

	public void graphAttributeAdded(String sourceId, long timeId, String attribute, Object value) {
		throw new UnsupportedOperationException();
	}

	public void graphAttributeChanged(String sourceId, long timeId, String attribute, Object oldValue,
			Object newValue) {
		throw new UnsupportedOperationException();
	}

	public void graphAttributeRemoved(String sourceId, long timeId, String attribute) {
		throw new UnsupportedOperationException();
	}

	public void nodeAttributeAdded(String sourceId, long timeId, String nodeId, String attribute, Object value) {
		throw new UnsupportedOperationException();
	}

	public void nodeAttributeChanged(String sourceId, long timeId, String nodeId, String attribute, Object oldValue,
			Object newValue) {
		throw new UnsupportedOperationException();
	}

	public void nodeAttributeRemoved(String sourceId, long timeId, String nodeId, String attribute) {
		throw new UnsupportedOperationException();
	}

	public void edgeAdded(String sourceId, long timeId, String edgeId, String fromNodeId, String toNodeId,
			boolean directed) {
		throw new UnsupportedOperationException();
	}

	public void edgeRemoved(String sourceId, long timeId, String edgeId) {
		throw new UnsupportedOperationException();
	}

	public void graphCleared(String sourceId, long timeId) {
		throw new UnsupportedOperationException();
	}

	public void nodeAdded(String sourceId, long timeId, String nodeId) {
		throw new UnsupportedOperationException();
	}

	public void nodeRemoved(String sourceId, long timeId, String nodeId) {
		throw new UnsupportedOperationException();
	}

	public void stepBegins(String sourceId, long timeId, double step) {
		throw new UnsupportedOperationException();
	}
}
