package it.unive.lisa.frontend;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.program.Program;
import java.io.IOException;

/**
 * SPI implemented by language-specific frontends that translate source code
 * into a LiSA {@link Program}. The contract is intentionally minimal: a
 * frontend's only responsibility is to produce the IR. Concerns specific to
 * particular analyses (network handler resolution, alias-set construction,
 * etc.) are bridged inside those analyses' modules via per-frontend adapter
 * classes, not through this interface.
 */
public interface LiSAFrontend {

	/**
	 * Parses the source program associated with this frontend instance and
	 * returns the resulting LiSA {@link Program} ready for analysis.
	 *
	 * @return the produced program
	 *
	 * @throws IOException             if the source cannot be read
	 * @throws AnalysisSetupException  if the frontend cannot build a valid
	 *                                     program from the source
	 */
	Program toLiSAProgram() throws IOException, AnalysisSetupException;
}
