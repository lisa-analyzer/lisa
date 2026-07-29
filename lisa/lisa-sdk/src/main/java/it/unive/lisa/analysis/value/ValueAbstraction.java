package it.unive.lisa.analysis.value;

import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * An abstraction for the values of program variables. This interface is used as
 * a container for defining the
 * {@link #canProcess(ValueExpression, ProgramPoint, SemanticOracle)} method,
 * which is used to determine if a given expression can be processed by the
 * domain.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ValueAbstraction {

	/**
	 * Yields {@code true} if the domain can process {@code expression},
	 * {@code false} otherwise. Being able to process an expression means being
	 * able to abstract its value, that is, that the type of values produced by
	 * the expression is the type of value that this domain abstracts. Note that
	 * the expression itself might have sub-expressions that this domain cannot
	 * handle (e.g., a string domain should return {@code true} for a substring
	 * operation even if some operands are not strings, as long as the result is
	 * a string). Also note that "to abstract its value" has a loose meaning, as
	 * it applies also to produce facts about the value (e.g, its relation to
	 * other program variables or the location of its definition) or the
	 * generation of constraints over that value (as defined by
	 * {@link ValueDomain#constraints(ValueDomain, ValueLattice, ValueExpression, ProgramPoint, SemanticOracle)}).
	 *
	 * @param e      the expression
	 * @param pp     the program point where this method is queried
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@code true} if this domain can process the value of {@code e},
	 *             {@code false} otherwise
	 */
	boolean canProcess(
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle);
}
