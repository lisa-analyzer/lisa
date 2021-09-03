package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.ScopeToken;

/**
 * A context sensitive token that is always the same (aka, do not track any
 * information about the call stack).
 */
public final class ContextInsensitiveToken implements ContextSensitivityToken {

	private static final ContextInsensitiveToken singleton = new ContextInsensitiveToken();

	private ContextInsensitiveToken() {
	}

	@Override
	public ContextSensitivityToken empty() {
		return this;
	}

	@Override
	public ContextSensitivityToken pushToken(ScopeToken c) {
		return this;
	}

	@Override
	public ContextSensitivityToken popToken() {
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
