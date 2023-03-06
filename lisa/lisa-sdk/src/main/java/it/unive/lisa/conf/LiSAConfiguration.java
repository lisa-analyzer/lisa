package it.unive.lisa.conf;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.LiSA;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.WorstCasePolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.file.FileManager;

/**
 * A holder for the configuration of a {@link LiSA} analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LiSAConfiguration extends BaseConfiguration {

	/**
	 * The type of graphs that can be dumped by LiSA.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static enum GraphType {

		/**
		 * No graphs are dumped.
		 */
		NONE,

		/**
		 * Graphs are dumped as an html page using javascript to visualize the
		 * graphs. Only root-level nodes are included in the graph: to get a
		 * complete graph with subn-odes, use {@link #HTML_WITH_SUBNODES}.
		 */
		HTML,

		/**
		 * Graphs are dumped as an html page using javascript to visualize the
		 * graphs. All nodes, including sub-nodes, are part of the visualized,
		 * creating a compound graph. Note: graphs generated with this option
		 * are big: files will have larger dimension and the viewer will be
		 * slower. For a lighter alternative, use {@link #HTML}.
		 */
		HTML_WITH_SUBNODES,

		/**
		 * Graphs are dumped in Dot format.
		 */
		DOT,

		/**
		 * Graphs are dumped in compound GraphML format. Only root-level nodes
		 * are included in the graph: to get a complete graph with sub-nodes,
		 * use {@link #GRAPHML_WITH_SUBNODES}.
		 */
		GRAPHML,

		/**
		 * Graphs are dumped in compound GraphML format.All nodes, including
		 * sub-nodes, are part of the graph, creating a compound graph. Note:
		 * graphs generated with this option are big: files will have larger
		 * dimension and the viewer will be slower. For a lighter alternative,
		 * use {@link #GRAPHML}.
		 */
		GRAPHML_WITH_SUBNODES;
	}

	/**
	 * The type of descending fixpoint phase algorithms that can be used.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static enum DescendingPhaseType {

		/**
		 * The descending phase is not computed.
		 */
		NONE,

		/**
		 * The descending phase is performed by applying the glb k-times.
		 */
		GLB,

		/**
		 * The descending phase always uses the narrowing operator.
		 */
		NARROWING;
	}

	/**
	 * The default number of fixpoint iteration on a given node after which
	 * calls to {@link Lattice#lub(Lattice)} gets replaced with
	 * {@link Lattice#widening(Lattice)}.
	 */
	public static final int DEFAULT_WIDENING_THRESHOLD = 5;

	/**
	 * The default number of maximum time glb can be called on a node during the
	 * descending phase of fixpoint algorithm.
	 */
	public static final int DEFAULT_DESCENDING_GLB_THRESHOLD = 5;

	/**
	 * The collection of {@link SyntacticCheck}s to execute. These checks will
	 * be immediately executed after LiSA is started, as they do not require any
	 * semantic information. Defaults to an empty set.
	 */
	public final Collection<SyntacticCheck> syntacticChecks = new HashSet<>();

	/**
	 * The collection of {@link SemanticCheck}s to execute. These will be
	 * executed after the fixpoint iteration has been completed, and will be
	 * provided with the computed fixpoint results (customizable through
	 * {@link #abstractState}) and the {@link CallGraph} (that can be customized
	 * through {@link #callGraph}) that has been built. Defaults to an empty
	 * set.
	 */
	public final Collection<SemanticCheck<?, ?, ?, ?>> semanticChecks = new HashSet<>();

	/**
	 * The {@link CallGraph} instance to use during the analysis. Defaults to
	 * {@code null}. Setting this field is optional: if an analysis is to be
	 * executed (that is, if {@link #abstractState} has been set), a default
	 * {@link CallGraph} instance will be created through
	 * {@link LiSAFactory#getDefaultFor(Class, Object...)} in order to perform
	 * the analysis.
	 */
	public CallGraph callGraph;

	/**
	 * The {@link InterproceduralAnalysis} instance to use during the analysis.
	 * Defaults to {@code null}. Setting this field is optional: if an analysis
	 * is to be executed (that is, if {@link #abstractState} has been set), a
	 * default {@link InterproceduralAnalysis} instance will be created through
	 * {@link LiSAFactory#getDefaultFor(Class, Object...)} in order to perform
	 * the analysis.
	 */
	public InterproceduralAnalysis<?, ?, ?, ?> interproceduralAnalysis;

	/**
	 * The {@link AbstractState} instance to run during the analysis. This will
	 * be used as singleton to retrieve the top instances needed to boot up the
	 * analysis, and can thus be any lattice element. If no value is set for
	 * this field, no analysis will be executed. Defaults to {@code null}.
	 */
	public AbstractState<?, ?, ?, ?> abstractState;

	/**
	 * Sets the format to use for dumping graph files, named
	 * {@code <cfg signature>[optional numeric hash].<format>}, in the working
	 * directory at the end of the analysis. These files will contain a graph
	 * representing each input {@link CFG}s' structure, and whose nodes will
	 * contain a representation of the results of the semantic analysis on each
	 * {@link Statement}. To customize where the graphs should be generated, use
	 * {@link #workdir}. Defaults to {@link GraphType#NONE} (that is, no graphs
	 * will be dumped).
	 */
	public GraphType analysisGraphs = GraphType.NONE;

	/**
	 * Whether or not the inputs {@link CFG}s to the analysis should be dumped
	 * in json format before the analysis starts. Graph files are named
	 * {@code <cfg signature>_cfg.json}, and are dumped in the path pointed to
	 * by {@link #workdir}. If {@link #analysisGraphs} is not set to
	 * {@link GraphType#NONE}, inputs will also be dumped using the format
	 * specified by that field. Defaults to {@code false}.
	 */
	public boolean serializeInputs;

	/**
	 * Whether or not the results of the analysis (if executed) should be dumped
	 * in json format. Graph files are named
	 * {@code <cfg signature>[optional numeric hash]json}, and are dumped in the
	 * path pointed to by {@link #workdir}. If {@link #analysisGraphs} is not
	 * set to {@link GraphType#NONE}, results will also be dumped using the
	 * format specified by that field. Defaults to {@code false}.
	 */
	public boolean serializeResults;

	/**
	 * Sets whether or not a json report file, named {@code report.json}, should
	 * be created and dumped in the working directory at the end of the
	 * analysis. This file will contain all the {@link Warning}s that have been
	 * generated, as well as a list of produced files. To customize where the
	 * report should be generated, use {@link #workdir}. Defaults to
	 * {@code false}.
	 */
	public boolean jsonOutput;

	/**
	 * The working directory for this instance of LiSA, that is, the directory
	 * files will be created, if any (log files excluded, use the logging
	 * configuration for controlling where those are placed). The value of this
	 * field is passed to the {@link FileManager} instance of the analysis.
	 * Defaults to the directory where LiSA was run from.
	 */
	public String workdir = Paths.get(".").toAbsolutePath().normalize().toString();

	/**
	 * The number of fixpoint iteration on a given node after which calls to
	 * {@link Lattice#lub(Lattice)} gets replaced with
	 * {@link Lattice#widening(Lattice)}. Defaults to
	 * {@link #DEFAULT_WIDENING_THRESHOLD}.
	 */
	public int wideningThreshold = DEFAULT_WIDENING_THRESHOLD;

	/**
	 * The number of fixpoint iteration on a given node after which calls to
	 * {@link Lattice#lub(Lattice)} gets replaced with
	 * {@link Lattice#widening(Lattice)}. Defaults to
	 * {@link #DEFAULT_WIDENING_THRESHOLD}.
	 */
	public int glbThreshold = DEFAULT_WIDENING_THRESHOLD;

	/**
	 * the type of descending phase that will be applied by the fixpoint
	 * algorithm.
	 */
	public DescendingPhaseType descendingPhaseType = DescendingPhaseType.NONE;

	/**
	 * The concrete class of {@link WorkingSet} to be used in fixpoints.
	 * Defaults to {@link FIFOWorkingSet}.
	 */
	public Class<?> fixpointWorkingSet = FIFOWorkingSet.class;

	/**
	 * The {@link OpenCallPolicy} to be used for computing the result of
	 * {@link OpenCall}s. Defaults to {@link WorstCasePolicy}.
	 */
	public OpenCallPolicy openCallPolicy = WorstCasePolicy.INSTANCE;

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("LiSA configuration:");
		try {
			for (Field field : LiSAConfiguration.class.getFields())
				if (!Modifier.isStatic(field.getModifiers())
						// we skip the semantic configuration
						&& !AbstractState.class.isAssignableFrom(field.getType())
						&& !CallGraph.class.isAssignableFrom(field.getType())
						&& !InterproceduralAnalysis.class.isAssignableFrom(field.getType())) {
					Object value = field.get(this);

					res.append("\n  ").append(field.getName());

					if (Collection.class.isAssignableFrom(field.getType())) {
						Collection<?> coll = (Collection<?>) value;
						res.append(" (").append(coll.size()).append(")").append((coll.isEmpty() ? "" : ":"));
						for (Object element : coll)
							res.append("\n    ").append(element.getClass().getSimpleName());
					} else if (Class.class.isAssignableFrom(field.getType()))
						res.append(": ").append(((Class<?>) value).getSimpleName());
					else if (OpenCallPolicy.class.isAssignableFrom(field.getType()))
						res.append(": ").append(((OpenCallPolicy) value).getClass().getSimpleName());
					else
						res.append(": ").append(String.valueOf(value));
				}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("Cannot access one of this class' public fields", e);
		}
		return res.toString();
	}

	/**
	 * Converts this configuration to a property bag, that is, a map from keys
	 * (fields of this class) to values (their values). {@link #abstractState},
	 * {@link #callGraph}, and {@link #interproceduralAnalysis} are omitted.
	 * 
	 * @return the property bag
	 */
	public Map<String, String> toPropertyBag() {
		Map<String, String> bag = new TreeMap<>();
		try {
			for (Field field : LiSAConfiguration.class.getFields())
				if (!Modifier.isStatic(field.getModifiers())
						// we skip the semantic configuration
						&& !AbstractState.class.isAssignableFrom(field.getType())
						&& !CallGraph.class.isAssignableFrom(field.getType())
						&& !InterproceduralAnalysis.class.isAssignableFrom(field.getType())) {
					Object value = field.get(this);

					String key = field.getName();

					String val;
					if (Collection.class.isAssignableFrom(field.getType()))
						val = StringUtils.join(((Collection<?>) value).stream().map(e -> e.getClass().getSimpleName())
								.sorted().collect(Collectors.toList()), ", ");
					else if (Class.class.isAssignableFrom(field.getType()))
						val = ((Class<?>) value).getSimpleName();
					else if (OpenCallPolicy.class.isAssignableFrom(field.getType()))
						val = ((OpenCallPolicy) value).getClass().getSimpleName();
					else
						val = String.valueOf(value);
					bag.put(key, val);
				}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("Cannot access one of this class' public fields", e);
		}

		// we force unix separators to have a uniform representation of
		// that works across different machines
		bag.put("workdir", FilenameUtils.separatorsToUnix(workdir));

		return bag;
	}
}