package it.unive.lisa;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.delete;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.outputs.compare.JsonReportComparer.BaseDiffAlgorithm;
import it.unive.lisa.outputs.compare.JsonReportComparer.DiffAlgorithm;
import it.unive.lisa.outputs.compare.JsonReportComparer.REPORTED_COMPONENT;
import it.unive.lisa.outputs.compare.JsonReportComparer.REPORT_TYPE;
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
	 * @param conf the configuration of the test to run (note that the workdir
	 *                 present into the configuration will be ignored, as it
	 *                 will be overwritten by the computed workdir)
	 */
	public void perform(CronConfiguration conf) {
		String testMethod = getCaller();
		System.out.println("### Testing " + testMethod);
		Objects.requireNonNull(conf);
		Objects.requireNonNull(conf.testDir);
		Objects.requireNonNull(conf.programFile);

		Path expectedPath = Paths.get(EXPECTED_RESULTS_DIR, conf.testDir);
		Path actualPath = Paths.get(ACTUAL_RESULTS_DIR, conf.testDir);
		Path target = Paths.get(expectedPath.toString(), conf.programFile);
		if (conf.testSubDir != null) {
			expectedPath = Paths.get(expectedPath.toString(), conf.testSubDir);
			actualPath = Paths.get(actualPath.toString(), conf.testSubDir);
		}

		Program program = readProgram(target);

		setupWorkdir(conf, actualPath);

		conf.jsonOutput = true;
		conf.optimize = false;

		// save disk space!
		System.clearProperty("lisa.json.indent");

		run(conf, program);

		File expFile = Paths.get(expectedPath.toString(), LiSA.REPORT_NAME).toFile();
		File actFile = Paths.get(actualPath.toString(), LiSA.REPORT_NAME).toFile();

		if (!expFile.exists()) {
			// no baseline defined, we end the test here
			System.out.println("No '" + LiSA.REPORT_NAME + "' found in the expected folder, exiting...");
			return;
		}

		compare(conf, expectedPath, actualPath, expFile, actFile, false);

		if (conf.compareWithOptimization) {
			System.out.println("### Testing " + testMethod + " with optimization enabled");

			// we parse the program again since the analysis might have
			// finalized it or modified it, and we want to start from scratch
			program = readProgram(target);

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
			CronConfiguration conf,
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
			if (optimized)
				assertTrue("Optimized results are different",
						JsonReportComparer.compare(
								expected,
								actual,
								expectedPath.toFile(),
								actualPath.toFile(),
								new OptimizedRunDiff()));
			else if (!update)
				assertTrue("Results are different",
						JsonReportComparer.compare(
								expected,
								actual,
								expectedPath.toFile(),
								actualPath.toFile()));
			else if (!JsonReportComparer.compare(
					expected,
					actual,
					expectedPath.toFile(),
					actualPath.toFile(),
					acc)) {
				System.err.println("Results are different, regenerating differences");
				regen(expectedPath, actualPath, expFile, actFile, acc);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
			fail("File not found: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace(System.err);
			fail("Unable to compare reports: " + e.getMessage());
		}
	}

	private void regen(Path expectedPath, Path actualPath, File expFile, File actFile, Accumulator acc)
			throws IOException {
		boolean updateReport = acc.changedWarnings || acc.changedConf || acc.changedInfos
				|| !acc.addedFilePaths.isEmpty() || !acc.removedFilePaths.isEmpty()
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
				copy(Paths.get(actualPath.toString(), f.toString()),
						Paths.get(expectedPath.toString(), f.toString()));
				System.err.println("- Copied (new) " + f);
			}
		for (Path f : acc.changedFileName) {
			Path fresh = Paths.get(expectedPath.toString(), f.toString());
			copy(Paths.get(actualPath.toString(), f.toString()),
					fresh,
					StandardCopyOption.REPLACE_EXISTING);
			System.err.println("- Copied (update) " + fresh);
		}
	}

	private Program readProgram(Path target) {
		Program program = null;
		try {
			program = IMPFrontend.processFile(target.toString(), true);
		} catch (ParsingException e) {
			e.printStackTrace(System.err);
			fail("Exception while parsing '" + target + "': " + e.getMessage());
		}
		return program;
	}

	private void run(LiSAConfiguration configuration, Program program) {
		LiSA lisa = new LiSA(configuration);
		try {
			lisa.run(program);
		} catch (AnalysisException e) {
			e.printStackTrace(System.err);
			fail("Analysis terminated with errors");
		}
	}

	private void setupWorkdir(LiSAConfiguration configuration, Path actualPath) {
		File workdir = actualPath.toFile();
		try {
			FileManager.forceDeleteFolder(workdir.toString());
		} catch (IOException e) {
			e.printStackTrace(System.err);
			fail("Cannot delete working directory '" + workdir + "': " + e.getMessage());
		}
		configuration.workdir = workdir.toString();
	}

	private class Accumulator implements DiffAlgorithm {

		private final Collection<Path> changedFileName = new HashSet<>();
		private final Collection<Path> addedFilePaths = new HashSet<>();
		private final Collection<Path> removedFilePaths = new HashSet<>();
		private boolean changedInfos = false;
		private boolean changedConf = false;
		private boolean changedWarnings = false;

		private final Path exp;

		public Accumulator(Path exp) {
			this.exp = exp;
		}

		@Override
		public void report(REPORTED_COMPONENT component, REPORT_TYPE type, Collection<?> reported) {
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
		public void fileDiff(String first, String second, String message) {
			Path file = Paths.get(first);
			changedFileName.add(exp.relativize(file));
		}

		@Override
		public void infoDiff(String key, String first, String second) {
			changedInfos = true;
		}

		@Override
		public void configurationDiff(String key, String first, String second) {
			changedConf = true;
		}
	}

	private static class OptimizedRunDiff extends BaseDiffAlgorithm {
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
		return trace[4].getClassName() + "::" + trace[4].getMethodName();
	}
}
