package it.unive.lisa.analysis.reachability;

/**
 * The reachability of a program point, as computed by {@link Reachability}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum ReachabilityStatus {

	/**
	 * The program point is not reached by any execution.
	 */
	UNREACHABLE,

	/**
	 * The program point might be reached by some executions, but not
	 * necessarily by all of them.
	 */
	POSSIBLY_REACHABLE,

	/**
	 * The program point is reached by every execution.
	 */
	REACHABLE;

	/**
	 * Composes {@code this} status, computed only with respect to the
	 * executions reaching another program point (e.g., the entry of a cfg),
	 * with the status of that program point. The result is {@link #REACHABLE}
	 * only if both are, and {@link #UNREACHABLE} if either one is.
	 *
	 * @param outer the reachability of the other program point
	 *
	 * @return the composed reachability
	 */
	public ReachabilityStatus compose(
			ReachabilityStatus outer) {
		if (this == UNREACHABLE || outer == UNREACHABLE)
			return UNREACHABLE;
		if (this == REACHABLE && outer == REACHABLE)
			return REACHABLE;
		return POSSIBLY_REACHABLE;
	}

	/**
	 * Yields the most certain status between {@code this} and {@code other}.
	 * The result is {@link #REACHABLE} if either one is, and
	 * {@link #UNREACHABLE} only if both are.
	 *
	 * @param other the other status
	 *
	 * @return the most certain status
	 */
	public ReachabilityStatus best(
			ReachabilityStatus other) {
		if (this == REACHABLE || other == REACHABLE)
			return REACHABLE;
		if (this == UNREACHABLE && other == UNREACHABLE)
			return UNREACHABLE;
		return POSSIBLY_REACHABLE;
	}
}
