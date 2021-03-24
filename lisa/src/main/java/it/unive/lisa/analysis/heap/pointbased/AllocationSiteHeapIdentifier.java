package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * A heap identifier that track also the program point where the corresponding
 * heap location has been instantiated and a field (optional). This class is
 * used in {@link PointBasedHeap}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class AllocationSiteHeapIdentifier extends HeapIdentifier {

	private final ProgramPoint pp;
	private final SymbolicExpression field;

	/**
	 * Builds an allocation site heap identifier from the program point where
	 * the corresponding heap location has been instantiated.
	 * 
	 * @param types the runtime types of this heap identifier
	 * @param pp    the program point this heap identifier site refers to
	 */
	public AllocationSiteHeapIdentifier(ExternalSet<Type> types, ProgramPoint pp) {
		this(types, pp, null);
	}

	/**
	 * Builds an allocation site heap identifier from the program point where
	 * the corresponding heap location has been instantiated and a field of this
	 * identifier.
	 * 
	 * @param types the runtime types of this heap identifier
	 * @param pp    the program point this allocation site refers to
	 * @param field the field of this heap identifier
	 */
	public AllocationSiteHeapIdentifier(ExternalSet<Type> types, ProgramPoint pp, SymbolicExpression field) {
		super(types, pp.hashCode() + "[" + field + "]", true);
		this.pp = pp;
		this.field = field;
	}

	/**
	 * Returns the program point of this heap identifier.
	 * 
	 * @return the program point
	 */
	public ProgramPoint getProgramPoint() {
		return pp;
	}

	@Override
	public String toString() {
		String ppStr = String.valueOf(pp.hashCode());
		return "pp@" + (ppStr.length() > 5 ? ppStr.substring(0, 5) : ppStr) + (field == null ? "" : "[" + field + "]");
	}

}
