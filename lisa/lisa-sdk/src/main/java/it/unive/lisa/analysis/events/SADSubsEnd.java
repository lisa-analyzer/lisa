package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import java.util.List;

/**
 * An event signaling the end of the substitutions application by a
 * {@link SimpleAbstractDomain}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <V> the type of {@link ValueLattice} handled by the analysis
 * @param <T> the type of {@link TypeLattice} handled by the analysis
 */
public class SADSubsEnd<V extends ValueLattice<V>, T extends TypeLattice<T>>
		extends
		Event
		implements
		DomainEvent,
		EndEvent {

	private final V value;
	private final V valueAfter;
	private final T type;
	private final T typeAfter;
	private final List<HeapReplacement> subs;

	/**
	 * Builds the event.
	 * 
	 * @param value      the value before the substitutions
	 * @param valueAfter the value after the substitutions
	 * @param type       the type before the substitutions
	 * @param typeAfter  the type after the substitutions
	 * @param subs       the substitutions applied
	 */
	public SADSubsEnd(
			V value,
			V valueAfter,
			T type,
			T typeAfter,
			List<HeapReplacement> subs) {
		this.value = value;
		this.valueAfter = valueAfter;
		this.type = type;
		this.typeAfter = typeAfter;
		this.subs = subs;
	}

	/**
	 * Yields the value before the substitutions.
	 * 
	 * @return the value
	 */
	public V getValue() {
		return value;
	}

	/**
	 * Yields the value after the substitutions.
	 * 
	 * @return the value after
	 */
	public V getValueAfter() {
		return valueAfter;
	}

	/**
	 * Yields the type before the substitutions.
	 * 
	 * @return the type
	 */
	public T getType() {
		return type;
	}

	/**
	 * Yields the type after the substitutions.
	 * 
	 * @return the type after
	 */
	public T getTypeAfter() {
		return typeAfter;
	}

	/**
	 * Yields the substitutions applied.
	 * 
	 * @return the substitutions
	 */
	public List<HeapReplacement> getSubs() {
		return subs;
	}

	@Override
	public String getTarget() {
		return SimpleAbstractDomain.class.getSimpleName() + ": application of substitutions";
	}
}
