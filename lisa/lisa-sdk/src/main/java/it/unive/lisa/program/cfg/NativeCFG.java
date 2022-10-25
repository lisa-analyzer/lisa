package it.unive.lisa.program.cfg;

import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A native cfg, representing a cfg that is usually provided by the runtime of
 * the programming language. This types of cfg are not really subjects of the
 * analysis, but they are nevertheless required for obtaining meaningful
 * results. <br>
 * <br>
 * NativeCFGs do not contain code, but they can be rewritten to a
 * {@link NaryExpression} (that <b>must</b> implement
 * {@link PluggableStatement}) providing their semantics through
 * {@link #rewrite(Statement, Expression...)}.<br>
 * <br>
 * Note that this class does not implement {@link #equals(Object)} nor
 * {@link #hashCode()} since all constructs are unique.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NativeCFG implements CodeMember {

	/**
	 * The descriptor of this control flow graph
	 */
	private final CodeMemberDescriptor descriptor;

	/**
	 * The class of the {@link NaryExpression} that provides the semantics of
	 * this native cfg
	 */
	private final Class<? extends NaryExpression> construct;

	/**
	 * Builds the native control flow graph.
	 * 
	 * @param descriptor the descriptor of this cfg
	 * @param construct  the class of the {@link NaryExpression} that provides
	 *                       the semantics of this native cfg; the class of the
	 *                       construct must also be a subtype of
	 *                       {@link PluggableStatement}
	 * 
	 * @throws IllegalArgumentException if the class of the construct does not
	 *                                      implement {@link PluggableStatement}
	 */
	public NativeCFG(CodeMemberDescriptor descriptor, Class<? extends NaryExpression> construct) {
		if (!PluggableStatement.class.isAssignableFrom(construct))
			throw new IllegalArgumentException(construct + " must implement the " + PluggableStatement.class.getName()
					+ " to be used within native cfgs");
		this.descriptor = descriptor;
		this.construct = construct;
	}

	@Override
	public CodeMemberDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public String toString() {
		return descriptor.toString();
	}

	/**
	 * Produces an {@link NaryExpression} providing the semantics of this native
	 * cfg. Such native call can be used when a call to this native cfg is found
	 * within the program to analyze.
	 * 
	 * @param original the {@link Statement} that must be rewritten as a call to
	 *                     this native cfg
	 * @param params   the parameters of the call to this cfg
	 * 
	 * @return a {@link NaryExpression} providing the semantics of this native
	 *             cfg, that can be used instead of calling this cfg
	 * 
	 * @throws CallResolutionException if something goes wrong while creating
	 *                                     the native call
	 */
	public NaryExpression rewrite(Statement original, Expression... params)
			throws CallResolutionException {

		try {
			Method builder = construct.getDeclaredMethod("build", CFG.class, CodeLocation.class, Expression[].class);
			NaryExpression instance = (NaryExpression) builder.invoke(null, original.getCFG(), original.getLocation(),
					params);
			((PluggableStatement) instance).setOriginatingStatement(original);
			return instance;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new CallResolutionException("Unable to create call to native construct " + construct.getName(), e);
		}
	}
}
