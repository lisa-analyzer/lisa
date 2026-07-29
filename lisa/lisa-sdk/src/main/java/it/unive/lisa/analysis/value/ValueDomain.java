package it.unive.lisa.analysis.value;

import it.unive.lisa.analysis.SemanticComponent;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValueAnalysis;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;

/**
 * A semantic domain that can evaluate the semantic of expressions that operate
 * on values, and not on memory locations. A value domain can handle instances
 * of {@link ValueExpression}s, and are associated to {@link ValueLattice}
 * instances.<br/>
 * <br/>
 * A {@link ValueDomain} can be employed in a {@link WholeValueAnalysis} to
 * cooperate with domains tracking different value kinds. The cooperation
 * happens through <i>constraints</i> (i.e., {@link BinaryExpression}s having a
 * constant on the left-hand side and an expression on the right-hand side). To
 * enable this cooperation the methods
 * {@link #canProcess(ValueExpression, ProgramPoint, SemanticOracle)} and
 * {@link #constraints(ValueDomain, ValueLattice, ValueExpression, ProgramPoint, SemanticOracle)}
 * must be overridden with the actual logic for the generation of constraints.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link ValueLattice} that this domain works with
 */
public interface ValueDomain<L extends ValueLattice<L>>
		extends
		ValueAbstraction,
		SemanticComponent<L, L, ValueExpression, Identifier> {

	/**
	 * Generates a set of constraints that model the concrete values of
	 * {@code e} in the state {@code state}. The constraints must be definite,
	 * as in with each constraint the set of concrete values shrinks. An empty
	 * set of constraints thus represents any possible concrete value. A
	 * {@code null} set of constraints represents a bottom value.<br/>
	 * <br/>
	 * Each constraint is given as a {@link BinaryExpression}, where the left
	 * operand is a constant/expression and the right operand is the expression
	 * whose value is being constrained, corresponding to the parameter
	 * {@code e}.<br/>
	 * <br/>
	 * The requesting domain is the one that is asking for the constraints, and
	 * it is used to avoid recursive calls to the same domain. The default
	 * implementation of this method returns an empty set of constraints unless
	 * the state is bottom, in which case it returns {@code null}.
	 * 
	 * @param requesting the domain that is requesting the constraints
	 * @param state      the abstract state from which the constraints are
	 *                       generated
	 * @param e          the expression whose value is being constrained
	 * @param pp         the program point at which the constraints are being
	 *                       generated
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return a set of constraints modeling the possible values of {@code e} in
	 *             the state {@code state}
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			L state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return null;
		return Collections.emptySet();
	}

	/**
	 * Builds a set of constraints that model the fact that {@code expr} is
	 * related to {@code constant} through {@code operator}.
	 *
	 * @param constantType the type of the constant
	 * @param constant     the constant value that {@code expr} is related to
	 * @param operator     the operator that relates {@code expr} and
	 *                         {@code constant}
	 * @param expr         the expression whose value is being constrained
	 * @param pp           the program point at which the constraints are being
	 *                         generated
	 *
	 * @return a set of constraints modeling the fact that {@code expr} is
	 *             related to {@code constant} through {@code operator}
	 */
	public static Set<BinaryExpression> makeConstraint(
			Type constantType,
			Object constant,
			BinaryOperator operator,
			SymbolicExpression expr,
			ProgramPoint pp) {
		return Collections.singleton(
				new BinaryExpression(
						pp.getProgram().getTypes().getBooleanType(),
						new Constant(constantType,
								constant,
								expr.getCodeLocation()),
						expr,
						operator,
						pp.getLocation()));
	}

	/**
	 * Builds a set of constraints that model the fact that {@code expr} is
	 * equal to {@code constant}.
	 *
	 * @param constantType the type of the constant
	 * @param constant     the constant value that {@code expr} is equal to
	 * @param expr         the expression whose value is being constrained
	 * @param pp           the program point at which the constraints are being
	 *                         generated
	 *
	 * @return a set of constraints modeling the fact that {@code expr} is equal
	 *             to {@code constant}
	 */
	public static Set<BinaryExpression> makeEqConstraint(
			Type constantType,
			Object constant,
			SymbolicExpression expr,
			ProgramPoint pp) {
		return makeConstraint(constantType, constant, ComparisonEq.INSTANCE, expr, pp);
	}

	/**
	 * Builds a set of constraints that model the fact that {@code expr} is in
	 * the range {@code [low, high]}. If {@code low} is {@code null}, then only
	 * the upper bound is generated. If {@code high} is {@code null}, then only
	 * the lower bound is generated. If both are {@code null}, then an empty set
	 * of constraints is returned.
	 *
	 * @param constantType the type of the constants
	 * @param low          the lower bound of the range, or {@code null} if no
	 *                         lower bound is to be generated
	 * @param high         the upper bound of the range, or {@code null} if no
	 *                         upper bound is to be generated
	 * @param expr         the expression whose value is being constrained
	 * @param pp           the program point at which the constraints are being
	 *                         generated
	 * 
	 * @return a set of constraints modeling the possible values of {@code expr}
	 *             in the range {@code [low, high]}
	 */
	public static Set<BinaryExpression> makeRangeConstraints(
			Type constantType,
			Object low,
			Object high,
			SymbolicExpression expr,
			ProgramPoint pp) {
		if (low == null && high == null)
			return Collections.emptySet();

		BinaryExpression lb = null, ub = null;
		if (low != null)
			lb = makeConstraint(constantType, low, ComparisonGe.INSTANCE, expr, pp).iterator().next();
		if (high != null)
			ub = makeConstraint(constantType, high, ComparisonLe.INSTANCE, expr, pp).iterator().next();

		if (low == null)
			return Collections.singleton(ub);
		if (high == null)
			return Collections.singleton(lb);
		return Set.of(lb, ub);
	}

}
