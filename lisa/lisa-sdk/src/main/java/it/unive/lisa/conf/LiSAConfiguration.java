package it.unive.lisa.conf;

import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.WorstCasePolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.logging.Log4jConfig;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowExtractor;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.CollectionUtilities;
import it.unive.lisa.util.collections.workset.OrderBasedWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.file.FileManager;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.apache.commons.io.FilenameUtils;

/**
 * A holder for the configuration of a {@link LiSA} analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LiSAConfiguration
		extends
		BaseConfiguration {

	static {
		// ensure that some logging configuration is in place
		// if not, we set a default configuration
		if (!Log4jConfig.isLog4jConfigured())
			Log4jConfig.initializeLogging();
	}

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
		DOT;
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
	public static final int DEFAULT_GLB_THRESHOLD = 5;

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
	 * {@link #analysis}) and the {@link CallGraph} (that can be customized
	 * through {@link #callGraph}) that has been built. Defaults to an empty
	 * set.
	 */
	public final Collection<SemanticCheck<?, ?>> semanticChecks = new HashSet<>();

	/**
	 * The {@link CallGraph} instance to use during the analysis. Defaults to
	 * {@code null}.
	 */
	public CallGraph callGraph;

	/**
	 * The {@link InterproceduralAnalysis} instance to use during the analysis.
	 * Defaults to {@code null}.
	 */
	public InterproceduralAnalysis<?, ?> interproceduralAnalysis;

	/**
	 * The {@link AbstractDomain} to be run during the analysis. If no value is
	 * set for this field, no analysis will be executed. Defaults to
	 * {@code null}.
	 */
	public AbstractDomain<?> analysis;

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
	 * Sets whether or not a json report file, named {@value LiSA#REPORT_NAME},
	 * should be created and dumped in the working directory at the end of the
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
	 * {@link #DEFAULT_WIDENING_THRESHOLD}. Note that widening can be invoked
	 * only on widening points (that is, on loop guards) to reduce cost and
	 * increase precision by setting {@link #useWideningPoints} to {@code true}.
	 * Use 0 or a negative number to always apply lubs.
	 */
	public int wideningThreshold = DEFAULT_WIDENING_THRESHOLD;

	/**
	 * The number of fixpoint iteration over a recursive call chain after which
	 * calls to {@link Lattice#lub(Lattice)} gets replaced with
	 * {@link Lattice#widening(Lattice)} to stabilize the results. Defaults to
	 * {@link #DEFAULT_WIDENING_THRESHOLD}.
	 */
	public int recursionWideningThreshold = DEFAULT_WIDENING_THRESHOLD;

	/**
	 * The number of descending fixpoint iteration on a given node where
	 * {@link Lattice#glb(Lattice)} can be applied. After the threshold is
	 * reached, no more glbs will be applied on that node and the descending
	 * chain will stop. Defaults to {@link #DEFAULT_GLB_THRESHOLD}. Use 0 or a
	 * negative number to never apply glbs.
	 */
	public int glbThreshold = DEFAULT_GLB_THRESHOLD;

	/**
	 * the type of descending phase that will be applied by the fixpoint
	 * algorithm.
	 */
	public DescendingPhaseType descendingPhaseType = DescendingPhaseType.NONE;

	/**
	 * The concrete class of {@link WorkingSet} to be used in fixpoints.
	 * Defaults to {@link OrderBasedWorkingSet}.
	 */
	public Class<?> fixpointWorkingSet = OrderBasedWorkingSet.class;

	/**
	 * The {@link OpenCallPolicy} to be used for computing the result of
	 * {@link OpenCall}s. Defaults to {@link WorstCasePolicy}.
	 */
	public OpenCallPolicy openCallPolicy = WorstCasePolicy.INSTANCE;

	/**
	 * If {@code true}, will cause the analysis to optimize fixpoint executions.
	 * This means that (i) basic blocks will be computed for each cfg, (ii)
	 * fixpoint computations will discard post-states of statements that are not
	 * ending a basic block, (iii) after the fixpoint terminates, only the
	 * pre-state of the cfg entrypoints and the post-states of widening points
	 * will be stored, discarding everything else. When the pre- or post-state
	 * of a non-widening point is queried, a fast fixpoint iteration will be ran
	 * to unwind (that is, re-propagate) the results and compute the missing
	 * states. Note that results are <b>not</b> unwinded for dumping results.
	 * Defaults to {@code false}.
	 */
	public boolean optimize = false;

	/**
	 * If {@code true}, will cause fixpoint iterations to use widening (and
	 * narrowing) only on widening points, using lub (or glb) on all other nodes
	 * regardless of the threshold. As widening is typically more expensive,
	 * this reduces resource consumption. Note that widening points correspond
	 * to the conditions of loops, as identified by
	 * {@link CFG#getCycleEntries()}. The latter relies on the
	 * {@link ControlFlowStructure}s of each CFG (either provided during
	 * construction of the CFG itself or extracted by a
	 * {@link ControlFlowExtractor}): this option should thus be set to
	 * {@code false} whenever such structures are not available by-design.
	 * Defaults to {@code true}.
	 */
	public boolean useWideningPoints = true;

	/**
	 * When {@link #optimize} is {@code true}, this predicate will be used to
	 * determine additional statements (also considering intermediate ones) for
	 * which the fixpoint results must be kept. This is useful for avoiding
	 * result unwinding due to {@link SemanticCheck}s querying for the
	 * post-state of statements. Note that statements for which
	 * {@link Statement#stopsExecution()} is {@code true} are always considered
	 * hotspots.
	 */
	public Predicate<Statement> hotspots = null;

	/**
	 * When {@link #optimize} is {@code true}, this field controls whether or
	 * not optimized results are automatically unwinded before dumping them to
	 * output files. Note that, if this field is {@code false} and
	 * {@link #optimize} is {@code true}, the post-state of every node that is
	 * not a widening point or that is not matched by {@link #hotspots} will
	 * appear as bottom states. Defaults to {@code true}.
	 */
	public boolean dumpForcesUnwinding = false;

	/**
	 * This predicate determines if an exception of a given type should have its
	 * separate continuation in the {@link AnalysisState}, or if it should be
	 * "smashed" into the summary exception continuation. All smashed exceptions
	 * share a unique {@link ProgramState}, as they are deemed as mostly noise
	 * or uninteresting.
	 */
	public Predicate<Type> shouldSmashException = null;

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("LiSA configuration:");
		try {
			for (Field field : LiSAConfiguration.class.getFields())
				if (!Modifier.isStatic(field.getModifiers())
						// we skip the semantic configuration
						&& !AbstractDomain.class.isAssignableFrom(field.getType())
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
					else if (Predicate.class.isAssignableFrom(field.getType()))
						// not sure how we can get more details reliably
						res.append(": ").append(value == null ? "unset" : "set");
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
	 * (fields of this class) to values (their values). {@link #analysis},
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
						&& !AbstractDomain.class.isAssignableFrom(field.getType())
						&& !CallGraph.class.isAssignableFrom(field.getType())
						&& !InterproceduralAnalysis.class.isAssignableFrom(field.getType())) {
					Object value = field.get(this);

					String key = field.getName();

					String val;
					if (Collection.class.isAssignableFrom(field.getType()))
						val = ((Collection<?>) value).stream()
								.map(e -> e.getClass().getSimpleName())
								.sorted()
								.collect(new CollectionUtilities.StringCollector<>(", "));
					else if (Class.class.isAssignableFrom(field.getType()))
						val = ((Class<?>) value).getSimpleName();
					else if (OpenCallPolicy.class.isAssignableFrom(field.getType()))
						val = ((OpenCallPolicy) value).getClass().getSimpleName();
					else if (Predicate.class.isAssignableFrom(field.getType()))
						// not sure how we can get more details reliably
						val = value == null ? "unset" : "set";
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
