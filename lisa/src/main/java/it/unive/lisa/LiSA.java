package it.unive.lisa;

import static it.unive.lisa.LiSAFactory.getDefaultFor;
import static it.unive.lisa.LiSAFactory.getInstance;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.types.InferredTypes;
import it.unive.lisa.analysis.inference.InferenceSystem;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.callgraph.CallGraphConstructionException;
import it.unive.lisa.checks.ChecksExecutor;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.syntactic.CheckTool;
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
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
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
	 * The collection of warnings that will be filled with the results of all
	 * the executed checks
	 */
	private final Collection<Warning> warnings;

	/**
	 * The {@link FileManager} instance that will be used during analyses
	 */
	private final FileManager fileManager;

	/**
	 * The {@link LiSAConfiguration} containing the settings of the analysis to
	 * run
	 */
	private final LiSAConfiguration conf;

	/**
	 * Builds a new LiSA instance.
	 * 
	 * @param conf the configuration of the analysis to run
	 */
	public LiSA(LiSAConfiguration conf) {
		// since the warnings collection will be filled AFTER the execution of
		// every concurrent bit has completed its execution, it is fine to use a
		// non thread-safe one
		this.warnings = new ArrayList<>();
		this.conf = conf;
		this.fileManager = new FileManager(conf.getWorkdir());
	}

	/**
	 * Runs LiSA, executing all the checks that have been added.
	 * 
	 * @param program the program to analyze
	 * 
	 * @throws AnalysisException if anything goes wrong during the analysis
	 */
	public void run(Program program) throws AnalysisException {
		printConfig();

		try {
			TimerLogger.execAction(log, "Analysis time", () -> runAux(program));
		} catch (AnalysisExecutionException e) {
			throw new AnalysisException("LiSA has encountered an exception while executing the analysis", e);
		}

		printStats();

		if (conf.isJsonOutput()) {
			log.info("Dumping reported warnings to 'report.json'");
			JsonReport report = new JsonReport(warnings, fileManager.createdFiles());
			try (Writer writer = fileManager.mkOutputFile("report.json")) {
				report.dump(writer);
				log.info("Report file dumped to report.json");
			} catch (IOException e) {
				log.error("Unable to dump report file", e);
			}
		}
	}

	private void printConfig() {
		log.info(conf.toString());
	}

	private void printStats() {
		log.info("LiSA statistics:");
		log.info("  " + warnings.size() + " warnings generated");
	}

	@SuppressWarnings("unchecked")
	private <H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			A extends AbstractState<A, H, V>> void runAux(Program program)
					throws AnalysisExecutionException {
		finalizeProgram(program);

		Collection<CFG> allCFGs = program.getAllCFGs();

		if (conf.isDumpCFGs())
			for (CFG cfg : IterationLogger.iterate(log, allCFGs, "Dumping input CFGs", "cfgs"))
				dumpCFG("", cfg, st -> "");

		CheckTool tool = new CheckTool();
		if (!conf.getSyntacticChecks().isEmpty())
			ChecksExecutor.executeAll(tool, program, conf.getSyntacticChecks());
		else
			log.warn("Skipping syntactic checks execution since none have been provided");

		CallGraph callGraph;
		try {
			callGraph = conf.getCallGraph() == null ? getDefaultFor(CallGraph.class) : conf.getCallGraph();
			if (conf.getCallGraph() == null)
				log.warn("No call graph set for this analysis, defaulting to " + callGraph.getClass().getSimpleName());
		} catch (AnalysisSetupException e) {
			throw new AnalysisExecutionException("Unable to create default call graph", e);
		}

		try {
			callGraph.build(program);
		} catch (CallGraphConstructionException e) {
			log.fatal("Exception while building the call graph for the input program", e);
			throw new AnalysisExecutionException("Exception while building the call graph for the input program", e);
		}

		if (conf.isInferTypes())
			inferTypes(allCFGs, callGraph);
		else
			log.warn("Type inference disabled: dynamic type information will not be available for following analysis");

		if (conf.getState() != null) {
			analyze(allCFGs, callGraph);
			Map<CFG, CFGWithAnalysisResults<A, H, V>> results = new IdentityHashMap<>(allCFGs.size());
			for (CFG cfg : allCFGs)
				results.put(cfg, callGraph.getAnalysisResultsOf(cfg));

			tool = new CheckToolWithAnalysisResults<>(tool, results);
			if (!conf.getSemanticChecks().isEmpty())
				ChecksExecutor.executeAll((CheckToolWithAnalysisResults<A, H, V>) tool, program,
						conf.getSemanticChecks());
			else
				log.warn("Skipping semantic checks execution since none have been provided");
		} else
			log.warn("Skipping analysis execution since no abstract sate has been provided");

		warnings.addAll(tool.getWarnings());
	}

	@SuppressWarnings("unchecked")
	private <A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>> void analyze(
			Collection<CFG> allCFGs, CallGraph callGraph) {
		A state = (A) conf.getState().top();
		TimerLogger.execAction(log, "Computing fixpoint over the whole program",
				() -> {
					try {
						callGraph.fixpoint(new AnalysisState<>(state, new Skip()));
					} catch (FixpointException e) {
						log.fatal("Exception during fixpoint computation", e);
						throw new AnalysisExecutionException("Exception during fixpoint computation", e);
					}
				});

		if (conf.isDumpAnalysis())
			for (CFG cfg : IterationLogger.iterate(log, allCFGs, "Dumping analysis results", "cfgs")) {
				CFGWithAnalysisResults<A, H, V> result = callGraph.getAnalysisResultsOf(cfg);
				dumpCFG("analysis___", result, st -> result.getAnalysisStateAt(st).toString());
			}
	}

	@SuppressWarnings("unchecked")
	private <H extends HeapDomain<H>> void inferTypes(Collection<CFG> allCFGs, CallGraph callGraph) {
		SimpleAbstractState<H, InferenceSystem<InferredTypes>> typesState;
		try {
			AbstractState<?, ?, ?> state = conf.getState();
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

		String message = conf.isDumpTypeInference() ? "Dumping type analysis and propagating it to cfgs"
				: "Propagating type information to cfgs";
		for (CFG cfg : IterationLogger.iterate(log, allCFGs, message, "cfgs")) {
			CFGWithAnalysisResults<SimpleAbstractState<H, InferenceSystem<InferredTypes>>, H,
					InferenceSystem<InferredTypes>> result = callGraph.getAnalysisResultsOf(cfg);
			if (conf.isDumpTypeInference())
				dumpCFG("typing___", result, st -> result.getAnalysisStateAt(st).toString());
			cfg.accept(new TypesPropagator<>(), result);
		}

		callGraph.clear();
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

	private void finalizeProgram(Program program) {
		// fill up the types cache by side effect on an external set
		Caches.types().clear();
		ExternalSet<Type> types = Caches.types().mkEmptySet();
		program.getRegisteredTypes().forEach(types::add);
		types = null;

		TimerLogger.execAction(log, "Finalizing input program", () -> {
			try {
				program.validateAndFinalize();
			} catch (ProgramValidationException e) {
				throw new AnalysisExecutionException("Unable to finalize target program", e);
			}
		});
	}

	private void dumpCFG(String filePrefix, CFG cfg, Function<Statement, String> labelGenerator) {
		try (Writer file = fileManager.mkDotFile(filePrefix + cfg.getDescriptor().getFullSignatureWithParNames())) {
			cfg.dump(file, st -> labelGenerator.apply(st));
		} catch (IOException e) {
			log.error("Exception while dumping the analysis results on " + cfg.getDescriptor().getFullSignature(),
					e);
		}
	}

	/**
	 * Yields an unmodifiable view of the warnings that have been generated
	 * during the analysis. Invoking this method before invoking
	 * {@link #run(Program)} will return an empty collection.
	 * 
	 * @return a view of the generated warnings
	 */
	public Collection<Warning> getWarnings() {
		return Collections.unmodifiableCollection(warnings);
	}
}
