package it.unive.lisa.interprocedural;

import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A context sensitive token that is always the same (aka, do not track any
 * information about the call stack). All results for a given cfg will be lubbed
 * together regardless of the call site.
 */
public class ContextInsensitiveToken implements ContextSensitivityToken {

	private ContextInsensitiveToken() {
		super();
	}

	@Override
	public ContextInsensitiveToken empty() {
		return new ContextInsensitiveToken();
	}

	@Override
	public ContextInsensitiveToken pushCall(CFGCall c) {
		return this;
	}

	@Override
	public ContextInsensitiveToken popCall(CFGCall c) {
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
		return new ContextInsensitiveToken();
	}

	@Override
	public int hashCode() {
		// we provide a deterministic hashcode as it is used for generating
		// filenames of the output files
		return ContextInsensitiveToken.class.getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		// instances are still unique
		return this == obj;
	}
}
