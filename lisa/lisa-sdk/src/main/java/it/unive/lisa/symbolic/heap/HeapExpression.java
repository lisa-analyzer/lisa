package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;

/**
 * A symbolic expression that identifies a location in the program's heap.
 * Instances of this expressions must be rewritten by {@link HeapDomain}s to a
 * {@link ValueExpression} before being evaluated from a {@link ValueDomain}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class HeapExpression
		extends
		SymbolicExpression {

	/**
	 * Builds the heap expression.
	 * 
	 * @param type     the static types of this expression
	 * @param location the code location of the statement that has generated
	 *                     this heap expression
	 */
	protected HeapExpression(
			Type type,
			CodeLocation location) {
		super(type, location);
	}

	@Override
	public final boolean mightNeedRewriting() {
		return true;
	}

}
