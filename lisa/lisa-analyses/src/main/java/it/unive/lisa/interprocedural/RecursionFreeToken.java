package it.unive.lisa.interprocedural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import it.unive.lisa.analysis.ScopeToken;

/**
 * A context sensitive token representing an entire call chain until a recursion
 * is encountered. This corresponds to having an unlimited {@link KDepthToken},
 * that only stops when a token that is already part of the chain is pushed.
 */
public class RecursionFreeToken implements ContextSensitivityToken {

	private static final RecursionFreeToken SINGLETON = new RecursionFreeToken();

	private final List<ScopeToken> tokens;
	private final boolean foundRecursion;

	private RecursionFreeToken() {
		tokens = Collections.emptyList();
		foundRecursion = false;
	}

	private RecursionFreeToken(List<ScopeToken> tokens, ScopeToken newToken) {
		this.tokens = new ArrayList<>(tokens.size() + 1);
		tokens.stream().forEach(this.tokens::add);
		this.tokens.add(newToken);
		foundRecursion = false;
	}

	private RecursionFreeToken(List<ScopeToken> tokens) {
		int oldsize = tokens.size();
		if (oldsize == 1)
			this.tokens = Collections.emptyList();
		else {
			this.tokens = new ArrayList<>(oldsize - 1);
			tokens.stream().limit(oldsize - 1).forEach(this.tokens::add);
		}
		foundRecursion = false;
	}

	private RecursionFreeToken(RecursionFreeToken other) {
		tokens = other.tokens;
		foundRecursion = true;
	}

	@Override
	public ContextSensitivityToken empty() {
		return new RecursionFreeToken();
	}

	@Override
	public ContextSensitivityToken pushToken(ScopeToken c) {
		// we try to prevent recursions here: it's better
		// to look for them starting from the end of the array
		for (int i = tokens.size() - 1; i >= 0; i--)
			if (tokens.get(i).equals(c))
				return new RecursionFreeToken(this);
		return new RecursionFreeToken(tokens, c);
	}

	@Override
	public ContextSensitivityToken popToken(ScopeToken c) {
		if (tokens.isEmpty())
			return this;
		return new RecursionFreeToken(tokens);
	}
	
	@Override
	public boolean limitReached() {
		return foundRecursion;
	}

	/**
	 * Return an empty token.
	 * 
	 * @return an empty token
	 */
	public static RecursionFreeToken getSingleton() {
		return SINGLETON;
	}

	@Override
	public String toString() {
		return tokens.toString();
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
		return Objects.hashCode(tokens);
	}
}
