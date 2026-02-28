package it.unive.lisa.outputs.compare;

import static java.lang.String.format;

import it.unive.lisa.listeners.TracingListener;
import it.unive.lisa.outputs.JSONReportDumper;
import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.outputs.json.JsonReport.JsonMessage;
import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableEdge;
import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableNode;
import it.unive.lisa.outputs.serializableGraph.SerializableNodeDescription;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * A class providing capabilities for finding differences between two
 * {@link JsonReport}s. Reports are compared by:
 * <ol>
 * <li>configurations ({@link JsonReport#getConfiguration()}) are compared fist,
 * treating each field as a string;</li>
 * <li>run information ({@link JsonReport#getInfo()}) are then compared,
 * treating each field as a string but ignoring timestamps (duration, start,
 * end) and LiSA's version;</li>
 * <li>warnings ({@link JsonReport#getWarnings()}) are then compared, using
 * {@link JsonMessage#compareTo(JsonMessage)} method;</li>
 * <li>notices ({@link JsonReport#getNotices()}) are then compared, using
 * {@link JsonMessage#compareTo(JsonMessage)} method;</li>
 * <li>the set of files produced during the analysis
 * ({@link JsonReport#getFiles()}) is then compared, matching their paths;</li>
 * <li>finally, the contents of every file produced by both analyses are
 * compared, excluding the report itself ({@link JSONReportDumper#REPORT_NAME})
 * and visualization-only files.</li>
 * </ol>
 * All differences are reported by showing them in the log of the analysis.
 * Comparison and reporting can be customized by overriding the following
 * methods:
 * <ul>
 * <li>{@link #shouldFailFast()} to determine if a full comparison is needed, or
 * if the comparison should halt at the first difference found;</li>
 * <li>and {@link #shouldCompareAdditionalInfo()} to decide which components of
 * the reports should be inspected;</li>
 * <li>and to decide which configuration and info keys should be ignored;</li>
 * <li>{@link #shouldCompareConfigurations()},
 * {@link #ignoredConfigurationKeys()}, and
 * {@link #compareConfs(JsonReport, JsonReport)} for comparison of
 * configurations;</li>
 * <li>{@link #shouldCompareRunInfos()}, {@link #ignoredRunInfoKeys()}, and
 * {@link #compareInfos(JsonReport, JsonReport)} for comparison of run
 * information;</li>
 * <li>{@link #shouldCompareWarnings()} and
 * {@link #compareWarnings(JsonReport, JsonReport)} for comparison of
 * warnings;</li>
 * <li>{@link #shouldCompareFiles()}, {@link #shouldCompareFileContents()},
 * {@link #compareFiles(JsonReport, JsonReport)},
 * {@link #compareFileContents(JsonReport, JsonReport, File, File)},
 * {@link #matchJsonGraphs(File, File)}, {@link #isVisualizationFile(String)},
 * and {@link #customFileCompare(File, File)} for comparison of files;</li>
 * <li>{@link #shouldCompareAdditionalInfo()} and
 * {@link #compareAdditionalInfo(JsonReport, JsonReport)} for comparison of
 * additional information;</li>
 * <li>{@link #compare(JsonReport, JsonReport, File, File)} for the overall
 * comparison process;</li>
 * <li>{@link #report(REPORTED_COMPONENT, REPORT_TYPE, Collection)},
 * {@link #configurationDiff(String, String, String)},
 * {@link #infoDiff(String, String, String)},
 * {@link #fileDiff(String, String, String)}, and
 * {@link #additionalInfoDiff(String)} for reporting differences.</li>
 * </ul>
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ResultComparer {

	private static final Logger LOG = LogManager.getLogger(ResultComparer.class);

	private static final String MISSING_FILE = "'%s' declared as output in the %s report does not exist";

	private static final String VIS_ONLY = "Skipping comparison of visualization-only files: '{}' and '{}'";

	private static final String CANNOT_COMPARE = "Cannot compare files '%s' and '%s'";

	private static final String MALFORMED_GRAPH = "Graphs are different but have same structure and descriptions";

	private static final String GRAPH_DIFF = "Graphs have different structure";

	private static final String NO_DESC = "%s graph does not have a description for node %d: %s";

	private static final String DESC_DIFF = "Different description for node %d (%s)";

	private static final String DESC_DIFF_VERBOSE = "Different description for node %d (%s):\n%s";

	private static final String TRACE_DIFF = "Line %d of trace file differs:\n\t'%s'\n\t<--->\n\t'%s'";

	private static final String FILES_ONLY = "Files only in the {} report:";

	private static final String WARNINGS_ONLY = "Warnings only in the {} report:";

	private static final String NOTICES_ONLY = "Notices only in the {} report:";

	private static final String INFOS_ONLY = "Run info keys only in the {} report:";

	private static final String CONFS_ONLY = "Configuration keys only in the {} report:";

	private static final String ADDITIONAL_INFO_ONLY = "Additional info is present only in the {} report:";

	private static final String FILE_DIFF = "['{}'] {}";

	private static final String VALUE_DIFF = "Different values for {} key '{}': '{}' and '{}'";

	private static final String ADD_INFO_DIFF = "Different additional info:\n{}";

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
		 * generated notices.
		 */
		NOTICES,

		/**
		 * Indicates that the difference was found in the collection of
		 * generated files.
		 */
		FILES,

		/**
		 * Indicates that the difference was found in the additional
		 * information.
		 */
		ADDITIONAL_INFO;
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
	 * 
	 * @return {@code true} if and only the two reports are equal
	 * 
	 * @throws IOException if errors happen while opening or reading the files
	 *                         contained in the reports
	 */
	public boolean compare(
			JsonReport first,
			JsonReport second,
			File firstFileRoot,
			File secondFileRoot)
			throws IOException {
		boolean sameConfs = !shouldCompareConfigurations() || compareConfs(first, second);
		if (shouldFailFast() && !sameConfs)
			return false;

		boolean sameInfos = !shouldCompareRunInfos() || compareInfos(first, second);
		if (shouldFailFast() && !sameInfos)
			return false;

		boolean sameWarnings = !shouldCompareWarnings() || compareWarnings(first, second);
		if (shouldFailFast() && !sameWarnings)
			return false;

		boolean sameNotices = !shouldCompareNotices() || compareNotices(first, second);
		if (shouldFailFast() && !sameNotices)
			return false;

		boolean sameFiles = !shouldCompareFiles() || compareFiles(first, second);
		if (shouldFailFast() && !sameFiles)
			return false;

		boolean sameFileContents = !shouldCompareFileContents();
		if (!sameFileContents) {
			if (!shouldCompareFiles()) {
				// files have not been compared yet, so we do this first
				sameFiles = compareFiles(first, second);
				if (shouldFailFast() && !sameFiles)
					return false;
			}

			sameFileContents = compareFileContents(first, second, firstFileRoot, secondFileRoot);
			if (shouldFailFast() && !sameFileContents)
				return false;
		}

		boolean sameAddInfo = !shouldCompareAdditionalInfo() || compareAdditionalInfo(first, second);
		if (shouldFailFast() && !sameAddInfo)
			return false;

		return sameConfs && sameInfos && sameWarnings && sameNotices && sameFiles && sameFileContents && sameAddInfo;
	}

	/**
	 * Compares the configurations ({@link JsonReport#getConfiguration()}) of
	 * both reports, excluding the configuration keys returned by
	 * {@link #ignoredConfigurationKeys()}.
	 * 
	 * @param first  the first report
	 * @param second the second report
	 * 
	 * @return {@code true} if the configurations are equal, {@code false}
	 *             otherwise
	 */
	public boolean compareConfs(
			JsonReport first,
			JsonReport second) {
		return compareBags(
				REPORTED_COMPONENT.CONFIGURATION,
				first.getConfiguration(),
				second.getConfiguration(),
				(
						key,
						fvalue,
						svalue) -> configurationDiff(key, fvalue, svalue),
				key -> ignoredConfigurationKeys().contains(key));
	}

	/**
	 * Compares the run information ({@link JsonReport#getInfo()}) of both
	 * reports, excluding the run information keys returned by
	 * {@link #ignoredRunInfoKeys()}.
	 * 
	 * @param first  the first report
	 * @param second the second report
	 * 
	 * @return {@code true} if the run information is equal, {@code false}
	 *             otherwise
	 */
	public boolean compareInfos(
			JsonReport first,
			JsonReport second) {
		return compareBags(
				REPORTED_COMPONENT.INFO,
				first.getInfo(),
				second.getInfo(),
				(
						key,
						fvalue,
						svalue) -> infoDiff(key, fvalue, svalue),
				key -> ignoredRunInfoKeys().contains(key));
	}

	private boolean compareBags(
			REPORTED_COMPONENT component,
			Map<String, String> first,
			Map<String, String> second,
			TriConsumer<String, String, String> reporter,
			Predicate<String> ignore) {
		CollectionsDiffBuilder<String> builder = new CollectionsDiffBuilder<>(String.class, first.keySet(),
				second.keySet());
		builder.compute(String::compareTo);

		if (!builder.getOnlyFirst().isEmpty())
			report(component, REPORT_TYPE.ONLY_FIRST, builder.getOnlyFirst());
		if (!builder.getOnlySecond().isEmpty())
			report(component, REPORT_TYPE.ONLY_SECOND, builder.getOnlySecond());

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
			report(component, REPORT_TYPE.COMMON, same);

		return builder.sameContent() && same.size() == builder.getCommons().size();
	}

	/**
	 * Compares the warnings ({@link JsonReport#getWarnings()}) of both reports,
	 * relying on the {@link Comparable#compareTo(Object)} method.
	 * 
	 * @param first  the first report
	 * @param second the second report
	 * 
	 * @return {@code true} if the warnings are equal, {@code false} otherwise
	 */
	public boolean compareWarnings(
			JsonReport first,
			JsonReport second) {
		CollectionsDiffBuilder<JsonMessage> warnings = new CollectionsDiffBuilder<>(
				JsonMessage.class,
				first.getWarnings(),
				second.getWarnings());
		warnings.compute(JsonMessage::compareTo);

		if (!warnings.getCommons().isEmpty())
			report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.COMMON, warnings.getCommons());
		if (!warnings.getOnlyFirst().isEmpty())
			report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_FIRST, warnings.getOnlyFirst());
		if (!warnings.getOnlySecond().isEmpty())
			report(REPORTED_COMPONENT.WARNINGS, REPORT_TYPE.ONLY_SECOND, warnings.getOnlySecond());
		return warnings.sameContent();
	}

	/**
	 * Compares the notices ({@link JsonReport#getNotices()}) of both reports,
	 * relying on the {@link Comparable#compareTo(Object)} method.
	 * 
	 * @param first  the first report
	 * @param second the second report
	 * 
	 * @return {@code true} if the notices are equal, {@code false} otherwise
	 */
	public boolean compareNotices(
			JsonReport first,
			JsonReport second) {
		CollectionsDiffBuilder<JsonMessage> notices = new CollectionsDiffBuilder<>(
				JsonMessage.class,
				first.getNotices(),
				second.getNotices());
		notices.compute(JsonMessage::compareTo);

		if (!notices.getCommons().isEmpty())
			report(REPORTED_COMPONENT.NOTICES, REPORT_TYPE.COMMON, notices.getCommons());
		if (!notices.getOnlyFirst().isEmpty())
			report(REPORTED_COMPONENT.NOTICES, REPORT_TYPE.ONLY_FIRST, notices.getOnlyFirst());
		if (!notices.getOnlySecond().isEmpty())
			report(REPORTED_COMPONENT.NOTICES, REPORT_TYPE.ONLY_SECOND, notices.getOnlySecond());
		return notices.sameContent();
	}

	/**
	 * Compares the lists of generated files ({@link JsonReport#getFiles()}) of
	 * both reports. This method is responsible only for checking if the reports
	 * contain the same generated files in terms of their names, without looking
	 * at their contents.
	 * 
	 * @param first  the first report
	 * @param second the second report
	 * 
	 * @return {@code true} if the files are equal, {@code false} otherwise
	 */
	public boolean compareFiles(
			JsonReport first,
			JsonReport second) {
		CollectionsDiffBuilder<String> files = new CollectionsDiffBuilder<>(String.class, first.getFiles(),
				second.getFiles());
		files.compute(String::compareTo);

		if (!files.getCommons().isEmpty())
			report(REPORTED_COMPONENT.FILES, REPORT_TYPE.COMMON, files.getCommons());
		if (!files.getOnlyFirst().isEmpty())
			report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_FIRST, files.getOnlyFirst());
		if (!files.getOnlySecond().isEmpty())
			report(REPORTED_COMPONENT.FILES, REPORT_TYPE.ONLY_SECOND, files.getOnlySecond());
		return files.sameContent();
	}

	/**
	 * Compares the contents of the common files in the two reports. This method
	 * should ignore files that are present in only one report. The type of
	 * comparison depends on the file type:
	 * <ul>
	 * <li>the JSON report itself is ignored;</li>
	 * <li>JSON graphs are compared through
	 * {@link #matchJsonGraphs(File, File)};</li>
	 * <li>visualization files (according to
	 * {@link #isVisualizationFile(String)}) are ignored;</li>
	 * <li>all other files are compared through
	 * {@link #customFileCompare(File, File)}.</li>
	 * </ul>
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
	 * @return {@code true} if the contents of all common files are equal,
	 *             {@code false} otherwise
	 * 
	 * @throws FileNotFoundException if a file declared in the reports is not
	 *                                   found
	 * @throws IOException           if an I/O error occurs while opening the
	 *                                   file
	 */
	public boolean compareFileContents(
			JsonReport first,
			JsonReport second,
			File firstFileRoot,
			File secondFileRoot)
			throws FileNotFoundException,
			IOException {
		CollectionsDiffBuilder<String> files = new CollectionsDiffBuilder<>(String.class, first.getFiles(),
				second.getFiles());
		files.compute(String::compareTo);
		boolean diffFound = false;
		for (Pair<String, String> pair : files.getCommons()) {
			File left = new File(firstFileRoot, pair.getLeft());
			if (!left.exists())
				throw new FileNotFoundException(format(MISSING_FILE, pair.getLeft(), "first"));

			File right = new File(secondFileRoot, pair.getRight());
			if (!right.exists())
				throw new FileNotFoundException(format(MISSING_FILE, pair.getRight(), "second"));

			String path = left.getName();
			if (FilenameUtils.getName(path).equals(JSONReportDumper.REPORT_NAME))
				continue;

			if (isJsonGraph(path))
				diffFound |= matchJsonGraphs(left, right);
			else if (FilenameUtils.getName(path).equals(TracingListener.TRACE_FNAME))
				diffFound |= matchTraceFiles(left, right);
			else if (isVisualizationFile(path))
				LOG.info(VIS_ONLY, left.toString(), right.toString());
			else
				diffFound |= customFileCompare(left, right);
		}
		return !diffFound;
	}

	/**
	 * Compares two JSON graph files for structural and content differences. The
	 * comparison tries to be as thorough as possible:
	 * <ul>
	 * <li>if the name, description, or other metadata are different, the
	 * comparison aborts early reporting a difference;</li>
	 * <li>structural differences (extra or removed nodes/edges, etc.) are then
	 * searched, and reported if found;</li>
	 * <li>differences in node labels are explored at last, reporting every
	 * differing field.</li>
	 * </ul>
	 * 
	 * @param left  the first JSON graph file
	 * @param right the second JSON graph file
	 * 
	 * @return {@code true} if the graphs are equivalent, {@code false}
	 *             otherwise
	 * 
	 * @throws IOException           if an I/O error occurs
	 * @throws FileNotFoundException if a file is not found
	 */
	public boolean matchJsonGraphs(
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
					if (!Objects.equals(leftGraph.getName(), rightGraph.getName())
							|| !Objects.equals(leftGraph.getDescription(), rightGraph.getDescription()))
						fileDiff(leftpath, rightpath, GRAPH_DIFF);
					else
						structuralDiff(leftGraph, rightGraph, leftpath, rightpath);
				else {
					CollectionsDiffBuilder<SerializableNodeDescription> builder = new CollectionsDiffBuilder<>(
							SerializableNodeDescription.class,
							leftGraph.getDescriptions(),
							rightGraph.getDescriptions());
					builder.compute(SerializableNodeDescription::compareTo);

					if (builder.sameContent())
						fileDiff(leftpath, rightpath, MALFORMED_GRAPH);
					else
						compareLabels(leftGraph, rightGraph, leftpath, rightpath, builder);
				}
			}
		}

		return diffFound;
	}

	private void structuralDiff(
			SerializableGraph leftGraph,
			SerializableGraph rightGraph,
			String leftpath,
			String rightpath) {
		CollectionsDiffBuilder<SerializableNode> nodeBuilder = new CollectionsDiffBuilder<>(
				SerializableNode.class,
				leftGraph.getNodes(),
				rightGraph.getNodes());
		nodeBuilder.compute(SerializableNode::compareTo);

		Collection<SerializableNode> onlyFirst = new TreeSet<>();
		Collection<SerializableNode> onlySecond = new TreeSet<>();
		Map<SerializableNode, SerializableNode> renamings = new TreeMap<>();
		findRenamings(leftGraph, rightGraph, nodeBuilder, onlyFirst, onlySecond, renamings);

		if (!renamings.isEmpty())
			fileDiff(
					leftpath,
					rightpath,
					"Nodes that changed offsets:\n\t"
							+ StringUtils.join(
									renamings.keySet().stream().map(SerializableNode::getText)
											.collect(Collectors.toList()),
									"\n\t"));
		if (!onlyFirst.isEmpty())
			fileDiff(
					leftpath,
					rightpath,
					"Nodes only in first graph:\n\t"
							+ StringUtils.join(onlyFirst.stream().map(n -> enrich(n)).collect(Collectors.toList()),
									"\n\t"));
		if (!onlySecond.isEmpty())
			fileDiff(
					leftpath,
					rightpath,
					"Nodes only in second graph:\n\t"
							+ StringUtils.join(onlySecond.stream().map(n -> enrich(n)).collect(Collectors.toList()),
									"\n\t"));

		CollectionsDiffBuilder<SerializableEdge> edgeBuilder = new CollectionsDiffBuilder<>(
				SerializableEdge.class,
				leftGraph.getEdges(),
				rightGraph.getEdges());
		edgeBuilder.compute(SerializableEdge::compareTo);

		Collection<SerializableEdge> onlyFirstEdges = new TreeSet<>();
		Collection<SerializableEdge> onlySecondEdges = new TreeSet<>();
		Map<SerializableEdge, SerializableEdge> renamingsEdges = new TreeMap<>();

		for (SerializableEdge edge : edgeBuilder.getOnlyFirst())
			for (SerializableEdge other : edgeBuilder.getOnlySecond())
				if (edge.equalsUpToIds(other, leftGraph.getNodes(), rightGraph.getNodes()))
					renamingsEdges.put(edge, other);
		edgeBuilder.getOnlyFirst()
				.stream()
				.filter(e -> !renamingsEdges.containsKey(e))
				.forEach(onlyFirstEdges::add);
		edgeBuilder.getOnlySecond()
				.stream()
				.filter(e -> !renamingsEdges.containsValue(e))
				.forEach(onlySecondEdges::add);

		if (!renamingsEdges.isEmpty())
			fileDiff(
					leftpath,
					rightpath,
					"Edges whose endpoints changed offsets:\n\t"
							+ StringUtils.join(
									renamingsEdges.keySet().stream().map(e -> enrich(e, leftGraph))
											.collect(Collectors.toList()),
									"\n\t"));
		if (!onlyFirstEdges.isEmpty())
			fileDiff(
					leftpath,
					rightpath,
					"Edges only in first graph:\n\t"
							+ StringUtils.join(
									onlyFirstEdges.stream().map(e -> enrich(e, leftGraph)).collect(Collectors.toList()),
									"\n\t"));
		if (!onlySecondEdges.isEmpty())
			fileDiff(
					leftpath,
					rightpath,
					"Edges only in second graph:\n\t"
							+ StringUtils.join(
									onlySecondEdges.stream().map(e -> enrich(e, rightGraph))
											.collect(Collectors.toList()),
									"\n\t"));
	}

	private void findRenamings(
			SerializableGraph leftGraph,
			SerializableGraph rightGraph,
			CollectionsDiffBuilder<SerializableNode> builder,
			Collection<SerializableNode> onlyFirst,
			Collection<SerializableNode> onlySecond,
			Map<SerializableNode, SerializableNode> renamings) {
		List<SerializableNode> firstSorted = new ArrayList<>(builder.getOnlyFirst());
		firstSorted.sort(
				(
						f,
						s) -> f.getText().compareTo(s.getText()));
		Iterator<SerializableNode> ol = firstSorted.iterator();
		List<SerializableNode> secondSorted = new ArrayList<>(builder.getOnlySecond());
		secondSorted.sort(
				(
						f,
						s) -> f.getText().compareTo(s.getText()));
		Iterator<SerializableNode> or = secondSorted.iterator();

		SerializableNode currentF = null;
		SerializableNode currentS = null;

		while (ol.hasNext() || or.hasNext() || currentF != null || currentS != null) {
			if (ol.hasNext() && currentF == null)
				currentF = ol.next();
			if (or.hasNext() && currentS == null)
				currentS = or.next();

			if (currentF == null)
				if (currentS == null)
					break;
				else {
					onlySecond.add(currentS);
					currentS = null;
					continue;
				}
			else if (currentS == null) {
				onlyFirst.add(currentF);
				currentF = null;
				continue;
			}

			int cmp = currentF.getText().compareTo(currentS.getText());
			if (currentF.equalsUpToIds(currentS, leftGraph.getNodes(), rightGraph.getNodes())) {
				renamings.put(currentF, currentS);
				currentF = null;
				currentS = null;
			} else if (cmp == 0) {
				onlyFirst.add(currentF);
				onlySecond.add(currentS);
				currentF = null;
				currentS = null;
			} else if (cmp < 0) {
				onlyFirst.add(currentF);
				currentF = null;
			} else {
				onlySecond.add(currentS);
				currentS = null;
			}
		}
	}

	private String enrich(
			SerializableNode node) {
		return node.getText()
				+ "\t(id: "
				+ node.getId()
				+ (node.getSubNodes() == null || node.getSubNodes().isEmpty() ? ""
						: ", subnodes: " + node.getSubNodes())
				+ ")";
	}

	private String enrich(
			SerializableEdge edge,
			SerializableGraph graph) {
		SerializableNode source = graph.getNodeById(edge.getSourceId());
		SerializableNode target = graph.getNodeById(edge.getDestId());
		return source.getText()
				+ " ---("
				+ edge.getKind()
				+ ")"
				+ (edge.getLabel() != null ? edge.getLabel() : "")
				+ "---> "
				+ target.getText();
	}

	private void compareLabels(
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

		while (ol.hasNext() && or.hasNext() || currentF != null || currentS != null) {
			if (ol.hasNext() && currentF == null)
				currentF = ol.next();
			if (or.hasNext() && currentS == null)
				currentS = or.next();

			if (currentF == null)
				if (currentS == null)
					break;
				else {
					fileDiff(
							leftpath,
							rightpath,
							format(NO_DESC, "First", currentS.getNodeId(), rlabels.get(currentS.getNodeId())));
					currentS = null;
					continue;
				}
			else if (currentS == null) {
				fileDiff(
						leftpath,
						rightpath,
						format(NO_DESC, "Second", currentF.getNodeId(), llabels.get(currentF.getNodeId())));
				currentF = null;
				continue;
			}

			int fid = currentF.getNodeId();
			int sid = currentS.getNodeId();
			if (fid == sid) {
				if (verboseLabelDiff())
					fileDiff(
							leftpath,
							rightpath,
							format(
									DESC_DIFF_VERBOSE,
									currentF.getNodeId(),
									llabels.get(currentF.getNodeId()),
									diff(currentF.getDescription(), currentS.getDescription())));
				else
					fileDiff(
							leftpath,
							rightpath,
							format(DESC_DIFF, currentF.getNodeId(), llabels.get(currentF.getNodeId())));
				currentF = null;
				currentS = null;
			} else if (fid < sid) {
				fileDiff(
						leftpath,
						rightpath,
						format(NO_DESC, "Second", currentF.getNodeId(), llabels.get(currentF.getNodeId())));
				currentF = null;
			} else {
				fileDiff(
						leftpath,
						rightpath,
						format(NO_DESC, "First", currentS.getNodeId(), rlabels.get(currentS.getNodeId())));
				currentS = null;
			}
		}
	}

	private static String diff(
			SerializableValue first,
			SerializableValue second) {
		StringBuilder builder = new StringBuilder();
		diff(0, builder, first, second);
		// this removes empty/whitespace lines in the middle
		return builder.toString().replaceAll("(?m)^[ \t]*\r?\n", "").trim();
	}

	private static boolean diff(
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

	private static boolean diff(
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

	private static boolean diff(
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
			builder.append("\t".repeat(depth)).append(">ACTUAL HAS ").append(ssize - min).append(" MORE ELEMENT(S):\n");
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

	private static boolean diff(
			int depth,
			StringBuilder builder,
			SerializableObject first,
			SerializableObject second) {
		SortedMap<String, SerializableValue> felements = first.getFields();
		SortedMap<String, SerializableValue> selements = second.getFields();

		CollectionsDiffBuilder<String> diff = new CollectionsDiffBuilder<>(String.class, felements.keySet(),
				selements.keySet());
		diff.compute(String::compareTo);

		boolean atLeastOne = false;
		StringBuilder inner;
		for (Pair<String, String> field : diff.getCommons())
			if (diff(
					depth + 1,
					inner = new StringBuilder(),
					felements.get(field.getLeft()),
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

	private static void fillWithDiff(
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

	/**
	 * Compares two trace files line by line, ignoring the time taken to
	 * complete each traced action.
	 * 
	 * @param left  the first trace file
	 * @param right the second trace file
	 * 
	 * @return {@code true} if the trace files are equal, {@code false}
	 *             otherwise
	 * 
	 * @throws IOException if an I/O error occurs
	 */
	public boolean matchTraceFiles(
			File left,
			File right)
			throws IOException {
		boolean diffFound = false;

		try (BufferedReader l = new BufferedReader(
				new InputStreamReader(
						new FileInputStream(left),
						StandardCharsets.UTF_8));
				BufferedReader r = new BufferedReader(
						new InputStreamReader(
								new FileInputStream(right),
								StandardCharsets.UTF_8))) {
			String lineL;
			String lineR;
			int lineNum = 1;
			while ((lineL = l.readLine()) != null & (lineR = r.readLine()) != null) {
				if (!lineL.equals(lineR)) {
					diffFound = true;
					fileDiff(
							left.toString(),
							right.toString(),
							format(TRACE_DIFF, lineNum, lineL, lineR));
				}
				lineNum++;
			}
			while ((lineL = l.readLine()) != null) {
				diffFound = true;
				fileDiff(
						left.toString(),
						right.toString(),
						format(TRACE_DIFF, lineNum, lineL, "<no line>"));
				lineNum++;
			}
			while ((lineR = r.readLine()) != null) {
				diffFound = true;
				fileDiff(
						left.toString(),
						right.toString(),
						format(TRACE_DIFF, lineNum, "<no line>", lineR));
				lineNum++;
			}
		}

		return diffFound;
	}

	/**
	 * Compares the additional info ({@link JsonReport#getAdditionalInfo()}) of
	 * both reports.
	 * 
	 * @param first  the first report
	 * @param second the second report
	 * 
	 * @return {@code true} if the additional info is equal, {@code false}
	 *             otherwise
	 */
	public boolean compareAdditionalInfo(
			JsonReport first,
			JsonReport second) {
		if (first.getAdditionalInfo() == null && second.getAdditionalInfo() == null)
			return true;

		if (first.getAdditionalInfo() == null) {
			report(
					REPORTED_COMPONENT.ADDITIONAL_INFO,
					REPORT_TYPE.ONLY_SECOND,
					Collections.singleton(second.getAdditionalInfo()));
			return false;
		}

		if (second.getAdditionalInfo() == null) {
			report(
					REPORTED_COMPONENT.ADDITIONAL_INFO,
					REPORT_TYPE.ONLY_FIRST,
					Collections.singleton(first.getAdditionalInfo()));
			return false;
		}

		if (first.getAdditionalInfo().equals(second.getAdditionalInfo()))
			return true;

		additionalInfoDiff(diff(first.getAdditionalInfo(), second.getAdditionalInfo()));

		return false;
	}

	/**
	 * Reports a difference in one of the components of the reports.
	 * 
	 * @param component the {@link REPORTED_COMPONENT} where the difference was
	 *                      found
	 * @param type      the {@link REPORT_TYPE} indicating what is the
	 *                      difference being reported
	 * @param reported  the collection of elements that are part of the report
	 */
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
		case NOTICES:
			if (isFirst)
				LOG.warn(NOTICES_ONLY, "first");
			else
				LOG.warn(NOTICES_ONLY, "second");
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
		case ADDITIONAL_INFO:
			if (isFirst)
				LOG.warn(ADDITIONAL_INFO_ONLY, "first");
			else
				LOG.warn(ADDITIONAL_INFO_ONLY, "second");
			break;
		default:
			break;
		}

		for (Object o : reported)
			LOG.warn("\t" + o);
	}

	/**
	 * Callback invoked whenever files from different reports but with matching
	 * names have different content.
	 * 
	 * @param first   the complete (i.e. path + file name) pathname of the first
	 *                    file
	 * @param second  the complete (i.e. path + file name) pathname of the
	 *                    second file
	 * @param message the message reporting the difference
	 */
	public void fileDiff(
			String first,
			String second,
			String message) {
		String fname = FilenameUtils.getName(first);
		LOG.warn(FILE_DIFF, fname, message);
	}

	/**
	 * Callback invoked whenever an analysis information key is mapped to two
	 * different values.
	 * 
	 * @param key    the information key
	 * @param first  the value in the first report
	 * @param second the value in the second report
	 */
	public void infoDiff(
			String key,
			String first,
			String second) {
		LOG.warn(VALUE_DIFF, "run info", key, first, second);
	}

	/**
	 * Callback invoked whenever a configuration key is mapped to two different
	 * values.
	 * 
	 * @param key    the configuration key
	 * @param first  the value in the first report
	 * @param second the value in the second report
	 */
	public void configurationDiff(
			String key,
			String first,
			String second) {
		LOG.warn(VALUE_DIFF, "configuration", key, first, second);
	}

	/**
	 * Callback invoked whenever an additional info's field is mapped to two
	 * different values.
	 * 
	 * @param message the message that reports the difference
	 */
	public void additionalInfoDiff(
			String message) {
		LOG.warn(ADD_INFO_DIFF, message);
	}

	/**
	 * If {@code true}, analysis configurations
	 * ({@link JsonReport#getConfiguration()}) will be compared.
	 * 
	 * @return whether or not configurations should be compared
	 */
	public boolean shouldCompareConfigurations() {
		return true;
	}

	/**
	 * If {@code true}, run information ({@link JsonReport#getInfo()}) will be
	 * compared.
	 * 
	 * @return whether or not run informations should be compared
	 */
	public boolean shouldCompareRunInfos() {
		return true;
	}

	/**
	 * If {@code true}, warnings ({@link JsonReport#getWarnings()}) will be
	 * compared.
	 * 
	 * @return whether or not warnings should be compared
	 */
	public boolean shouldCompareWarnings() {
		return true;
	}

	/**
	 * If {@code true}, notices ({@link JsonReport#getNotices()}) will be
	 * compared.
	 * 
	 * @return whether or not notices should be compared
	 */
	public boolean shouldCompareNotices() {
		return true;
	}

	/**
	 * If {@code true}, files ({@link JsonReport#getFiles()}) will be compared.
	 * 
	 * @return whether or not files should be compared
	 */
	public boolean shouldCompareFiles() {
		return true;
	}

	/**
	 * If {@code true}, content of files produced by both analyses will be
	 * compared.
	 * 
	 * @return whether or not file contents should be compared
	 */
	public boolean shouldCompareFileContents() {
		return true;
	}

	/**
	 * If {@code true}, additional info ({@link JsonReport#getAdditionalInfo()})
	 * will be compared.
	 * 
	 * @return whether or not additional info should be compared
	 */
	public boolean shouldCompareAdditionalInfo() {
		return true;
	}

	/**
	 * If {@code true}, comparison will halt after the first failed category
	 * (warnings, files, ...) without reporting the full diff.
	 * 
	 * @return whether or not comparison should fail fast
	 */
	public boolean shouldFailFast() {
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
	public boolean isJsonGraph(
			String path) {
		return path.endsWith(".graph.json");
	}

	/**
	 * Yields whether or not the file pointed by the given path is for
	 * visualizing results and can thus be skipped as its content depends solely
	 * on other files.
	 * 
	 * @param path the path pointing to the file
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isVisualizationFile(
			String path) {
		String ext = FilenameUtils.getExtension(path);
		return ext.equals("dot")
				|| ext.equals("graphml")
				|| ext.equals("png")
				|| ext.equals("html")
				|| ext.equals("js")
				|| ext.equals("css");
	}

	/**
	 * Custom comparison method for files that are neither json graphs nor
	 * visualization-only files. By default, this method raises an
	 * {@link UnsupportedOperationException} at every call.
	 * 
	 * @param expected the file from the expected report
	 * @param actual   the file from the actual report
	 * 
	 * @return whether or not a difference was found in the files
	 * 
	 * @throws UnsupportedOperationException if file comparisons are not
	 *                                           supported for the given file
	 *                                           type
	 */
	public boolean customFileCompare(
			File expected,
			File actual) {
		throw new UnsupportedOperationException(format(CANNOT_COMPARE, expected.toString(), actual.toString()));
	}

	/**
	 * If {@code true}, the full diff between two labels will be reported when a
	 * difference is found. Otherwise, a simple message is reported.
	 * 
	 * @return whether or not label diff reporting should be verbose
	 */
	public boolean verboseLabelDiff() {
		return true;
	}

	/**
	 * Yields the keys in {@link JsonReport#getConfiguration()} that should be
	 * ignored when comparing configurations. By default, no key should be
	 * ignored.
	 * 
	 * @return the keys to ignore
	 */
	public Collection<String> ignoredConfigurationKeys() {
		return Set.of();
	}

	/**
	 * Yields the keys in {@link JsonReport#getInfo()} that should be ignored
	 * when comparing run info. By default, the following keys are ignored:
	 * <ul>
	 * <li>duration</li>
	 * <li>start</li>
	 * <li>end</li>
	 * <li>version</li>
	 * </ul>
	 * 
	 * @return the keys to ignore
	 */
	public Collection<String> ignoredRunInfoKeys() {
		return Set.of("duration", "start", "end", "version");
	}

}
