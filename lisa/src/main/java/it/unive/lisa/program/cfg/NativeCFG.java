package it.unive.lisa.program.cfg;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.callgraph.CallResolutionException;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NativeCall;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A native cfg, representing a cfg that is usually provided by the runtime of
 * the programming language. This types of cfg are not really subjects of the
 * analysis, but they are nevertheless required for obtaining meaningful
 * results. <br>
 * <br>
 * NativeCFGs do not contain code, but they can be rewritten to a
 * {@link NativeCall} providing their semantics through
 * {@link #rewrite(Statement, Expression...)}.
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
	 *                       semantics of this native cfg; the class of the
	 *                       construct must also be a subtype of
	 *                       {@link PluggableStatement}
	 * 
	 * @throws IllegalArgumentException if the class of the construct does not
	 *                                      implement {@link PluggableStatement}
	 */
	public NativeCFG(CFGDescriptor descriptor, Class<? extends NativeCall> construct) {
		if (!PluggableStatement.class.isAssignableFrom(construct))
			throw new IllegalArgumentException(construct + " must implement the " + PluggableStatement.class.getName()
					+ " to be used within native cfgs");
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
	 * @param original the {@link Statement} that must be rewritten as a call to
	 *                     this native cfg
	 * @param params   the parameters of the call to this cfg
	 * 
	 * @return a {@link NativeCall} providing the semantics of this native cfg,
	 *             that can be used instead of calling this cfg
	 * 
	 * @throws CallResolutionException if something goes wrong while creating
	 *                                     the native call
	 */
	public NativeCall rewrite(Statement original, Expression... params)
			throws CallResolutionException {
		// the extra 4 are cfg, file, line, col
		Object[] pars = new Object[params.length + 4];
		pars[0] = original.getCFG();
		pars[1] = original.getSourceFile();
		pars[2] = original.getLine();
		pars[3] = original.getCol();
		for (int i = 0; i < params.length; i++)
			pars[i + 4] = params[i];

		try {
			NativeCall instance = LiSAFactory.getInstance(construct, pars);
			((PluggableStatement) instance).setOriginatingStatement(original);
			return instance;
		} catch (AnalysisSetupException e) {
			throw new CallResolutionException("Unable to create call to native construct " + construct.getName(), e);
		}
	}
}
