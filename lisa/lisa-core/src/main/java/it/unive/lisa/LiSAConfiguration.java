package it.unive.lisa;

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
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A holder for the configuration of a {@link LiSA} analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LiSAConfiguration {

	/**
	 * The default number of fixpoint iteration on a given node after which
	 * calls to {@link Lattice#lub(Lattice)} gets replaced with
	 * {@link Lattice#widening(Lattice)}.
	 */
	public static final int DEFAULT_WIDENING_THRESHOLD = 5;

	/**
	 * The collection of syntactic checks to execute
	 */
	private final Collection<SyntacticCheck> syntacticChecks;

	/**
	 * The collection of semantic checks to execute
	 */
	private final Collection<SemanticCheck<?, ?, ?, ?>> semanticChecks;

	/**
	 * The callgraph to use during the analysis
	 */
	private CallGraph callGraph;

	/**
	 * The interprocedural analysis to use during the analysis
	 */
	private InterproceduralAnalysis<?, ?, ?, ?> interproceduralAnalysis;

	/**
	 * The abstract state to run during the analysis
	 */
	private AbstractState<?, ?, ?, ?> abstractState;

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
	 * Whether the output file should be in JSON format
	 */
	private boolean jsonFile;

	/**
	 * The workdir that LiSA should use as root for all generated files (log
	 * files excluded, use the logging configuration for controlling where those
	 * are placed).
	 */
	private String workdir;

	/**
	 * The number of fixpoint iteration on a given node after which calls to
	 * {@link Lattice#lub(Lattice)} gets replaced with
	 * {@link Lattice#widening(Lattice)}.
	 */
	private int wideningThreshold;

	/**
	 * The concrete class of {@link WorkingSet} to be used in fixpoints.
	 */
	private Class<?> fixpointWorkingSet;

	/**
	 * The {@link OpenCallPolicy} to be used for computing the result of
	 * {@link OpenCall}s.
	 */
	private OpenCallPolicy openCallPolicy;

	/**
	 * Builds a new configuration object, with default settings. By default:
	 * <ul>
	 * <li>no syntactic check is executed</li>
	 * <li>no {@link AbstractState} is set for the analysis</li>
	 * <li>no {@link CallGraph} is set for the analysis</li>
	 * <li>no {@link InterproceduralAnalysis} is set for the analysis</li>
	 * <li>the workdir is the one where LiSA was executed</li>
	 * <li>the input program will not be dumped</li>
	 * <li>no type inference will be run</li>
	 * <li>the type inference will not be dumped</li>
	 * <li>the results of the analysis will not be dumped</li>
	 * <li>the json report will not be dumped</li>
	 * <li>the default warning threshold ({@value #DEFAULT_WIDENING_THRESHOLD})
	 * will be used</li>
	 * <li>the open call policy used is {@link WorstCasePolicy}</li>
	 * </ul>
	 */
	public LiSAConfiguration() {
		this.syntacticChecks = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.semanticChecks = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.workdir = Paths.get(".").toAbsolutePath().normalize().toString();
		this.wideningThreshold = DEFAULT_WIDENING_THRESHOLD;
		this.fixpointWorkingSet = FIFOWorkingSet.class;
		this.openCallPolicy = WorstCasePolicy.INSTANCE;
		this.jsonFile = true;
	}

	/**
	 * Adds the given syntactic check to the ones that will be executed. These
	 * checks will be immediately executed after LiSA is started.
	 * 
	 * @param check the check to execute
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration addSyntacticCheck(SyntacticCheck check) {
		syntacticChecks.add(check);
		return this;
	}

	/**
	 * Adds the given syntactic checks to the ones that will be executed. These
	 * checks will be immediately executed after LiSA is started.
	 * 
	 * @param checks the checks to execute
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration addSyntacticChecks(Collection<SyntacticCheck> checks) {
		syntacticChecks.addAll(checks);
		return this;
	}

	/**
	 * Adds the given semantic check to the ones that will be executed. These
	 * checks will be executed after the fixpoint iteration has been completed.
	 * 
	 * @param check the check to execute
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration addSemanticCheck(SemanticCheck<?, ?, ?, ?> check) {
		semanticChecks.add(check);
		return this;
	}

	/**
	 * Adds the given semantic checks to the ones that will be executed. These
	 * checks will be executed after the fixpoint iteration has been completed.
	 * 
	 * @param checks the checks to execute
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration addSemanticChecks(Collection<SemanticCheck<?, ?, ?, ?>> checks) {
		semanticChecks.addAll(checks);
		return this;
	}

	/**
	 * Sets the {@link CallGraph} to use for the analysis. Any existing value is
	 * overwritten.
	 * 
	 * @param callGraph the callgraph to use
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setCallGraph(CallGraph callGraph) {
		this.callGraph = callGraph;
		return this;
	}

	/**
	 * Sets the {@link InterproceduralAnalysis} to use for the analysis. Any
	 * existing value is overwritten.
	 * 
	 * @param analysis the interprocedural analysis to use
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setInterproceduralAnalysis(InterproceduralAnalysis<?, ?, ?, ?> analysis) {
		this.interproceduralAnalysis = analysis;
		return this;
	}

	/**
	 * Sets the {@link AbstractState} to use for the analysis. Any existing
	 * value is overwritten.
	 * 
	 * @param state the abstract state to use
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setAbstractState(AbstractState<?, ?, ?, ?> state) {
		this.abstractState = state;
		return this;
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
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setDumpCFGs(boolean dumpCFGs) {
		this.dumpCFGs = dumpCFGs;
		return this;
	}

	/**
	 * Sets whether or not dot files, named {@code typing__<cfg name>.dot},
	 * should be created and dumped in the working directory at the end of the
	 * type inference. These files will contain a dot graph representing the
	 * each input {@link CFG}s' structure, and whose nodes will contain a
	 * textual representation of the results of the type inference on each
	 * {@link Statement}.<br>
	 * <br>
	 * To customize where the graphs should be generated, use
	 * {@link #setWorkdir(String)}.
	 * 
	 * @param dumpTypeInference if {@code true}, a dot graph will be generated
	 *                              after the type inference for each input cfg
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setDumpTypeInference(boolean dumpTypeInference) {
		this.dumpTypeInference = dumpTypeInference;
		return this;
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
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setDumpAnalysis(boolean dumpAnalysis) {
		this.dumpAnalysis = dumpAnalysis;
		return this;
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
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setJsonOutput(boolean jsonOutput) {
		this.jsonOutput = jsonOutput;
		return this;
	}

	public LiSAConfiguration setJsonFile(boolean jsonFile){
		this.jsonFile = jsonFile;
		return this;
	}

	/**
	 * Sets the working directory for this instance of LiSA, that is, the
	 * directory files will be created, if any. If files need to be created and
	 * this method has not been invoked, LiSA will create them in the directory
	 * where it was executed from.
	 * 
	 * @param workdir the path (relative or absolute) to the working directory
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setWorkdir(String workdir) {
		this.workdir = Paths.get(workdir).toAbsolutePath().normalize().toString();
		return this;
	}

	/**
	 * Sets the concrete class of {@link WorkingSet} to be used in fixpoints.
	 * 
	 * @param fixpointWorkingSet the class
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setFixpointWorkingSet(Class<? extends WorkingSet<Statement>> fixpointWorkingSet) {
		this.fixpointWorkingSet = fixpointWorkingSet;
		return this;
	}

	/**
	 * Sets the number of fixpoint iteration on a given node after which calls
	 * to {@link Lattice#lub(Lattice)} gets replaced with
	 * {@link Lattice#widening(Lattice)}.
	 * 
	 * @param wideningThreshold the threshold
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setWideningThreshold(int wideningThreshold) {
		this.wideningThreshold = wideningThreshold;
		return this;
	}

	/**
	 * Sets the {@link OpenCallPolicy} to use during the analysis.
	 * 
	 * @param openCallPolicy the policy to use
	 * 
	 * @return the current (modified) configuration
	 */
	public LiSAConfiguration setOpenCallPolicy(OpenCallPolicy openCallPolicy) {
		this.openCallPolicy = openCallPolicy;
		return this;
	}

	/**
	 * Yields the {@link CallGraph} for the analysis. Might be {@code null} if
	 * none was set.
	 * 
	 * @return the call graph for the analysis
	 */
	public CallGraph getCallGraph() {
		return callGraph;
	}

	/**
	 * Yields the {@link InterproceduralAnalysis} for the analysis. Might be
	 * {@code null} if none was set.
	 * 
	 * @return the interprocedural analysis for the analysis
	 */
	public InterproceduralAnalysis<?, ?, ?, ?> getInterproceduralAnalysis() {
		return interproceduralAnalysis;
	}

	/**
	 * Yields the {@link AbstractState} for the analysis. Might be {@code null}
	 * if none was set.
	 * 
	 * @return the abstract state for the analysis
	 */
	public AbstractState<?, ?, ?, ?> getAbstractState() {
		return abstractState;
	}

	/**
	 * Yields the collection of {@link SyntacticCheck}s that are to be executed
	 * during the analysis.
	 * 
	 * @return the syntactic checks
	 */
	public Collection<SyntacticCheck> getSyntacticChecks() {
		return syntacticChecks;
	}

	/**
	 * Yields the collection of {@link SemanticCheck}s that are to be executed
	 * during the analysis.
	 * 
	 * @return the semantic checks
	 */
	public Collection<SemanticCheck<?, ?, ?, ?>> getSemanticChecks() {
		return semanticChecks;
	}

	/**
	 * Yields whether or not the input program should be dumped in the form of
	 * dot files representing single {@link CFG}s.
	 * 
	 * @return {@code true} if input program should be dumped
	 */
	public boolean isDumpCFGs() {
		return dumpCFGs;
	}

	/**
	 * Yields whether or not the results of type inference, if run, should be
	 * dumped in the form of dot files representing results on single
	 * {@link CFG}s.
	 * 
	 * @return {@code true} if type inference should be dumped
	 */
	public boolean isDumpTypeInference() {
		return dumpTypeInference;
	}

	/**
	 * Yields whether or not the results of analysis, if run, should be dumped
	 * in the form of dot files representing results on single {@link CFG}s.
	 * 
	 * @return {@code true} if the analysis should be dumped
	 */
	public boolean isDumpAnalysis() {
		return dumpAnalysis;
	}

	/**
	 * Yields whether or not the results a json report file should be dumped at
	 * the end of the analysis.
	 * 
	 * @return {@code true} if the report should be produced
	 */
	public boolean isJsonOutput() {
		return jsonOutput;
	}

	public boolean isJsonFile(){
		return jsonFile;
	}

	/**
	 * Yields the working directory where LiSA will dump all of its outputs.
	 * 
	 * @return the working directory
	 */
	public String getWorkdir() {
		return workdir;
	}

	/**
	 * Yields the concrete class of {@link WorkingSet} to be used in fixpoints.
	 * 
	 * @return the working set class
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends WorkingSet<Statement>> getFixpointWorkingSet() {
		return (Class<? extends WorkingSet<Statement>>) fixpointWorkingSet;
	}

	/**
	 * Yields the number of fixpoint iteration on a given node after which calls
	 * to {@link Lattice#lub(Lattice)} gets replaced with
	 * {@link Lattice#widening(Lattice)}.
	 * 
	 * @return the widening threshold
	 */
	public int getWideningThreshold() {
		return wideningThreshold;
	}

	/**
	 * Yields the {@link OpenCallPolicy} to use during the analysis.
	 * 
	 * @return the policy
	 */
	public OpenCallPolicy getOpenCallPolicy() {
		return openCallPolicy;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((callGraph == null) ? 0 : callGraph.hashCode());
		result = prime * result + (dumpAnalysis ? 1231 : 1237);
		result = prime * result + (dumpCFGs ? 1231 : 1237);
		result = prime * result + (dumpTypeInference ? 1231 : 1237);
		result = prime * result + ((fixpointWorkingSet == null) ? 0 : fixpointWorkingSet.hashCode());
		result = prime * result + ((interproceduralAnalysis == null) ? 0 : interproceduralAnalysis.hashCode());
		result = prime * result + (jsonOutput ? 1231 : 1237);
		result = prime * result + ((semanticChecks == null) ? 0 : semanticChecks.hashCode());
		result = prime * result + ((abstractState == null) ? 0 : abstractState.hashCode());
		result = prime * result + ((syntacticChecks == null) ? 0 : syntacticChecks.hashCode());
		result = prime * result + wideningThreshold;
		result = prime * result + ((workdir == null) ? 0 : workdir.hashCode());
		result = prime * result + ((openCallPolicy == null) ? 0 : openCallPolicy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LiSAConfiguration other = (LiSAConfiguration) obj;
		if (callGraph == null) {
			if (other.callGraph != null)
				return false;
		} else if (!callGraph.equals(other.callGraph))
			return false;
		if (dumpAnalysis != other.dumpAnalysis)
			return false;
		if (dumpCFGs != other.dumpCFGs)
			return false;
		if (dumpTypeInference != other.dumpTypeInference)
			return false;
		if (fixpointWorkingSet == null) {
			if (other.fixpointWorkingSet != null)
				return false;
		} else if (!fixpointWorkingSet.equals(other.fixpointWorkingSet))
			return false;
		if (interproceduralAnalysis == null) {
			if (other.interproceduralAnalysis != null)
				return false;
		} else if (!interproceduralAnalysis.equals(other.interproceduralAnalysis))
			return false;
		if (jsonOutput != other.jsonOutput)
			return false;
		if (semanticChecks == null) {
			if (other.semanticChecks != null)
				return false;
		} else if (!semanticChecks.equals(other.semanticChecks))
			return false;
		if (abstractState == null) {
			if (other.abstractState != null)
				return false;
		} else if (!abstractState.equals(other.abstractState))
			return false;
		if (syntacticChecks == null) {
			if (other.syntacticChecks != null)
				return false;
		} else if (!syntacticChecks.equals(other.syntacticChecks))
			return false;
		if (wideningThreshold != other.wideningThreshold)
			return false;
		if (workdir == null) {
			if (other.workdir != null)
				return false;
		} else if (!workdir.equals(other.workdir))
			return false;
		if (openCallPolicy == null) {
			if (other.openCallPolicy != null)
				return false;
		} else if (!openCallPolicy.equals(other.openCallPolicy))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("LiSA configuration:")
				.append("\n  workdir: ")
				.append(String.valueOf(workdir))
				.append("\n  dump input cfgs: ")
				.append(dumpCFGs)
				.append("\n  dump inferred types: ")
				.append(dumpTypeInference)
				.append("\n  dump analysis results: ")
				.append(dumpAnalysis)
				.append("\n  dump json report: ")
				.append(jsonOutput)
				.append("\n  ")
				.append(syntacticChecks.size())
				.append(" syntactic checks to execute")
				.append((syntacticChecks.isEmpty() ? "" : ":"));
		// TODO automatic way to keep this updated?
		for (SyntacticCheck check : syntacticChecks)
			res.append("\n      ")
					.append(check.getClass().getSimpleName());
		res.append("\n  ")
				.append(semanticChecks.size())
				.append(" semantic checks to execute")
				.append((semanticChecks.isEmpty() ? "" : ":"));
		for (SemanticCheck<?, ?, ?, ?> check : semanticChecks)
			res.append("\n      ")
					.append(check.getClass().getSimpleName());
		return res.toString();
	}
}
