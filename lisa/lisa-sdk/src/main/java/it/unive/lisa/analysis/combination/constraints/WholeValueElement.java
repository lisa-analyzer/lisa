package it.unive.lisa.analysis.combination.constraints;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Set;

/**
 * A {@link Lattice} that can be employed in a {@link WholeValue} to model the
 * concrete values of a given type. Different data types can be modeled by
 * different implementations of this interface, that communicate together
 * through <i>constraints</i> (i.e., {@link BinaryExpression}s having a constant
 * on the left-hand side and an expression on the right-hand side).
 * 
 * @param <L> the concrete type of this lattice
 */
public interface WholeValueElement<L extends WholeValueElement<L>>
		extends
		Lattice<L> {

	/**
	 * Generates a set of constraints that model the concrete values represented
	 * by this lattice element. The constraints must be definite, as in with
	 * each constraint the set of concrete values shrinks. An empty set of
	 * constraints thus represents any possible concrete value. A {@code null}
	 * set of constraints represents a bottom value.<br/>
	 * <br/>
	 * Each constraint is given as a {@link BinaryExpression}, where the left
	 * operand is a constant and the right operand is the expression whose value
	 * is being constrained, corresponding to the parameter {@code e}.
	 * 
	 * @param e  the expression whose value is being constrained
	 * @param pp the program point at which the constraints are being generated
	 * 
	 * @return a set of constraints modeling this abstract element
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	Set<BinaryExpression> constraints(
			ValueExpression e,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Generates a new instance of this lattice that overapproximates the given
	 * set of constraints. The constraints are definite, as in with each
	 * constraint the set of concrete values shrinks. An empty set of
	 * constraints thus represents any possible concrete value. A {@code null}
	 * set of constraints represents a bottom value.<br/>
	 * <br/>
	 * Each constraint is given as a {@link BinaryExpression}, where the left
	 * operand is a constant and the right operand is the expression whose value
	 * is being constrained.
	 * 
	 * @param constraints the constraints modeling the concrete values
	 * @param pp          the program point at which the constraints are being
	 *                        generated
	 * 
	 * @return a new instance of this lattice that overapproximates the given
	 *             constraints
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp)
			throws SemanticException;

}
