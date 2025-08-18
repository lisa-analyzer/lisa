package it.unive.lisa.lattices.heap.allocations;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
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
 * An instance of {@link FunctionalLattice} representing a map from identifiers
 * to sets of {@link AllocationSite}s, that also tracks the fields of each
 * allocation site that have been assigned to some value.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapEnvWithFields extends FunctionalLattice<HeapEnvWithFields, Identifier, AllocationSites>
		implements
		HeapLattice<HeapEnvWithFields> {

	/**
	 * Tracks the fields of each allocation site.
	 */
	public final GenericMapLattice<AllocationSite, ExpressionSet> fields;

	/**
	 * Builds an empty environment.
	 */
	public HeapEnvWithFields() {
		super(new AllocationSites().top());
		this.fields = new GenericMapLattice<AllocationSite, ExpressionSet>(new ExpressionSet()).top();
	}

	/**
	 * Builds an environment containing the given mapping. If function is
	 * {@code null}, the new environment is the top environment if
	 * {@code lattice.isTop()} and {@code fields.isTop()} hold, and it is the
	 * bottom environment if {@code lattice.isBottom()} and
	 * {@code fields.isBottom()} hold.
	 * 
	 * @param lattice  a singleton instance to be used during semantic
	 *                     operations to retrieve top and bottom values
	 * @param function the function representing the mapping contained in the
	 *                     new environment; can be {@code null}
	 * @param fields   the fields of each allocation site that have been
	 *                     assigned to some value
	 */
	public HeapEnvWithFields(
			AllocationSites lattice,
			Map<Identifier, AllocationSites> function,
			GenericMapLattice<AllocationSite, ExpressionSet> fields) {
		super(lattice, function);
		this.fields = fields;
	}

	@Override
	public Pair<HeapEnvWithFields, List<HeapReplacement>> pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		Pair<HeapEnvWithFields, List<HeapReplacement>> result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.pushScope(scope, pp);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException("Pushing the scope '" + scope + "' raised an error", holder.get());

		if (result.getRight().isEmpty())
			return result;

		// the last substitution contains the removed ids
		HeapReplacement base = result.getRight().get(result.getRight().size() - 1);
		Set<AllocationSite> sites = new HashSet<>();
		for (Identifier id : base.getSources())
			if (id instanceof AllocationSite)
				sites.add((AllocationSite) id);
		GenericMapLattice<AllocationSite, ExpressionSet> f = fields.removeAll(sites);
		return Pair
			.of(new HeapEnvWithFields(result.getLeft().lattice, result.getLeft().function, f), result.getRight());
	}

	@Override
	public Pair<HeapEnvWithFields, List<HeapReplacement>> popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		Pair<HeapEnvWithFields, List<HeapReplacement>> result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.popScope(scope, pp);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException("Popping the scope '" + scope + "' raised an error", holder.get());

		if (result.getRight().isEmpty())
			return result;

		// the last substitution contains the removed ids
		HeapReplacement base = result.getRight().get(result.getRight().size() - 1);
		Set<AllocationSite> sites = new HashSet<>();
		for (Identifier id : base.getSources())
			if (id instanceof AllocationSite)
				sites.add((AllocationSite) id);
		GenericMapLattice<AllocationSite, ExpressionSet> f = fields.removeAll(sites);
		return Pair
			.of(new HeapEnvWithFields(result.getLeft().lattice, result.getLeft().function, f), result.getRight());
	}

	private Pair<HeapEnvWithFields, List<HeapReplacement>> liftIdentifiers(
			UnaryOperator<Identifier> lifter)
			throws SemanticException {
		if (isBottom() || isTop())
			return Pair.of(this, List.of());

		Map<Identifier, AllocationSites> function = mkNewFunction(null, false);
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
			return Pair.of(new HeapEnvWithFields(lattice, function, fields), Collections.emptyList());

		r.addAll(expand(removed));
		return Pair.of(new HeapEnvWithFields(lattice, function, fields), r);
	}

	@Override
	public Pair<HeapEnvWithFields, List<HeapReplacement>> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return Pair.of(this, List.of());

		GenericMapLattice<AllocationSite, ExpressionSet> f = fields;
		if (id instanceof AllocationSite)
			f = f.remove((AllocationSite) id);

		Map<Identifier, AllocationSites> result = mkNewFunction(function, false);
		result.remove(id);
		HeapReplacement r = new HeapReplacement().withSource(id);

		return Pair.of(new HeapEnvWithFields(lattice, result, f), expand(r));
	}

	@Override
	public Pair<HeapEnvWithFields, List<HeapReplacement>> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return Pair.of(this, List.of());

		Set<AllocationSite> sites = new HashSet<>();
		for (Identifier id : ids)
			if (id instanceof AllocationSite)
				sites.add((AllocationSite) id);
		GenericMapLattice<AllocationSite, ExpressionSet> f = fields.removeAll(sites);

		Map<Identifier, AllocationSites> result = mkNewFunction(function, false);
		for (Identifier id : ids)
			result.remove(id);

		HeapReplacement r = new HeapReplacement();
		ids.forEach(r::addSource);

		return Pair.of(new HeapEnvWithFields(lattice, result, f), expand(r));
	}

	@Override
	public Pair<HeapEnvWithFields, List<HeapReplacement>> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return Pair.of(this, List.of());

		Set<AllocationSite> sites = getKeys().stream()
			.filter(test)
			.filter(k -> k instanceof AllocationSite)
			.map(k -> (AllocationSite) k)
			.collect(Collectors.toSet());
		GenericMapLattice<AllocationSite, ExpressionSet> f = fields.removeAll(sites);

		Map<Identifier, AllocationSites> result = mkNewFunction(function, false);
		Set<Identifier> keys = result.keySet().stream().filter(test::test).collect(Collectors.toSet());
		keys.forEach(result::remove);

		if (keys.isEmpty())
			return Pair.of(new HeapEnvWithFields(lattice, function, f), Collections.emptyList());

		HeapReplacement r = new HeapReplacement();
		keys.forEach(r::addSource);

		return Pair.of(new HeapEnvWithFields(lattice, result, f), expand(r));
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
	public AllocationSites stateOfUnknown(
			Identifier key) {
		return lattice.unknownValue(key);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return getKeys().contains(id);
	}

	@Override
	public HeapEnvWithFields lubAux(
			HeapEnvWithFields other)
			throws SemanticException {
		HeapEnvWithFields lub = super.lubAux(other);
		return new HeapEnvWithFields(lub.lattice, lub.function, fields.lub(other.fields));
	}

	@Override
	public HeapEnvWithFields glbAux(
			HeapEnvWithFields other)
			throws SemanticException {
		HeapEnvWithFields glb = super.glbAux(other);
		return new HeapEnvWithFields(glb.lattice, glb.function, fields.glb(other.fields));
	}

	@Override
	public boolean lessOrEqualAux(
			HeapEnvWithFields other)
			throws SemanticException {
		return super.lessOrEqualAux(other) && fields.lessOrEqual(other.fields);
	}

	@Override
	public HeapEnvWithFields top() {
		return isTop() ? this : new HeapEnvWithFields(lattice.top(), null, fields.top());
	}

	@Override
	public boolean isTop() {
		return super.isTop() && fields.isTop();
	}

	@Override
	public HeapEnvWithFields bottom() {
		return isBottom() ? this : new HeapEnvWithFields(lattice.bottom(), null, fields.bottom());
	}

	@Override
	public boolean isBottom() {
		return super.isBottom() && fields.isBottom();
	}

	@Override
	public HeapEnvWithFields mk(
			AllocationSites lattice,
			Map<Identifier, AllocationSites> function) {
		return new HeapEnvWithFields(lattice, function, fields);
	}

	@Override
	public List<HeapReplacement> expand(
			HeapReplacement base)
			throws SemanticException {
		HeapReplacement sub = new HeapReplacement();
		for (Identifier id : base.getSources())
			lattice.reachableOnlyFrom(this, id).forEach(k -> {
				sub.addSource(k);
				if (k instanceof AllocationSite)
					for (SymbolicExpression field : fields.getOtDefault((AllocationSite) k, new ExpressionSet()))
						sub.addSource(((AllocationSite) k).withField(field));
			});
		return List.of(sub);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		HeapEnvWithFields other = (HeapEnvWithFields) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}

}
