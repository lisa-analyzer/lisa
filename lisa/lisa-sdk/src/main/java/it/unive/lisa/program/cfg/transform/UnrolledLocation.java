package it.unive.lisa.program.cfg.transform;

import it.unive.lisa.program.cfg.CodeLocation;
import java.util.Objects;

/**
 * A {@link CodeLocation} for a statement produced by loop unrolling.
 * {@link #getCodeLocation()} delegates to the wrapped original, so
 * source-position renderers (DOT, JSON, warnings) are unaffected.
 *
 * @author <a href="mailto:giacomo12596@gmail.com">Giacomo Zanatta</a>
 */
public class UnrolledLocation
		implements
		CodeLocation {

	/**
	 * The un-cloned source location this instance was derived from.
	 */
	private final CodeLocation original;

	/**
	 * The 1-based unroll iteration index.
	 */
	private final int iteration;

	/**
	 * Builds an unrolled location wrapping {@code original}.
	 *
	 * @param original  the source location of the un-cloned statement, must be
	 *                      non-null
	 * @param iteration 1-based unroll iteration index, must be {@code >= 1}
	 *
	 * @throws IllegalArgumentException if {@code original} is {@code null} or
	 *                                      {@code iteration < 1}
	 */
	public UnrolledLocation(
			CodeLocation original,
			int iteration) {
		if (original == null)
			throw new IllegalArgumentException("original location cannot be null");
		if (iteration < 1)
			throw new IllegalArgumentException("iteration must be >= 1, was " + iteration);
		// Flatten nested UnrolledLocations to keep equals/hashCode cheap and
		// avoid unbounded nesting under repeated transformations. If an
		// unwound statement is unrolled again by a later pass, the caller
		// supplies a fresh iteration index derived from its own logic; the
		// wrapper chain stays flat.
		this.original = (original instanceof UnrolledLocation)
				? ((UnrolledLocation) original).original
				: original;
		this.iteration = iteration;
	}

	/**
	 * Yields the un-cloned source location this instance was derived from.
	 *
	 * @return the original location
	 */
	public CodeLocation getOriginal() {
		return original;
	}

	/**
	 * Yields the 1-based unroll iteration index.
	 *
	 * @return the iteration index
	 */
	public int getIteration() {
		return iteration;
	}

	@Override
	public String getCodeLocation() {
		return original.getCodeLocation();
	}

	@Override
	public int compareTo(
			CodeLocation o) {
		if (o instanceof UnrolledLocation) {
			UnrolledLocation other = (UnrolledLocation) o;
			int cmp = original.compareTo(other.original);
			if (cmp != 0)
				return cmp;
			return Integer.compare(iteration, other.iteration);
		}
		int cmp = original.compareTo(o);
		return cmp != 0 ? cmp : 1;
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
		UnrolledLocation other = (UnrolledLocation) obj;
		return iteration == other.iteration && Objects.equals(original, other.original);
	}

	@Override
	public int hashCode() {
		return Objects.hash(original, iteration);
	}

	@Override
	public String toString() {
		return original.getCodeLocation() + "[unroll=" + iteration + "]";
	}
}
