package it.unive.lisa;

import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.logging.Log4jConfig;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.outputs.LiSAOutput;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
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

	static {
		// ensure that some logging configuration is in place
		// if not, we set a default configuration
		if (!Log4jConfig.isLog4jConfigured())
			Log4jConfig.initializeLogging();
	}

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
				conf.analysis == null ? null : new Analysis(conf.analysis, conf.shouldSmashError));
		Application app = new Application(programs);
		ReportingTool tool;

		try {
			tool = TimerLogger.execSupplier(LOG, "Analysis time", () -> runner.run(app));
		} catch (AnalysisExecutionException e) {
			throw new AnalysisException("LiSA has encountered an exception while executing the analysis", e);
		}

		LiSARunInfo stats = new LiSARunInfo(
				tool.getWarnings(),
				tool.getNotices(),
				fileManager.createdFiles(),
				app,
				start,
				new DateTime());
		LOG.info("LiSA statistics:\n" + stats);

		LiSAReport report = new LiSAReport(
				conf,
				stats,
				tool.getWarnings(),
				tool.getNotices(),
				fileManager.createdFiles());
		if (infoProvider != null)
			infoProvider.accept(report);

		Collection<LiSAOutput> reportOutputs = new HashSet<>();
		for (LiSAOutput output : conf.outputs)
			if (output.isReportOutput())
				reportOutputs.add(output);
			else
				try {
					output.dump(app, report, tool, fileManager);
				} catch (IOException e) {
					LOG.error("Unable to dump output using " + output.getClass().getSimpleName(), e);
				}

		try {
			fileManager.generateSupportFiles();
		} catch (IOException e) {
			LOG.error("Exception while generating supporting files for visualization");
			LOG.error(e);
		}

		// we regenerate stats and report to have up-to-date information
		stats = new LiSARunInfo(
				tool.getWarnings(),
				tool.getNotices(),
				fileManager.createdFiles(),
				app,
				start,
				new DateTime());
		report = new LiSAReport(
				conf,
				stats,
				tool.getWarnings(),
				tool.getNotices(),
				fileManager.createdFiles());
		if (infoProvider != null)
			infoProvider.accept(report);

		// we dump these last to have up-to-date information in the report
		for (LiSAOutput output : reportOutputs)
			try {
				output.dump(app, report, tool, fileManager);
			} catch (IOException e) {
				LOG.error("Unable to dump output using " + output.getClass().getSimpleName(), e);
			}

		return report;
	}

}
