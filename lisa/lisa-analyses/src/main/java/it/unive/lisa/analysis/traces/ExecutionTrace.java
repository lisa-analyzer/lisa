package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;
import java.util.Deque;
import java.util.LinkedList;
import org.apache.commons.lang3.StringUtils;

/**
 * An execution trace, made of {@link TraceToken}s representing the
 * intraprocedural control-flow instructions that have been traversed up to now.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ExecutionTrace {

	private final Deque<TraceToken> tokens;

	/**
	 * Builds a new empty execution trace.
	 */
	public ExecutionTrace() {
		tokens = new LinkedList<>();
	}

	private ExecutionTrace(ExecutionTrace other) {
		tokens = new LinkedList<>(other.tokens);
	}

	/**
	 * Adds the given {@link TraceToken} at the top of this trace.
	 * 
	 * @param token the token to add
	 * 
	 * @return the updated trace
	 */
	public ExecutionTrace push(TraceToken token) {
		ExecutionTrace res = new ExecutionTrace(this);
		res.tokens.addFirst(token);
		return res;
	}

	/**
	 * Removes the head of this trace.
	 * 
	 * @return the updated trace
	 */
	public ExecutionTrace pop() {
		if (tokens.isEmpty())
			return this;

		ExecutionTrace res = new ExecutionTrace(this);
		res.tokens.removeFirst();
		return res;
	}

	/**
	 * Yields the head (top-most) {@link TraceToken} in this trace.
	 * 
	 * @return the head of the trace
	 */
	public TraceToken getHead() {
		return tokens.getFirst();
	}

	/**
	 * Yields the number of {@link Branching} tokens in this trace.
	 * 
	 * @return the number of branches
	 */
	public int numberOfBranches() {
		return (int) tokens.stream().filter(t -> t instanceof Branching).count();
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
	public TraceToken lastLoopTokenFor(ProgramPoint guard) {
		for (TraceToken tok : tokens)
			if ((tok instanceof LoopSummary || tok instanceof LoopIteration) && tok.getProgramPoint() == guard)
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
		ExecutionTrace other = (ExecutionTrace) obj;
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
