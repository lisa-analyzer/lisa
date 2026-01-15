package it.unive.lisa.analysis.heap;

import it.unive.lisa.analysis.SemanticComponent;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A semantic domain that can evaluate the semantic of expressions that operate
 * on heap locations, and not on concrete values. A heap domain can handle
 * instances of {@link HeapExpression}s. Transformers of this domain yield an
 * abstract state and the substitutions induced by the transformer itself. The
 * substitutions, in the form of a list of {@link HeapReplacement}s that
 * <b>must</b> be processed in their order of appearance, that the creation of
 * this heap domain caused. Each substitution maps {@link Identifier}s in the
 * pre-state to {@link Identifier}s in the post state. If no substitution needs
 * to be applied, an empty list is generated.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link HeapLattice} that this domain works with
 */
public interface HeapDomain<L extends HeapLattice<L>>
		extends
		SemanticComponent<L, Pair<L, List<HeapReplacement>>, SymbolicExpression, Identifier> {

	/**
	 * Rewrites the given expression to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param state      the state of the domain to be queried
	 * @param expression the expression to rewrite
	 * @param pp         the program point where the rewrite happens
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	ExpressionSet rewrite(
			L state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Rewrites the given expressions to a simpler form containing no sub
	 * expressions regarding the heap (that is, {@link HeapExpression}s). Every
	 * expression contained in the result can be safely cast to
	 * {@link ValueExpression}.
	 * 
	 * @param state       the state of the domain to be queried
	 * @param expressions the expressions to rewrite
	 * @param pp          the program point where the rewrite happens
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the rewritten expressions
	 * 
	 * @throws SemanticException if something goes wrong while rewriting
	 */
	default ExpressionSet rewrite(
			L state,
			ExpressionSet expressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		for (SymbolicExpression expr : expressions)
			if (!expr.mightNeedRewriting())
				result.add(expr);
			else
				result.addAll(rewrite(state, expr, pp, oracle).elements());
		return new ExpressionSet(result);
	}

	/**
	 * Yields whether or not the two given expressions are aliases, that is, if
	 * they point to the same region of memory. Note that, for this method to
	 * return {@link Satisfiability#SATISFIED}, both expressions should be
	 * pointers to other expressions.
	 * 
	 * @param state  the state of the domain to be queried
	 * @param x      the first expression
	 * @param y      the second expression
	 * @param pp     the {@link ProgramPoint} where the computation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return whether or not the two expressions are aliases
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability alias(
			L state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Yields all the {@link Identifier}s that are reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the given
	 * expression. This corresponds to recursively explore the memory region
	 * reachable by {@code e}, traversing all possible pointers until no more
	 * are available.
	 * 
	 * @param state  the state of the domain to be queried
	 * @param e      the expression corresponding to the starting point
	 * @param pp     the {@link ProgramPoint} where the computation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the expressions representing memory regions reachable from
	 *             {@code e}
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	default ExpressionSet reachableFrom(
			L state,
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> ws = new HashSet<>();
		ws.add(e);

		Set<SymbolicExpression> result = new HashSet<>();
		Set<SymbolicExpression> prev = new HashSet<>();
		Set<SymbolicExpression> locs = new HashSet<>();
		rewrite(state, e, pp, oracle).elements().stream().forEach(result::add);

		do {
			ws.addAll(locs);
			prev = new HashSet<>(result);
			for (SymbolicExpression id : ws) {
				ExpressionSet rewritten = rewrite(state, id, pp, oracle);
				locs = new HashSet<>();
				for (SymbolicExpression r : rewritten) {
					if (r instanceof MemoryPointer) {
						HeapLocation l = ((MemoryPointer) r).getReferencedLocation();
						locs.add(l);
						result.add(l);
					} else
						locs.add(r);
				}
			}
		} while (!prev.equals(result));

		return new ExpressionSet(result);
	}

	/**
	 * Yields whether or not the {@link Identifier} represented (directly or
	 * after rewriting) by the second expression is reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the first
	 * expression. Note that, for this method to return
	 * {@link Satisfiability#SATISFIED}, not only {@code x} needs to be a
	 * pointer to another expression, but the latter should be a pointer as
	 * well, and so on until {@code y} is reached.
	 * 
	 * @param state  the state of the domain to be queried
	 * @param x      the first expression
	 * @param y      the second expression
	 * @param pp     the {@link ProgramPoint} where the computation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return whether or not the second expression can be reached from the
	 *             first one
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability isReachableFrom(
			L state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Yields whether or not the {@link Identifier} represented (directly or
	 * after rewriting) by the second expression is reachable starting from the
	 * {@link Identifier} represented (directly or after rewriting) by the first
	 * expression, and vice versa. This is equivalent to invoking
	 * {@code isReachableFrom(x, y, pp, oracle).and(isReachableFrom(y, x, pp, oracle))},
	 * that corresponds to the default implementation of this method.
	 * 
	 * @param state  the state of the domain to be queried
	 * @param x      the first expression
	 * @param y      the second expression
	 * @param pp     the {@link ProgramPoint} where the computation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return whether or not the two expressions are mutually reachable
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	default Satisfiability areMutuallyReachable(
			L state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return isReachableFrom(state, x, y, pp, oracle).and(isReachableFrom(state, y, x, pp, oracle));
	}

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
	class HeapReplacement {

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
		 */
		public void addSource(
				Identifier id) {
			sources.add(id);
		}

		/**
		 * Extends this replacement by adding the given identifier to the
		 * sources of this replacement.
		 * 
		 * @param id the identifier to add
		 * 
		 * @return this replacement
		 */
		public HeapReplacement withSource(
				Identifier id) {
			addSource(id);
			return this;
		}

		/**
		 * Adds an {@link Identifier} to the set of identifiers that are the
		 * targets of this replacement.
		 * 
		 * @param id the identifier to add
		 */
		public void addTarget(
				Identifier id) {
			targets.add(id);
		}

		/**
		 * Extends this replacement by adding the given identifier to the
		 * targets of this replacement.
		 * 
		 * @param id the identifier to add
		 * 
		 * @return this replacement
		 */
		public HeapReplacement withTarget(
				Identifier id) {
			addTarget(id);
			return this;
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
		public boolean equals(
				Object obj) {
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
			return "{sources: "
					+ StringUtils.join(sources, ", ")
					+ "} -> {targets: "
					+ StringUtils.join(targets, ", ")
					+ "}";
		}

	}

}
