package it.unive.lisa.analysis.combination.constraints;

import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
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

	/**
	 * Yields the constraints modeling the indexes of the first occurrences of 
	 * of {@code other} in {@code this}.
	 *
	 * @param other the string to be searched
	 * 
	 * @return the constraints denoting the indexes of the first occurrences
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	Set<BinaryExpression> indexOf_constr(
			BinaryExpression expression,
			D other, 
			ProgramPoint pp)
			throws SemanticException;

	D substring(Set<BinaryExpression> a1, 
				Set<BinaryExpression> a2, 
				ProgramPoint pp) 
				throws SemanticException;
}
