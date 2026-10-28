package it.unive.lisa.lattices.heap.allocations;

import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.type.Type;

/**
 * A heap identifier used by allocation-site-based heap domains (see
 * {@link it.unive.lisa.analysis.heap.pointbased.AllocationSiteBasedAnalysis}),
 * that also tracks the source code location where it has been allocated
 * ({@link #getLocationName()}) and, optionally, a field accessed on it
 * ({@link #getField()}) for field-sensitive domains. Allocation sites can be
 * strong (representing exactly one runtime object) or weak (representing
 * possibly more than one, e.g., because they lie inside a loop), as reflected
 * by {@link #isWeak()}.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public abstract class AllocationSite
		extends
		HeapLocation {

	private final String locationName;

	private final String field;

	private final ScopeId<?> context;

	/**
	 * Builds a strong allocation site without field and context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(
			Type staticType,
			String locationName,
			CodeLocation location) {
		this(staticType, locationName, (String) null, null, false, location);
	}

	/**
	 * Builds an allocation site without field and context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param isWeak       whether or not this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(
			Type staticType,
			String locationName,
			boolean isWeak,
			CodeLocation location) {
		this(staticType, locationName, (String) null, null, isWeak, location);
	}

	/**
	 * Builds a strong allocation site with the given context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param context      the interprocedural context where this allocation
	 *                         site has been allocated (can be {@code null})
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(
			Type staticType,
			String locationName,
			ScopeId<?> context,
			CodeLocation location) {
		this(staticType, locationName, (String) null, context, false, location);
	}

	/**
	 * Builds an allocation site with the given context.
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
	public AllocationSite(
			Type staticType,
			String locationName,
			ScopeId<?> context,
			boolean isWeak,
			CodeLocation location) {
		this(staticType, locationName, (String) null, context, isWeak, location);
	}

	/**
	 * Builds a strong allocation site with the given field.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the name of the field of this allocation site
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(
			Type staticType,
			String locationName,
			String field,
			CodeLocation location) {
		this(staticType, locationName, field, null, false, location);
	}

	/**
	 * Builds an allocation site with the given field.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the name of the field of this allocation site
	 * @param isWeak       whether or not this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(
			Type staticType,
			String locationName,
			String field,
			boolean isWeak,
			CodeLocation location) {
		this(staticType, locationName, field, null, isWeak, location);
	}

	/**
	 * Builds a strong allocation site with the given field and context.
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
	public AllocationSite(
			Type staticType,
			String locationName,
			String field,
			ScopeId<?> context,
			CodeLocation location) {
		this(staticType, locationName, field, context, false, location);
	}

	/**
	 * Builds a strong allocation site with the given field.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			CodeLocation location) {
		this(staticType, locationName, field == null ? null : field.toString(), null, false, location);
	}

	/**
	 * Builds an allocation site with the given field.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param isWeak       whether or not this allocation site is weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			boolean isWeak,
			CodeLocation location) {
		this(staticType, locationName, field == null ? null : field.toString(), null, isWeak, location);
	}

	/**
	 * Builds a strong allocation site with the given field and context.
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
	public AllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			ScopeId<?> context,
			CodeLocation location) {
		this(staticType, locationName, field == null ? null : field.toString(), context, false, location);
	}

	/**
	 * Builds an allocation site with the given field and context.
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
	public AllocationSite(
			Type staticType,
			String locationName,
			SymbolicExpression field,
			ScopeId<?> context,
			boolean isWeak,
			CodeLocation location) {
		this(staticType, locationName, field == null ? null : field.toString(), context, isWeak, location);
	}

	/**
	 * Builds an allocation site from its source code location, its field, and
	 * the interprocedural context where it has been allocated, specifying if
	 * it is weak. The context is part of the identity of the allocation site
	 * (through {@link ScopeId#getHash()}) only if it is not {@code null} and it
	 * is not a starting id (see {@link ScopeId#isStartingId()}): otherwise, it
	 * is discarded and the allocation site is equivalent to one built without
	 * a context.
	 * 
	 * @param staticType   the static type of this allocation site
	 * @param locationName the source code location string representation where
	 *                         this allocation site has been allocated
	 * @param field        the field of this allocation site
	 * @param context      the interprocedural context where this allocation
	 *                         site has been allocated (can be {@code null})
	 * @param isWeak       boolean value specifying if this allocation site is
	 *                         weak
	 * @param location     the code location of the statement that has generated
	 *                         this expression
	 */
	public AllocationSite(
			Type staticType,
			String locationName,
			String field,
			ScopeId<?> context,
			boolean isWeak,
			CodeLocation location) {
		super(staticType, nameOf(locationName, field, context), isWeak, location);
		this.locationName = locationName;
		this.field = field;
		this.context = isQualifying(context) ? context : null;
	}

	private static boolean isQualifying(
			ScopeId<?> context) {
		return context != null && !context.isStartingId();
	}

	private static String nameOf(
			String locationName,
			String field,
			ScopeId<?> context) {
		return "pp@"
				+ locationName
				+ (isQualifying(context) ? "<" + context.getHash() + ">" : "")
				+ (field == null ? "" : "[" + field + "]");
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

	/**
	 * Yields the interprocedural context where this allocation site has been
	 * allocated. This is {@code null} if the allocation site is not
	 * context-sensitive, or if it has been allocated in a starting context
	 * (see {@link ScopeId#isStartingId()}).
	 * 
	 * @return the context (can be {@code null})
	 */
	public ScopeId<?> getContext() {
		return context;
	}

	/**
	 * Yields a weak copy of this allocation site, that is, a copy where
	 * {@link #isWeak()} returns {@code true}.
	 * 
	 * @return the weak copy
	 */
	public abstract AllocationSite toWeak();

	/**
	 * Yields a modified version of this allocation site by accessing the given
	 * field.
	 * 
	 * @param field the field to access
	 * 
	 * @return the modified allocation site
	 */
	public abstract AllocationSite withField(
			SymbolicExpression field);

	/**
	 * Yields a modified version of this allocation site by removing any field.
	 * 
	 * @return the modified allocation site
	 */
	public abstract AllocationSite withoutField();

	/**
	 * Yields a modified version of this allocation site with the given type.
	 * 
	 * @param type the new type
	 * 
	 * @return the modified allocation site
	 */
	public abstract AllocationSite withType(
			Type type);

	/**
	 * Yields a modified version of this allocation site allocated in the given
	 * interprocedural context.
	 * 
	 * @param context the new context (can be {@code null})
	 * 
	 * @return the modified allocation site
	 */
	public abstract AllocationSite withContext(
			ScopeId<?> context);

}
