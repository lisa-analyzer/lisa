package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.memory.MemoryDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.memory.MemoryExpression;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;

/**
 * A symbolic expression that represents an operation on the program's state.
 * For this expression to be evaluated from a {@link ValueDomain}, all nested
 * {@link MemoryExpression} must be rewritten by a {@link MemoryDomain}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class ValueExpression
		extends
		SymbolicExpression {

	/**
	 * Builds the memory expression.
	 * 
	 * @param staticType the static type of this expression
	 * @param location   the code location of the statement that has generated
	 *                       this value expression
	 */
	protected ValueExpression(
			Type staticType,
			CodeLocation location) {
		super(staticType, location);
	}

	/**
	 * Yields the same value expression removing any negation, namely the
	 * {@link LogicalNegation} operator, preserving its semantics, if possible.
	 * 
	 * @return the same value expression removing any negation, preserving its
	 *             semantics
	 */
	public ValueExpression removeNegations() {
		return this;
	}

}
