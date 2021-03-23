package it.unive.lisa;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;

public class LiSAConfiguration {

	/**
	 * The collection of syntactic checks to execute
	 */
	private final Collection<SyntacticCheck> syntacticChecks;

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

	public LiSAConfiguration() {
		this.syntacticChecks = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.inferTypes = false;
		this.dumpCFGs = false;
		this.dumpTypeInference = false;
		this.dumpAnalysis = false;
		this.workdir = Paths.get(".").toAbsolutePath().normalize().toString();
	}

	/**
	 * Adds the given syntactic check to the ones that will be executed. These
	 * checks will be immediately executed after LiSA is started.
	 * 
	 * @param check the check to execute
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
	 */
	public LiSAConfiguration addSyntacticChecks(Collection<SyntacticCheck> checks) {
		syntacticChecks.addAll(checks);
		return this;
	}

	/**
	 * Sets the {@link CallGraph} to use for the analysis. Any existing value is
	 * overwritten.
	 * 
	 * @param <T>       the concrete type of the call graph
	 * @param callGraph the callgraph to use
	 */
	public <T extends CallGraph> LiSAConfiguration setCallGraph(T callGraph) {
		this.callGraph = callGraph;
		return this;
	}

	/**
	 * Sets the {@link AbstractState} to use for the analysis. Any existing
	 * value is overwritten.
	 * 
	 * @param state the abstract state to use
	 */
	public LiSAConfiguration setAbstractState(AbstractState<?, ?, ?> state) {
		this.state = state;
		return this;
	}

	/**
	 * Sets whether or not runtime types should be inferred before executing the
	 * semantic analysis. If type inference is not executed, the runtime types
	 * of expressions will correspond to their static type.
	 * 
	 * @param inferTypes if {@code true}, type inference will be ran before the
	 *                       semantic analysis
	 */
	public LiSAConfiguration setInferTypes(boolean inferTypes) {
		this.inferTypes = inferTypes;
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
	 * To decide whether or not the type inference should be executed, use
	 * {@link #setInferTypes(boolean)}.<br>
	 * <br>
	 * To customize where the graphs should be generated, use
	 * {@link #setWorkdir(String)}.
	 * 
	 * @param dumpTypeInference if {@code true}, a dot graph will be generated
	 *                              after the type inference for each input cfg
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
	 */
	public LiSAConfiguration setJsonOutput(boolean jsonOutput) {
		this.jsonOutput = jsonOutput;
		return this;
	}

	/**
	 * Sets the working directory for this instance of LiSA, that is, the
	 * directory files will be created, if any. If files need to be created and
	 * this method has not been invoked, LiSA will create them in the directory
	 * where it was executed from.
	 * 
	 * @param workdir the path (relative or absolute) to the working directory
	 */
	public LiSAConfiguration setWorkdir(String workdir) {
		this.workdir = Paths.get(workdir).toAbsolutePath().normalize().toString();
		return this;
	}

	public CallGraph getCallGraph() {
		return callGraph;
	}

	public AbstractState<?, ?, ?> getState() {
		return state;
	}

	public void setState(AbstractState<?, ?, ?> state) {
		this.state = state;
	}

	public Collection<SyntacticCheck> getSyntacticChecks() {
		return syntacticChecks;
	}

	public boolean isInferTypes() {
		return inferTypes;
	}

	public boolean isDumpCFGs() {
		return dumpCFGs;
	}

	public boolean isDumpTypeInference() {
		return dumpTypeInference;
	}

	public boolean isDumpAnalysis() {
		return dumpAnalysis;
	}

	public boolean isJsonOutput() {
		return jsonOutput;
	}

	public String getWorkdir() {
		return workdir;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((callGraph == null) ? 0 : callGraph.hashCode());
		result = prime * result + (dumpAnalysis ? 1231 : 1237);
		result = prime * result + (dumpCFGs ? 1231 : 1237);
		result = prime * result + (dumpTypeInference ? 1231 : 1237);
		result = prime * result + (inferTypes ? 1231 : 1237);
		result = prime * result + (jsonOutput ? 1231 : 1237);
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((syntacticChecks == null) ? 0 : syntacticChecks.hashCode());
		result = prime * result + ((workdir == null) ? 0 : workdir.hashCode());
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
		if (inferTypes != other.inferTypes)
			return false;
		if (jsonOutput != other.jsonOutput)
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (syntacticChecks == null) {
			if (other.syntacticChecks != null)
				return false;
		} else if (!syntacticChecks.equals(other.syntacticChecks))
			return false;
		if (workdir == null) {
			if (other.workdir != null)
				return false;
		} else if (!workdir.equals(other.workdir))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String res = "LiSA configuration:" +
				"\n  workdir: " + String.valueOf(workdir) +
				"\n  dump input cfgs: " + dumpCFGs +
				"\n  infer types: " + inferTypes +
				"\n  dump inferred types: " + dumpTypeInference +
				"\n  dump analysis results: " + dumpAnalysis +
				"\n  dump json report: " + jsonOutput +
				"\n  " + syntacticChecks.size() + " syntactic checks to execute"
				+ (syntacticChecks.isEmpty() ? "" : ":");
		for (SyntacticCheck check : syntacticChecks)
			res += "\n      " + check.getClass().getSimpleName();
		return res;
	}
}
