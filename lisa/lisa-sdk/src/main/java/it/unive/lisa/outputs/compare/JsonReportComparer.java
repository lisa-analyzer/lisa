package it.unive.lisa.outputs.compare;

import static java.lang.String.format;

import it.unive.lisa.LiSA;
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
 * {@link JsonReport}s. Reports are compared by:
 * <ol>
 * <li>configurations ({@link JsonReport#getConfiguration()}) are compared fist,
 * treating each field as a string</li>
 * <li>run information ({@link JsonReport#getInfo()}) are then compared,
 * treating each field as a string but ignoring timestamps (duration, start,
 * end) and LiSA's version</li>
 * <li>warnings ({@link JsonReport#getWarnings()}) are then compared, using
 * {@link JsonWarning#compareTo(JsonWarning)} method</li>
 * <li>the set of files produced during the analysis
 * ({@link JsonReport#getFiles()}) is then compared, matching their paths</li>
 * <li>finally, the contents of every file produced by both analyses are
 * compared, excluding the report itself ({@link LiSA#REPORT_NAME}) and
 * visualization-only files</li>
 * </ol>
 * Comparison can be customized providing an implementation of
 * {@link DiffAlgorithm}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JsonReportComparer {

	private static final Logger LOG = LogManager.getLogger(JsonReportComparer.class);

	private static final String MISSING_FILE = "'%s' declared as output in the %s report does not exist";

	private static final String VIS_ONLY = "Skipping comparison of visualization-only files: '{}' and '{}'";

	private static final String CANNOT_COMPARE = "Cannot compare files '%s' and '%s'";

	private static final String MALFORMED_GRAPH = "Graphs are different but have same structure and descriptions";

	private static final String GRAPH_DIFF = "Graphs have different structure";

	private static final String NO_DESC = "%s graph does not have a desciption for node %d: %s";

	private static final String DESC_DIFF = "Different desciption for node %d (%s)";

	private static final String DESC_DIFF_VERBOSE = "Different desciption for node %d (%s):\n%s";

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
	public interface DiffAlgorithm {
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
		void report(
				REPORTED_COMPONENT component,
				REPORT_TYPE type,
				Collection<?> reported);

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
		void fileDiff(
				String first,
				String second,
				String message);

		/**
		 * Callback invoked by a {@link JsonReportComparer} whenever an analysis
		 * information key is mapped to two different values.
		 * 
		 * @param key    the information key
		 * @param first  the value in the first report
		 * @param second the value in the second report
		 */
		void infoDiff(
				String key,
				String first,
				String second);

		/**
		 * Callback invoked by a {@link JsonReportComparer} whenever a
		 * configuration key is mapped to two different values.
		 * 
		 * @param key    the configuration key
		 * @param first  the value in the first report
		 * @param second the value in the second report
		 */
		void configurationDiff(
				String key,
				String first,
				String second);

		/**
		 * If {@code true}, analysis configurations
		 * ({@link JsonReport#getConfiguration()}) will be compared.
		 * 
		 * @return whether or not configurations should be compared
		 */
		default boolean shouldCompareConfigurations() {
			return true;
		}

		/**
		 * If {@code true}, run information ({@link JsonReport#getInfo()}) will
		 * be compared.
		 * 
		 * @return whether or not run informations should be compared
		 */
		default boolean shouldCompareRunInfos() {
			return true;
		}

		/**
		 * If {@code true}, warnings ({@link JsonReport#getWarnings()}) will be
		 * compared.
		 * 
		 * @return whether or not warnings should be compared
		 */
		default boolean shouldCompareWarnings() {
			return true;
		}

		/**
		 * If {@code true}, files ({@link JsonReport#getFiles()}) will be
		 * compared.
		 * 
		 * @return whether or not files should be compared
		 */
		default boolean shouldCompareFiles() {
			return true;
		}

		/**
		 * If {@code true}, content of files produced by both analyses will be
		 * compared.
		 * 
		 * @return whether or not file contents should be compared
		 */
		default boolean shouldCompareFileContents() {
			return true;
		}

		/**
		 * If {@code true}, comparison will halt after the first failed category
		 * (warnings, files, ...) without reporting the full diff.
		 * 
		 * @return whether or not comparison should fail fast
		 */
		default boolean shouldFailFast() {
			return false;
		}

		/**
		 * Yields whether or not the file pointed by the given path should be
		 * considered a json graph.
		 * 
		 * @param path the path pointing to the file
		 * 
		 * @return {@code true} if that condition holds
		 */
		default boolean isJsonGraph(
				String path) {
			return FilenameUtils.getExtension(path).equals("json");
		}

		/**
		 * Yields whether or not the file pointed by the given path is for
		 * visualizing results and can thus be skipped as its content depends
		 * solely on other files.
		 * 
		 * @param path the path pointing to the file
		 * 
		 * @return {@code true} if that condition holds
		 */
		default boolean isVisualizationFile(
				String path) {
			String ext = FilenameUtils.getExtension(path);
			return ext.equals("dot")
					|| ext.equals("graphml")
					|| ext.equals("html")
					|| ext.equals("js");
		}

		/**
		 * Custom comparison method for files that are neither json graphs nor
		 * visualization-only files.
		 * 
		 * @param expected the file from the expected report
		 * @param actual   the file from the actual report
		 * 
		 * @return whether or not a difference was found in the files
		 * 
		 * @throws UnsupportedOperationException if file comparisons are not
		 *                                           supported for the given
		 *                                           file type
		 */
		default boolean customFileCompare(
				File expected,
				File actual) {
			throw new UnsupportedOperationException(format(CANNOT_COMPARE, expected.toString(), actual.toString()));
		}

		/**
		 * If {@code true}, the full diff between two labels will be reported
		 * when a difference is found. Otherwise, a simple message is reported.
		 * 
		 * @return whether or not label diff reporting should be verbose
		 */
		default boolean verboseLabelDiff() {
			return true;
		}
	}

	/**
	 * Compares the two reports using {@link BaseDiffAlgorithm}. See this class'
	 * documentation for information about how reports are compared.
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
	public static boolean compare(
			JsonReport first,
			JsonReport second,
			File firstFileRoot,
			File secondFileRoot)
			throws IOException {
		return compare(first, second, firstFileRoot, secondFileRoot, new BaseDiffAlgorithm());
	}

	/**
	 * Compares the two reports according to the given algorithm. See this
	 * class' documentation for information about how reports are compared.
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
	 * @param diff           the {@link DiffAlgorithm} that will be used for
	 *                           dumping the differences found in the two
	 *                           reports and for customizing the reporting
	 *                           process
	 * 
	 * @return {@code true} if and only the two reports are equal
	 * 
	 * @throws IOException if errors happen while opening or reading the files
	 *                         contained in the reports
	 */
	public static boolean compare(
			JsonReport first,
			JsonReport second,
			File firstFileRoot,
			File secondFileRoot,
			DiffAlgorithm diff)
			throws IOException {
		boolean sameConfs = !diff.shouldCompareConfigurations() || compareConfs(first, second, diff);
		if (diff.shouldFailFast() && !sameConfs)
			return false;

		boolean sameInfos = !diff.shouldCompareRunInfos() || compareInfos(first, second, diff);
		if (diff.shouldFailFast() && !sameInfos)
			return false;

		boolean sameWarnings = !diff.shouldCompareWarnings() || compareWarnings(first, second, diff);
		if (diff.shouldFailFast() && !sameWarnings)
			return false;

		boolean sameFiles = !diff.shouldCompareFiles();
		CollectionsDiffBuilder<String> files = null;
		if (!sameFiles) {
			files = compareFiles(first, second, diff);
			sameFiles = files.sameContent();
			if (diff.shouldFailFast() && !sameFiles)
				return false;
		}

		boolean sameFileContents = !diff.shouldCompareFileContents();
		if (!sameFileContents) {
			if (files == null) {
				files = compareFiles(first, second, diff);
				sameFiles = files.sameContent();
			}

			sameFileContents = compareFileContents(firstFileRoot, secondFileRoot, diff, files);
			if (diff.shouldFailFast() && !(sameFileContents && sameFiles))
				return false;
		}

		return sameConfs && sameInfos && sameWarnings && sameFiles && sameFileContents;
	}

	private static boolean compareFileContents(
			File firstFileRoot,
			File secondFileRoot,
			DiffAlgorithm diff,
			CollectionsDiffBuilder<String> files)
			throws FileNotFoundException,
			IOException {
		boolean diffFound = false;
		for (Pair<String, String> pair : files.getCommons()) {
			File left = new File(firstFileRoot, pair.getLeft());
			if (!left.exists())
				throw new FileNotFoundException(format(MISSING_FILE, pair.getLeft(), "first"));

			File right = new File(secondFileRoot, pair.getRight());
			if (!right.exists())
				throw new FileNotFoundException(format(MISSING_FILE, pair.getRight(), "second"));

			String path = left.getName();
			if (FilenameUtils.getName(path).equals(LiSA.REPORT_NAME))
				continue;

			if (diff.isJsonGraph(path))
				diffFound |= matchJsonGraphs(diff, left, right);
			else if (diff.isVisualizationFile(path))
				LOG.info(VIS_ONLY, left.toString(), right.toString());
			else
				diffFound |= diff.customFileCompare(left, right);
		}
		return !diffFound;
	}

	private static CollectionsDiffBuilder<String> compareFiles(
			JsonReport first,
			JsonReport second,
			DiffAlgorithm diff) {
		CollectionsDiffBuilder<String> files = new CollectionsDiffBuilder<>(
				String.class,
				first.getFiles(),
				second.getFiles());
		files.compute(String::compareTo);

		if (!files.getCommons().isEmpty())
			diff.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.COMMON, files.getCommons());
		if (!files.getOnlyFirst().isEmpty())
			diff.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_FIRST, files.getOnlyFirst());
		if (!files.getOnlySecond().isEmpty())
			diff.report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_SECOND, files.getOnlySecond());
		return files;
	}

	private static boolean compareConfs(
			JsonReport first,
			JsonReport second,
			DiffAlgorithm diff) {
		return compareBags(
				REPORTED_COMPONENT.CONFIGURATION,
				first.getConfiguration(),
				second.getConfiguration(),
				diff,
				(
						key,
						fvalue,
						svalue) -> diff.configurationDiff(key, fvalue, svalue),
				key -> false);
	}

	private static final Set<String> INFO_BLACKLIST = Set.of("duration", "start", "end", "version");

	private static boolean compareInfos(
			JsonReport first,
			JsonReport second,
			DiffAlgorithm diff) {
		return compareBags(
				REPORTED_COMPONENT.INFO,
				first.getInfo(),
				second.getInfo(),
				diff,
				(
						key,
						fvalue,
						svalue) -> diff.infoDiff(key, fvalue, svalue),
				// we are really only interested in code metrics here,
				// information like timestamps and version are not useful - we
				// still use a blacklist approach to ensure that new fields are
				// tested by default
				key -> INFO_BLACKLIST.contains(key));
	}

	private static boolean compareBags(
			REPORTED_COMPONENT component,
			Map<String, String> first,
			Map<String, String> second,
			DiffAlgorithm diff,
			TriConsumer<String, String, String> reporter,
			Predicate<String> ignore) {
		CollectionsDiffBuilder<String> builder = new CollectionsDiffBuilder<>(
				String.class,
				first.keySet(),
				second.keySet());
		builder.compute(String::compareTo);

		if (!builder.getOnlyFirst().isEmpty())
			diff.report(component, REPORT_TYPE.ONLY_FIRST, builder.getOnlyFirst());
		if (!builder.getOnlySecond().isEmpty())
			diff.report(component, REPORT_TYPE.ONLY_SECOND, builder.getOnlySecond());

		Collection<Pair<String, String>> same = new HashSet<>();
		if (!builder.getCommons().isEmpty())
			for (Pair<String, String> entry : builder.getCommons()) {
				String key = entry.getKey();
				String fvalue = first.get(key);
				String svalue = second.get(key);
				if (fvalue.equals(svalue) || ignore.test(key))
					same.add(Pair.of(key, fvalue));
				else
					reporter.accept(key, fvalue, svalue);
			}

		if (!same.isEmpty())
			diff.report(component, REPORT_TYPE.COMMON, same);

		return builder.sameContent() && same.size() == builder.getCommons().size();
	}

	private static boolean compareWarnings(
			JsonReport first,
			JsonReport second,
			DiffAlgorithm diff) {
		CollectionsDiffBuilder<JsonWarning> warnings = new CollectionsDiffBuilder<>(
				JsonWarning.class,
				first.getWarnings(),
				second.getWarnings());
		warnings.compute(JsonWarning::compareTo);

		if (!warnings.getCommons().isEmpty())
			diff.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.COMMON, warnings.getCommons());
		if (!warnings.getOnlyFirst().isEmpty())
			diff.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_FIRST, warnings.getOnlyFirst());
		if (!warnings.getOnlySecond().isEmpty())
			diff.report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_SECOND, warnings.getOnlySecond());
		return warnings.sameContent();
	}

	private static boolean matchJsonGraphs(
			DiffAlgorithm diff,
			File left,
			File right)
			throws IOException,
			FileNotFoundException {
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
					diff.fileDiff(leftpath, rightpath, GRAPH_DIFF);
				else {
					CollectionsDiffBuilder<SerializableNodeDescription> builder = new CollectionsDiffBuilder<>(
							SerializableNodeDescription.class,
							leftGraph.getDescriptions(),
							rightGraph.getDescriptions());
					builder.compute(SerializableNodeDescription::compareTo);

					if (builder.sameContent())
						diff.fileDiff(leftpath, rightpath, MALFORMED_GRAPH);
					else
						compareLabels(diff, leftGraph, rightGraph, leftpath, rightpath, builder);
				}
			}
		}

		return diffFound;
	}

	private static void compareLabels(
			DiffAlgorithm diff,
			SerializableGraph leftGraph,
			SerializableGraph rightGraph,
			String leftpath,
			String rightpath,
			CollectionsDiffBuilder<SerializableNodeDescription> builder) {
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

			if (currentF == null)
				if (currentS == null)
					break;
				else {
					diff.fileDiff(leftpath, rightpath, format(
							NO_DESC,
							"First",
							currentS.getNodeId(),
							rlabels.get(currentS.getNodeId())));
					currentS = null;
					continue;
				}
			else if (currentS == null) {
				diff.fileDiff(leftpath, rightpath, format(
						NO_DESC,
						"Second",
						currentF.getNodeId(),
						llabels.get(currentF.getNodeId())));
				currentF = null;
				continue;
			}

			int fid = currentF.getNodeId();
			int sid = currentS.getNodeId();
			if (fid == sid) {
				if (diff.verboseLabelDiff())
					diff.fileDiff(leftpath, rightpath, format(
							DESC_DIFF_VERBOSE,
							currentF.getNodeId(),
							llabels.get(currentF.getNodeId()),
							diff(currentF.getDescription(), currentS.getDescription())));
				else
					diff.fileDiff(leftpath, rightpath, format(
							DESC_DIFF,
							currentF.getNodeId(),
							llabels.get(currentF.getNodeId())));
				currentF = null;
				currentS = null;
			} else if (fid < sid) {
				diff.fileDiff(leftpath, rightpath, format(
						NO_DESC,
						"Second",
						currentF.getNodeId(),
						llabels.get(currentF.getNodeId())));
				currentF = null;
			} else {
				diff.fileDiff(leftpath, rightpath, format(
						NO_DESC,
						"First",
						currentS.getNodeId(),
						rlabels.get(currentS.getNodeId())));
				currentS = null;
			}
		}
	}

	/**
	 * A {@link DiffAlgorithm} that dumps the differences using this class'
	 * logger, will compare all fields of the report, and will not fail fast.
	 * This class provides an implementation only of non-default method from
	 * {@link DiffAlgorithm}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class BaseDiffAlgorithm implements DiffAlgorithm {

		private static final Logger LOG = LogManager.getLogger(BaseDiffAlgorithm.class);

		private static final String FILES_ONLY = "Files only in the {} report:";
		private static final String WARNINGS_ONLY = "Warnings only in the {} report:";
		private static final String INFOS_ONLY = "Run info keys only in the {} report:";
		private static final String CONFS_ONLY = "Configuration keys only in the {} report:";
		private static final String FILE_DIFF = "['{}', '{}'] {}";
		private static final String VALUE_DIFF = "Different values for {} key '{}': '{}' and '{}'";

		@Override
		public void report(
				REPORTED_COMPONENT component,
				REPORT_TYPE type,
				Collection<?> reported) {
			if (type == REPORT_TYPE.COMMON)
				return;

			boolean isFirst = type == REPORT_TYPE.ONLY_FIRST;

			switch (component) {
			case FILES:
				if (isFirst)
					LOG.warn(FILES_ONLY, "first");
				else
					LOG.warn(FILES_ONLY, "second");
				break;
			case WARNINGS:
				if (isFirst)
					LOG.warn(WARNINGS_ONLY, "first");
				else
					LOG.warn(WARNINGS_ONLY, "second");
				break;
			case INFO:
				if (isFirst)
					LOG.warn(INFOS_ONLY, "first");
				else
					LOG.warn(INFOS_ONLY, "second");
				break;
			case CONFIGURATION:
				if (isFirst)
					LOG.warn(CONFS_ONLY, "first");
				else
					LOG.warn(CONFS_ONLY, "second");
				break;
			default:
				break;
			}

			for (Object o : reported)
				LOG.warn("\t" + o);
		}

		@Override
		public void fileDiff(
				String first,
				String second,
				String message) {
			LOG.warn(FILE_DIFF, first, second, message);
		}

		@Override
		public void infoDiff(
				String key,
				String first,
				String second) {
			LOG.warn(VALUE_DIFF, "run info", key, first, second);
		}

		@Override
		public void configurationDiff(
				String key,
				String first,
				String second) {
			LOG.warn(VALUE_DIFF, "configuration", key, first, second);
		}
	}

	private static final String diff(
			SerializableValue first,
			SerializableValue second) {
		StringBuilder builder = new StringBuilder();
		diff(0, builder, first, second);
		return builder.toString().replaceAll("(?m)^[ \t]*\r?\n", "").trim();
	}

	private static final boolean diff(
			int depth,
			StringBuilder builder,
			SerializableValue first,
			SerializableValue second) {
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

	private static final boolean diff(
			int depth,
			StringBuilder builder,
			SerializableString first,
			SerializableString second) {
		if (!first.getValue().equals(second.getValue())) {
			fillWithDiff(depth, builder, first, second);
			return true;
		}
		return false;
	}

	private static final boolean diff(
			int depth,
			StringBuilder builder,
			SerializableArray first,
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

	private static final boolean diff(
			int depth,
			StringBuilder builder,
			SerializableObject first,
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

	private static final void fillWithDiff(
			int depth,
			StringBuilder builder,
			SerializableValue first,
			SerializableValue second) {
		builder.append("\t".repeat(depth))
				.append(first)
				.append("\n")
				.append("\t".repeat(depth))
				.append("<--->\n")
				.append("\t".repeat(depth))
				.append(second);
	}
}
