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
	public StackAllocationSite(Type staticType, String locationName, boolean isWeak, CodeLocation location) {
		this(staticType, locationName, null, isWeak, location);
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
	public StackAllocationSite(Type staticType, String locationName, SymbolicExpression field, boolean isWeak,
			CodeLocation location) {
		super(staticType, locationName, field, isWeak, location);
	}

}
