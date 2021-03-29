package it.unive.lisa;

import static it.unive.lisa.outputs.compare.JsonReportComparer.compare;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.program.Program;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;

public abstract class AnalysisTestExecutor {

	protected static final String EXPECTED_RESULTS_DIR = "imp-testcases";
	protected static final String ACTUAL_RESULTS_DIR = "test-outputs";

	/**
	 * Performs a test, running an analysis. The test will fail if:
	 * <ul>
	 * <li>The imp file cannot be parsed (i.e. a {@link ParsingException} is
	 * thrown)</li>
	 * <li>The previous working directory using for the test execution cannot be
	 * deleted</li>
	 * <li>The analysis run terminates with an {@link AnalysisException}</li>
	 * <li>One of the json reports (either the one generated during the test
	 * execution or the one used as baseline) cannot be found or cannot be
	 * opened</li>
	 * <li>The two json reports are different</li>
	 * <li>The external files mentioned in the reports are different</li>
	 * </ul>
	 * 
	 * @param folder        the name of the sub-folder; this is used for
	 *                          searching expected results and as a working
	 *                          directory for executing tests in the test
	 *                          execution folder
	 * @param source        the name of the imp source file to be searched in
	 *                          the given folder
	 * @param configuration the configuration of the analysis to run (note that
	 *                          the workdir present into the configuration will
	 *                          be ignored, as it will be overwritten by the
	 *                          computed workdir)
	 */
	protected void perform(String folder, String source, LiSAConfiguration configuration) {
		System.out.println("Testing " + getCaller());
		Path expectedPath = Paths.get(EXPECTED_RESULTS_DIR, folder);
		Path actualPath = Paths.get(ACTUAL_RESULTS_DIR, folder);
		Path target = Paths.get(expectedPath.toString(), source);

		Program program = null;
		try {
			program = IMPFrontend.processFile(target.toString());
		} catch (ParsingException e) {
			e.printStackTrace(System.err);
			fail("Exception while parsing '" + target + "': " + e.getMessage());
		}

		File workdir = actualPath.toFile();
		if (workdir.exists()) {
			System.out.println(workdir + " already exists: deleting...");
			try {
				FileUtils.forceDelete(workdir);
			} catch (IOException e) {
				e.printStackTrace(System.err);
				fail("Cannot delete working directory '" + workdir + "': " + e.getMessage());
			}
		}
		configuration.setWorkdir(workdir.toString());

		configuration.setJsonOutput(true);

		LiSA lisa = new LiSA(configuration);
		try {
			lisa.run(program);
		} catch (AnalysisException e) {
			e.printStackTrace(System.err);
			fail("Analysis terminated with errors");
		}

		File expFile = Paths.get(expectedPath.toString(), "report.json").toFile();
		File actFile = Paths.get(actualPath.toString(), "report.json").toFile();
		try (FileReader l = new FileReader(expFile); FileReader r = new FileReader(actFile)) {
			JsonReport expected = JsonReport.read(l);
			JsonReport actual = JsonReport.read(r);
			assertTrue("Results are different", compare(expected, actual, expectedPath.toFile(), actualPath.toFile()));
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
			fail("Unable to find report file");
		} catch (IOException e) {
			e.printStackTrace(System.err);
			fail("Unable to compare reports");
		}

	}

	private String getCaller() {
		StackTraceElement[] trace = Thread.getAllStackTraces().get(Thread.currentThread());
		// 0: java.lang.Thread.dumpThreads()
		// 1: java.lang.Thread.getAllStackTraces()
		// 2: it.unive.lisa.test.AnalysisTest.getCaller()
		// 3: it.unive.lisa.test.AnalysisTest.perform()
		// 4: caller
		return trace[4].getClassName() + "::" + trace[4].getMethodName();
	}
}
