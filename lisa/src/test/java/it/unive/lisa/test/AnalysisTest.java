package it.unive.lisa.test;

import static it.unive.lisa.outputs.compare.JsonReportComparer.compare;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.program.Program;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;

public abstract class AnalysisTest {

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
	 * @param folder     the name of the sub-folder; this is used for searching
	 *                       expected results and as a working directory for
	 *                       executing tests in the test execution folder
	 * @param source     the name of the imp source file to be searched in the
	 *                       given folder
	 * @param inferTypes whether or not type inference should be run
	 * @param dumpTypes  whether or not inferred type information should be
	 *                       dumped
	 * @param state      the abstract state for the analysis; if {@code null},
	 *                       no analysis is executed, otherwise the results are
	 *                       also dumped
	 */
	protected void perform(String folder, String source, boolean inferTypes, boolean dumpTypes,
			AbstractState<?, ?, ?> state) {
		System.out.println("Testing " + getCaller());
		Path expectedPath = Paths.get(EXPECTED_RESULTS_DIR, folder);
		Path actualPath = Paths.get(ACTUAL_RESULTS_DIR, folder);
		Path target = Paths.get(expectedPath.toString(), source);

		LiSA lisa = new LiSA();

		Program program = null;
		try {
			program = IMPFrontend.processFile(target.toString());
		} catch (ParsingException e) {
			e.printStackTrace(System.err);
			fail("Exception while parsing '" + target + "': " + e.getMessage());
		}

		lisa.setProgram(program);
		lisa.setInferTypes(inferTypes);
		lisa.setDumpTypeInference(dumpTypes);
		lisa.setJsonOutput(true);

		if (state != null) {
			lisa.setAbstractState(state);
			lisa.setDumpAnalysis(true);
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
		lisa.setWorkdir(workdir.toString());

		try {
			lisa.run();
		} catch (AnalysisException e) {
			e.printStackTrace(System.err);
			fail("Analysis terminated with errors");
		}

		File expFile = Paths.get(expectedPath.toString(), "report.json").toFile();
		File actFile = Paths.get(actualPath.toString(), "report.json").toFile();
		try {
			JsonReport expected = JsonReport.read(new FileReader(expFile));
			JsonReport actual = JsonReport.read(new FileReader(actFile));
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
