package it.unive.lisa.outputs.compare;

import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.outputs.json.JsonReport.JsonWarning;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableNodeDescription;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class providing capabilities for finding differences between two
 * {@link JsonReport}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JsonReportComparer {

	private static final Logger LOG = LogManager.getLogger(JsonReportComparer.class);

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
			if (!left.exists())
				throw new FileNotFoundException(
						pair.getLeft() + " declared as output in the first report does not exist");

			File right = new File(secondFileRoot, pair.getRight());
			if (!right.exists())
				throw new FileNotFoundException(
						pair.getRight() + " declared as output in the second report does not exist");

			String ext = FilenameUtils.getExtension(left.getName());
			if (ext.endsWith("json"))
				diffFound |= matchJsonGraphs(reporter, left, right);
			else if (ext.equals("dot")
					|| ext.equals("graphml")
					|| ext.equals("html")
					|| ext.equals("js"))
				LOG.info("Skipping comparison of visualization-only files: " + left.toString() + " and "
						+ right.toString());
			else
				throw new UnsupportedOperationException("Cannot compare files with extension " + ext);
		}

		return !diffFound;
	}

	private static boolean matchJsonGraphs(DiffReporter reporter, File left, File right)
			throws IOException, FileNotFoundException {
		boolean diffFound = false;

		try (Reader l = new InputStreamReader(new FileInputStream(left), StandardCharsets.UTF_8);
				Reader r = new InputStreamReader(new FileInputStream(right), StandardCharsets.UTF_8)) {
			SerializableGraph leftGraph = SerializableGraph.readGraph(l);
			SerializableGraph rightGraph = SerializableGraph.readGraph(r);
			if (!leftGraph.equals(rightGraph)) {
				diffFound = true;
				String leftpath = left.toString();
				String rightpath = right.toString();

				if (!leftGraph.sameStructure(rightGraph))
					reporter.fileDiff(leftpath, rightpath, "Graphs have different structure");
				else {
					CollectionsDiffBuilder<SerializableNodeDescription> builder = new CollectionsDiffBuilder<>(
							SerializableNodeDescription.class, leftGraph.getDescriptions(),
							rightGraph.getDescriptions());
					builder.compute(SerializableNodeDescription::compareTo);

					if (builder.sameContent())
						reporter.fileDiff(leftpath, rightpath,
								"Graphs are different but have same structure and descriptions");
					else {
						Map<Integer, String> llabels = new HashMap<>();
						Map<Integer, String> rlabels = new HashMap<>();
						leftGraph.getNodes().forEach(d -> llabels.put(d.getId(), d.getText()));
						rightGraph.getNodes().forEach(d -> rlabels.put(d.getId(), d.getText()));

						Iterator<SerializableNodeDescription> ol = builder.getOnlyFirst().iterator();
						Iterator<SerializableNodeDescription> or = builder.getOnlySecond().iterator();

						SerializableNodeDescription currentF = null;
						SerializableNodeDescription currentS = null;

						while (ol.hasNext() && or.hasNext()) {
							if (ol.hasNext() && currentF == null)
								currentF = ol.next();
							if (or.hasNext() && currentS == null)
								currentS = or.next();

							if (currentF == null) {
								if (currentS == null)
									break;
								else {
									reporter.fileDiff(leftpath, rightpath,
											"First graph does not have a desciption for node "
													+ currentS.getNodeId() + ": "
													+ rlabels.get(currentS.getNodeId()));
									currentS = null;
									continue;
								}
							} else {
								if (currentS == null) {
									reporter.fileDiff(leftpath, rightpath,
											"Second graph does not have a desciption for node "
													+ currentF.getNodeId() + ": "
													+ llabels.get(currentF.getNodeId()));
									currentF = null;
									continue;
								}
							}

							int fid = currentF.getNodeId();
							int sid = currentS.getNodeId();
							if (fid == sid) {
								reporter.fileDiff(leftpath, rightpath,
										"Different desciption for node "
												+ currentF.getNodeId() + ": "
												+ llabels.get(currentF.getNodeId()));
								currentF = null;
								currentS = null;
							} else if (fid < sid) {
								reporter.fileDiff(leftpath, rightpath,
										"Second graph does not have a desciption for node "
												+ currentF.getNodeId() + ": "
												+ llabels.get(currentF.getNodeId()));
								currentF = null;
							} else {
								reporter.fileDiff(leftpath, rightpath,
										"First graph does not have a desciption for node "
												+ currentS.getNodeId() + ": "
												+ rlabels.get(currentS.getNodeId()));
								currentS = null;
							}
						}
					}
				}
			}
		}

		return diffFound;
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
