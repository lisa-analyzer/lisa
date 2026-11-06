package it.unive.lisa.outputs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.LiSAReport;
import it.unive.lisa.ReportingTool;
import it.unive.lisa.program.Application;
import it.unive.lisa.util.file.FileManager;
import org.junit.jupiter.api.Test;

public class LiSAOutputTest {

	private static class PlainOutput
			implements
			LiSAOutput {

		@Override
		public void dump(
				Application app,
				LiSAReport report,
				ReportingTool tool,
				FileManager fileManager) {
		}
	}

	@Test
	public void isReportOutputDefaultsToFalse() {
		assertFalse(new PlainOutput().isReportOutput());
	}

	@Test
	public void jsonReportDumperOverridesIsReportOutputToTrue() {
		// JSONReportDumper is the one output that must be postponed until
		// the rest of the report has been finalized, per LiSA#run's
		// two-phase dumping logic
		assertTrue(new JSONReportDumper().isReportOutput());
	}

}
