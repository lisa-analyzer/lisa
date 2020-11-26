package it.unive.lisa;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.callgraph.CallGraph;
import it.unive.lisa.analysis.callgraph.intraproc.IntraproceduralCallGraph;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.ValueEnvironment;
import it.unive.lisa.analysis.nonrelational.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.NonRelationalValueDomain;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.FixpointException;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.checks.syntactic.SyntacticChecksExecutor;
import it.unive.lisa.checks.warnings.Warning;
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
	 * The collection of warnings that will be filled with the results of all the
	 * executed checks
	 */
	private final Collection<Warning> warnings;

	/**
	 * The callgraph to use during the analysis
	 */
	private CallGraph callGraph;

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
		// since the warnings collection will be filled AFTER the execution of every
		// concurrent bit has completed its execution, it is fine to use a non
		// thread-safe one
		this.warnings = new ArrayList<>();
		this.valueDomains = new ArrayList<>();
		this.heapDomains = new ArrayList<>();
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
	public void setCallGraph(CallGraph callGraph) {
		this.callGraph = callGraph;
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
	 * Adds a new {@link NonRelationalHeapDomain} to execute during the analysis.
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
	 * Adds a new {@link NonRelationalValueDomain} to execute during the analysis.
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
		log.info("  " + inputs.size() + " CFGs to analyze");
		log.info("  " + syntacticChecks.size() + " syntactic checks to execute"
				+ (syntacticChecks.isEmpty() ? "" : ":"));
		for (SyntacticCheck check : syntacticChecks)
			log.info("      " + check.getClass().getSimpleName());
		log.info("  call graph implementation: "
				+ (callGraph == null ? "none provided" : callGraph.getClass().getSimpleName()));
		log.info("  " + heapDomains.size() + " heap domains to execute" + (heapDomains.isEmpty() ? "" : ":"));
		for (HeapDomain<?> domain : heapDomains)
			log.info("      " + domain.getClass().getSimpleName());
		log.info("  " + valueDomains.size() + " value domains to execute" + (valueDomains.isEmpty() ? "" : ":"));
		for (ValueDomain<?> domain : valueDomains)
			log.info("      " + domain.getClass().getSimpleName());
	}

	private void printStats() {
		log.info("LiSA statistics:");
		log.info("  " + warnings.size() + " warnings generated");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void runAux() throws AnalysisExecutionException {
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

		// to proceed, at least one domain must have been specified
		if (valueDomains.isEmpty() && heapDomains.isEmpty()) {
			log.warn("Skipping analysis execution since no abstract domains have been provided");
			return;
		}

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
		} else if (valueDomains.isEmpty()) {
			// TODO we should have a base analysis that can serve as default
			log.warn("Skipping analysis execution since no abstract domains have been provided");
			return;
		}

		HeapDomain heap = heapDomains.iterator().next();
		ValueDomain value = valueDomains.iterator().next();

		try {
			callGraph.fixpoint(new AnalysisState(new AbstractState((HeapDomain) heap.top(), (ValueDomain) value.top()),
					new Skip()));
		} catch (FixpointException e) {
			log.fatal("Exception during fixpoint computation", e);
			throw new AnalysisExecutionException("Exception during fixpoint computation", e);
		}

		for (CFG cfg : inputs) {
			CFGWithAnalysisResults<?, ?> result = callGraph.getAnalysisResultsOf(cfg);
			try (Writer file = FileManager.mkDotFile(cfg.getDescriptor().getFullSignature())) {
				result.dump(file, cfg.getDescriptor().getFullSignature(),
						st -> result.getAnalysisStateAt(st).toString());
			} catch (IOException e) {
				log.error("Exception while dumping the analysis results on " + cfg.getDescriptor().getFullSignature(),
						e);
			}
		}
	}

	/**
	 * Yields an unmodifiable view of the warnings that have been generated during
	 * the analysis. Invoking this method before invoking {@link #run()} will return
	 * an empty collection.
	 * 
	 * @return a view of the generated warnings
	 */
	public Collection<Warning> getWarnings() {
		return Collections.unmodifiableCollection(warnings);
	}
}
