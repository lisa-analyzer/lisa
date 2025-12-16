package it.unive.lisa.analysis.combination.constraints;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import java.util.Set;

/**
 * A {@link BaseNonRelationalValueDomain} that is specifically designed to model
 * concrete values of a string type. This interface provides additional methods
 * to compute operations regarding strings where either the return value or the
 * arguments are of a different data type, and are thus expressed as
 * constraints.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of lattice produced by this domain
 */
public interface WholeValueStringDomain<L extends Lattice<L>>
		extends
		BaseNonRelationalValueDomain<L> {

	/**
	 * Simplified semantics of the string contains operator, checking a single
	 * character is part of the string.
	 * 
	 * @param current the lattice instance
	 * @param c       the character to check
	 * 
	 * @return whether or not the character is part of the string
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability containsChar(
			L current,
			char c)
			throws SemanticException;

	/**
	 * Yields the constraints modeling the indexes of the first occurrences of
	 * of {@code other} in {@code current}.
	 *
	 * @param expression the expression representing the indexOf operation, to
	 *                       be included in the constraints
	 * @param current    the lattice instance
	 * @param other      the string to be searched
	 * @param pp         the program point at which the constraints are being
	 *                       generated
	 * 
	 * @return the constraints denoting the indexes of the first occurrences
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	Set<BinaryExpression> indexOf_constr(
			BinaryExpression expression,
			L current,
			L other,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Yields the result of the substring operation, where the indexes are
	 * expressed as constraints.
	 * 
	 * @param current the lattice instance
	 * @param a1      the set of constraints representing the starting index
	 * @param a2      the set of constraints representing the ending index
	 * @param pp      the program point at which the operation is being computed
	 * 
	 * @return the result of the substring operation, expressed as a new
	 *             instance of this domain
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L substring(
			L current,
			Set<BinaryExpression> a1,
			Set<BinaryExpression> a2,
			ProgramPoint pp)
			throws SemanticException;

}
