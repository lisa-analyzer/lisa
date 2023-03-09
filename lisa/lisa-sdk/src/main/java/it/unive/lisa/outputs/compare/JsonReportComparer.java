package it.unive.lisa.outputs.compare;

import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.outputs.json.JsonReport.JsonWarning;
import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableNodeDescription;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Predicate;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;

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
		 * Indicates that the difference was found in the information about the
		 * analysis.
		 */
		INFO,

		/**
		 * Indicates that the difference was found in the configuration of the
		 * analysis.
		 */
		CONFIGURATION,

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

		/**
		 * Callback invoked by a {@link JsonReportComparer} whenever an analysis
		 * information key is mapped to two different values.
		 * 
		 * @param key    the information key
		 * @param first  the value in the first report
		 * @param second the value in the second report
		 */
		void infoDiff(String key, String first, String second);

		/**
		 * Callback invoked by a {@link JsonReportComparer} whenever a
		 * configuration key is mapped to two different values.
		 * 
		 * @param key    the configuration key
		 * @param first  the value in the first report
		 * @param second the value in the second report
		 */
		void configurationDiff(String key, String first, String second);
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

		boolean sameConfs = compareConfs(first, second, reporter);
		boolean sameInfos = compareInfos(first, second, reporter);
		boolean sameWarnings = compareWarnings(first, second, reporter);
		CollectionsDiffBuilder<String> files = compareFiles(first, second, reporter);

		if (!sameConfs || !sameInfos || !sameWarnings || !files.sameContent())
			return false;

		boolean diffFound = compareFileContents(firstFileRoot, secondFileRoot, reporter, files);
		return !diffFound;
	}

	private static boolean compareFileContents(File firstFileRoot, File secondFileRoot, DiffReporter reporter,
			CollectionsDiffBuilder<String> files) throws FileNotFoundException, IOException {
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

			if (FilenameUtils.getName(left.getName()).equals("report.json"))
				continue;

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
		return diffFound;
	}

	private static CollectionsDiffBuilder<String> compareFiles(JsonReport first, JsonReport second,
			DiffReporter reporter) {
		CollectionsDiffBuilder<String> files = new CollectionsDiffBuilder<>(String.class, first.getFiles(),
				second.getFiles());
		files.compute(String::compareTo);

		if (!files.getCommons().isEmpty())
			reporter.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.COMMON, files.getCommons());
		if (!files.getOnlyFirst().isEmpty())
			reporter.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_FIRST, files.getOnlyFirst());
		if (!files.getOnlySecond().isEmpty())
			reporter.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_SECOND, files.getOnlySecond());
		return files;
	}

	private static boolean compareConfs(JsonReport first, JsonReport second,
			DiffReporter reporter) {
		return compareBags(REPORTED_COMPONENT.CONFIGURATION, first.getConfiguration(), second.getConfiguration(),
				reporter, (key, fvalue, svalue) -> reporter.configurationDiff(key, fvalue, svalue), key -> false);
	}

	private static final Set<String> INFO_BLACKLIST = Set.of("duration", "start", "end", "version");

	private static boolean compareInfos(JsonReport first, JsonReport second,
			DiffReporter reporter) {
		return compareBags(REPORTED_COMPONENT.INFO, first.getInfo(), second.getInfo(), reporter,
				(key, fvalue, svalue) -> reporter.infoDiff(key, fvalue, svalue),
				// we are really only interested in code metrics here,
				// information like timestamps and version are not useful for
				// the test system - we still use a blacklist approach to ensure
				// that new fields are tested by default
				key -> INFO_BLACKLIST.contains(key));
	}

	private static boolean compareBags(REPORTED_COMPONENT component, Map<String, String> first,
			Map<String, String> second, DiffReporter reporter, TriConsumer<String, String, String> diffReporter,
			Predicate<String> ignore) {
		CollectionsDiffBuilder<
				String> builder = new CollectionsDiffBuilder<>(String.class, first.keySet(), second.keySet());
		builder.compute(String::compareTo);

		if (!builder.getOnlyFirst().isEmpty())
			reporter.report(component, REPORT_TYPE.ONLY_FIRST, builder.getOnlyFirst());
		if (!builder.getOnlySecond().isEmpty())
			reporter.report(component, REPORT_TYPE.ONLY_SECOND, builder.getOnlySecond());

		Collection<Pair<String, String>> same = new HashSet<>();
		if (!builder.getCommons().isEmpty()) {
			for (Pair<String, String> entry : builder.getCommons()) {
				String key = entry.getKey();
				String fvalue = first.get(key);
				String svalue = second.get(key);
				if (fvalue.equals(svalue) || ignore.test(key))
					same.add(Pair.of(key, fvalue));
				else
					diffReporter.accept(key, fvalue, svalue);
			}
		}

		if (!same.isEmpty())
			reporter.report(component, REPORT_TYPE.COMMON, same);

		return builder.sameContent() && same.size() == builder.getCommons().size();
	}

	private static boolean compareWarnings(JsonReport first, JsonReport second,
			DiffReporter reporter) {
		CollectionsDiffBuilder<JsonWarning> warnings = new CollectionsDiffBuilder<>(JsonWarning.class,
				first.getWarnings(), second.getWarnings());
		warnings.compute(JsonWarning::compareTo);

		if (!warnings.getCommons().isEmpty())
			reporter.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.COMMON, warnings.getCommons());
		if (!warnings.getOnlyFirst().isEmpty())
			reporter.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_FIRST, warnings.getOnlyFirst());
		if (!warnings.getOnlySecond().isEmpty())
			reporter.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_SECOND, warnings.getOnlySecond());
		return warnings.sameContent();
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
												+ currentF.getNodeId() + " ("
												+ llabels.get(currentF.getNodeId()) + "):\n"
												+ diff(currentF.getDescription(), currentS.getDescription()));
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
					LOG.warn("Files only in the first report:");
				else
					LOG.warn("Files only in the second report:");
				break;
			case WARNINGS:
				if (isFirst)
					LOG.warn("Warnings only in the first report:");
				else
					LOG.warn("Warnings only in the second report:");
				break;
			case INFO:
				if (isFirst)
					LOG.warn("Run info keys only in the first report:");
				else
					LOG.warn("Run info keys only in the second report:");
				break;
			case CONFIGURATION:
				if (isFirst)
					LOG.warn("Configuration keys only in the first report:");
				else
					LOG.warn("Configuration keys only in the second report:");
				break;
			default:
				break;
			}

			for (Object o : reported)
				LOG.warn("\t" + o);
		}

		@Override
		public void fileDiff(String first, String second, String message) {
			LOG.warn("['" + first + "', '" + second + "'] " + message);
		}

		@Override
		public void infoDiff(String key, String first, String second) {
			LOG.warn("Different values for run info key '" + key + "': '" + first + "' and '" + second + "'");
		}

		@Override
		public void configurationDiff(String key, String first, String second) {
			LOG.warn("Different values for configuration key '" + key + "': '" + first + "' and '" + second + "'");
		}
	}

	private static final String diff(SerializableValue first, SerializableValue second) {
		StringBuilder builder = new StringBuilder();
		diff(0, builder, first, second);
		return builder.toString().replaceAll("(?m)^[ \t]*\r?\n", "").trim();
	}

	private static final boolean diff(int depth, StringBuilder builder,
			SerializableValue first, SerializableValue second) {
		if (first.getClass() != second.getClass()) {
			fillWithDiff(depth, builder, first, second);
			return true;
		}

		if (first instanceof SerializableString)
			return diff(depth, builder, (SerializableString) first, (SerializableString) second);

		if (first instanceof SerializableArray)
			return diff(depth, builder, (SerializableArray) first, (SerializableArray) second);

		return diff(depth, builder, (SerializableObject) first, (SerializableObject) second);
	}

	private static final boolean diff(int depth, StringBuilder builder, SerializableString first,
			SerializableString second) {
		if (!first.getValue().equals(second.getValue())) {
			fillWithDiff(depth, builder, first, second);
			return true;
		}
		return false;
	}

	private static final boolean diff(int depth, StringBuilder builder, SerializableArray first,
			SerializableArray second) {
		List<SerializableValue> felements = first.getElements();
		List<SerializableValue> selements = second.getElements();
		int fsize = felements.size();
		int ssize = selements.size();
		int min = Math.min(fsize, ssize);
		boolean atLeastOne = false;
		StringBuilder inner;
		for (int i = 0; i < min; i++)
			if (diff(depth + 1, inner = new StringBuilder(), felements.get(i), selements.get(i))) {
				builder.append("\t".repeat(depth))
						.append(">ELEMENT #")
						.append(i)
						.append(":\n")
						.append(inner.toString())
						.append("\n");
				atLeastOne = true;
			}

		if (fsize > min) {
			builder.append("\t".repeat(depth))
					.append(">EXPECTED HAS ")
					.append(fsize - min)
					.append(" MORE ELEMENT(S):\n");
			for (int i = min; i < fsize; i++) {
				builder.append("\t".repeat(depth + 1))
						.append(">ELEMENT #")
						.append(i)
						.append(":\n")
						.append("\t".repeat(depth + 2))
						.append(felements.get(i))
						.append("\n");
			}
			atLeastOne = true;
		}

		if (ssize > min) {
			builder.append("\t".repeat(depth))
					.append(">ACTUAL HAS ")
					.append(ssize - min)
					.append(" MORE ELEMENT(S):\n");
			for (int i = min; i < ssize; i++) {
				builder.append("\t".repeat(depth + 1))
						.append(">ELEMENT #")
						.append(i)
						.append(":\n")
						.append("\t".repeat(depth + 2))
						.append(selements.get(i))
						.append("\n");
			}
			atLeastOne = true;
		}

		return atLeastOne;
	}

	private static final boolean diff(int depth, StringBuilder builder, SerializableObject first,
			SerializableObject second) {
		SortedMap<String, SerializableValue> felements = first.getFields();
		SortedMap<String, SerializableValue> selements = second.getFields();

		CollectionsDiffBuilder<
				String> diff = new CollectionsDiffBuilder<>(String.class, felements.keySet(), selements.keySet());
		diff.compute(String::compareTo);

		boolean atLeastOne = false;
		StringBuilder inner;
		for (Pair<String, String> field : diff.getCommons())
			if (diff(depth + 1, inner = new StringBuilder(), felements.get(field.getLeft()),
					selements.get(field.getLeft()))) {
				builder.append("\t".repeat(depth))
						.append(">FIELD ")
						.append(field.getLeft())
						.append(":\n")
						.append(inner.toString())
						.append("\n");
				atLeastOne = true;
			}

		if (!diff.getOnlyFirst().isEmpty()) {
			builder.append("\t".repeat(depth))
					.append(">EXPECTED HAS ")
					.append(diff.getOnlyFirst().size())
					.append(" MORE FIELD(S):\n");
			for (String field : diff.getOnlyFirst()) {
				builder.append("\t".repeat(depth + 1))
						.append(">FIELD ")
						.append(field)
						.append(":\n")
						.append("\t".repeat(depth + 2))
						.append(felements.get(field))
						.append("\n");
			}
			atLeastOne = true;
		}

		if (!diff.getOnlySecond().isEmpty()) {
			builder.append("\t".repeat(depth))
					.append(">ACTUAL HAS ")
					.append(diff.getOnlySecond().size())
					.append(" MORE FIELD(S):\n");
			for (String field : diff.getOnlySecond()) {
				builder.append("\t".repeat(depth + 1))
						.append(">FIELD ")
						.append(field)
						.append(":\n")
						.append("\t".repeat(depth + 2))
						.append(selements.get(field))
						.append("\n");
			}
			atLeastOne = true;
		}

		return atLeastOne;
	}

	private static final void fillWithDiff(int depth, StringBuilder builder,
			SerializableValue first, SerializableValue second) {
		builder.append("\t".repeat(depth))
				.append(first)
				.append("\n")
				.append("\t".repeat(depth))
				.append("<--->\n")
				.append("\t".repeat(depth))
				.append(second);
	}
}
