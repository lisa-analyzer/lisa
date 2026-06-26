package it.unive.lisa.lattices.memory.allocations;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A memory allocation site, that is an allocation site pointing to something
 * allocated in the dynamic memory, the memory.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class MemoryAllocationSite
		extends
		AllocationSite {

	/**
	 * Builds a memory allocation site.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param isWeak       if this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public MemoryAllocationSite(
			Type staticType,
			String locationName,
			boolean isWeak,
			CodeLocation location) {
		this(staticType, locationName, (String) null, isWeak, location);
	}

	/**
	 * Builds a memory allocation site.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param isWeak       if this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public MemoryAllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, isWeak, location);
	}

	/**
	 * Builds a memory allocation site.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the name of the field of this allocation site
	 * @param isWeak       if this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public MemoryAllocationSite(
			Type staticType,
			String locationName,
			String field,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, isWeak, location);
	}

	@Override
	public MemoryAllocationSite toWeak() {
		return isWeak() ? this
				: new MemoryAllocationSite(getStaticType(), getLocationName(), getField(), true, getCodeLocation());
	}

	@Override
	public MemoryAllocationSite withField(
			SymbolicExpression field) {
		if (getField() != null)
			throw new IllegalStateException("Cannot add a field to an allocation site that already has one");
		return new MemoryAllocationSite(getStaticType(), getLocationName(), field, isWeak(), getCodeLocation());
	}

	@Override
	public MemoryAllocationSite withType(
			Type type) {
		return new MemoryAllocationSite(type, getLocationName(), getField(), isWeak(), getCodeLocation());
	}

	@Override
	public MemoryAllocationSite withoutField() {
		if (getField() == null)
			return this;
		return new MemoryAllocationSite(getStaticType(), getLocationName(), isWeak(), getCodeLocation());
	}

	@Override
	public MemoryAllocationSite asNonAllocation() {
		if (!isAllocation())
			return this;
		return new MemoryAllocationSite(getStaticType(), getLocationName(), isWeak(), getCodeLocation());
	}

}
