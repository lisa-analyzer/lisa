package it.unive.lisa.outputs;

import it.unive.lisa.LiSAReport;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.program.Application;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An output that dumps the {@link CallGraph} produced by the analysis, if any,
 * in dot format.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class DotCallGraph<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		LiSAOutput {

	private static final Logger LOG = LogManager.getLogger(DotCallGraph.class);

	@Override
	@SuppressWarnings("unchecked")
	public void dump(
			Application app,
			LiSAReport report,
			CheckTool tool,
			FileManager fileManager)
			throws IOException {
		if (!(tool instanceof CheckToolWithAnalysisResults)) {
			LOG.warn("No analysis results available, skipping execution of output " + this.getClass().getSimpleName());
			return;
		}

		CheckToolWithAnalysisResults<A, D> ctool = (CheckToolWithAnalysisResults<A, D>) tool;
		CallGraph callGraph = ctool.getCallGraph();
		if (callGraph == null) {
			LOG.warn("No call graph produced by the analysis, skipping execution of output "
					+ this.getClass().getSimpleName());
			return;
		}

		SerializableGraph graph = callGraph.toSerializableGraph();
		String filename = "callgraph";
		try {
			fileManager.mkDotFile(filename, writer -> graph.toDot().dump(writer));
		} catch (IOException e) {
			LOG.error("Exception while dumping the call graph");
			LOG.error(e);
		}
	}

}
