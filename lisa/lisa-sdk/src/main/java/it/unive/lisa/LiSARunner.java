package it.unive.lisa;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.checks.ChecksExecutor;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.events.EventListener;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.file.FileManager;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An auxiliary analysis runner for executing LiSA analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class LiSARunner<A extends AbstractLattice<A>, D extends AbstractDomain<A>> {

	private static final Logger LOG = LogManager.getLogger(LiSARunner.class);

	private final LiSAConfiguration conf;

	private final FileManager fileManager;

	private final InterproceduralAnalysis<A, D> interproc;

	private final Analysis<A, D> analysis;

	private final CallGraph callGraph;

	/**
	 * Builds the runner.
	 * 
	 * @param conf        the configuration of the analysis
	 * @param fileManager the file manager for the analysis
	 * @param interproc   the interprocedural analysis to use
	 * @param callGraph   the call graph to use
	 * @param analysis    the analysis to run
	 */
	LiSARunner(
			LiSAConfiguration conf,
			FileManager fileManager,
			InterproceduralAnalysis<A, D> interproc,
			CallGraph callGraph,
			Analysis<A, D> analysis) {
		this.conf = conf;
		this.fileManager = fileManager;
		this.interproc = interproc;
		this.callGraph = callGraph;
		this.analysis = analysis;
	}

	/**
	 * Executes the runner on the target program.
	 * 
	 * @param app the application to analyze
	 * 
	 * @return the tool used to run the checks, that contains all warnings and
	 *             notices
	 */
	CheckTool run(
			Application app) {
		finalize(app);

		Collection<CFG> allCFGs = app.getAllCFGs();
		FixpointConfiguration<A, D> fixconf = new FixpointConfiguration<>(conf);
		CheckTool tool = new CheckTool(conf, fileManager);

		if (fixconf.usesOptimizedForwardFixpoint() || fixconf.usesOptimizedBackwardFixpoint())
			allCFGs.forEach(CFG::computeBasicBlocks);

		if (!conf.syntacticChecks.isEmpty())
			ChecksExecutor.executeAll(tool, app, conf.syntacticChecks);
		else
			LOG.warn("Skipping syntactic checks execution since none have been provided");

		if (canAnalyze()) {
			EventQueue events;
			if (conf.synchronousListeners.isEmpty() && conf.asynchronousListeners.isEmpty())
				// to avoid unnecessary overhead in calling posting events, we
				// set this to null and require null checks during the analysis
				events = null;
			else
				events = new EventQueue(conf.synchronousListeners, conf.asynchronousListeners, tool);

			init(app, events);

			analyze(fixconf, tool);

			Map<CFG, Collection<AnalyzedCFG<A>>> results = new IdentityHashMap<>(allCFGs.size());
			for (CFG cfg : allCFGs)
				results.put(cfg, interproc.getAnalysisResultsOf(cfg));

			CheckToolWithAnalysisResults<A, D> tool2 = new CheckToolWithAnalysisResults<>(
					tool,
					results,
					callGraph,
					analysis);

			@SuppressWarnings({ "rawtypes", "unchecked" })
			Collection<SemanticCheck<A, D>> semanticChecks = (Collection) conf.semanticChecks;
			if (!semanticChecks.isEmpty())
				ChecksExecutor.executeAll(tool2, app, semanticChecks);
			else
				LOG.warn("Skipping semantic checks execution since none have been provided");

			tool = tool2;
		}

		return tool;
	}

	private void finalize(
			Application app) {
		for (Program p : app.getPrograms()) {
			TypeSystem types = p.getTypes();
			// make sure the basic types are registered
			types.registerType(types.getBooleanType());
			types.registerType(types.getStringType());
			types.registerType(types.getIntegerType());
			for (Type t : types.getTypes())
				if (types.canBeReferenced(t))
					types.registerType(types.getReference(t));

			TimerLogger.execAction(LOG, "Finalizing input program", () -> {
				try {
					p.getFeatures().getProgramValidationLogic().validateAndFinalize(p);
				} catch (ProgramValidationException e) {
					throw new AnalysisExecutionException("Unable to finalize target program", e);
				}
			});
		}
	}

	private void init(
			Application app,
			EventQueue events) {
		analysis.setEventQueue(events);

		try {
			callGraph.init(app, events);
		} catch (CallGraphConstructionException e) {
			LOG.fatal("Exception while building the call graph for the input program", e);
			throw new AnalysisSetupException("Exception while building the call graph for the input program", e);
		}

		try {
			interproc.init(app, callGraph, conf.openCallPolicy, events, analysis);
		} catch (InterproceduralAnalysisException e) {
			LOG.fatal("Exception while building the interprocedural analysis for the input program", e);
			throw new AnalysisSetupException(
					"Exception while building the interprocedural analysis for the input program",
					e);
		}
	}

	private boolean canAnalyze() {
		if (interproc == null) {
			LOG.warn("Skipping analysis execution since no interprocedural analysis has been provided");
			return false;
		}

		if (callGraph == null && interproc.needsCallGraph())
			throw new AnalysisSetupException(
					"The provided interprocedural analysis needs a call graph to function, but none has been provided");

		if (analysis == null) {
			LOG.warn("Skipping analysis execution since no analysis has been provided");
			return false;
		}

		return true;
	}

	private void analyze(
			FixpointConfiguration<A, D> fixconf,
			CheckTool tool) {
		TimerLogger.execAction(LOG, "Initializing event listeners", () -> {
			for (EventListener listener : conf.synchronousListeners)
				listener.beforeExecution(tool);
			for (EventListener listener : conf.asynchronousListeners)
				listener.beforeExecution(tool);
		});

		AnalysisState<A> state = this.analysis.makeLattice();
		TimerLogger.execAction(LOG, "Computing fixpoint over the whole program", () -> {
			try {
				interproc.fixpoint(state, fixconf);
			} catch (FixpointException e) {
				LOG.fatal("Exception during fixpoint computation", e);
				throw new AnalysisExecutionException("Exception during fixpoint computation", e);
			}
		});

		TimerLogger.execAction(LOG, "Shutting down event listeners", () -> {
			for (EventListener listener : conf.synchronousListeners)
				listener.afterExecution(tool);
			for (EventListener listener : conf.asynchronousListeners)
				listener.afterExecution(tool);
		});
	}

}
