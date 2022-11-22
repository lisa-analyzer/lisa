package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.ScopeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A context sensitive token representing an entire call chain up until a
 * recursion.
 */
public class RecursionFreeToken implements ContextSensitivityToken {

	private static final RecursionFreeToken singleton = new RecursionFreeToken(null);

	private final List<ScopeToken> tokens;

	private RecursionFreeToken(ScopeToken newToken) {
		this(Collections.emptyList(), newToken);
	}

	private RecursionFreeToken(List<ScopeToken> tokens, ScopeToken newToken) {
		this.tokens = new ArrayList<>(tokens.size() + (newToken == null ? 0 : 1));
		this.tokens.addAll(tokens);
		if (newToken != null)
			this.tokens.add(newToken);
	}

	@Override
	public ContextSensitivityToken empty() {
		return new RecursionFreeToken(null);
	}

	@Override
	public ContextSensitivityToken pushToken(ScopeToken c) {
		if (tokens.contains(c))
			return this;
		return new RecursionFreeToken(tokens, c);
	}

	@Override
	public ContextSensitivityToken popToken() {
		List<ScopeToken> toks = new ArrayList<>(tokens);
		toks.remove(tokens.size() - 1);
		return new RecursionFreeToken(toks, null);
	}

	/**
	 * Return an empty token.
	 * 
	 * @return an empty token
	 */
	public static RecursionFreeToken getSingleton() {
		return singleton;
	}

	@Override
	public String toString() {
		return String.valueOf(tokens);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		RecursionFreeToken that = (RecursionFreeToken) o;
		return Objects.equals(tokens, that.tokens);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tokens);
	}

}
