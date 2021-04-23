package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.program.cfg.statement.Call;
import java.util.Objects;

/**
 * A context sensitive token representing a call point.
 */
public class CallPointContextSensitiveToken extends ContextSensitiveToken {

	private static final CallPointContextSensitiveToken singleton = new CallPointContextSensitiveToken(null);

	private final Call callPoint;

	private CallPointContextSensitiveToken(Call callPoint) {
		this.callPoint = callPoint;
	}

	@Override
	public ContextSensitiveToken empty() {
		return new CallPointContextSensitiveToken(null);
	}

	@Override
	public ContextSensitiveToken pushCall(Call c) {
		return new CallPointContextSensitiveToken(c);
	}

	/**
	 * Return an empty token.
	 * 
	 * @return an empty token
	 */
	public static CallPointContextSensitiveToken getSingleton() {
		return singleton;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CallPointContextSensitiveToken that = (CallPointContextSensitiveToken) o;
		return Objects.equals(callPoint, that.callPoint);
	}

	@Override
	public int hashCode() {
		return Objects.hash(callPoint);
	}

}
