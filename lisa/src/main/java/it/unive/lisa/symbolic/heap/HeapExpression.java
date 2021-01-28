package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * A symbolic expression that identifies a location in the program's heap.
 * Instances of this expressions must be rewritten by {@link HeapDomain}s to a
 * {@link ValueExpression} before being evaluated from a {@link ValueDomain}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class HeapExpression extends SymbolicExpression {

	/**
	 * Builds the heap expression.
	 * 
	 * @param types the runtime types of this expression
	 */
	protected HeapExpression(ExternalSet<Type> types) {
		super(types);
	}

}
