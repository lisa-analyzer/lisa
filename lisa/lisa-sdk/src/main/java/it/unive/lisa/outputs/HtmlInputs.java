package it.unive.lisa.outputs;

import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;

/**
 * An output that dumps each input cfg as an html file, optionally including
 * subnodes, with no information on the analysis results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HtmlInputs
		extends
		InputCFGDumper {

	private final boolean withSubnodes;

	/**
	 * Builds the html input dumper.
	 * 
	 * @param withSubnodes whether to include subnodes in the dump
	 */
	public HtmlInputs(
			boolean withSubnodes) {
		this.withSubnodes = withSubnodes;
	}

	@Override
	protected void dump(
			FileManager fileManager,
			SerializableGraph graph,
			String filename)
			throws IOException {
		fileManager.mkHtmlFile(filename, writer -> graph.toHtml(withSubnodes, "results").dump(writer));
		fileManager.usedHtmlViewer();
	}
}
