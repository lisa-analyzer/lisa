package it.unive.lisa.outputs;

import java.io.IOException;

import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.util.file.FileManager;

/**
 * An output that dumps each input cfg as a json file, with no information on
 * the analysis results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JSONInputs
		extends
		InputCFGDumper {

	@Override
	protected void dump(
			FileManager fileManager,
			SerializableGraph graph,
			String filename)
			throws IOException {
		fileManager.mkJsonFile(filename + ".graph", writer -> graph.dump(writer));
	}

}
