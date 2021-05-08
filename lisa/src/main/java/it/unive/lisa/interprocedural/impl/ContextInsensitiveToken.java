package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.ScopeToken;

/**
 * A context sensitive token that is always the same (aka, do not track any
 * information about the call stack).
 */
public class ContextInsensitiveToken implements ContextSensitiveToken {

	private static final ContextInsensitiveToken singleton = new ContextInsensitiveToken();

	private ContextInsensitiveToken() {
	}

	@Override
	public ContextSensitiveToken empty() {
		return this;
	}

	@Override
	public ContextSensitiveToken pushToken(ScopeToken c) {
		return this;
	}

	@Override
	public ContextSensitiveToken popToken(ScopeToken c) {
		return this;
	}

	@Override
	public String toString() {
		return "<empty>";
	}

	/**
	 * Return an instance of the class.
	 * 
	 * @return an instance of the class
	 */
	public static ContextInsensitiveToken getSingleton() {
		return singleton;
	}
}
