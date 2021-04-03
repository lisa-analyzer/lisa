package it.unive.lisa.analysis.impl.heap.pointbased;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * A heap identifier that track also the source location where it has been
 * allocated and a field (optional). This class is used in
 * {@link PointBasedHeap} and {@link FieldSensitivePointBasedHeap}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class AllocationSite extends HeapLocation {

	private final String id;

	/**
	 * Builds a strong allocation site from its source code location (without
	 * field).
	 * 
	 * @param types the runtime types of this allocation site
	 * @param id    the source code location string representation where this
	 *                  allocation site has been allocated
	 */
	public AllocationSite(ExternalSet<Type> types, String id) {
		this(types, id, false);
	}

	/**
	 * Builds an allocation site from its source code location (without field)
	 * and specifying if it is weak.
	 * 
	 * @param types  the runtime types of this allocation site
	 * @param id     the source code location string representation where this
	 *                   allocation site has been allocated
	 * @param isWeak boolean value specifying if this allocation site is weak
	 */
	public AllocationSite(ExternalSet<Type> types, String id, boolean isWeak) {
		this(types, id, null, isWeak);
	}

	/**
	 * Builds a strong allocation site from its source code location and its
	 * field.
	 * 
	 * @param types the runtime types of this allocation site
	 * @param id    the source code location string representation where this
	 *                  allocation site has been allocated
	 * @param field the field of this allocation site
	 */
	public AllocationSite(ExternalSet<Type> types, String id, SymbolicExpression field) {
		this(types, id, field, false);
	}

	/**
	 * Builds an allocation site from its source code location and its field and
	 * specifying if it is weak.
	 * 
	 * @param types  the runtime types of this allocation site
	 * @param id     the source code location string representation where this
	 *                   allocation site has been allocated
	 * @param field  the field of this allocation site
	 * @param isWeak boolean value specifying if this allocation site is weak
	 */
	public AllocationSite(ExternalSet<Type> types, String id, SymbolicExpression field, boolean isWeak) {
		super(types, "pp@" + id + (field == null ? "" : "[" + field + "]"), isWeak);
		this.id = id;
	}

	/**
	 * Returns the source code location string representation where this
	 * allocation site has been allocated.
	 * 
	 * @return the source code location string representation where this
	 *             allocation site has been allocated.
	 */
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return getName() + "[" + (isWeak() ? "w" : "s") + "]";
	}
}
