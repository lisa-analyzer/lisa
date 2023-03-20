package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.ScopeToken;

/**
 * A context sensitive token that is always the same (aka, do not track any
 * information about the call stack). All results for a given cfg will be lubbed
 * together regardless of the call site.
 */
public class ContextInsensitiveToken extends SlidingStackToken<Object> {

	private static final Object FLAG = new Object();

	private ContextInsensitiveToken() {
		super();
	}
	
	private ContextInsensitiveToken(ContextInsensitiveToken parent) {
		super(parent);
	}

	@Override
	public ContextInsensitiveToken empty() {
		return new ContextInsensitiveToken();
	}

	@Override
	public ContextInsensitiveToken pushToken(ScopeToken c) {
		ContextInsensitiveToken res = new ContextInsensitiveToken(this);
		res.pushStackForToken(c, FLAG);
		return res;
	}

	@Override
	public ContextInsensitiveToken popToken(ScopeToken c) {
		ContextInsensitiveToken res = new ContextInsensitiveToken(this);
		res.popStackForToken(c, FLAG);
		return res;
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
		return new ContextInsensitiveToken();
	}
}
