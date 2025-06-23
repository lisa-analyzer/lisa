package it.unive.lisa.analysis.combination.constraints;

import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.symbolic.value.BinaryExpression;

public interface WholeValueStringDomain<D extends WholeValueStringDomain<D>> 
        extends WholeValueDomain<D> {
	
	/**
	 * Simplified semantics of the string contains operator, checking a single
	 * character is part of the string.
	 * 
	 * @param c the character to check
	 * 
	 * @return whether or not the character is part of the string
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability containsChar(
			char c)
			throws SemanticException;

	D substring(Set<BinaryExpression> a1, 
				Set<BinaryExpression> a2) 
				throws SemanticException;
}
