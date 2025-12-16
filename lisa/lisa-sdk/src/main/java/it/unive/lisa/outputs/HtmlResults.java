package it.unive.lisa.outputs;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;

/**
 * An output that dumps each input cfg as an html file, optionally includiong
 * subnodes, including the results produced by the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class HtmlResults<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		OutputCFGDumper<A, D> {

	private final boolean withSubnodes;

	/**
	 * Builds the html input dumper.
	 * 
	 * @param withSubnodes whether to include subnodes in the dump
	 */
	public HtmlResults(
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
