package it.unive.lisa.triage.pdr;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The frames of property-directed reachability, kept per location. Frame
 * {@code F_i(l)} over-approximates the states at location {@code l} that are
 * reachable in at most {@code i} steps, and it is represented as the
 * conjunction of the <i>lemmas</i> of {@code l} whose level is at least
 * {@code i} (delta encoding): each lemma is stored only once, at the highest
 * level where it is known to hold. Lemmas at level {@link #INFINITY} hold at
 * every level, and they form an inductive invariant. Frame {@code F_0} is the
 * set of initial states, and it is not represented here.<br>
 * <br>
 * The frames are monotone by construction: {@code F_{i+1}(l) ⊆ F_i(l)} as sets
 * of lemmas implies the reverse inclusion between the represented sets of
 * states. If, after propagation, no lemma has exactly level {@code i} at any
 * location, then {@code F_i = F_{i+1}} everywhere, and the lemmas with level at
 * least {@code i} are an inductive invariant ({@link #convergedLevel()}).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <P> the type of locations
 * @param <L> the type of lemmas
 */
public class Frames<P, L> {

	/**
	 * The level of lemmas that hold at every level.
	 */
	public static final int INFINITY = Integer.MAX_VALUE;

	private final Map<P, Map<L, Integer>> lemmas = new LinkedHashMap<>();

	private int depth = 1;

	/**
	 * Builds the frames, with only frame {@code F_1}.
	 */
	public Frames() {
	}

	/**
	 * Yields the index of the last frame.
	 *
	 * @return the index
	 */
	public int depth() {
		return depth;
	}

	/**
	 * Adds a new, empty frame after the last one.
	 */
	public void extend() {
		depth++;
	}

	/**
	 * Records that the given lemma holds at the given location in all frames up
	 * to the given level (included). If the lemma is already known to hold up
	 * to a higher level, nothing changes.
	 *
	 * @param location the location
	 * @param lemma    the lemma
	 * @param level    the level, between {@code 1} and {@link #depth()}, or
	 *                     {@link #INFINITY}
	 *
	 * @return {@code true} if the frames changed
	 *
	 * @throws IllegalArgumentException if the level is out of bounds
	 */
	public boolean add(
			P location,
			L lemma,
			int level) {
		if (level < 1 || (level > depth && level != INFINITY))
			throw new IllegalArgumentException("Level " + level + " outside of [1, " + depth + "]");
		Map<L, Integer> map = lemmas.computeIfAbsent(location, l -> new LinkedHashMap<>());
		Integer previous = map.get(lemma);
		if (previous != null && previous >= level)
			return false;
		map.put(lemma, level);
		return true;
	}

	/**
	 * Yields the level of the given lemma at the given location.
	 *
	 * @param location the location
	 * @param lemma    the lemma
	 *
	 * @return the level, or {@code 0} if the lemma is not in any frame
	 */
	public int levelOf(
			P location,
			L lemma) {
		Map<L, Integer> map = lemmas.get(location);
		if (map == null)
			return 0;
		return map.getOrDefault(lemma, 0);
	}

	/**
	 * Yields the lemmas whose conjunction is frame {@code F_level} at the given
	 * location, that is, the ones with level at least {@code level}.
	 *
	 * @param location the location
	 * @param level    the level
	 *
	 * @return the lemmas
	 */
	public Set<L> frame(
			P location,
			int level) {
		return filter(location, level, false);
	}

	/**
	 * Yields the lemmas of the given location with exactly the given level:
	 * these are the candidates for propagation to the next level.
	 *
	 * @param location the location
	 * @param level    the level
	 *
	 * @return the lemmas
	 */
	public Set<L> exactly(
			P location,
			int level) {
		return filter(location, level, true);
	}

	private Set<L> filter(
			P location,
			int level,
			boolean exact) {
		Map<L, Integer> map = lemmas.get(location);
		if (map == null)
			return Collections.emptySet();
		Set<L> result = new HashSet<>();
		for (Map.Entry<L, Integer> e : map.entrySet())
			if (exact ? e.getValue() == level : e.getValue() >= level)
				result.add(e.getKey());
		return result;
	}

	/**
	 * Yields the locations having at least one lemma.
	 *
	 * @return the locations
	 */
	public Set<P> locations() {
		return Collections.unmodifiableSet(lemmas.keySet());
	}

	/**
	 * Yields the smallest level {@code i}, with {@code 1 <= i < depth()}, such
	 * that no location has lemmas with exactly level {@code i}. At that level,
	 * {@code F_i = F_{i+1}} at every location, and the lemmas with level at
	 * least {@code i} form an inductive invariant (see
	 * {@link #invariant(int)}).
	 *
	 * @return the level, or {@code -1} if the frames did not converge
	 */
	public int convergedLevel() {
		for (int i = 1; i < depth; i++) {
			boolean empty = true;
			for (Map<L, Integer> map : lemmas.values())
				if (map.containsValue(i)) {
					empty = false;
					break;
				}
			if (empty)
				return i;
		}
		return -1;
	}

	/**
	 * Yields frame {@code F_level} at every location having lemmas there. When
	 * {@code level} is the one returned by {@link #convergedLevel()}, this is
	 * an inductive invariant.
	 *
	 * @param level the level
	 *
	 * @return the lemmas of each location
	 */
	public Map<P, Set<L>> invariant(
			int level) {
		Map<P, Set<L>> result = new HashMap<>();
		for (P location : lemmas.keySet()) {
			Set<L> frame = frame(location, level);
			if (!frame.isEmpty())
				result.put(location, frame);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("depth ").append(depth);
		for (Map.Entry<P, Map<L, Integer>> e : lemmas.entrySet())
			sb.append("\n").append(e.getKey()).append(": ").append(e.getValue());
		return sb.toString();
	}
}
