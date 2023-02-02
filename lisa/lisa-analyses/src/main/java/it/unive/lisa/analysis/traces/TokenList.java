package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;
import java.util.Deque;
import java.util.LinkedList;
import org.apache.commons.lang3.StringUtils;

public class TokenList {

	private final Deque<Token> tokens;

	public TokenList() {
		tokens = new LinkedList<>();
	}

	private TokenList(TokenList other) {
		tokens = new LinkedList<>(other.tokens);
	}

	public TokenList push(Token token) {
		TokenList res = new TokenList(this);
		res.tokens.addFirst(token);
		return res;
	}

	public TokenList pop() {
		TokenList res = new TokenList(this);
		res.tokens.removeFirst();
		return res;
	}

	public Token getHead() {
		return tokens.getFirst();
	}

	public int numberOfConditions() {
		return (int) tokens.stream().filter(t -> t instanceof ConditionalToken).count();
	}

	public Token lastLoopTokenFor(ProgramPoint guard) {
		for (Token tok : tokens)
			if ((tok instanceof LoopSummaryToken || tok instanceof LoopIterationToken) && tok.getStatement() == guard)
				return tok;

		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tokens == null) ? 0 : tokens.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TokenList other = (TokenList) obj;
		if (tokens == null) {
			if (other.tokens != null)
				return false;
		} else if (!tokens.equals(other.tokens))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "<" + StringUtils.join(tokens, "::") + ">";
	}
}
