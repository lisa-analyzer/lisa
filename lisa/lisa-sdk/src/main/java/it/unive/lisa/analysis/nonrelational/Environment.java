package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticComponent;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An environment that maps {@link Identifier}s to instances of a lattice
 * structure {@code L}. This is a {@link FunctionalLattice}, that is, it
 * implements a function mapping keys (identifiers) to values (instances of the
 * lattice), and lattice operations are automatically lifted for individual
 * elements of the environment if they are mapped to the same key. An
 * environment is a {@link DomainLattice}, that is, it can be used as a lattice
 * structure for a {@link SemanticComponent} or a {@link SemanticDomain}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of values that appear in this environment
 * @param <E> the concrete type of environment
 */
public abstract class Environment<L extends Lattice<L>,
		E extends Environment<L, E>>
		extends
		FunctionalLattice<E, Identifier, L>
		implements
		DomainLattice<E, E> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public Environment(
			L domain) {
		super(domain);
	}

	/**
	 * Builds an environment containing the given mapping. If function is
	 * {@code null}, the new environment is the top environment if
	 * {@code lattice.isTop()} holds, and it is the bottom environment if
	 * {@code lattice.isBottom()} holds.
	 * 
	 * @param domain   a singleton instance to be used during semantic
	 *                     operations to retrieve top and bottom values
	 * @param function the function representing the mapping contained in the
	 *                     new environment; can be {@code null}
	 */
	public Environment(
			L domain,
			Map<Identifier, L> function) {
		super(domain, function);
	}

	@Override
	public E pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		E result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.pushScope(scope, pp);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException(
					"Pushing the scope '" + scope + "' raised an error",
					holder.get());

		return result;
	}

	@Override
	public E popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		E result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.popScope(scope, pp);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException(
					"Popping the scope '" + scope + "' raised an error",
					holder.get());

		return result;
	}

	@SuppressWarnings("unchecked")
	private E liftIdentifiers(
			UnaryOperator<Identifier> lifter)
			throws SemanticException {
		if (isBottom() || isTop())
			return (E) this;

		Map<Identifier, L> function = mkNewFunction(null, false);
		for (Identifier id : getKeys()) {
			Identifier lifted = lifter.apply(id);
			if (lifted != null)
				if (!function.containsKey(lifted))
					function.put(lifted, getState(id));
				else
					function.put(lifted, getState(id).lub(function.get(lifted)));

		}

		return mk(lattice, function);
	}

	@Override
	@SuppressWarnings("unchecked")
	public E forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return (E) this;

		Map<Identifier, L> result = mkNewFunction(function, false);
		result.remove(id);

		return mk(lattice, result);
	}

	@Override
	@SuppressWarnings("unchecked")
	public E forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return (E) this;

		Map<Identifier, L> result = mkNewFunction(function, false);
		for (Identifier id : ids)
			result.remove(id);

		return mk(lattice, result);
	}

	@Override
	@SuppressWarnings("unchecked")
	public E forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return (E) this;

		Map<Identifier, L> result = mkNewFunction(function, false);
		Set<Identifier> keys = result.keySet().stream().filter(test::test).collect(Collectors.toSet());
		keys.forEach(result::remove);

		return mk(lattice, result);
	}

	@Override
	public Set<Identifier> lubKeys(
			Set<Identifier> k1,
			Set<Identifier> k2)
			throws SemanticException {
		Set<Identifier> keys = new HashSet<>();
		CollectionsDiffBuilder<Identifier> builder = new CollectionsDiffBuilder<>(Identifier.class, k1, k2);
		// this is needed for a name-only comparison
		builder.compute(Comparator.comparing(Identifier::getName));
		keys.addAll(builder.getOnlyFirst());
		keys.addAll(builder.getOnlySecond());
		for (Pair<Identifier, Identifier> pair : builder.getCommons())
			try {
				keys.add(pair.getLeft().lub(pair.getRight()));
			} catch (SemanticException e) {
				throw new SemanticException(
						"Unable to lub " + pair.getLeft() + " and " + pair.getRight(),
						e);
			}
		return keys;
	}

	@Override
	public L stateOfUnknown(
			Identifier key) {
		return lattice.unknownValue(key);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return getKeys().contains(id);
	}

}
