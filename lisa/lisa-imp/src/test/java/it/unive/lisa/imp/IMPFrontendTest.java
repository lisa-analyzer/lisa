package it.unive.lisa.imp;

import static org.junit.Assert.fail;

import it.unive.lisa.outputs.JSONInputs;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.testing.AnalysisTestExecutor;
import it.unive.lisa.util.testing.TestConfiguration;
import java.nio.file.Path;
import org.junit.Test;

public class IMPFrontendTest
		extends
		AnalysisTestExecutor {

	public IMPFrontendTest() {
		super("imp-testcases", AnalysisTestExecutor.DEFAULT_ACTUAL_DIR);
	}

	@Test
	public void testExampleProgram() {
		TestConfiguration conf = new TestConfiguration();
		conf.testDir = "basic";
		conf.programFile = "example.imp";
		conf.outputs.add(new JSONInputs());
		perform(conf);
	}

	@Test
	public void testErrors() {
		TestConfiguration conf = new TestConfiguration();
		conf.testDir = "errors";
		conf.programFile = "try-catch.imp";
		conf.outputs.add(new JSONInputs());
		perform(conf);
	}

	@Test
	public void testErrorsWithEmptyBodies() {
		TestConfiguration conf = new TestConfiguration();
		conf.testDir = "errors-empty";
		conf.programFile = "try-catch-empty.imp";
		conf.outputs.add(new JSONInputs());
		perform(conf);
	}

	@Test
	public void testErrorsWithReturns() {
		TestConfiguration conf = new TestConfiguration();
		conf.testDir = "errors-returns";
		conf.programFile = "try-catch-returns.imp";
		conf.outputs.add(new JSONInputs());
		perform(conf);
	}

	@Test
	public void testBreakContinue() {
		TestConfiguration conf = new TestConfiguration();
		conf.testDir = "break-continue";
		conf.programFile = "break-continue.imp";
		conf.outputs.add(new JSONInputs());
		perform(conf);
	}

	@Test
	public void testErrorsWithBreakAndContinue() {
		TestConfiguration conf = new TestConfiguration();
		conf.testDir = "errors-break-continue";
		conf.programFile = "errors-break-continue.imp";
		conf.outputs.add(new JSONInputs());
		perform(conf);
	}

	@Test
	public void testNestedErrors() {
		TestConfiguration conf = new TestConfiguration();
		conf.testDir = "nested-errors";
		conf.programFile = "nested-errors.imp";
		conf.outputs.add(new JSONInputs());
		perform(conf);
	}

	@Test
	public void testDeadcode() {
		// the true expected results of this test are the log
		// messages reporting unreachable nodes
		TestConfiguration conf = new TestConfiguration();
		conf.testDir = "deadcode";
		conf.programFile = "deadcode.imp";
		conf.outputs.add(new JSONInputs());
		perform(conf);
	}

	@Override
	public Program readProgram(
			TestConfiguration conf,
			Path target) {
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
