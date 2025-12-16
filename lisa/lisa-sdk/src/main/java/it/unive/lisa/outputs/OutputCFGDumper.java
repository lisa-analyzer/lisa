package it.unive.lisa.outputs;

import it.unive.lisa.LiSAReport;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.util.Collection;
import java.util.function.BiFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An output that dumps each input cfg, including the results produced by the
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public abstract class OutputCFGDumper<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		LiSAOutput {

	private static final Logger LOG = LogManager.getLogger(JSONResults.class);

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
		Collection<CFG> allCFGs = app.getAllCFGs();
		FixpointConfiguration<A, D> fixconf = new FixpointConfiguration<>(report.getConfiguration());
		boolean isOptimized = fixconf.usesOptimizedForwardFixpoint() || fixconf.usesOptimizedBackwardFixpoint();
		BiFunction<CFG, Statement, SerializableValue> labeler;
		if (isOptimized && report.getConfiguration().dumpForcesUnwinding)
			labeler = (
					cfg,
					st) -> ((OptimizedAnalyzedCFG<A, D>) cfg)
							.getUnwindedAnalysisStateAfter(st, fixconf)
							.representation()
							.toSerializableValue();
		else
			labeler = (
					cfg,
					st) -> ((AnalyzedCFG<A>) cfg)
							.getAnalysisStateAfter(st)
							.representation()
							.toSerializableValue();

		for (CFG cfg : IterationLogger.iterate(LOG, allCFGs, "Dumping analysis results", "cfgs"))
			for (AnalyzedCFG<A> result : ctool.getResultOf(cfg)) {
				SerializableGraph graph = result.toSerializableGraph(labeler);
				String filename = cfg.getDescriptor().getFullSignatureWithParNames();
				if (!result.getId().isStartingId())
					filename += "_" + result.getId().hashCode();

				try {
					dump(fileManager, graph, filename);
				} catch (IOException e) {
					LOG.error(
							"Exception while dumping the analysis results on {}",
							cfg.getDescriptor().getFullSignature());
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
