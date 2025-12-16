package it.unive.lisa.outputs;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;

/**
 * An output that dumps each input cfg as a dot file, including the results
 * produced by the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class DotResults<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		OutputCFGDumper<A, D> {

	@Override
	protected void dump(
			FileManager fileManager,
			SerializableGraph graph,
			String filename)
			throws IOException {
		fileManager.mkDotFile(filename, writer -> graph.toDot().dump(writer));
	}

}
