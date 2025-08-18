package it.unive.lisa.analysis.nonrelational.heap;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An {@link Environment} that is also a {@link HeapLattice}, tracking locations
 * pointed by variables and heap locations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of the lattice used to represent the locations stored in
 *                this environment
 */
public class HeapEnvironment<L extends HeapValue<L>> extends FunctionalLattice<HeapEnvironment<L>, Identifier, L>
		implements
		HeapLattice<HeapEnvironment<L>> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public HeapEnvironment(
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
	public HeapEnvironment(
			L domain,
			Map<Identifier, L> function) {
		super(domain, function);
	}

	@Override
	public Pair<HeapEnvironment<L>, List<HeapReplacement>> pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		Pair<HeapEnvironment<L>, List<HeapReplacement>> result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.pushScope(scope, pp);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException("Pushing the scope '" + scope + "' raised an error", holder.get());

		return result;
	}

	@Override
	public Pair<HeapEnvironment<L>, List<HeapReplacement>> popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		Pair<HeapEnvironment<L>, List<HeapReplacement>> result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.popScope(scope, pp);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException("Popping the scope '" + scope + "' raised an error", holder.get());

		return result;
	}

	private Pair<HeapEnvironment<L>, List<HeapReplacement>> liftIdentifiers(
			UnaryOperator<Identifier> lifter)
			throws SemanticException {
		if (isBottom() || isTop())
			return Pair.of(this, List.of());

		Map<Identifier, L> function = mkNewFunction(null, false);
		HeapReplacement removed = new HeapReplacement();
		List<HeapReplacement> r = new LinkedList<>();

		for (Identifier id : getKeys()) {
			Identifier lifted = lifter.apply(id);
			if (lifted != null) {
				if (lifted.equals(id))
					// we track the renaming
					r.add(new HeapReplacement().withSource(id).withTarget(lifted));
				if (!function.containsKey(lifted))
					function.put(lifted, getState(id));
				else
					function.put(lifted, getState(id).lub(function.get(lifted)));
			} else
				// we track the removal
				removed.addSource(id);
		}

		if (r.isEmpty() && removed.getSources().isEmpty())
			return Pair.of(new HeapEnvironment<>(lattice, function), Collections.emptyList());

		r.addAll(expand(removed));
		return Pair.of(new HeapEnvironment<>(lattice, function), r);
	}

	@Override
	public Pair<HeapEnvironment<L>, List<HeapReplacement>> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return Pair.of(this, List.of());

		Map<Identifier, L> result = mkNewFunction(function, false);
		result.remove(id);
		HeapReplacement r = new HeapReplacement().withSource(id);

		return Pair.of(new HeapEnvironment<>(lattice, result), expand(r));
	}

	@Override
	public Pair<HeapEnvironment<L>, List<HeapReplacement>> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return Pair.of(this, List.of());

		Map<Identifier, L> result = mkNewFunction(function, false);
		for (Identifier id : ids)
			result.remove(id);

		HeapReplacement r = new HeapReplacement();
		ids.forEach(r::addSource);

		return Pair.of(new HeapEnvironment<>(lattice, result), expand(r));
	}

	@Override
	public Pair<HeapEnvironment<L>, List<HeapReplacement>> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return Pair.of(this, List.of());

		Map<Identifier, L> result = mkNewFunction(function, false);
		Set<Identifier> keys = result.keySet().stream().filter(test::test).collect(Collectors.toSet());
		keys.forEach(result::remove);

		if (keys.isEmpty())
			return Pair.of(new HeapEnvironment<>(lattice, result), Collections.emptyList());

		HeapReplacement r = new HeapReplacement();
		keys.forEach(r::addSource);

		return Pair.of(new HeapEnvironment<>(lattice, result), expand(r));
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
				throw new SemanticException("Unable to lub " + pair.getLeft() + " and " + pair.getRight(), e);
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

	@Override
	public HeapEnvironment<L> top() {
		return isTop() ? this : new HeapEnvironment<>(lattice.top(), null);
	}

	@Override
	public HeapEnvironment<L> bottom() {
		return isBottom() ? this : new HeapEnvironment<>(lattice.bottom(), null);
	}

	@Override
	public HeapEnvironment<L> mk(
			L lattice,
			Map<Identifier, L> function) {
		return new HeapEnvironment<>(lattice, function);
	}

	@Override
	public List<HeapReplacement> expand(
			HeapReplacement base)
			throws SemanticException {
		HeapReplacement sub = new HeapReplacement();
		for (Identifier id : base.getSources())
			lattice.reachableOnlyFrom(this, id).forEach(sub::addSource);
		return List.of(sub);
	}

}
