package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import java.util.List;

/**
 * An event signaling the start of the substitutions application by a
 * {@link SimpleAbstractDomain}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <V> the type of {@link ValueLattice} handled by the analysis
 * @param <T> the type of {@link TypeLattice} handled by the analysis
 */
public class SADSubsStart<V extends ValueLattice<V>, T extends TypeLattice<T>>
		extends
		Event
		implements
		DomainEvent,
		StartEvent {

	private final V value;
	private final T type;
	private final List<HeapReplacement> subs;

	/**
	 * Builds the event.
	 * 
	 * @param value the value before the substitutions
	 * @param type  the type before the substitutions
	 * @param subs  the substitutions applied
	 */
	public SADSubsStart(
			V value,
			T type,
			List<HeapReplacement> subs) {
		this.value = value;
		this.type = type;
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
	 * Yields the type before the substitutions.
	 * 
	 * @return the type
	 */
	public T getType() {
		return type;
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
