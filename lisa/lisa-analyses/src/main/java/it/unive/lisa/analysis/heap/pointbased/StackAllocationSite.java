package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A stack allocation site, that is an allocation site pointing to something
 * that has been allocated in the stack.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class StackAllocationSite extends AllocationSite {

	/**
	 * Builds a stack allocation site.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param isWeak       if this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			boolean isWeak,
			CodeLocation location) {
		this(staticType, locationName, (String) null, isWeak, location);
	}

	/**
	 * Builds a stack allocation site.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param isWeak       if this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, isWeak, location);
	}

	/**
	 * Builds a stack allocation site.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the name of the field of this allocation site
	 * @param isWeak       if this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			String field,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, isWeak, location);
	}

	@Override
	public StackAllocationSite toWeak() {
		return isWeak() ? this
				: new StackAllocationSite(
						getStaticType(),
						getLocationName(),
						getField(),
						true,
						getCodeLocation());
	}

	@Override
	public StackAllocationSite withField(
			SymbolicExpression field) {
		if (getField() != null)
			throw new IllegalStateException("Cannot add a field to an allocation site that already has one");
		return new StackAllocationSite(
				getStaticType(),
				getLocationName(),
				field,
				isWeak(),
				getCodeLocation());
	}

	@Override
	public StackAllocationSite withType(
			Type type) {
		return new StackAllocationSite(
				type,
				getLocationName(),
				getField(),
				isWeak(),
				getCodeLocation());
	}

	@Override
	public StackAllocationSite withoutField() {
		if (getField() == null)
			return this;
		return new StackAllocationSite(
				getStaticType(),
				getLocationName(),
				isWeak(),
				getCodeLocation());
	}

	@Override
	public StackAllocationSite asNonAllocation() {
		if (!isAllocation())
			return this;
		return new StackAllocationSite(
				getStaticType(),
				getLocationName(),
				getField(),
				isWeak(),
				getCodeLocation());
	}
}
