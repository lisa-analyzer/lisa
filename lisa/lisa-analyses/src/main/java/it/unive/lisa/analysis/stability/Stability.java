package it.unive.lisa.analysis.stability;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticEvaluator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the stability abstract domain (
 * <a href="https://doi.org/10.1145/3689609.3689995">Stability paper</a>). This
 * domain computes per-variable numerical trends to infer stability, covariance
 * and contravariance relations on program variables, exploiting an auxiliary
 * domain of choice. This is implemented as an open product where the stability
 * domain gathers information from the auxiliary one through boolean
 * queries.<br>
 * <br>
 * Implementation-wise, this class is built as a product between a given
 * {@link ValueDomain} {@code aux} and a {@link ValueEnvironment} {@code trends}
 * of {@link Trend} instances, representing per-variable trends. Queries are
 * carried over by the
 * {@link SemanticDomain#satisfies(SymbolicExpression, ProgramPoint, SemanticOracle)}
 * operator invoked on {@code aux}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the kind of lattice tracked by the auxiliary domain
 */
public class Stability<L extends ValueLattice<L>>
		implements
		ValueDomain<ValueLatticeProduct<ValueEnvironment<Trend>, L>>,
		SemanticEvaluator {

	private final ValueDomain<L> aux;

	/**
	 * Builds the top stability domain, using {@code aux} as auxiliary domain.
	 * 
	 * @param aux the auxiliary domain
	 */
	public Stability(
			ValueDomain<L> aux) {
		this.aux = aux;
	}

	/**
	 * Yields {@code true} if the {@code aux.satisfies(query, pp, oracle)}
	 * returns {@link Satisfiability#SATISFIED}.
	 * 
	 * @param query  the query to execute
	 * @param pp     the {@link ProgramPoint} where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@code true} if the query is always satisfied
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	private boolean query(
			L state,
			BinaryExpression query,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return aux.satisfies(state, query, pp, oracle) == Satisfiability.SATISFIED;
	}

	/**
	 * Builds a {@link BinaryExpression} in the form of "l operator r".
	 * 
	 * @param operator the {@link BinaryOperator} to apply
	 * @param l        the left operand
	 * @param r        the right operand
	 * @param pp       the {@link ProgramPoint} where the expression is being
	 *                     built
	 *
	 * @return the new BinaryExpression
	 */
	private BinaryExpression binary(
			BinaryOperator operator,
			SymbolicExpression l,
			SymbolicExpression r,
			ProgramPoint pp) {
		return new BinaryExpression(
				pp.getProgram().getTypes().getBooleanType(),
				l,
				r,
				operator,
				SyntheticLocation.INSTANCE);
	}

	/**
	 * Builds a {@link Constant} with value {@code c}.
	 *
	 * @param c  the integer constant
	 * @param pp the {@link ProgramPoint} where the expression is being built
	 * 
	 * @return the new Constant
	 */
	private Constant constantInt(
			int c,
			ProgramPoint pp) {
		return new Constant(pp.getProgram().getTypes().getIntegerType(), c, SyntheticLocation.INSTANCE);
	}

	/**
	 * Generates a {@link Trend} based on the relationship between {@code a} and
	 * {@code b} in {@link #aux}.
	 * 
	 * @param a      the first expression
	 * @param b      the second expression
	 * @param pp     the {@link ProgramPoint} where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@link Trend#INC} if {@code a > b}
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	private Trend increasingIfGreater(
			L state,
			SymbolicExpression a,
			SymbolicExpression b,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (query(state, binary(ComparisonEq.INSTANCE, a, b, pp), pp, oracle))
			return Trend.STABLE;
		else if (query(state, binary(ComparisonGt.INSTANCE, a, b, pp), pp, oracle))
			return Trend.INC;
		else if (query(state, binary(ComparisonGe.INSTANCE, a, b, pp), pp, oracle))
			return Trend.NON_DEC;
		else if (query(state, binary(ComparisonLt.INSTANCE, a, b, pp), pp, oracle))
			return Trend.DEC;
		else if (query(state, binary(ComparisonLe.INSTANCE, a, b, pp), pp, oracle))
			return Trend.NON_INC;
		else if (query(state, binary(ComparisonNe.INSTANCE, a, b, pp), pp, oracle))
			return Trend.NON_STABLE;
		else
			return Trend.TOP;
	}

	/**
	 * Generates a {@link Trend} based on the relationship between {@code a} and
	 * {@code b} in {@link #aux}.
	 * 
	 * @param a      the first expression
	 * @param b      the second expression
	 * @param pp     the {@link ProgramPoint} where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@link Trend#INC} if {@code a < b}
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	private Trend increasingIfLess(
			L state,
			SymbolicExpression a,
			SymbolicExpression b,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return increasingIfGreater(state, a, b, pp, oracle).invert();
	}

	/**
	 * Generates a {@link Trend} based on the relationship between {@code a} and
	 * {@code b} in {@link #aux}.
	 * 
	 * @param a      the first expression
	 * @param b      the second expression
	 * @param pp     the {@link ProgramPoint} where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@link Trend#NON_DEC} if {@code a > b}
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	private Trend nonDecreasingIfGreater(
			L state,
			SymbolicExpression a,
			SymbolicExpression b,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (query(state, binary(ComparisonEq.INSTANCE, a, b, pp), pp, oracle))
			return Trend.STABLE;
		else if (query(state, binary(ComparisonGt.INSTANCE, a, b, pp), pp, oracle)
				|| query(state, binary(ComparisonGe.INSTANCE, a, b, pp), pp, oracle))
			return Trend.NON_DEC;
		else if (query(state, binary(ComparisonLt.INSTANCE, a, b, pp), pp, oracle)
				|| query(state, binary(ComparisonLe.INSTANCE, a, b, pp), pp, oracle))
			return Trend.NON_INC;
		else
			return Trend.TOP;
	}

	/**
	 * Generates a {@link Trend} based on the relationship between {@code a} and
	 * {@code b} in {@link #aux}.
	 * 
	 * @param a      the first expression
	 * @param b      the second expression
	 * @param pp     the {@link ProgramPoint} where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@link Trend#NON_DEC} if {@code a < b}
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	private Trend nonDecreasingIfLess(
			L state,
			SymbolicExpression a,
			SymbolicExpression b,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return nonDecreasingIfGreater(state, a, b, pp, oracle).invert();
	}

	/**
	 * Generates a {@link Trend} based on the relationship between {@code a} and
	 * {@code b} in {@link #aux}.
	 * 
	 * @param a      the expression
	 * @param pp     the {@link ProgramPoint} where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@link Trend#INC} if {@code 0 < a < 1 || 0 <= a < 1}
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	private Trend increasingIfBetweenZeroAndOne(
			L state,
			SymbolicExpression a,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Constant zero = constantInt(0, pp);
		Constant one = constantInt(1, pp);

		if (query(state, binary(ComparisonEq.INSTANCE, a, zero, pp), pp, oracle)
				|| query(state, binary(ComparisonEq.INSTANCE, a, one, pp), pp, oracle))
			return Trend.STABLE;
		else if (query(state, binary(ComparisonGt.INSTANCE, a, zero, pp), pp, oracle)
				&& query(state, binary(ComparisonLt.INSTANCE, a, one, pp), pp, oracle))
			return Trend.INC;
		else if (query(state, binary(ComparisonGe.INSTANCE, a, zero, pp), pp, oracle)
				&& query(state, binary(ComparisonLe.INSTANCE, a, one, pp), pp, oracle))
			return Trend.NON_DEC;
		else if (query(state, binary(ComparisonLt.INSTANCE, a, zero, pp), pp, oracle)
				&& query(state, binary(ComparisonGt.INSTANCE, a, one, pp), pp, oracle))
			return Trend.DEC;
		else if (query(state, binary(ComparisonLe.INSTANCE, a, zero, pp), pp, oracle)
				&& query(state, binary(ComparisonGe.INSTANCE, a, one, pp), pp, oracle))
			return Trend.NON_INC;
		else if (query(state, binary(ComparisonNe.INSTANCE, a, zero, pp), pp, oracle)
				&& query(state, binary(ComparisonNe.INSTANCE, a, one, pp), pp, oracle))
			return Trend.NON_STABLE;
		else
			return Trend.TOP;
	}

	/**
	 * Generates a {@link Trend} based on the value of {@code a} in
	 * {@link #aux}.
	 * 
	 * @param a      the expression
	 * @param pp     the {@link ProgramPoint} where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@link Trend#INC} if {@code a < 0 || a > 1}
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	private Trend increasingIfOutsideZeroAndOne(
			L state,
			SymbolicExpression a,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return increasingIfBetweenZeroAndOne(state, a, pp, oracle).invert();
	}

	/**
	 * Generates a {@link Trend} based on the value of {@code a} in
	 * {@link #aux}.
	 * 
	 * @param a      the expression
	 * @param pp     the {@link ProgramPoint} where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@link Trend#NON_DEC} if {@code 0 < a < 1 || 0 <= a < 1}
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	private Trend nonDecreasingIfBetweenZeroAndOne(
			L state,
			SymbolicExpression a,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Constant zero = constantInt(0, pp);
		Constant one = constantInt(1, pp);

		if (query(state, binary(ComparisonEq.INSTANCE, a, zero, pp), pp, oracle)
				|| query(state, binary(ComparisonEq.INSTANCE, a, one, pp), pp, oracle))
			return Trend.STABLE;
		else if (query(state, binary(ComparisonGe.INSTANCE, a, zero, pp), pp, oracle)
				&& query(state, binary(ComparisonLe.INSTANCE, a, one, pp), pp, oracle))
			return Trend.NON_DEC;
		else if (query(state, binary(ComparisonLe.INSTANCE, a, zero, pp), pp, oracle)
				|| query(state, binary(ComparisonGe.INSTANCE, a, one, pp), pp, oracle))
			return Trend.NON_INC;
		else
			return Trend.TOP;
	}

	/**
	 * Generates a {@link Trend} based on the value of {@code a} in
	 * {@link #aux}.
	 * 
	 * @param a      the expression
	 * @param pp     the {@link ProgramPoint} where the evaluation happens
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return {@link Trend#NON_DEC} if {@code a < 0 || a > 1}
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	private Trend nonDecreasingIfOutsideZeroAndOne(
			L state,
			SymbolicExpression a,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return nonDecreasingIfBetweenZeroAndOne(state, a, pp, oracle).invert();
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<Trend>, L> assign(
			ValueLatticeProduct<ValueEnvironment<Trend>, L> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;

		L post = aux.assign(state.second, id, expression, pp, oracle);
		if (post.isBottom())
			return state.bottom();

		if (!canProcess(id, pp, oracle) || !canProcess(expression, pp, oracle))
			return new ValueLatticeProduct<>(state.first, post);

		if (!state.first.knowsIdentifier(id))
			return new ValueLatticeProduct<>(state.first.putState(id, Trend.STABLE), post);

		Trend t = Trend.TOP;

		if ((expression instanceof Constant))
			t = increasingIfLess(state.second, id, expression, pp, oracle);
		else if (expression instanceof UnaryExpression
				&& ((UnaryExpression) expression).getOperator() instanceof NumericNegation)
			t = increasingIfLess(state.second, id, expression, pp, oracle);
		else if (expression instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression) expression;
			BinaryOperator op = be.getOperator();
			SymbolicExpression left = be.getLeft();
			SymbolicExpression right = be.getRight();

			boolean isLeft = id.equals(left);
			boolean isRight = id.equals(right);

			// x = a / 0
			if (op instanceof DivisionOperator
					&& query(state.second, binary(ComparisonEq.INSTANCE, right, constantInt(0, pp), pp), pp, oracle))
				return state.bottom();

			if (isLeft || isRight) {
				SymbolicExpression other = isLeft ? right : left;
				if (op instanceof AdditionOperator)
					// x = x + other || x = other + x
					t = increasingIfGreater(state.second, other, constantInt(0, pp), pp, oracle);
				else if (op instanceof SubtractionOperator) {
					// x = x - other
					if (isLeft)
						t = increasingIfLess(state.second, other, constantInt(0, pp), pp, oracle);
					else
						t = increasingIfLess(state.second, id, expression, pp, oracle);
				} else if (op instanceof MultiplicationOperator) {
					// x = x * other || x = other * x
					if (query(state.second, binary(ComparisonEq.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
							|| query(
									state.second,
									binary(ComparisonEq.INSTANCE, other, constantInt(1, pp), pp),
									pp,
									oracle))
						// id == 0 || other == 1
						t = Trend.STABLE;
					else if (query(state.second, binary(ComparisonGt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
						// id > 0
						t = increasingIfGreater(state.second, other, constantInt(1, pp), pp, oracle);
					else if (query(state.second, binary(ComparisonLt.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
						// id < 0
						t = increasingIfLess(state.second, other, constantInt(1, pp), pp, oracle);
					else if (query(state.second, binary(ComparisonGe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
						// id >= 0
						t = nonDecreasingIfGreater(state.second, other, constantInt(1, pp), pp, oracle);
					else if (query(state.second, binary(ComparisonLe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle))
						// id <= 0
						t = nonDecreasingIfLess(state.second, other, constantInt(1, pp), pp, oracle);
					else if (query(state.second, binary(ComparisonNe.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
							&& query(
									state.second,
									binary(ComparisonNe.INSTANCE, other, constantInt(1, pp), pp),
									pp,
									oracle))
						// id != 0 && other != 1
						t = Trend.NON_STABLE;
				} else if (op instanceof DivisionOperator) {
					// x = x / other
					if (isLeft) {
						if (query(state.second, binary(ComparisonEq.INSTANCE, id, constantInt(0, pp), pp), pp, oracle)
								|| query(
										state.second,
										binary(ComparisonEq.INSTANCE, other, constantInt(1, pp), pp),
										pp,
										oracle))
							// id == 0 || other == 1
							t = Trend.STABLE;
						else if (query(
								state.second,
								binary(ComparisonGt.INSTANCE, id, constantInt(0, pp), pp),
								pp,
								oracle))
							// id > 0
							t = increasingIfBetweenZeroAndOne(state.second, other, pp, oracle);
						else if (query(
								state.second,
								binary(ComparisonLt.INSTANCE, id, constantInt(0, pp), pp),
								pp,
								oracle))
							// id < 0
							t = increasingIfOutsideZeroAndOne(state.second, other, pp, oracle);
						else if (query(
								state.second,
								binary(ComparisonGe.INSTANCE, id, constantInt(0, pp), pp),
								pp,
								oracle))
							// id >= 0
							t = nonDecreasingIfBetweenZeroAndOne(state.second, other, pp, oracle);
						else if (query(
								state.second,
								binary(ComparisonLe.INSTANCE, id, constantInt(0, pp), pp),
								pp,
								oracle))
							// id <= 0
							t = nonDecreasingIfOutsideZeroAndOne(state.second, other, pp, oracle);
						else if (query(
								state.second,
								binary(ComparisonNe.INSTANCE, id, constantInt(0, pp), pp),
								pp,
								oracle)
								&& query(
										state.second,
										binary(ComparisonNe.INSTANCE, other, constantInt(1, pp), pp),
										pp,
										oracle))
							// id != 0 && other != 1
							t = Trend.NON_STABLE;
					} else
						t = increasingIfLess(state.second, id, expression, pp, oracle);
				}
			} else
				t = increasingIfLess(state.second, id, expression, pp, oracle);
		}

		ValueEnvironment<Trend> trnd = stabilize(state.first).putState(id, t);
		if (trnd.isBottom())
			return state.bottom();
		return new ValueLatticeProduct<>(trnd, post);
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<Trend>, L> smallStepSemantics(
			ValueLatticeProduct<ValueEnvironment<Trend>, L> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		L post = aux.smallStepSemantics(state.second, expression, pp, oracle);
		ValueEnvironment<Trend> sss = stabilize(state.first);
		if (post.isBottom() || sss.isBottom())
			return state.bottom();
		return new ValueLatticeProduct<>(sss, post);
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<Trend>, L> assume(
			ValueLatticeProduct<ValueEnvironment<Trend>, L> state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		L post = aux.assume(state.second, expression, src, dest, oracle);
		if (post.isBottom() || state.first.isBottom())
			return state.bottom();
		return new ValueLatticeProduct<>(state.first, post);
	}

	@Override
	public Satisfiability satisfies(
			ValueLatticeProduct<ValueEnvironment<Trend>, L> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return aux.satisfies(state.second, expression, pp, oracle);
	}

	/**
	 * Yields the combination of the given trends. This operation is to be
	 * interpreted as the sequential concatenation of the two: if two (blocks
	 * of) instructions are executed sequentially, a variable having {@code t1}
	 * trend in the former and {@code t2} trend in the latter would have
	 * {@code t1.combine(t2)} as an overall trend. This delegates to
	 * {@link Trend#combine(Trend)} for single-trend combination.
	 * 
	 * @param first  the first trends
	 * @param second the second trends
	 * 
	 * @return the combination of the two trends
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public ValueEnvironment<Trend> combine(
			ValueEnvironment<Trend> first,
			ValueEnvironment<Trend> second)
			throws SemanticException {
		ValueEnvironment<Trend> result = new ValueEnvironment<>(second.lattice, second.function);

		for (Identifier id : second.getKeys())
			// we iterate only on the keys of post to remove the ones that went
			// out of scope
			if (first.knowsIdentifier(id)) {
				Trend tmp = first.getState(id).combine(second.getState(id));
				result = result.putState(id, tmp);
			}

		return result;
	}

	/**
	 * Yields a mapping from {@link Trend}s to the {@link Identifier}s having
	 * that trend.
	 * 
	 * @param trends the trends to be mapped
	 * 
	 * @return the mapping
	 */
	public Map<Trend, Set<Identifier>> getCovarianceClasses(
			ValueEnvironment<Trend> trends) {
		Map<Trend, Set<Identifier>> map = new HashMap<>();

		for (Identifier id : trends.getKeys()) {
			Trend t = trends.getState(id);
			map.computeIfAbsent(t, k -> new HashSet<>()).add(id);
		}

		return map;
	}

	private static ValueEnvironment<Trend> stabilize(
			ValueEnvironment<Trend> trends) {
		ValueEnvironment<Trend> result = new ValueEnvironment<>(trends.lattice);

		for (Identifier id : trends.getKeys())
			result = result.putState(id, Trend.STABLE);

		return result;
	}

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return expression.getStaticType().isNumericType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp);
		} catch (SemanticException e) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		return rts.stream().anyMatch(Type::isNumericType);
	}

	@Override
	public ValueLatticeProduct<ValueEnvironment<Trend>, L> makeLattice() {
		return new ValueLatticeProduct<>(new ValueEnvironment<>(Trend.TOP), aux.makeLattice());
	}

}
