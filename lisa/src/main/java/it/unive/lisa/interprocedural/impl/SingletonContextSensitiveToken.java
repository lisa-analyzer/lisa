package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.program.cfg.statement.Call;

/**
 * A context sensitive token that is always the same (aka, do not track any
 * information about the call stack).
 */
public class SingletonContextSensitiveToken implements ContextSensitiveToken {

	private static final SingletonContextSensitiveToken singleton = new SingletonContextSensitiveToken();

	private SingletonContextSensitiveToken() {
	}

	@Override
	public ContextSensitiveToken empty() {
		return this;
	}

	@Override
	public ContextSensitiveToken pushCall(Call c) {
		return this;
	}

	/**
	 * Return an instance of the class.
	 * 
	 * @return an instance of the class
	 */
	public static SingletonContextSensitiveToken getSingleton() {
		return singleton;
	}
}
