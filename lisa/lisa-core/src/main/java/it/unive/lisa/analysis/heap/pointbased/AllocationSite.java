package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.type.Type;

/**
 * A heap identifier that track also the source location where it has been
 * allocated and a field (optional). This class is used in
 * {@link PointBasedHeap} and {@link FieldSensitivePointBasedHeap}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class AllocationSite extends HeapLocation {

	private final String locationName;

	private final String field;

	/**
	 * Builds a strong allocation site from its source code location (without
	 * field).
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(Type staticType, String locationName, CodeLocation location) {
		this(staticType, locationName, false, location);
	}

	/**
	 * Builds an allocation site from its source code location (without field)
	 * and specifying if it is weak.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param isWeak       boolean value specifying if this allocation site is
	 *                         weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(Type staticType, String locationName, boolean isWeak, CodeLocation location) {
		this(staticType, locationName, null, isWeak, location);
	}

	/**
	 * Builds a strong allocation site from its source code location and its
	 * field.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(Type staticType, String locationName, SymbolicExpression field,
			CodeLocation location) {
		this(staticType, locationName, field, false, location);
	}

	/**
	 * Builds an allocation site from its source code location and its field and
	 * specifying if it is weak.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param isWeak       boolean value specifying if this allocation site is
	 *                         weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(Type staticType, String locationName, SymbolicExpression field, boolean isWeak,
			CodeLocation location) {
		super(staticType, "pp@" + locationName + (field == null ? "" : "[" + field + "]"), isWeak, location);
		this.locationName = locationName;
		this.field = field.toString();
	}

	/**
	 * Yields the code location string representation where this allocation site
	 * has been allocated.
	 * 
	 * @return the code location string representation where this allocation
	 *             site has been allocated.
	 */
	public String getLocationName() {
		return locationName;
	}

	/**
	 * Yields the string that identifies the subfield of the location
	 * ({@link #getLocationName()}) that this allocation site has been
	 * allocated.
	 * 
	 * @return the subfield name (can be {@code null})
	 */
	public String getField() {
		return field;
	}
}
