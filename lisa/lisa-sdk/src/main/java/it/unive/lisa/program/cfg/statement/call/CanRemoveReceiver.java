package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;

/**
 * Marker for {@link Call}s whose first parameter can be removed. This happens
 * when the {@link CallType} of an {@link UnresolvedCall} is
 * {@link CallType#UNKNOWN}, and a match is found among non-instance code
 * members by the {@link CallGraph}. In this case, the first parameter (that is
 * supposed to be the receiver for instance calls) is removed from the list of
 * parameters, and its string value is used as qualifier for the resolved call.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface CanRemoveReceiver {

	/**
	 * Yields an instance of {@link TruncatedParamsCall} that wraps a copy of
	 * the receiver of this call up to the parameters, where the first one has
	 * been removed.
	 * 
	 * @return the call wrapping the copy of this one
	 */
	TruncatedParamsCall removeFirstParameter();

	/**
	 * Truncates the given array of expressions by removing the first element.
	 * 
	 * @param parameters the original array of expressions
	 * 
	 * @return the truncated array
	 */
	public static Expression[] truncate(
			Expression[] parameters) {
		if (parameters.length == 0)
			return parameters;
		Expression[] truncatedParams = new Expression[parameters.length - 1];
		if (truncatedParams.length == 0)
			return truncatedParams;
		System.arraycopy(parameters, 1, truncatedParams, 0, parameters.length - 1);
		return truncatedParams;
	}
}
