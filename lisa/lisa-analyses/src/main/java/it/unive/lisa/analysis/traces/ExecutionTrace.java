package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

/**
 * An execution trace, made of {@link TraceToken}s representing the
 * intraprocedural control-flow instructions that have been traversed up to now.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ExecutionTrace {

	private static final TraceToken[] EMPTY_TRACE = new TraceToken[0];

	/**
	 * An empty execution trace, with no tokens.
	 */
	public static final ExecutionTrace EMPTY = new ExecutionTrace();

	private final TraceToken[] tokens;

	/**
	 * Builds a new empty execution trace.
	 */
	private ExecutionTrace() {
		tokens = EMPTY_TRACE;
	}

	private ExecutionTrace(
			TraceToken[] tokens) {
		this.tokens = tokens;
	}

	/**
	 * Adds the given {@link TraceToken} at the top of this trace.
	 * 
	 * @param token the token to add
	 * 
	 * @return the updated trace
	 */
	public ExecutionTrace push(
			TraceToken token) {
		int len = this.tokens.length;
		TraceToken[] tokens = new TraceToken[len + 1];
		System.arraycopy(this.tokens, 0, tokens, 0, len);
		tokens[len] = token;
		return new ExecutionTrace(tokens);
	}

	/**
	 * Removes the head of this trace.
	 * 
	 * @return the updated trace
	 */
	public ExecutionTrace pop() {
		if (tokens.length == 0)
			return this;

		int len = this.tokens.length;
		TraceToken[] tokens = new TraceToken[len - 1];
		System.arraycopy(this.tokens, 0, tokens, 0, len - 1);
		return new ExecutionTrace(tokens);
	}

	/**
	 * Yields the head of the execution trace.
	 * 
	 * @return the head
	 */
	public TraceToken getHead() {
		return tokens[tokens.length - 1];
	}

	/**
	 * Yields the number of {@link Branching} tokens in this trace.
	 * 
	 * @return the number of branches
	 */
	public int numberOfBranches() {
		int count = 0;
		for (TraceToken token : tokens)
			if (token instanceof Branching)
				count++;
		return count;
	}

	/**
	 * Yields the last loop token ({@link LoopSummary} or {@link LoopIteration})
	 * for the given guard, if any.
	 * 
	 * @param guard the loop guard
	 * 
	 * @return the last (top-most) loop token for the given guard, or
	 *             {@code null} if no such token exist
	 */
	public TraceToken lastLoopTokenFor(
			ProgramPoint guard) {
		for (int i = tokens.length - 1; i >= 0; i--) {
			TraceToken tok = tokens[i];
			if ((tok instanceof LoopSummary || tok instanceof LoopIteration) && tok.getProgramPoint() == guard)
				return tok;
		}

		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(tokens);
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExecutionTrace other = (ExecutionTrace) obj;
		if (!Arrays.equals(tokens, other.tokens))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "<" + StringUtils.join(tokens, "::") + ">";
	}

}
