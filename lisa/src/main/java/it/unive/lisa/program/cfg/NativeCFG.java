package it.unive.lisa.program.cfg;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.callgraph.CallResolutionException;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NativeCall;

public class NativeCFG implements CodeMember {

	/**
	 * The descriptor of this control flow graph.
	 */
	private final CFGDescriptor descriptor;

	private final Class<? extends NativeCall> construct;

	/**
	 * Builds the control flow graph.
	 * 
	 * @param descriptor the descriptor of this cfg
	 */
	public NativeCFG(CFGDescriptor descriptor, Class<? extends NativeCall> construct) {
		this.descriptor = descriptor;
		this.construct = construct;
	}

	public CFGDescriptor getDescriptor() {
		return descriptor;
	}

	public NativeCall rewrite(CFG cfg, String sourceFile, int line, int col, Expression... params)
			throws CallResolutionException {
		// the extra 4 are cfg, file, line, col
		Object[] pars = new Object[params.length + 4];
		pars[0] = cfg;
		pars[1] = sourceFile;
		pars[2] = line;
		pars[3] = col;
		for (int i = 0; i < params.length; i++)
			pars[i + 4] = params[i];

		try {
			return LiSAFactory.getInstance(construct, pars);
		} catch (AnalysisSetupException e) {
			throw new CallResolutionException("Unable to create call to native construct " + construct.getName(), e);
		}
	}
}
