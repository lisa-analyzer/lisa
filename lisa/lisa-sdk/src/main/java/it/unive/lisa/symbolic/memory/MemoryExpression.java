package it.unive.lisa.symbolic.memory;

import it.unive.lisa.analysis.memory.MemoryDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;

/**
 * A symbolic expression that identifies a location in the program's memory.
 * Instances of this expressions must be rewritten by {@link MemoryDomain}s to a
 * {@link ValueExpression} before being evaluated from a {@link ValueDomain}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class MemoryExpression
		extends
		SymbolicExpression {

	/**
	 * Builds the memory expression.
	 * 
	 * @param type     the static types of this expression
	 * @param location the code location of the statement that has generated
	 *                     this memory expression
	 */
	protected MemoryExpression(
			Type type,
			CodeLocation location) {
		super(type, location);
	}

	@Override
	public final boolean mightNeedRewriting() {
		return true;
	}

}
