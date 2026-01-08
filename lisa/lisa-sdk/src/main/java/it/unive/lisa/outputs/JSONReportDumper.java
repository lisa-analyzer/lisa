package it.unive.lisa.outputs;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.CheckTool;
import it.unive.lisa.LiSAReport;
import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.program.Application;
import it.unive.lisa.util.file.FileManager;

/**
 * An output that dumps the analysis report in JSON format to
 * {@value #REPORT_NAME}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JSONReportDumper
		implements
		LiSAOutput {

	private static final Logger LOG = LogManager.getLogger(JSONReportDumper.class);

	/**
	 * The name of the json report that LiSA can optionally dump.
	 */
	public static final String REPORT_NAME = "report.json";

	@Override
	public void dump(
			Application app,
			LiSAReport report,
			CheckTool tool,
			FileManager fileManager)
			throws IOException {
		LOG.info("Dumping analysis report to '" + REPORT_NAME + "'");
		fileManager.mkOutputFile(REPORT_NAME, writer -> {
			JsonReport json = new JsonReport(report);
			json.dump(writer);
			LOG.info("Report file dumped to '" + REPORT_NAME + "'");
		});
	}

	@Override
	public boolean isReportOutput() {
		return true;
	}
}
