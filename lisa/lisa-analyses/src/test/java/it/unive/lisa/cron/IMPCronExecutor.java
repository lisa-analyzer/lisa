package it.unive.lisa.cron;

import static org.junit.Assert.fail;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.testing.AnalysisTestExecutor;
import it.unive.lisa.util.testing.TestConfiguration;
import java.nio.file.Path;

public class IMPCronExecutor extends AnalysisTestExecutor {

	public IMPCronExecutor() {
		super("imp-testcases", AnalysisTestExecutor.DEFAULT_ACTUAL_DIR);
	}

	@Override
	public Program readProgram(
			TestConfiguration conf,
			Path target) {
		Program program = null;
		try {
			program = IMPFrontend.processFile(
				target.toString(),
				conf instanceof CronConfiguration ? !((CronConfiguration) conf).allMethods : true);
		} catch (ParsingException e) {
			e.printStackTrace(System.err);
			fail("Exception while parsing '" + target + "': " + e.getMessage());
		}
		return program;
	}

}
