package it.unive.lisa.outputs;

import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;

/**
 * An output that dumps each input cfg as a dot file, with no information on the
 * analysis results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class DotInputs
		extends
		InputCFGDumper {

	@Override
	protected void dump(
			FileManager fileManager,
			SerializableGraph graph,
			String filename)
			throws IOException {
		fileManager.mkDotFile(filename, writer -> graph.toDot().dump(writer));
	}

}
