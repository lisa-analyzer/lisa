package it.unive.lisa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.checks.syntactic.SyntacticChecksExecutor;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.logging.TimerLogger;

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
	 * Builds a new LiSA instance.
	 */
	public LiSA() {
		this.inputs = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.syntacticChecks = Collections.newSetFromMap(new ConcurrentHashMap<>());
		// since the warnings collection will be filled AFTER the execution of every
		// concurrent bit has completed its execution, it is fine to use a non
		// thread-safe one
		this.warnings = new ArrayList<>();
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
	 */
	public void run() {
		printConfig();
		TimerLogger.execAction(log, "Analysis time", () -> runAux());
		printStats();
	}
	
	private void printConfig() {
		log.info("LiSA setup:");
		log.info("  " + inputs.size() + " CFGs to analyze");
		log.info("  " + syntacticChecks.size() + " syntactic checks to execute");
	}

	private void printStats() {
		log.info("LiSA statistics:");
		log.info("  " + warnings.size() + " warnings generated");
	}

	private void runAux() {
		CheckTool tool = new CheckTool();
		SyntacticChecksExecutor.executeAll(tool, inputs, syntacticChecks);
		warnings.addAll(tool.getWarnings());
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
