package it.unive.lisa.program.cfg;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.callgraph.CallResolutionException;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NativeCall;

/**
 * A native cfg, representing a cfg that is usually provided by the runtime of
 * the programming language. This types of cfg are not really subjects of the
 * analysis, but they are nevertheless required for obtaining meaningful
 * results. <br>
 * <br>
 * NativeCFGs do not contain code, but they can be rewritten to a
 * {@link NativeCall} providing their semantics through
 * {@link #rewrite(CFG, String, int, int, Expression...)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NativeCFG implements CodeMember {

	/**
	 * The descriptor of this control flow graph
	 */
	private final CFGDescriptor descriptor;

	/**
	 * The class of the {@link NativeCall} that provides the semantics of this
	 * native cfg
	 */
	private final Class<? extends NativeCall> construct;

	/**
	 * Builds the native control flow graph.
	 * 
	 * @param descriptor the descriptor of this cfg
	 * @param construct  the class of the {@link NativeCall} that provides the
	 *                       semantics of this native cfg
	 */
	public NativeCFG(CFGDescriptor descriptor, Class<? extends NativeCall> construct) {
		this.descriptor = descriptor;
		this.construct = construct;
	}

	@Override
	public CFGDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Produces a {@link NativeCall} providing the semantics of this native cfg.
	 * Such native call can be used when a call to this native cfg is found
	 * within the program to analyze.
	 * 
	 * @param cfg        the {@link CFG} where the call to this cfg happens
	 * @param sourceFile the sourceFile where the call to this cfg happens
	 * @param line       the line where the call to this cfg happens
	 * @param col        the column where the call to this cfg happens
	 * @param params     the parameters of the call to this cfg
	 * 
	 * @return a {@link NativeCall} providing the semantics of this native cfg,
	 *             that can be used instead of calling this cfg
	 * 
	 * @throws CallResolutionException if something goes wrong while creating
	 *                                     the native call
	 */
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
