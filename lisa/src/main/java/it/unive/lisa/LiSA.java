package it.unive.lisa;

import static it.unive.lisa.LiSAFactory.getDefaultFor;
import static it.unive.lisa.LiSAFactory.getInstance;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.InferredTypes;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.callgraph.CallGraphConstructionException;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.checks.syntactic.SyntacticChecksExecutor;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the central class of the LiSA library. While LiSA's functionalities
 * can be extended by providing additional implementations for each component,
 * code executing LiSA should rely solely on this class to engage the analysis,
 * provide inputs to it and retrieve its results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LiSA {

	private static final Logger log = LogManager.getLogger(LiSA.class);

	/**
	 * The program to analyze
	 */
	private Program program;

	/**
	 * The collection of syntactic checks to execute
	 */
	private final Collection<SyntacticCheck> syntacticChecks;

	/**
	 * The collection of warnings that will be filled with the results of all
	 * the executed checks
	 */
	private final Collection<Warning> warnings;

	/**
	 * The callgraph to use during the analysis
	 */
	private CallGraph callGraph;

	/**
	 * The abstract state to run during the analysis
	 */
	private AbstractState<?, ?, ?> state;

	/**
	 * Whether or not type inference should be executed before the analysis
	 */
	private boolean inferTypes;

	/**
	 * Whether or not the input cfgs should be dumped to dot format. This is
	 * useful for checking if the inputs that reach LiSA are well formed.
	 */
	private boolean dumpCFGs;

	/**
	 * Whether or not the result of type inference should be dumped to dot
	 * format, if it is executed
	 */
	private boolean dumpTypeInference;

	/**
	 * Whether or not the result of the analysis should be dumped to dot format,
	 * if it is executed
	 */
	private boolean dumpAnalysis;

	/**
	 * Whether or not the warning list should be dumped to a json file
	 */
	private boolean jsonOutput;

	/**
	 * The workdir that LiSA should use as root for all generated files (log
	 * files excluded, use the logging configuration for controlling where those
	 * are placed).
	 */
	private String workdir;

	/**
	 * Builds a new LiSA instance.
	 */
	public LiSA() {
		this.syntacticChecks = Collections.newSetFromMap(new ConcurrentHashMap<>());
		// since the warnings collection will be filled AFTER the execution of
		// every concurrent bit has completed its execution, it is fine to use a
		// non thread-safe one
		this.warnings = new ArrayList<>();
		this.inferTypes = false;
		this.dumpCFGs = false;
		this.dumpTypeInference = false;
		this.dumpAnalysis = false;
		this.workdir = Paths.get(".").toAbsolutePath().normalize().toString();
	}

	/**
	 * Sets the program to analyze. Any previous value set through this method
	 * is lost.
	 * 
	 * @param program the program to analyze
	 */
	public void setProgram(Program program) {
		this.program = program;
	}

	/**
	 * Adds the given syntactic check to the ones that will be executed. These
	 * checks will be immediately executed after LiSA is started.
	 * 
	 * @param check the check to execute
	 */
	public void addSyntacticCheck(SyntacticCheck check) {
		syntacticChecks.add(check);
	}

	/**
	 * Sets the {@link CallGraph} to use for the analysis. Any existing value is
	 * overwritten.
	 * 
	 * @param <T>       the concrete type of the call graph
	 * @param callGraph the callgraph to use
	 */
	public <T extends CallGraph> void setCallGraph(T callGraph) {
		this.callGraph = callGraph;
	}

	/**
	 * Sets the {@link AbstractState} to use for the analysis. Any existing
	 * value is overwritten.
	 * 
	 * @param state the abstract state to use
	 */
	public void setAbstractState(AbstractState<?, ?, ?> state) {
		this.state = state;
	}

	/**
	 * Sets whether or not runtime types should be inferred before executing the
	 * semantic analysis. If type inference is not executed, the runtime types
	 * of expressions will correspond to their static type.
	 * 
	 * @param inferTypes if {@code true}, type inference will be ran before the
	 *                       semantic analysis
	 */
	public void setInferTypes(boolean inferTypes) {
		this.inferTypes = inferTypes;
	}

	/**
	 * Sets whether or not dot files, named {@code <cfg name>.dot}, should be
	 * created and dumped in the working directory at the start of the
	 * execution. These files will contain a dot graph representing the each
	 * input {@link CFG}s' structure.<br>
	 * <br>
	 * To customize where the graphs should be generated, use
	 * {@link #setWorkdir(String)}.
	 * 
	 * @param dumpCFGs if {@code true}, a dot graph will be generated before
	 *                     starting the analysis for each input cfg
	 */
	public void setDumpCFGs(boolean dumpCFGs) {
		this.dumpCFGs = dumpCFGs;
	}

	/**
	 * Sets whether or not dot files, named {@code typing__<cfg name>.dot},
	 * should be created and dumped in the working directory at the end of the
	 * type inference. These files will contain a dot graph representing the
	 * each input {@link CFG}s' structure, and whose nodes will contain a
	 * textual representation of the results of the type inference on each
	 * {@link Statement}.<br>
	 * <br>
	 * To decide whether or not the type inference should be executed, use
	 * {@link #setInferTypes(boolean)}.<br>
	 * <br>
	 * To customize where the graphs should be generated, use
	 * {@link #setWorkdir(String)}.
	 * 
	 * @param dumpTypeInference if {@code true}, a dot graph will be generated
	 *                              after the type inference for each input cfg
	 */
	public void setDumpTypeInference(boolean dumpTypeInference) {
		this.dumpTypeInference = dumpTypeInference;
	}

	/**
	 * Sets whether or not dot files, named {@code analysis__<cfg name>.dot},
	 * should be created and dumped in the working directory at the end of the
	 * analysis. These files will contain a dot graph representing the each
	 * input {@link CFG}s' structure, and whose nodes will contain a textual
	 * representation of the results of the semantic analysis on each
	 * {@link Statement}.<br>
	 * <br>
	 * To customize where the graphs should be generated, use
	 * {@link #setWorkdir(String)}.
	 * 
	 * @param dumpAnalysis if {@code true}, a dot graph will be generated after
	 *                         the semantic analysis for each input cfg
	 */
	public void setDumpAnalysis(boolean dumpAnalysis) {
		this.dumpAnalysis = dumpAnalysis;
	}

	/**
	 * Sets whether or not a json report file, named {@code report.json}, should
	 * be created and dumped in the working directory at the end of the
	 * analysis. This file will contain all the {@link Warning}s that have been
	 * generated, as well as a list of produced files.<br>
	 * <br>
	 * To customize where the report should be generated, use
	 * {@link #setWorkdir(String)}.
	 * 
	 * @param jsonOutput if {@code true}, a json report will be generated after
	 *                       the analysis
	 */
	public void setJsonOutput(boolean jsonOutput) {
		this.jsonOutput = jsonOutput;
	}

	/**
	 * Sets the working directory for this instance of LiSA, that is, the
	 * directory files will be created, if any. If files need to be created and
	 * this method has not been invoked, LiSA will create them in the directory
	 * where it was executed from.
	 * 
	 * @param workdir the path (relative or absolute) to the working directory
	 */
	public void setWorkdir(String workdir) {
		this.workdir = Paths.get(workdir).toAbsolutePath().normalize().toString();
	}

	/**
	 * Adds the given syntactic checks to the ones that will be executed. These
	 * checks will be immediately executed after LiSA is started.
	 * 
	 * @param checks the checks to execute
	 */
	public void addSyntacticChecks(Collection<SyntacticCheck> checks) {
		syntacticChecks.addAll(checks);
	}

	/**
	 * Runs LiSA, executing all the checks that have been added.
	 * 
	 * @throws AnalysisException if anything goes wrong during the analysis
	 */
	public void run() throws AnalysisException {
		printConfig();

		try {
			TimerLogger.execAction(log, "Analysis time", this::runAux);
		} catch (AnalysisExecutionException e) {
			throw new AnalysisException("LiSA has encountered an exception while executing the analysis", e);
		}

		printStats();

		if (jsonOutput) {
			log.info("Dumping reported warnings to 'report.json'");
			JsonReport report = new JsonReport(warnings, FileManager.createdFiles());
			try (Writer writer = FileManager.mkOutputFile("report.json")) {
				report.dump(writer);
				log.info("Report file dumped to report.json");
			} catch (IOException e) {
				log.error("Unable to dump report file", e);
			}
		}

		FileManager.clearCreatedFiles();
	}

	private void printConfig() {
		log.info("LiSA setup:");
		log.info("  workdir: " + String.valueOf(workdir));
		log.info("  dump input cfgs: " + dumpCFGs);
		log.info("  infer types: " + inferTypes);
		log.info("  dump inferred types: " + dumpTypeInference);
		log.info("  dump analysis results: " + dumpAnalysis);
		log.info("  " + syntacticChecks.size() + " syntactic checks to execute"
				+ (syntacticChecks.isEmpty() ? "" : ":"));
		for (SyntacticCheck check : syntacticChecks)
			log.info("      " + check.getClass().getSimpleName());
		log.info("  dump json report: " + jsonOutput);
	}

	private void printStats() {
		log.info("LiSA statistics:");
		log.info("  " + warnings.size() + " warnings generated");
	}

	@SuppressWarnings("unchecked")
	private <H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			A extends AbstractState<A, H, V>> void runAux()
					throws AnalysisExecutionException {
		// fill up the types cache by side effect on an external set
		ExternalSet<Type> types = Caches.types().mkEmptySet();
		program.getRegisteredTypes().forEach(types::add);
		types = null;

		FileManager.setWorkdir(workdir);
		Collection<CFG> allCFGs = program.getAllCFGs();

		TimerLogger.execAction(log, "Finalizing input program", () -> {
			try {
				program.validateAndFinalize();
			} catch (ProgramValidationException e) {
				throw new AnalysisExecutionException("Unable to finalize target program", e);
			}
		});

		if (dumpCFGs)
			for (CFG cfg : IterationLogger.iterate(log, allCFGs, "Dumping input CFGs", "cfgs"))
				dumpCFG("", cfg, st -> "");

		CheckTool tool = new CheckTool();
		if (!syntacticChecks.isEmpty()) {
			SyntacticChecksExecutor.executeAll(tool, allCFGs, syntacticChecks);
			warnings.addAll(tool.getWarnings());
		} else
			log.warn("Skipping syntactic checks execution since none have been provided");

		if (callGraph == null) {
			try {
				callGraph = getDefaultFor(CallGraph.class);
			} catch (AnalysisSetupException e) {
				throw new AnalysisExecutionException("Unable to create default call graph", e);
			}
			log.warn("No call graph set for this analysis, defaulting to " + callGraph.getClass().getSimpleName());
		}

		try {
			callGraph.build(program);
		} catch (CallGraphConstructionException e) {
			log.fatal("Exception while building the call graph for the input program", e);
			throw new AnalysisExecutionException("Exception while building the call graph for the input program", e);
		}

		if (inferTypes) {
			SimpleAbstractState<H, InferenceSystem<InferredTypes>> typesState;
			try {
				HeapDomain<?> heap;
				if (state != null)
					heap = state.getHeapState();
				else
					heap = getDefaultFor(HeapDomain.class);
				// type inference is executed with the simplest abstract state
				typesState = getInstance(SimpleAbstractState.class, heap, new InferenceSystem<>(new InferredTypes()))
						.top();
			} catch (AnalysisSetupException e) {
				throw new AnalysisExecutionException("Unable to itialize type inference", e);
			}

			TimerLogger.execAction(log, "Computing type information",
					() -> {
						try {
							callGraph.fixpoint(new AnalysisState<>(typesState, new Skip()));
						} catch (FixpointException e) {
							log.fatal("Exception during fixpoint computation", e);
							throw new AnalysisExecutionException("Exception during fixpoint computation", e);
						}
					});

			String message = dumpTypeInference ? "Dumping type analysis and propagating it to cfgs"
					: "Propagating type information to cfgs";
			for (CFG cfg : IterationLogger.iterate(log, allCFGs, message, "cfgs")) {
				CFGWithAnalysisResults<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
						InferenceSystem<InferredTypes>> result = callGraph.getAnalysisResultsOf(cfg);
				if (dumpTypeInference)
					dumpCFG("typing___", result, st -> result.getAnalysisStateAt(st).toString());
				cfg.accept(new TypesPropagator<>(), result);
			}

			callGraph.clear();
		} else
			log.warn("Type inference disabled: dynamic type information will not be available for following analysis");

		if (state == null) {
			log.warn("Skipping analysis execution since no abstract sate has been provided");
			return;
		}

		A state = (A) this.state.top();
		TimerLogger.execAction(log, "Computing fixpoint over the whole program",
				() -> {
					try {
						callGraph.fixpoint(new AnalysisState<>(state, new Skip()));
					} catch (FixpointException e) {
						log.fatal("Exception during fixpoint computation", e);
						throw new AnalysisExecutionException("Exception during fixpoint computation", e);
					}
				});

		if (dumpAnalysis)
			for (CFG cfg : IterationLogger.iterate(log, allCFGs, "Dumping analysis results", "cfgs")) {
				CFGWithAnalysisResults<A, H, V> result = callGraph.getAnalysisResultsOf(cfg);
				dumpCFG("analysis___", result, st -> result.getAnalysisStateAt(st).toString());
			}
	}

	private static class TypesPropagator<H extends HeapDomain<H>>
			implements GraphVisitor<CFG, Statement, Edge, CFGWithAnalysisResults<
					SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H, InferenceSystem<InferredTypes>>> {

		@Override
		public boolean visit(CFGWithAnalysisResults<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
				InferenceSystem<InferredTypes>> tool, CFG graph) {
			return true;
		}

		@Override
		public boolean visit(CFGWithAnalysisResults<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
				InferenceSystem<InferredTypes>> tool, CFG graph, Edge edge) {
			return true;
		}

		@Override
		public boolean visit(CFGWithAnalysisResults<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
				InferenceSystem<InferredTypes>> tool, CFG graph, Statement node) {
			if (node instanceof Expression) {
				((Expression) node).setRuntimeTypes(tool.getAnalysisStateAt(node).getState().getValueState()
						.getInferredValue().getRuntimeTypes());
			}
			return true;
		}
	}

	private void dumpCFG(String filePrefix, CFG cfg, Function<Statement, String> labelGenerator) {
		try (Writer file = FileManager.mkDotFile(filePrefix + cfg.getDescriptor().getFullSignatureWithParNames())) {
			cfg.dump(file, st -> labelGenerator.apply(st));
		} catch (IOException e) {
			log.error("Exception while dumping the analysis results on " + cfg.getDescriptor().getFullSignature(),
					e);
		}
	}

	/**
	 * Yields an unmodifiable view of the warnings that have been generated
	 * during the analysis. Invoking this method before invoking {@link #run()}
	 * will return an empty collection.
	 * 
	 * @return a view of the generated warnings
	 */
	public Collection<Warning> getWarnings() {
		return Collections.unmodifiableCollection(warnings);
	}
}
