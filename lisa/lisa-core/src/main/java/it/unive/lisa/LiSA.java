package it.unive.lisa;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the central class of the LiSA library. While LiSA's functionalities
 * can be extended by providing additional implementations for each component,
 * code executing LiSA should rely solely on this class to engage the analysis,
 * provide inputs to it and retrieve its results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LiSA {

	private static final Logger LOG = LogManager.getLogger(LiSA.class);

	/**
	 * The collection of warnings that will be filled with the results of all
	 * the executed checks
	 */
	private final Collection<Warning> warnings;

	/**
	 * The {@link FileManager} instance that will be used during analyses
	 */
	private final FileManager fileManager;

	/**
	 * The {@link LiSAConfiguration} containing the settings of the analysis to
	 * run
	 */
	private final LiSAConfiguration conf;

	/**
	 * Builds a new LiSA instance.
	 * 
	 * @param conf the configuration of the analysis to run
	 */
	public LiSA(LiSAConfiguration conf) {
		this.warnings = new ArrayList<>();
		this.conf = conf;
		this.fileManager = new FileManager(conf.workdir);
	}

	/**
	 * Runs LiSA, executing all the checks that have been added.
	 * 
	 * @param programs the programs, each written in a single programming
	 *                     language, to analyze
	 * 
	 * @throws AnalysisException if anything goes wrong during the analysis
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Program... programs) throws AnalysisException {
		printConfig();

		CallGraph callGraph;
		try {
			callGraph = conf.callGraph == null ? getDefaultFor(CallGraph.class) : conf.callGraph;
			if (conf.callGraph == null)
				LOG.warn("No call graph set for this analysis, defaulting to {}", callGraph.getClass().getSimpleName());
		} catch (AnalysisSetupException e) {
			throw new AnalysisExecutionException("Unable to create default call graph", e);
		}

		InterproceduralAnalysis interproc;
		try {
			interproc = conf.interproceduralAnalysis == null ? getDefaultFor(InterproceduralAnalysis.class)
					: conf.interproceduralAnalysis;
			if (conf.interproceduralAnalysis == null)
				LOG.warn("No interprocedural analysis set for this analysis, defaulting to {}",
						interproc.getClass().getSimpleName());
		} catch (AnalysisSetupException e) {
			throw new AnalysisExecutionException("Unable to create default interprocedural analysis", e);
		}

		LiSARunner runner = new LiSARunner(conf, interproc, callGraph, conf.abstractState);
		Application app = new Application(programs);

		try {
			warnings.addAll(TimerLogger.execSupplier(LOG, "Analysis time", () -> runner.run(app, fileManager)));
		} catch (AnalysisExecutionException e) {
			throw new AnalysisException("LiSA has encountered an exception while executing the analysis", e);
		}

		printStats();

		if (conf.jsonOutput) {
			LOG.info("Dumping reported warnings to 'report.json'");
			JsonReport report = new JsonReport(warnings, fileManager.createdFiles());
			try {
				fileManager.mkOutputFile("report.json", writer -> {
					report.dump(writer);
					LOG.info("Report file dumped to report.json");
				});
			} catch (IOException e) {
				LOG.error("Unable to dump report file", e);
			}
		}
	}

	private void printConfig() {
		LOG.info(conf.toString());
	}

	private void printStats() {
		LOG.info("LiSA statistics:");
		LOG.info("  {} warnings generated", warnings.size());
	}

	/**
	 * Yields an unmodifiable view of the warnings that have been generated
	 * during the analysis. Invoking this method before invoking
	 * {@link #run(Program...)} will return an empty collection.
	 * 
	 * @return a view of the generated warnings
	 */
	public Collection<Warning> getWarnings() {
		return Collections.unmodifiableCollection(warnings);
	}
}
