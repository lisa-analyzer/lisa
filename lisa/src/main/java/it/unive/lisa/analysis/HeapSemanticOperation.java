package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * A semantic operation on the heap state of the program, that rewrites
 * expressions and provides a substitution of the available identifiers.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface HeapSemanticOperation {
	/**
	 * Yields the expressions that were computed during the generation of this
	 * heap domain. When evaluating semantics of expressions or assignments, a
	 * heap domain might rewrite an expression to get rid of the parts that
	 * access heap structures, substituting them with synthetic
	 * {@link HeapIdentifier}s representing the accessed locations. The
	 * expressions returned by this method should not contain
	 * {@link HeapExpression}s.<br>
	 * <br>
	 * If no rewriting was necessary for the generation this domain instance, a
	 * singleton collection containing just the original expression is returned
	 * instead.<br>
	 * <br>
	 * The collection returned by this method usually contains one expression,
	 * but instances created through lattice operations (e.g., lub) might
	 * contain more.
	 * 
	 * @return the rewritten expression, or the original one
	 */
	Collection<ValueExpression> getRewrittenExpressions();

	/**
	 * Yields the substitution, in the form of a list of
	 * {@link HeapReplacement}s that <b>must</b> be processed in their order of
	 * appearance, that the creation of this heap domain caused. This
	 * substitution maps {@link Identifier}s in the pre-state to
	 * {@link Identifier}s in the post state. If no substitution needs to be
	 * applied, this method should return an empty list.
	 * 
	 * @return the list of replacements
	 */
	List<HeapReplacement> getSubstitution();

	/**
	 * A replacement between {@link Identifier}s caused by a change in the heap
	 * abstraction. A replacement express a relation between two sets of
	 * identifiers (returned by {@link #getSources()} and
	 * {@link #getTargets()}). The semantics of a replacement is to assign to
	 * every identifier in {@link #getTargets()} the upper bound of the value of
	 * each identifier in {@link #getSources()}, and then forget about all the
	 * sources identifiers that are not also the targets.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static final class HeapReplacement {

		/**
		 * The set of identifiers that are the sources of the replacement
		 */
		private final Set<Identifier> sources;

		/**
		 * The set of identifiers that are the targets of the replacement
		 */
		private final Set<Identifier> targets;

		/**
		 * Builds the replacement. It starts with empty sets of sources and
		 * targets. New ones can be added with {@link #addSource(Identifier)}
		 * and {@link #addTarget(Identifier)}.
		 */
		public HeapReplacement() {
			this.sources = new HashSet<>();
			this.targets = new HashSet<>();
		}

		/**
		 * Adds an {@link Identifier} to the set of identifiers that are the
		 * sources of this replacement.
		 * 
		 * @param id the identifier to add
		 * 
		 * @throws IllegalArgumentException if the given identifier is already
		 *                                      in the set of target identifiers
		 */
		public void addSource(Identifier id) {
			if (targets.contains(id))
				throw new IllegalArgumentException(
						"The given identifier has already been considered as target of this replacement");

			sources.add(id);
		}

		/**
		 * Adds an {@link Identifier} to the set of identifiers that are the
		 * targets of this replacement.
		 * 
		 * @param id the identifier to add
		 * 
		 * @throws IllegalArgumentException if the given identifier is already
		 *                                      in the set of source identifiers
		 */
		public void addTarget(Identifier id) {
			if (sources.contains(id))
				throw new IllegalArgumentException(
						"The given identifier has already been considered as source of this replacement");

			targets.add(id);
		}

		/**
		 * Yields the set of identifiers that this replacement originates from,
		 * that is, the ones whose value will be assigned to the targets.
		 * 
		 * @return the sources of this replacement
		 */
		public Set<Identifier> getSources() {
			return sources;
		}

		/**
		 * Yields the set of identifiers that are targeted by this replacement,
		 * that is, the ones that will be assigned.
		 * 
		 * @return the targets of this replacement
		 */
		public Set<Identifier> getTargets() {
			return targets;
		}

		/**
		 * Yields the collection of identifiers that must be removed after the
		 * application of this replacement, that is, the identifiers that are in
		 * {@link #getSources()} but not in {@link #getTargets()}.
		 * 
		 * @return the identifiers to forget after this replacement
		 */
		public Collection<Identifier> getIdsToForget() {
			HashSet<Identifier> copy = new HashSet<>(sources);
			copy.removeAll(targets);
			return copy;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sources == null) ? 0 : sources.hashCode());
			result = prime * result + ((targets == null) ? 0 : targets.hashCode());
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
			HeapReplacement other = (HeapReplacement) obj;
			if (sources == null) {
				if (other.sources != null)
					return false;
			} else if (!sources.equals(other.sources))
				return false;
			if (targets == null) {
				if (other.targets != null)
					return false;
			} else if (!targets.equals(other.targets))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "{sources: " + StringUtils.join(sources, ", ") + "} -> {targets: " + StringUtils.join(targets, ", ")
					+ "}";
		}
	}

}
