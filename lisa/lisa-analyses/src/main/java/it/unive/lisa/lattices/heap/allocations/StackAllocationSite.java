package it.unive.lisa.lattices.heap.allocations;

import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A stack allocation site, that is, an {@link AllocationSite} pointing to
 * something that has been allocated on the stack, as opposed to
 * {@link HeapAllocationSite}s. Assigning a stack allocation site to an
 * identifier behaves as a shallow copy rather than as aliasing (see
 * {@code AllocationSiteBasedAnalysis.shallowCopy}), reflecting value semantics.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class StackAllocationSite
		extends
		AllocationSite {

	/**
	 * Builds a strong stack allocation site without field and context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			CodeLocation location) {
		super(staticType, locationName, location);
	}

	/**
	 * Builds a stack allocation site without field and context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param isWeak       whether or not this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, isWeak, location);
	}

	/**
	 * Builds a strong stack allocation site with the given context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param context      the interprocedural context where this allocation
	 *                         site has been allocated (can be {@code null})
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			ScopeId<?> context,
			CodeLocation location) {
		super(staticType, locationName, context, location);
	}

	/**
	 * Builds a stack allocation site with the given context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param context      the interprocedural context where this allocation
	 *                         site has been allocated (can be {@code null})
	 * @param isWeak       whether or not this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			ScopeId<?> context,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, context, isWeak, location);
	}

	/**
	 * Builds a strong stack allocation site with the given field.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the name of the field of this allocation site
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			String field,
			CodeLocation location) {
		super(staticType, locationName, field, location);
	}

	/**
	 * Builds a stack allocation site with the given field.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the name of the field of this allocation site
	 * @param isWeak       whether or not this allocation site is weak
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

	/**
	 * Builds a strong stack allocation site with the given field and context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the name of the field of this allocation site
	 * @param context      the interprocedural context where this allocation
	 *                         site has been allocated (can be {@code null})
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			String field,
			ScopeId<?> context,
			CodeLocation location) {
		super(staticType, locationName, field, context, location);
	}

	/**
	 * Builds a stack allocation site with the given field and context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the name of the field of this allocation site
	 * @param context      the interprocedural context where this allocation
	 *                         site has been allocated (can be {@code null})
	 * @param isWeak       whether or not this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			String field,
			ScopeId<?> context,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, context, isWeak, location);
	}

	/**
	 * Builds a strong stack allocation site with the given field.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			CodeLocation location) {
		super(staticType, locationName, field, location);
	}

	/**
	 * Builds a stack allocation site with the given field.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param isWeak       whether or not this allocation site is weak
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
	 * Builds a strong stack allocation site with the given field and context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param context      the interprocedural context where this allocation
	 *                         site has been allocated (can be {@code null})
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			ScopeId<?> context,
			CodeLocation location) {
		super(staticType, locationName, field, context, location);
	}

	/**
	 * Builds a stack allocation site with the given field and context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param context      the interprocedural context where this allocation
	 *                         site has been allocated (can be {@code null})
	 * @param isWeak       whether or not this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public StackAllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			ScopeId<?> context,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, context, isWeak, location);
	}

	@Override
	public StackAllocationSite toWeak() {
		return isWeak() ? this
				: new StackAllocationSite(getStaticType(), getLocationName(), getField(), getContext(), true,
						getCodeLocation());
	}

	@Override
	public StackAllocationSite withField(
			SymbolicExpression field) {
		if (getField() != null)
			throw new IllegalStateException("Cannot add a field to an allocation site that already has one");
		return new StackAllocationSite(getStaticType(), getLocationName(), field.toString(), getContext(), isWeak(),
				getCodeLocation());
	}

	@Override
	public StackAllocationSite withType(
			Type type) {
		return new StackAllocationSite(type, getLocationName(), getField(), getContext(), isWeak(), getCodeLocation());
	}

	@Override
	public StackAllocationSite withoutField() {
		if (getField() == null)
			return this;
		return new StackAllocationSite(getStaticType(), getLocationName(), getContext(), isWeak(),
				getCodeLocation());
	}

	@Override
	public StackAllocationSite asNonAllocation() {
		if (!isAllocation())
			return this;
		return new StackAllocationSite(getStaticType(), getLocationName(), getField(), getContext(), isWeak(),
				getCodeLocation());
	}

	@Override
	public StackAllocationSite withContext(
			ScopeId<?> context) {
		return new StackAllocationSite(getStaticType(), getLocationName(), getField(), context, isWeak(), getCodeLocation());
	}

}
