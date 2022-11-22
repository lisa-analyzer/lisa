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
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

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
		this.conf = conf;
		this.fileManager = new FileManager(conf.workdir);
	}

	/**
	 * Runs LiSA, executing all the checks that have been added.
	 * 
	 * @param programs the programs, each written in a single programming
	 *                     language, to analyze
	 * 
	 * @return the {@link LiSAReport} containing the details of the analysis
	 * 
	 * @throws AnalysisException if anything goes wrong during the analysis
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public LiSAReport run(Program... programs) throws AnalysisException {
		LOG.info(conf.toString());

		DateTime start = new DateTime();

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
		Collection<Warning> warnings;

		try {
			warnings = TimerLogger.execSupplier(LOG, "Analysis time", () -> runner.run(app, fileManager));
		} catch (AnalysisExecutionException e) {
			throw new AnalysisException("LiSA has encountered an exception while executing the analysis", e);
		}

		LiSARunInfo stats = new LiSARunInfo(warnings, fileManager.createdFiles(), app, start, new DateTime());
		LOG.info("LiSA statistics:\n" + stats);

		LiSAReport report = new LiSAReport(conf, stats, warnings, fileManager.createdFiles());
		if (conf.jsonOutput) {
			LOG.info("Dumping analysis report to 'report.json'");
			try {
				fileManager.mkOutputFile("report.json", writer -> {
					JsonReport json = new JsonReport(report);
					json.dump(writer);
					LOG.info("Report file dumped to report.json");
				});
			} catch (IOException e) {
				LOG.error("Unable to dump report file", e);
			}
		}

		return report;
	}
}
