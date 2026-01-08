package it.unive.lisa.outputs;

import java.io.IOException;

import it.unive.lisa.CheckTool;
import it.unive.lisa.LiSAReport;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.program.Application;
import it.unive.lisa.util.file.FileManager;

/**
 * An output that can dump analysis results produced by LiSA.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface LiSAOutput {

	/**
	 * Uses the {@code fileManager} to create the output files and dump
	 * instances of this output to it, using the remaining parameters to
	 * generate the contents.
	 * 
	 * @param app         the application that has been analyzed
	 * @param report      the report produced by the analysis (note that the
	 *                        report is incomplete, as information on outputs is
	 *                        not available yet -- eg, number of generated
	 *                        files)
	 * @param tool        the check tool used during the analysis (if a semantic
	 *                        analysis has been performed, it will be an
	 *                        instance of {@link CheckToolWithAnalysisResults})
	 * @param fileManager the file manager to use for creating and writing the
	 *                        output file
	 * 
	 * @throws IOException if an I/O error occurs while writing
	 */
	void dump(
			Application app,
			LiSAReport report,
			CheckTool tool,
			FileManager fileManager)
			throws IOException;

	/**
	 * Yields whether or not this output dumps the analysis report, and should
	 * thus be postponed after all other outputs to have up-to-date information.
	 * 
	 * @return {@code true} if this output dumps the analysis report,
	 *             {@code false} otherwise
	 */
	default boolean isReportOutput() {
		return false;
	}
}
