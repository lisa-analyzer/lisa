package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.type.Type;

/**
 * A call to another cfg.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Call extends NaryExpression {

	/**
	 * The original {@link UnresolvedCall} that has been resolved to this one
	 */
	private UnresolvedCall source = null;

	/**
	 * Builds a call happening at the given source location. The
	 * {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where the expression is defined within the
	 *                       program
	 * @param targetName the name of the target of this call
	 * @param staticType the static type of this call
	 * @param parameters the parameters of this call
	 */
	protected Call(CFG cfg, CodeLocation location, String targetName, Type staticType, Expression... parameters) {
		super(cfg, location, targetName, staticType, parameters);
	}

	/**
	 * Builds a call happening at the given source location.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where the expression is defined within the
	 *                       program
	 * @param targetName the name of the target of this call
	 * @param order      the evaluation order of the sub-expressions
	 * @param staticType the static type of this call
	 * @param parameters the parameters of this call
	 */
	protected Call(CFG cfg, CodeLocation location, String targetName, EvaluationOrder order, Type staticType,
			Expression... parameters) {
		super(cfg, location, targetName, order, staticType, parameters);
	}

	/**
	 * Yields the call that this call originated from, if any. A call <i>r</i>
	 * originates from a call <i>u</i> if:
	 * <ul>
	 * <li><i>u</i> is an {@link UnresolvedCall}, while <i>r</i> is not,
	 * and</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to <i>r</i>, or</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to a call <i>c</i> (e.g. a
	 * {@link HybridCall}), and its semantics generated the call <i>u</i></li>
	 * </ul>
	 * 
	 * @return the call that this one originated from
	 */
	public final UnresolvedCall getSource() {
		return source;
	}

	/**
	 * Yields the name of the target of this call. This is a shortcut to invoke
	 * {@link #getConstructName()}.
	 * 
	 * @return the name of the target
	 */
	public String getTargetName() {
		return getConstructName();
	}

	/**
	 * Yields the parameters of this call. This is a shortcut to invoke
	 * {@link #getSubExpressions()}.
	 * 
	 * @return the parameters of this call
	 */
	public final Expression[] getParameters() {
		return getSubExpressions();
	}

	/**
	 * Sets the call that this call originated from. A call <i>r</i> originates
	 * from a call <i>u</i> if:
	 * <ul>
	 * <li><i>u</i> is an {@link UnresolvedCall}, while <i>r</i> is not,
	 * and</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to <i>r</i>, or</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to a call <i>c</i> (e.g. a
	 * {@link HybridCall}), and its semantics generated the call <i>u</i></li>
	 * </ul>
	 * 
	 * @param source the call that this one originated from
	 */
	public final void setSource(UnresolvedCall source) {
		this.source = source;
	}
}
