package it.unive.lisa;

import java.io.File;
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

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.ValueEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.callgraph.impl.intraproc.IntraproceduralCallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFG.SemanticFunction;
import it.unive.lisa.cfg.FixpointException;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.checks.syntactic.SyntacticChecksExecutor;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.util.file.FileManager;

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
	 * The collection of CFG instances that are to be analyzed
	 */
	private final Collection<CFG> inputs;

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
	 * The workdir that LiSA should use as root for all generated files (log
	 * files excluded, use the logging configuration for controlling where those
	 * are placed).
	 */
	private String workdir;

	/**
	 * The value domains to run during the analysis
	 */
	private final Collection<ValueDomain<?>> valueDomains;

	/**
	 * The heap domains to run during the analysis
	 */
	private final Collection<HeapDomain<?>> heapDomains;

	/**
	 * Builds a new LiSA instance.
	 */
	public LiSA() {
		this.inputs = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.syntacticChecks = Collections.newSetFromMap(new ConcurrentHashMap<>());
		// since the warnings collection will be filled AFTER the execution of
		// every
		// concurrent bit has completed its execution, it is fine to use a non
		// thread-safe one
		this.warnings = new ArrayList<>();
		this.valueDomains = new ArrayList<>();
		this.heapDomains = new ArrayList<>();
		this.inferTypes = false;
		this.dumpCFGs = false;
		this.dumpTypeInference = false;
		this.dumpAnalysis = false;
		this.workdir = Paths.get(".").toAbsolutePath().normalize().toString();
	}

	/**
	 * Adds the given cfg to the ones under analysis.
	 * 
	 * @param cfg the cfg to analyze
	 */
	public void addCFG(CFG cfg) {
		inputs.add(cfg);
	}

	/**
	 * Adds the given cfgs to the ones under analysis.
	 * 
	 * @param cfgs the cfgs to analyze
	 */
	public void addCFGs(Collection<CFG> cfgs) {
		inputs.addAll(cfgs);
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
	 * @param callGraph the callgraph to use
	 */
	public <T extends CallGraph> void setCallGraph(T callGraph) {
		this.callGraph = callGraph;
	}

	public void setInferTypes(boolean inferTypes) {
		this.inferTypes = inferTypes;
	}
	
	public void setDumpCFGs(boolean dumpCFGs) {
		this.dumpCFGs = dumpCFGs;
	}
	
	public void setDumpTypeInference(boolean dumpTypeInference) {
		this.dumpTypeInference = dumpTypeInference;
	}
	
	public void setDumpAnalysis(boolean dumpAnalysis) {
		this.dumpAnalysis = dumpAnalysis;
	}
	
	public void setWorkdir(String workdir) {
		this.workdir = Paths.get(workdir).toAbsolutePath().normalize().toString();
	}

	/**
	 * Adds a new {@link HeapDomain} to execute during the analysis.
	 * 
	 * @param <T>    the concrete instance of domain to add
	 * @param domain the domain to execute
	 */
	public <T extends HeapDomain<T>> void addHeapDomain(T domain) {
		this.heapDomains.add(domain);
	}

	/**
	 * Adds a new {@link NonRelationalHeapDomain} to execute during the
	 * analysis.
	 * 
	 * @param <T>    the concrete instance of domain to add
	 * @param domain the domain to execute
	 */
	public <T extends NonRelationalHeapDomain<T>> void addNonRelationalHeapDomain(T domain) {
		this.heapDomains.add(new HeapEnvironment<>(domain));
	}

	/**
	 * Adds a new {@link ValueDomain} to execute during the analysis.
	 * 
	 * @param <T>    the concrete instance of domain to add
	 * @param domain the domain to execute
	 */
	public <T extends ValueDomain<T>> void addValueDomain(T domain) {
		this.valueDomains.add(domain);
	}

	/**
	 * Adds a new {@link NonRelationalValueDomain} to execute during the
	 * analysis.
	 * 
	 * @param <T>    the concrete instance of domain to add
	 * @param domain the domain to execute
	 */
	public <T extends NonRelationalValueDomain<T>> void addNonRelationalValueDomain(T domain) {
		this.valueDomains.add(new ValueEnvironment<>(domain));
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
	}

	private void printConfig() {
		log.info("LiSA setup:");
		log.info("  workdir: " + String.valueOf(workdir));
		log.info("  " + inputs.size() + " CFGs to analyze");
		log.info("  dump input cfgs: " + dumpCFGs);
		log.info("  infer types: " + inferTypes);
		log.info("  dump inferred types: " + dumpTypeInference);
		log.info("  " + heapDomains.size() + " heap domains to execute");
		log.info("  " + valueDomains.size() + " value domains to execute");
		log.info("  dump analysis results: " + dumpAnalysis);
		log.info("  " + syntacticChecks.size() + " syntactic checks to execute"
				+ (syntacticChecks.isEmpty() ? "" : ":"));
		for (SyntacticCheck check : syntacticChecks)
			log.info("      " + check.getClass().getSimpleName());
	}

	private void printStats() {
		log.info("LiSA statistics:");
		log.info("  " + warnings.size() + " warnings generated");
	}

	@SuppressWarnings({ "unchecked" })
	private <H extends HeapDomain<H>, V extends ValueDomain<V>> void runAux() throws AnalysisExecutionException {
		FileManager.setWorkingDir(workdir);
		
		if (dumpCFGs)
			for (CFG cfg : IterationLogger.iterate(log, inputs, "Dumping input CFGs", "cfgs")) 
				dumpCFG("", cfg, st -> "");
		
		CheckTool tool = new CheckTool();
		if (!syntacticChecks.isEmpty()) {
			SyntacticChecksExecutor.executeAll(tool, inputs, syntacticChecks);
			warnings.addAll(tool.getWarnings());
		} else
			log.warn("Skipping syntactic checks execution since none have been provided");

		if (callGraph == null) {
			log.warn("No call graph set for this analysis, defaulting to a non-interprocedural implementation");
			callGraph = new IntraproceduralCallGraph();
		}

		inputs.forEach(callGraph::addCFG);

		// TODO we want to support these eventually
		if (valueDomains.size() > 1) {
			log.fatal("Analyses with a combination of value domains are not supported yet");
			throw new AnalysisExecutionException("Analyses with a combination of value domains are not supported yet");
		}

		// TODO we want to support these eventually
		if (heapDomains.size() > 1) {
			log.fatal("Analyses with a combination of heap domains are not supported yet");
			throw new AnalysisExecutionException("Analyses with a combination of heap domains are not supported yet");
		}

		if (heapDomains.isEmpty()) {
			log.warn("No heap domain has been set for this analysis, defaulting to a monolithic implementation");
			heapDomains.add(new MonolithicHeap());
		}

		H heap = (H) heapDomains.iterator().next();

		if (inferTypes) {
			TimerLogger.execAction(log, "Computing type information",
					() -> computeFixpoint(heap, new TypeEnvironment(), Statement::typeInference));
			
			if (dumpTypeInference)
				for (CFG cfg : IterationLogger.iterate(log, inputs, "Dumping type analysis", "cfgs")) {
					CFGWithAnalysisResults<?, ?> result = callGraph.getAnalysisResultsOf(cfg);
					dumpCFG("typing___", result, st -> result.getAnalysisStateAt(st).toString());
				}

			callGraph.clear();
		} else
			log.warn("No type domain provided: dynamic type information will not be available for following analyses");

		if (valueDomains.isEmpty()) {
			// TODO we should have a base analysis that can serve as default
			log.warn("Skipping analysis execution since no abstract domains have been provided");
			return;
		}

		V value = (V) valueDomains.iterator().next();
		TimerLogger.execAction(log, "Computing fixpoint over the whole program",
				() -> computeFixpoint(heap, value, Statement::semantics));

		if (dumpAnalysis)
			for (CFG cfg : IterationLogger.iterate(log, inputs, "Dumping analysis results", "cfgs")) {
				CFGWithAnalysisResults<?, ?> result = callGraph.getAnalysisResultsOf(cfg);
				dumpCFG("analysis___", result, st -> result.getAnalysisStateAt(st).toString());
			}
	}

	private void dumpCFG(String filePrefix, CFG cfg, Function<Statement, String> labelGenerator) {
		try (Writer file = FileManager.mkDotFile(filePrefix + cfg.getDescriptor().getFullSignature())) {
			cfg.dump(file, cfg.getDescriptor().getFullSignature(), st -> labelGenerator.apply(st));
		} catch (IOException e) {
			log.error("Exception while dumping the analysis results on " + cfg.getDescriptor().getFullSignature(),
					e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <H extends HeapDomain<H>, V extends ValueDomain<V>> void computeFixpoint(H heap, V value,
			SemanticFunction<H, V> semantics) {
		try {
			callGraph.fixpoint(new AnalysisState(new AbstractState(heap.top(), value.top()), new Skip()), semantics);
		} catch (FixpointException e) {
			log.fatal("Exception during fixpoint computation", e);
			throw new AnalysisExecutionException("Exception during fixpoint computation", e);
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
