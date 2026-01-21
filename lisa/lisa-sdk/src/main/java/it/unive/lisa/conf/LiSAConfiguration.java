package it.unive.lisa.conf;

import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.events.EventListener;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.TopExecutionPolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.logging.Log4jConfig;
import it.unive.lisa.outputs.LiSAOutput;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowExtractor;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.fixpoints.AnalysisFixpoint;
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardAscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardAscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.CollectionUtilities;
import it.unive.lisa.util.collections.workset.OrderBasedWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint;
import it.unive.lisa.util.file.FileManager;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
	 * The collection of {@link LiSAOutput}s to produce at the end of the
	 * analysis. Defaults to an empty set.
	 */
	public Collection<LiSAOutput> outputs = new HashSet<>();

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
	 * The {@link ForwardCFGFixpoint} to use for forward fixpoint iterations
	 * over individual {@link CFG}s. Defaults to
	 * {@link ForwardAscendingFixpoint}.
	 */
	public ForwardCFGFixpoint<?, ?> forwardFixpoint = new ForwardAscendingFixpoint<>();

	/**
	 * The {@link ForwardCFGFixpoint} to use for the descending phase of forward
	 * fixpoint iterations over individual {@link CFG}s. Defaults to
	 * {@code null}, meaning that no forward descending phase should be run.
	 */
	public ForwardCFGFixpoint<?, ?> forwardDescendingFixpoint = null;

	/**
	 * The {@link BackwardCFGFixpoint} to use for backward fixpoint iterations
	 * over individual {@link CFG}s. Defaults to
	 * {@link ForwardAscendingFixpoint}.
	 */
	public BackwardCFGFixpoint<?, ?> backwardFixpoint = new BackwardAscendingFixpoint<>();

	/**
	 * The {@link BackwardCFGFixpoint} to use for the descending phase of
	 * backward fixpoint iterations over individual {@link CFG}s. Defaults to
	 * {@code null}, meaning that no backward descending phase should be run.
	 */
	public BackwardCFGFixpoint<?, ?> backwardDescendingFixpoint = null;

	/**
	 * The {@link WorkingSet} to be used in fixpoints. Note that the instance
	 * passed to this field is used as a factory to create new working sets
	 * through {@link WorkingSet#mk()}, and its contents are thus ignored.
	 * Defaults to {@link OrderBasedWorkingSet}.
	 */
	public WorkingSet<Statement> fixpointWorkingSet = new OrderBasedWorkingSet();

	/**
	 * The {@link OpenCallPolicy} to be used for computing the result of
	 * {@link OpenCall}s. Defaults to {@link TopExecutionPolicy}.
	 */
	public OpenCallPolicy openCallPolicy = TopExecutionPolicy.INSTANCE;

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
	 * When an optimized fixpoint is used (i.e., when invocations of
	 * {@link AnalysisFixpoint#isOptimized()} on {@link #forwardFixpoint},
	 * {@link #forwardDescendingFixpoint}, {@link #backwardFixpoint}, or
	 * {@link #backwardDescendingFixpoint} yields {@code true}), this predicate
	 * will be used to determine additional statements (also considering
	 * intermediate ones) for which the fixpoint results must be kept. This is
	 * useful for avoiding result unwinding due to {@link SemanticCheck}s
	 * querying for the post-state of statements. Note that statements for which
	 * {@link Statement#stopsExecution()} is {@code true} are always considered
	 * hotspots.
	 */
	public Predicate<Statement> hotspots = null;

	/**
	 * When an optimized fixpoint is used (i.e., when invocations of
	 * {@link AnalysisFixpoint#isOptimized()} on {@link #forwardFixpoint},
	 * {@link #forwardDescendingFixpoint}, {@link #backwardFixpoint}, or
	 * {@link #backwardDescendingFixpoint} yields {@code true}), this field
	 * controls whether or not optimized results are automatically unwinded
	 * before dumping them to output files. Note that, if this field is
	 * {@code false} and an optimized fixpoint is used, the post-state of every
	 * node that is not a widening point or that is not matched by the
	 * fixpoint's custom hotspots predicate will appear as bottom states.
	 * Defaults to {@code false}.
	 */
	public boolean dumpForcesUnwinding = false;

	/**
	 * This predicate determines if an error of a given type should have its
	 * separate entry in the {@link AnalysisState} errors, or if it should be
	 * "smashed" into the summary error state. All smashed errors share a unique
	 * {@link ProgramState}, as they are deemed as mostly noise or
	 * uninteresting. Defaults to {@code null}, meaning that no error is
	 * smashed.
	 */
	public Predicate<Type> shouldSmashError = null;

	/**
	 * The collection of synchronous {@link EventListener}s to register with the
	 * analysis' event queue. These listeners will be executed in the same
	 * thread as the analysis itself. The order of execution of the listeners
	 * preservers the insertion order into this collection. Synchronous
	 * listeners are executed before asynchronous ones. Defaults to an empty
	 * list.
	 */
	public final List<EventListener> synchronousListeners = new LinkedList<>();

	/**
	 * The collection of asynchronous {@link EventListener}s to register with
	 * the analysis' event queue. These listeners will be executed in a separate
	 * thread, and will thus not block the analysis. The order of execution of
	 * the listeners preservers the insertion order into this collection.
	 * Synchronous listeners are executed before asynchronous ones. Defaults to
	 * an empty list.
	 */
	public final List<EventListener> asynchronousListeners = new LinkedList<>();

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
					} else if (WorkingSet.class.isAssignableFrom(field.getType())
							|| OpenCallPolicy.class.isAssignableFrom(field.getType())
							|| Fixpoint.class.isAssignableFrom(field.getType()))
						res.append(": ").append(value == null ? "unset" : value.getClass().getSimpleName());
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
					else if (WorkingSet.class.isAssignableFrom(field.getType())
							|| OpenCallPolicy.class.isAssignableFrom(field.getType())
							|| Fixpoint.class.isAssignableFrom(field.getType()))
						val = value == null ? "unset" : value.getClass().getSimpleName();
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
