package it.unive.lisa.analysis;

import it.unive.lisa.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;

/**
 * A generic mapping from string keys to {@link Lattice} instances that can
 * store custom user-defined information inside the {@link AnalysisState}. This
 * class itself is a special case of {@link FunctionalLattice} where values are
 * not required to have the same type.<br>
 * <br>
 * Keys that are missing from the mapping are treated as carrying no
 * information: the least upper bound of two instances contains the union of
 * their keys, and an empty mapping is less or equal than any other one. Hence,
 * an empty mapping is <b>not</b> the top element of this lattice, that is
 * instead represented by the unique instance {@link #TOP}.<br>
 * <br>
 * Values that are also {@link ScopedObject}s are subject to scoping: when a
 * scope is pushed or popped on this object, the operation is forwarded to each
 * of them, while the remaining values are left untouched.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FixpointInfo
		implements
		BaseLattice<FixpointInfo>,
		ScopedObject<FixpointInfo>,
		Iterable<Map.Entry<String, Lattice<?>>> {

	/**
	 * The unique bottom instance of this class.
	 */
	public static final FixpointInfo BOTTOM = new FixpointInfo() {

		@Override
		public boolean isBottom() {
			return true;
		}

		@Override
		public boolean isTop() {
			return false;
		}

	};

	/**
	 * The unique top instance of this class.
	 */
	public static final FixpointInfo TOP = new FixpointInfo() {

		@Override
		public boolean isBottom() {
			return false;
		}

		@Override
		public boolean isTop() {
			return true;
		}

	};

	/**
	 * The function containing the additional information.
	 */
	public Map<String, Lattice<?>> function;

	/**
	 * Builds the function.
	 */
	public FixpointInfo() {
		this(null);
	}

	/**
	 * Builds the function by using the given mapping.
	 * 
	 * @param mapping the mapping to use
	 */
	public FixpointInfo(
			Map<String, Lattice<?>> mapping) {
		this.function = mapping != null && mapping.isEmpty() ? null : mapping;
	}

	private Map<String, Lattice<?>> mkNewFunction(
			Map<String, Lattice<?>> other,
			boolean preserveNull) {
		if (other == null)
			return preserveNull ? null : new HashMap<>();
		return new HashMap<>(other);
	}

	/**
	 * Yields the information associated to the given key.
	 * 
	 * @param key the key
	 * 
	 * @return the mapped information
	 */
	public Lattice<?> get(
			String key) {
		if (isBottom() || isTop() || function == null)
			return null;
		return function.get(key);
	}

	/**
	 * Yields the information associated to the given key, casted to the given
	 * type.
	 * 
	 * @param <T>  the type to cast the return value of this method to
	 * @param key  the key
	 * @param type the type to cast the retrieved information to
	 * 
	 * @return the mapped information
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(
			String key,
			Class<T> type) {
		return (T) get(key);
	}

	/**
	 * Yields a new instance of this class where the given information has been
	 * mapped to the given key. This is a strong update, meaning that the
	 * information previously mapped to the same key, if any, is lost. For a
	 * weak update, use {@link #putWeak(String, Lattice)}.
	 * 
	 * @param key  the key
	 * @param info the information to store
	 * 
	 * @return a new instance with the updated mapping
	 */
	public FixpointInfo put(
			String key,
			Lattice<?> info) {
		// we are only adding elements here, so it is fine to not preserve null
		Map<String, Lattice<?>> result = mkNewFunction(function, false);
		result.put(key, info);
		return new FixpointInfo(result);
	}

	/**
	 * Yields a new instance of this class where the given information has been
	 * mapped to the given key. This is a weak update, meaning that the
	 * information previously mapped to the same key, if any, is lubbed together
	 * with the given one, and the result is stored inside the mapping instead.
	 * For a strong update, use {@link #put(String, Lattice)}.
	 * 
	 * @param key  the key
	 * @param info the information to store
	 *
	 * @return a new instance with the updated mapping
	 *
	 * @throws SemanticException        if something goes wrong during the lub
	 * @throws IllegalArgumentException if this mapping already associates
	 *                                      {@code key} to a {@link Lattice}
	 *                                      instance whose runtime type differs
	 *                                      from {@code info}'s one
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public FixpointInfo putWeak(
			String key,
			Lattice<?> info)
			throws SemanticException {
		Map<String, Lattice<?>> result = mkNewFunction(function, false);
		// need to leave this raw to not have the compiler complaining about the
		// lub invocation
		Lattice prev = get(key);
		if (prev != null && prev.getClass() != info.getClass())
			throw new IllegalArgumentException(
					"The given lattice instance has a different type ("
							+ info.getClass().getName()
							+ ") from the one already associated with the given key ("
							+ prev.getClass().getName()
							+ ")");
		result.put(key, prev != null ? prev.lub(info) : info);
		return new FixpointInfo(result);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FixpointInfo lubAux(
			FixpointInfo other)
			throws SemanticException {
		Map<String, Lattice<?>> function = mkNewFunction(null, false);
		Set<String> keys = SetUtils.union(this.getKeys(), other.getKeys());
		for (String key : keys)
			try {
				// need to leave this raw to not have the compiler complaining
				// about the lub invocation
				Lattice v = this.get(key);
				Lattice<?> o = other.get(key);
				if (v != null && o != null)
					function.put(key, v.lub(o));
				else if (v != null)
					function.put(key, v);
				else
					function.put(key, o);
			} catch (SemanticException e) {
				throw new SemanticException("Exception while operating on '" + key + "'", e);
			}
		return new FixpointInfo(function);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FixpointInfo upchainAux(
			FixpointInfo other)
			throws SemanticException {
		Map<String, Lattice<?>> function = mkNewFunction(null, false);
		Set<String> keys = SetUtils.union(this.getKeys(), other.getKeys());
		for (String key : keys)
			try {
				// need to leave this raw to not have the compiler complaining
				// about the chain invocation
				Lattice v = this.get(key);
				Lattice<?> o = other.get(key);
				if (v != null && o != null)
					function.put(key, v.upchain(o));
				else if (v != null)
					function.put(key, v);
				else
					function.put(key, o);
			} catch (SemanticException e) {
				throw new SemanticException("Exception while operating on '" + key + "'", e);
			}
		return new FixpointInfo(function);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FixpointInfo glbAux(
			FixpointInfo other)
			throws SemanticException {
		Map<String, Lattice<?>> function = mkNewFunction(null, false);
		Set<String> keys = SetUtils.intersection(this.getKeys(), other.getKeys());
		for (String key : keys)
			try {
				// need to leave this raw to not have the compiler complaining
				// about the lub invocation
				Lattice v = this.get(key);
				Lattice<?> o = other.get(key);
				if (v != null && o != null)
					function.put(key, v.glb(o));
			} catch (SemanticException e) {
				throw new SemanticException("Exception while operating on '" + key + "'", e);
			}
		return new FixpointInfo(function);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FixpointInfo downchainAux(
			FixpointInfo other)
			throws SemanticException {
		Map<String, Lattice<?>> function = mkNewFunction(null, false);
		Set<String> keys = SetUtils.intersection(this.getKeys(), other.getKeys());
		for (String key : keys)
			try {
				// need to leave this raw to not have the compiler complaining
				// about the chain invocation
				Lattice v = this.get(key);
				Lattice<?> o = other.get(key);
				if (v != null && o != null)
					function.put(key, v.downchain(o));
			} catch (SemanticException e) {
				throw new SemanticException("Exception while operating on '" + key + "'", e);
			}
		return new FixpointInfo(function);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FixpointInfo wideningAux(
			FixpointInfo other)
			throws SemanticException {
		Map<String, Lattice<?>> function = mkNewFunction(null, false);
		Set<String> keys = SetUtils.union(this.getKeys(), other.getKeys());
		for (String key : keys)
			try {
				// need to leave this raw to not have the compiler complaining
				// about the lub invocation
				Lattice v = this.get(key);
				Lattice<?> o = other.get(key);
				if (v != null && o != null)
					function.put(key, v.widening(o));
				else if (v != null)
					function.put(key, v);
				else
					function.put(key, o);
			} catch (SemanticException e) {
				throw new SemanticException("Exception while operating on '" + key + "'", e);
			}
		return new FixpointInfo(function);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FixpointInfo narrowingAux(
			FixpointInfo other)
			throws SemanticException {
		Map<String, Lattice<?>> function = mkNewFunction(null, false);
		Set<String> keys = SetUtils.intersection(this.getKeys(), other.getKeys());
		for (String key : keys)
			try {
				// need to leave this raw to not have the compiler complaining
				// about the lub invocation
				Lattice v = this.get(key);
				Lattice<?> o = other.get(key);
				if (v != null && o != null)
					function.put(key, v.narrowing(o));
			} catch (SemanticException e) {
				throw new SemanticException("Exception while operating on '" + key + "'", e);
			}
		return new FixpointInfo(function);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean lessOrEqualAux(
			FixpointInfo other)
			throws SemanticException {
		if (function != null)
			for (String key : function.keySet()) {
				// need to leave this raw to not have the compiler complaining
				// about the lub invocation
				Lattice v = get(key);
				Lattice<?> o = other.get(key);
				if (v != null && (o == null || !v.lessOrEqual(o)))
					return false;
			}

		return true;
	}

	@Override
	public FixpointInfo top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return false;
	}

	@Override
	public FixpointInfo bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((function == null) ? 0 : function.hashCode());
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
		FixpointInfo other = (FixpointInfo) obj;
		if (function == null) {
			if (other.function != null)
				return false;
		} else if (!function.equals(other.function))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (isTop())
			return Lattice.TOP_STRING;

		if (isBottom())
			return Lattice.BOTTOM_STRING;

		return function == null ? "{}" : function.toString();
	}

	@Override
	public Iterator<Entry<String, Lattice<?>>> iterator() {
		if (function == null)
			return Collections.emptyIterator();
		return function.entrySet().iterator();
	}

	/**
	 * Yields the set of keys currently in this mapping.
	 * 
	 * @return the set of keys
	 */
	public Set<String> getKeys() {
		if (function == null)
			return Collections.emptySet();
		return function.keySet();
	}

	/**
	 * Yields the values currently in this mapping.
	 * 
	 * @return the set of values
	 */
	public Collection<Lattice<?>> getValues() {
		if (function == null)
			return Collections.emptySet();
		return function.values();
	}

	/**
	 * Yields the map associated with this mapping.
	 * 
	 * @return the associated map
	 */
	public Map<String, Lattice<?>> getMap() {
		return function;
	}

	@Override
	public FixpointInfo pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return onScopedValues(v -> (Lattice<?>) v.pushScope(token, pp));
	}

	@Override
	public FixpointInfo popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return onScopedValues(v -> (Lattice<?>) v.popScope(token, pp));
	}

	private FixpointInfo onScopedValues(
			ScopingFunction op)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<String, Lattice<?>> result = null;
		for (Entry<String, Lattice<?>> entry : function.entrySet())
			if (entry.getValue() instanceof ScopedObject)
				try {
					Lattice<?> scoped = op.apply((ScopedObject<?>) entry.getValue());
					if (scoped != entry.getValue()) {
						if (result == null)
							result = mkNewFunction(function, false);
						result.put(entry.getKey(), scoped);
					}
				} catch (SemanticException e) {
					throw new SemanticException("Exception while scoping '" + entry.getKey() + "'", e);
				}

		return result == null ? this : new FixpointInfo(result);
	}

	/**
	 * Yields the information after returning from a call, given that this is
	 * the information at the end of the callee, and the given one is the
	 * information of the caller before the call. Each value of this mapping
	 * that is a {@link CallAwareObject} is combined with the value mapped to
	 * the same key in the caller's information, if it has the same type (see
	 * {@link CallAwareObject#onCallReturn(Object, ProgramPoint)}), while the
	 * remaining values are left untouched.
	 *
	 * @param caller the information of the caller, before the call
	 * @param call   the program point of the call
	 *
	 * @return the information after the call
	 *
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public FixpointInfo onCallReturn(
			FixpointInfo caller,
			ProgramPoint call)
			throws SemanticException {
		if (isTop() || isBottom() || function == null || caller == null)
			return this;

		Map<String, Lattice<?>> result = null;
		for (Entry<String, Lattice<?>> entry : function.entrySet()) {
			Lattice<?> value = entry.getValue();
			Lattice<?> before = caller.get(entry.getKey());
			if (!(value instanceof CallAwareObject) || before == null || before.getClass() != value.getClass())
				continue;
			try {
				// need to leave this raw to not have the compiler complaining
				// about the invocation
				Lattice<?> returned = (Lattice<?>) ((CallAwareObject) value).onCallReturn(before, call);
				if (returned != value) {
					if (result == null)
						result = mkNewFunction(function, false);
					result.put(entry.getKey(), returned);
				}
			} catch (SemanticException e) {
				throw new SemanticException("Exception while returning from a call on '" + entry.getKey() + "'", e);
			}
		}

		return result == null ? this : new FixpointInfo(result);
	}

	/**
	 * A scoping operation (push or pop) to apply to a scoped value.
	 */
	@FunctionalInterface
	private interface ScopingFunction {

		Lattice<?> apply(
				ScopedObject<?> value)
				throws SemanticException;

	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (function == null)
			return new StringRepresentation("empty");

		return new MapRepresentation(function, StringRepresentation::new, Lattice::representation);
	}

	/**
	 * Yields whether this object is empty, that is, it contains no information
	 * at all (i.e., it is either top or bottom, or the mapping is empty).
	 * 
	 * @return whether this mapping is empty
	 */
	public boolean isEmpty() {
		return isTop() || isBottom() || function == null || function.isEmpty();
	}

}
