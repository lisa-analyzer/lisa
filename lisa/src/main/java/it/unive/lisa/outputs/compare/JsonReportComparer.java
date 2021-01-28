package it.unive.lisa.outputs.compare;

import it.unive.lisa.outputs.DotGraph;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.JsonReport.JsonWarning;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A class providing capabilities for finding differences between two
 * {@link JsonReport}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JsonReportComparer {

	/**
	 * An enumeration defining the different type of reports that can be issued.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public enum REPORT_TYPE {
		/**
		 * Indicates that the objects being reported are present in both
		 * reports.
		 */
		COMMON,

		/**
		 * Indicates that the objects being reported are present only in the
		 * first report.
		 */
		ONLY_FIRST,

		/**
		 * Indicates that the objects being reported are present only in the
		 * second report.
		 */
		ONLY_SECOND;
	}

	/**
	 * An enumeration defining the different components of a {@link JsonReport},
	 * in order to distinguish which one of them caused a difference being
	 * reported.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public enum REPORTED_COMPONENT {
		/**
		 * Indicates that the difference was found in the collection of
		 * generated warnings.
		 */
		WARNINGS,

		/**
		 * Indicates that the difference was found in the collection of
		 * generated files.
		 */
		FILES;
	}

	/**
	 * An object that provides callbacks for reporting differences when
	 * comparing {@link JsonReport}s.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public interface DiffReporter {
		/**
		 * Reports a difference in one of the components of the reports.
		 * 
		 * @param component the {@link REPORTED_COMPONENT} where the difference
		 *                      was found
		 * @param type      the {@link REPORT_TYPE} indicating what is the
		 *                      difference being reported
		 * @param reported  the collection of elements that are part of the
		 *                      report
		 */
		void report(REPORTED_COMPONENT component, REPORT_TYPE type, Collection<?> reported);

		/**
		 * Callback invoked by a {@link JsonReportComparer} whenever files from
		 * different reports but with matching names have different content.
		 * 
		 * @param first   the complete (i.e. path + file name) pathname of the
		 *                    first file
		 * @param second  the complete (i.e. path + file name) pathname of the
		 *                    second file
		 * @param message the message reporting the difference
		 */
		void fileDiff(String first, String second, String message);
	}

	/**
	 * Compares the two reports. The {@link DiffReporter} used during the
	 * comparison will dump the differences to {@link System#err}.
	 * 
	 * @param first          the first report
	 * @param second         the second report
	 * @param firstFileRoot  the root folder for the resolution of the file
	 *                           names in the first report (this should be
	 *                           either the workdir of that analysis or the
	 *                           folder where {@code first} lies)
	 * @param secondFileRoot the root folder for the resolution of the file
	 *                           names in the second report (this should be
	 *                           either the workdir of that analysis or the
	 *                           folder where {@code second} lies)
	 * 
	 * @return {@code true} if and only the two reports are equal
	 * 
	 * @throws IOException if errors happen while opening or reading the files
	 *                         contained in the reports
	 */
	public static boolean compare(JsonReport first, JsonReport second, File firstFileRoot, File secondFileRoot)
			throws IOException {
		return compare(first, second, firstFileRoot, secondFileRoot, new BaseDiffReporter());
	}

	/**
	 * Compares the two reports.
	 * 
	 * @param first          the first report
	 * @param second         the second report
	 * @param firstFileRoot  the root folder for the resolution of the file
	 *                           names in the first report (this should be
	 *                           either the workdir of that analysis or the
	 *                           folder where {@code first} lies)
	 * @param secondFileRoot the root folder for the resolution of the file
	 *                           names in the second report (this should be
	 *                           either the workdir of that analysis or the
	 *                           folder where {@code second} lies)
	 * @param reporter       the {@link DiffReporter} that will be used for
	 *                           dumping the differences found in the two
	 *                           reports
	 * 
	 * @return {@code true} if and only the two reports are equal
	 * 
	 * @throws IOException if errors happen while opening or reading the files
	 *                         contained in the reports
	 */
	public static boolean compare(JsonReport first, JsonReport second, File firstFileRoot, File secondFileRoot,
			DiffReporter reporter) throws IOException {
		CollectionsDiffBuilder<JsonWarning> warnings = new CollectionsDiffBuilder<>(JsonWarning.class,
				first.getWarnings(), second.getWarnings());
		warnings.compute(JsonWarning::compareTo);

		if (!warnings.getCommons().isEmpty())
			reporter.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.COMMON, warnings.getCommons());
		if (!warnings.getOnlyFirst().isEmpty())
			reporter.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_FIRST, warnings.getOnlyFirst());
		if (!warnings.getOnlySecond().isEmpty())
			reporter.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_SECOND, warnings.getOnlySecond());

		CollectionsDiffBuilder<String> files = new CollectionsDiffBuilder<>(String.class, first.getFiles(),
				second.getFiles());
		files.compute(String::compareTo);

		if (!files.getCommons().isEmpty())
			reporter.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.COMMON, files.getCommons());
		if (!files.getOnlyFirst().isEmpty())
			reporter.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_FIRST, files.getOnlyFirst());
		if (!files.getOnlySecond().isEmpty())
			reporter.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_SECOND, files.getOnlySecond());

		if (!warnings.sameContent() || !files.sameContent())
			return false;

		boolean diffFound = false;
		for (Pair<String, String> pair : files.getCommons()) {
			File left = new File(firstFileRoot, pair.getLeft());
			File right = new File(secondFileRoot, pair.getRight());

			if (!left.exists())
				throw new FileNotFoundException(
						pair.getLeft() + " declared as output in the first report does not exist");
			if (!right.exists())
				throw new FileNotFoundException(
						pair.getRight() + " declared as output in the second report does not exist");

			if (left.getName().endsWith(".dot"))
				if (!matchDotGraphs(left, right)) {
					reporter.fileDiff(left.toString(), right.toString(), "Graphs are different");
					diffFound = true;
				}
		}

		return !diffFound;
	}

	private static boolean matchDotGraphs(File left, File right) throws IOException {
		DotGraph<Statement, Edge, CFG> lDot = DotGraph.readDot(new FileReader(left));
		DotGraph<Statement, Edge, CFG> rDot = DotGraph.readDot(new FileReader(right));
		return lDot.equals(rDot);
	}

	private static class BaseDiffReporter implements DiffReporter {

		@Override
		public void report(REPORTED_COMPONENT component, REPORT_TYPE type, Collection<?> reported) {
			if (type == REPORT_TYPE.COMMON)
				return;

			boolean isFirst = type == REPORT_TYPE.ONLY_FIRST;

			switch (component) {
			case FILES:
				if (isFirst)
					System.err.println("Files only in the first report:");
				else
					System.err.println("Files only in the second report:");
				break;
			case WARNINGS:
				if (isFirst)
					System.err.println("Warnings only in the first report:");
				else
					System.err.println("Warnings only in the second report:");
				break;
			default:
				break;
			}

			for (Object o : reported)
				System.err.println("\t" + o);
		}

		@Override
		public void fileDiff(String first, String second, String message) {
			System.err.println("['" + first + "', '" + second + "'] " + message);
		}
	}
}
