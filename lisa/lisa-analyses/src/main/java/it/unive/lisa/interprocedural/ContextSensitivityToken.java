package it.unive.lisa.interprocedural;

import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A token for interprocedural analysis that tunes the level of context
 * sensitivity. This works as a mask over the call stack, keeping track only of
 * some of the calls appearing in it.
 */
public interface ContextSensitivityToken extends ScopeId {
	
	// we redefine this just to give a more specific return type
	@Override
	ContextSensitivityToken push(CFGCall c);
}
