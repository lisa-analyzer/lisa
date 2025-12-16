package it.unive.lisa.outputs;

import it.unive.lisa.LiSAReport;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An output that dumps each input cfg, with no information on the analysis
 * results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class InputCFGDumper
		implements
		LiSAOutput {

	private static final Logger LOG = LogManager.getLogger(InputCFGDumper.class);

	@Override
	public void dump(
			Application app,
			LiSAReport report,
			CheckTool tool,
			FileManager fileManager)
			throws IOException {
		Collection<CFG> allCFGs = app.getAllCFGs();
		for (CFG cfg : IterationLogger.iterate(LOG, allCFGs, "Dumping input cfgs", "cfgs")) {
			SerializableGraph graph = cfg.toSerializableGraph();
			String filename = cfg.getDescriptor().getFullSignatureWithParNames() + "_cfg";

			try {
				dump(fileManager, graph, filename);
			} catch (IOException e) {
				LOG.error("Exception while dumping the analysis inputs on {}", cfg.getDescriptor().getFullSignature());
				LOG.error(e);
			}
		}
	}

	/**
	 * Dumps the given {@link SerializableGraph} using the provided
	 * {@link FileManager} and filename.
	 * 
	 * @param fileManager the file manager to use
	 * @param graph       the graph to dump
	 * @param filename    the filename to use
	 * 
	 * @throws IOException if something goes wrong while dumping the graph
	 */
	protected abstract void dump(
			FileManager fileManager,
			SerializableGraph graph,
			String filename)
			throws IOException;

}
