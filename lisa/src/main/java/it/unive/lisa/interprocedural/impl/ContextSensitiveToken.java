package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.program.cfg.statement.Call;

/**
 * A context sensitive token for interprocedural analysis.
 */
abstract public class ContextSensitiveToken {

	/**
	 * A token without any context sensititivity.
	 * 
	 * @return an empty context sensitive token
	 */
	abstract public ContextSensitiveToken empty();

	/**
	 * Creates a context sensitive token with the given call on the top of the
	 * stack.
	 * 
	 * @param c the call to be pushed at the top of the context sensitive
	 * 
	 * @return a token with the given call on the top of the call stack
	 */
	abstract public ContextSensitiveToken pushCall(Call c);
}
