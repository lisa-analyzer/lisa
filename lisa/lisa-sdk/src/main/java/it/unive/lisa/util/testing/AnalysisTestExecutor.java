package it.unive.lisa.util.testing;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.LiSA;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.outputs.compare.ResultComparer;
import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.file.FileManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Utility class to perform tests that run complete analyses, optionally
 * comparing the obtained results with previous ones.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class AnalysisTestExecutor {

	/**
	 * The default root directory where the expected results of the tests are
	 * stored, if any.
	 */
	public static final String DEFAULT_EXPECTED_DIR = "lisa-testcases";

	/**
	 * The default root directory where the tests should be executed.
	 */
	public static final String DEFAULT_ACTUAL_DIR = "test-outputs";

	private final String expectedResultsDir;

	private final String actualResultsDir;

	/**
	 * Creates a new test executor that will use the default directories to
	 * store the expected and actual results of the tests.
	 */
	public AnalysisTestExecutor() {
		this(DEFAULT_EXPECTED_DIR, DEFAULT_ACTUAL_DIR);
	}

	/**
	 * Creates a new test executor that will use the given directories to store
	 * the expected and actual results of the tests.
	 * 
	 * @param expectedResultsDir the root directory where the expected results
	 *                               of the tests are stored, if any; this is
	 *                               the folder where
	 *                               {@link TestConfiguration#testDir} will be
	 *                               searched for.
	 * @param actualResultsDir   the root directory where the actual results of
	 *                               the tests are stored; this is the folder
	 *                               where {@link TestConfiguration#testDir}
	 *                               will be created.
	 */
	public AnalysisTestExecutor(
			String expectedResultsDir,
			String actualResultsDir) {
		Objects.requireNonNull(expectedResultsDir, "Expected results directory cannot be null");
		Objects.requireNonNull(actualResultsDir, "Actual results directory cannot be null");
		this.expectedResultsDir = expectedResultsDir;
		this.actualResultsDir = actualResultsDir;
	}

	/**
	 * Performs a test, running an analysis. The test will fail if:
	 * <ul>
	 * <li>The target program cannot be parsed</li>
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
	 * @param conf the configuration of the test to run (note that the workdir
	 *                 present into the configuration will be ignored, as it
	 *                 will be overwritten by the computed workdir)
	 */
	public void perform(
			TestConfiguration conf) {
		Objects.requireNonNull(conf);
		Objects.requireNonNull(conf.testDir);
		Objects.requireNonNull(conf.programFile);
		Path expectedPath = Paths.get(expectedResultsDir, conf.testDir);
		Path target = Paths.get(expectedPath.toString(), conf.programFile);
		Program program = readProgram(conf, target);
		perform(conf, program);
	}

	/**
	 * Performs a test, running an analysis. The test will fail if:
	 * <ul>
	 * <li>The target program cannot be parsed</li>
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
	 * @param conf    the configuration of the test to run (note that the
	 *                    workdir present into the configuration will be
	 *                    ignored, as it will be overwritten by the computed
	 *                    workdir)
	 * @param program the program to analyze
	 */
	public void perform(
			TestConfiguration conf,
			Program program) {
		String testMethod = getCaller();
		System.out.println("### Testing " + testMethod);
		Objects.requireNonNull(conf);
		Objects.requireNonNull(conf.testDir);

		Path expectedPath = Paths.get(expectedResultsDir, conf.testDir);
		Path actualPath = Paths.get(actualResultsDir, conf.testDir);
		if (conf.testSubDir != null) {
			expectedPath = Paths.get(expectedPath.toString(), conf.testSubDir);
			actualPath = Paths.get(actualPath.toString(), conf.testSubDir);
		}

		setupWorkdir(conf, actualPath);

		conf.jsonOutput = true;

		// save disk space!
		System.clearProperty("lisa.json.indent");

		run(conf, program);

		File expFile = Paths.get(expectedPath.toString(), LiSA.REPORT_NAME).toFile();
		File actFile = Paths.get(actualPath.toString(), LiSA.REPORT_NAME).toFile();

		if (!expFile.exists()) {
			boolean update = "true".equals(System.getProperty("lisa.cron.update")) || conf.forceUpdate;
			if (!update) {
				System.out.println("No '" + LiSA.REPORT_NAME + "' found in the expected folder, exiting...");
				return;
			} else {
				System.out.println("No '" + LiSA.REPORT_NAME + "' found in the expected folder, copying results...");
				copyFiles(expectedPath, actualPath, expFile, actFile);
			}
		}

		compare(conf, expectedPath, actualPath, expFile, actFile, false);

		if (conf.compareWithOptimization && !conf.optimize) {
			System.out.println("### Testing " + testMethod + " with optimization enabled");

			// we parse the program again since the analysis might have
			// finalized it or modified it, and we want to start from scratch
			// TODO: might need to enable this again
			// program = readProgram(target, allMethods);

			conf.optimize = true;
			actualPath = Paths.get(actualPath.toString(), "optimized");
			conf.workdir = actualPath.toFile().toString();
			conf.dumpForcesUnwinding = true;

			run(conf, program);

			actFile = Paths.get(actualPath.toString(), LiSA.REPORT_NAME).toFile();
			compare(conf, expectedPath, actualPath, expFile, actFile, true);
		}
	}

	private void compare(
			TestConfiguration conf,
			Path expectedPath,
			Path actualPath,
			File expFile,
			File actFile,
			boolean optimized) {
		boolean update = "true".equals(System.getProperty("lisa.cron.update")) || conf.forceUpdate;
		try (FileReader l = new FileReader(expFile); FileReader r = new FileReader(actFile)) {
			JsonReport expected = JsonReport.read(l);
			JsonReport actual = JsonReport.read(r);
			Accumulator acc = new Accumulator(expectedPath);
			OptimizedRunDiff opt = new OptimizedRunDiff();
			if (optimized)
				failIf(
						"Optimized results are different",
						!opt.compare(expected, actual, expectedPath.toFile(), actualPath.toFile()));
			else if (update) {
				if (!acc.compare(expected, actual, expectedPath.toFile(), actualPath.toFile())) {
					System.err.println("Results are different, regenerating differences");
					regen(expectedPath, actualPath, expFile, actFile, acc);
				}
			} else
				failIf(
						"Results are different",
						!conf.reportComparer.compare(expected, actual, expectedPath.toFile(), actualPath.toFile()));
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
			throw new TestException("File not found", e);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw new TestException("Unable to compare reports", e);
		}
	}

	private void failIf(
			String message,
			boolean condition) {
		if (condition)
			throw new TestException(message);
	}

	private void copyFiles(
			Path expectedPath,
			Path actualPath,
			File expFile,
			File actFile) {
		try (FileReader r = new FileReader(actFile)) {
			JsonReport actual = JsonReport.read(r);

			createDirectories(expectedPath);

			copy(actFile.toPath(), expFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			System.err.println("- Copied " + LiSA.REPORT_NAME);

			for (String file : actual.getFiles()) {
				Path f = Paths.get(file);
				if (!f.getFileName().toString().equals(LiSA.REPORT_NAME)) {
					Path path = Paths.get(expectedPath.toString(), f.toString());
					createDirectories(path.getParent());
					copy(Paths.get(actualPath.toString(), f.toString()), path);
					System.err.println("- Copied (new) " + f);
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
			throw new TestException("File not found", e);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw new TestException("Unable to compare reports", e);
		}
	}

	private void regen(
			Path expectedPath,
			Path actualPath,
			File expFile,
			File actFile,
			Accumulator acc)
			throws IOException {
		boolean updateReport = acc.changedWarnings
				|| acc.changedConf
				|| acc.changedInfos
				|| !acc.addedFilePaths.isEmpty()
				|| !acc.removedFilePaths.isEmpty()
				|| !acc.changedFileName.isEmpty();
		if (updateReport) {
			copy(actFile.toPath(), expFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			System.err.println("- Updated " + LiSA.REPORT_NAME);
		}
		for (Path f : acc.removedFilePaths) {
			delete(Paths.get(expectedPath.toString(), f.toString()));
			System.err.println("- Deleted " + f);
		}
		for (Path f : acc.addedFilePaths)
			if (!f.getFileName().toString().equals(LiSA.REPORT_NAME)) {
				Path path = Paths.get(expectedPath.toString(), f.toString());
				createDirectories(path.getParent());
				copy(Paths.get(actualPath.toString(), f.toString()), path);
				System.err.println("- Copied (new) " + f);
			}
		for (Path f : acc.changedFileName) {
			Path fresh = Paths.get(expectedPath.toString(), f.toString());
			copy(Paths.get(actualPath.toString(), f.toString()), fresh, StandardCopyOption.REPLACE_EXISTING);
			System.err.println("- Copied (update) " + fresh);
		}
	}

	/**
	 * Reads the program to be analyzed, given the test configuration and target
	 * path. This method is responsible for parsing the program file.
	 * 
	 * @param conf   the test configuration
	 * @param target the target path
	 * 
	 * @return the parsed program
	 */
	public abstract Program readProgram(
			TestConfiguration conf,
			Path target);

	private void run(
			LiSAConfiguration configuration,
			Program program) {
		LiSA lisa = new LiSA(configuration);
		try {
			lisa.run(program);
		} catch (AnalysisException e) {
			throw new TestException("Analysis terminated with errors", e);
		}
	}

	private void setupWorkdir(
			LiSAConfiguration configuration,
			Path actualPath) {
		File workdir = actualPath.toFile();
		try {
			FileManager.forceDeleteFolder(workdir.toString());
		} catch (IOException e) {
			throw new TestException("Cannot delete working directory '" + workdir + "'", e);
		}
		configuration.workdir = workdir.toString();
	}

	private class Accumulator
			extends
			ResultComparer {

		private final Collection<Path> changedFileName = new HashSet<>();

		private final Collection<Path> addedFilePaths = new HashSet<>();

		private final Collection<Path> removedFilePaths = new HashSet<>();

		private boolean changedInfos = false;

		private boolean changedConf = false;

		private boolean changedWarnings = false;

		private final Path exp;

		public Accumulator(
				Path exp) {
			this.exp = exp;
		}

		@Override
		public void report(
				REPORTED_COMPONENT component,
				REPORT_TYPE type,
				Collection<?> reported) {
			switch (type) {
			case ONLY_FIRST:
				switch (component) {
				case FILES:
					reported.forEach(e -> removedFilePaths.add(Paths.get((String) e)));
					break;
				case WARNINGS:
					changedWarnings = true;
					break;
				case INFO:
					changedInfos = true;
					break;
				case CONFIGURATION:
					changedConf = true;
					break;
				default:
					break;
				}
				break;
			case ONLY_SECOND:
				switch (component) {
				case FILES:
					reported.forEach(e -> addedFilePaths.add(Paths.get((String) e)));
					break;
				case WARNINGS:
					changedWarnings = true;
					break;
				case INFO:
					changedInfos = true;
					break;
				case CONFIGURATION:
					changedConf = true;
					break;
				default:
					break;
				}
				break;
			case COMMON:
			default:
				break;

			}
		}

		@Override
		public void fileDiff(
				String first,
				String second,
				String message) {
			Path file = Paths.get(first);
			changedFileName.add(exp.relativize(file));
		}

		@Override
		public void infoDiff(
				String key,
				String first,
				String second) {
			changedInfos = true;
		}

		@Override
		public void configurationDiff(
				String key,
				String first,
				String second) {
			changedConf = true;
		}

	}

	private static class OptimizedRunDiff
			extends
			ResultComparer {

		@Override
		public boolean shouldCompareConfigurations() {
			// optimized runs use the same configuration except for
			// optimize and workdir. Is fine to skip these as they
			// have already been checked in the regular test execution
			return false;
		}

	}

	private String getCaller() {
		StackTraceElement[] trace = Thread.getAllStackTraces().get(Thread.currentThread());
		// 0: java.lang.Thread.dumpThreads()
		// 1: java.lang.Thread.getAllStackTraces()
		// 2: it.unive.lisa.test.AnalysisTest.getCaller()
		// 3: it.unive.lisa.test.AnalysisTest.perform()
		// 4: caller
		for (StackTraceElement e : trace) {
			if (!e.getClassName().equals("java.lang.Thread")
					&& !e.getClassName().equals(AnalysisTestExecutor.class.getName()))
				return e.getClassName() + "::" + e.getMethodName();
		}

		throw new AnalysisException("Unable to find caller test method");
	}

}
