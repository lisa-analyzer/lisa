package it.unive.lisa.cron;

import static org.junit.Assert.fail;

import java.nio.file.Path;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.testing.AnalysisTestExecutor;
import it.unive.lisa.util.testing.TestConfiguration;

public class IMPCronExecutor extends AnalysisTestExecutor {

    @Override
    public Program readProgram(TestConfiguration conf, Path target) {
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
