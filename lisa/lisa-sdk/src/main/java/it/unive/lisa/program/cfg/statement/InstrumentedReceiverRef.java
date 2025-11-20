package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;

/**
 * A reference to an instrumented receiver, that is, a reference to a location
 * being created and initialized by the program. This is a specialized variable
 * reference that is used to represent objects, arrays, or other data structures
 * that are being initialized and that have not been assigned to a variable yet,
 * and thus live on the stack.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InstrumentedReceiverRef
		extends
		VariableRef {

	private final boolean isArray;

	/**
	 * Creates a reference to an instrumented receiver.
	 * 
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location where the expression is defined within the
	 *                     program
	 * @param array    whether this is an array receiver or an object receiver
	 */
	public InstrumentedReceiverRef(
			CFG cfg,
			CodeLocation location,
			boolean array) {
		super(cfg, location, InstrumentedReceiver.getName(array, location));
		this.isArray = array;
	}

	/**
	 * Creates a reference to an instrumented receiver.
	 * 
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location where the expression is defined within the
	 *                     program
	 * @param array    whether this is an array receiver or an object receiver
	 * @param type     the type of this receiver
	 */
	public InstrumentedReceiverRef(
			CFG cfg,
			CodeLocation location,
			boolean array,
			Type type) {
		super(cfg, location, InstrumentedReceiver.getName(array, location), type);
		this.isArray = array;
	}

	@Override
	public Variable getVariable() {
		InstrumentedReceiver v = new InstrumentedReceiver(getStaticType(), isArray, getAnnotations(), getLocation());
		return v;
	}

}
