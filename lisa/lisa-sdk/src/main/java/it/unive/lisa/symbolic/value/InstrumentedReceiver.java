package it.unive.lisa.symbolic.value;

import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;

/**
 * An instrumented receiver, that is, a reference to a location being created
 * and initialized by the program. This is a specialized variable that is used
 * to represent objects, arrays, or other data structures that are being
 * initialized and that have not been assigned to a variable yet, and thus live
 * on the stack.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InstrumentedReceiver
		extends
		Variable {

	/**
	 * Builds the instrumented receiver.
	 * 
	 * @param staticType the static type of this receiver, determined at its
	 *                       construction
	 * @param array      whether this is an array receiver or an object receiver
	 * @param location   the code location of the statement that has generated
	 *                       this receiver
	 */
	public InstrumentedReceiver(
			Type staticType,
			boolean array,
			CodeLocation location) {
		this(staticType, array, new Annotations(), location);
	}

	/**
	 * Builds the instrumented receiver.
	 * 
	 * @param staticType  the static type of this receiver, determined at its
	 *                        construction
	 * @param array       whether this is an array receiver or an object
	 *                        receiver
	 * @param annotations the annotations of this receiver
	 * @param location    the code location of the statement that has generated
	 *                        this receiver
	 */
	public InstrumentedReceiver(
			Type staticType,
			boolean array,
			Annotations annotations,
			CodeLocation location) {
		super(staticType, getName(array, location), annotations, location);
	}

	/**
	 * Yields the name to be used for an instrumented receiver used for the
	 * creation of an entity at the given code location.
	 * 
	 * @param array    whether this is an array receiver or an object receiver
	 * @param location the code location where this receiver is created
	 * 
	 * @return the name to be used for this receiver
	 */
	public static String getName(
			boolean array,
			CodeLocation location) {
		return "$" + (array ? "array" : "rec") + "@" + location;
	}

	@Override
	public boolean isInstrumentedReceiver() {
		return true;
	}

}
