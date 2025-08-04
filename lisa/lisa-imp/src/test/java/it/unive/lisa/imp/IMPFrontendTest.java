package it.unive.lisa.imp;

import static org.junit.Assert.fail;

import java.nio.file.Path;

import org.junit.Test;

import it.unive.lisa.program.Program;
import it.unive.lisa.util.testing.AnalysisTestExecutor;
import it.unive.lisa.util.testing.TestConfiguration;

public class IMPFrontendTest extends AnalysisTestExecutor {

	@Test
	public void testExampleProgram() {
		try {
			IMPFrontend.processFile("example.imp", false);
		} catch (ParsingException e) {
			fail("Processing the example file thrown an exception: " + e);
		}
	}

	@Test
	public void testErrors() {
		TestConfiguration conf = new TestConfiguration();
		conf.testDir = "errors";
		conf.programFile = "try-catch.imp";
		conf.jsonOutput = true;
		conf.serializeInputs = true;
		perform(conf);
	}

	@Override
	public Program readProgram(TestConfiguration conf, Path target) {
		Program program = null;
		try {
			program = IMPFrontend.processFile(target.toString(), false);
		} catch (ParsingException e) {
			e.printStackTrace(System.err);
			fail("Exception while parsing '" + target + "': " + e.getMessage());
		}
		return program;
	}

}
