package it.unive.lisa;

import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;
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
	 * The name of the json report that LiSA can optionally dump.
	 */
	public static final String REPORT_NAME = "report.json";

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
	public LiSA(
			LiSAConfiguration conf) {
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
	public LiSAReport run(
			Program... programs)
			throws AnalysisException {
		return run(null, programs);
	}

	/**
	 * Runs LiSA, executing all the checks that have been added.
	 * 
	 * @param infoProvider a callback that is invoked at the end of the
	 *                         analysis, right before the report is dumped to
	 *                         json. This can be used to provide additional
	 *                         information to the report that will be included
	 *                         in the dump
	 * @param programs     the programs, each written in a single programming
	 *                         language, to analyze
	 * 
	 * @return the {@link LiSAReport} containing the details of the analysis
	 * 
	 * @throws AnalysisException if anything goes wrong during the analysis
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public LiSAReport run(
			Consumer<LiSAReport> infoProvider,
			Program... programs)
			throws AnalysisException {
		LOG.info(conf.toString());

		DateTime start = new DateTime();
		LiSARunner runner = new LiSARunner(
				conf,
				fileManager,
				conf.interproceduralAnalysis,
				conf.callGraph,
				conf.abstractState);
		Application app = new Application(programs);
		Collection<Warning> warnings;

		try {
			warnings = TimerLogger.execSupplier(LOG, "Analysis time", () -> runner.run(app));
		} catch (AnalysisExecutionException e) {
			throw new AnalysisException("LiSA has encountered an exception while executing the analysis", e);
		}

		LiSARunInfo stats = new LiSARunInfo(warnings, fileManager.createdFiles(), app, start, new DateTime());
		LOG.info("LiSA statistics:\n" + stats);

		LiSAReport report = new LiSAReport(conf, stats, warnings, fileManager.createdFiles());
		if (infoProvider != null)
			infoProvider.accept(report);
		if (conf.jsonOutput) {
			LOG.info("Dumping analysis report to '" + REPORT_NAME + "'");
			try {
				fileManager.mkOutputFile(REPORT_NAME, writer -> {
					JsonReport json = new JsonReport(report);
					json.dump(writer);
					LOG.info("Report file dumped to '" + REPORT_NAME + "'");
				});
			} catch (IOException e) {
				LOG.error("Unable to dump report file", e);
			}
		}

		return report;
	}
}
