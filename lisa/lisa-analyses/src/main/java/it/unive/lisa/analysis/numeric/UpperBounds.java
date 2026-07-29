package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.NumericAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.ArithmeticOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Relational implementation of the upper bounds analysis of
 * <a href="https://doi.org/10.1016/j.scico.2009.04.004">this paper</a>.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UpperBounds
		implements
		NumericAbstraction<ValueEnvironment<DefiniteIdSet>> {

	@Override
	public ValueEnvironment<DefiniteIdSet> assign(
			ValueEnvironment<DefiniteIdSet> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// cleanup: if a variable is reassigned, it can no longer be an
		// upperbound of other variables
		Map<Identifier, DefiniteIdSet> cleanup = new HashMap<>();
		for (Map.Entry<Identifier, DefiniteIdSet> entry : state) {
			if (entry.getKey().equals(id))
				continue;
			if (!entry.getValue().contains(id))
				cleanup.put(entry.getKey(), entry.getValue());
			else {
				Set<Identifier> copy = new HashSet<>(entry.getValue().elements);
				copy.remove(id);
				cleanup.put(entry.getKey(), new DefiniteIdSet(copy));
			}
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression) expression;
			BinaryOperator op = be.getOperator();

			if (be.getLeft() instanceof Identifier
					&& be.getRight() instanceof Constant
					&& !be.getLeft().equals(id)
					&& op instanceof ArithmeticOperator) {
				// casting to Number here is safe: since we know it is a numeric
				// operation, we must have a constant here
				double sign = Math.signum(((Number) ((Constant) be.getRight()).getValue()).doubleValue());
				if (op instanceof SubtractionOperator) {
					// id = y - c (where c is the constant)
					Identifier y = (Identifier) be.getLeft();
					if (sign > 0)
						cleanup.put(id, state.getState(y).add(y));
					else if (sign < 0)
						// this is effectively an addition
						cleanup.put(y, state.getState(y).add(id));
				} else if (op instanceof AdditionOperator) {
					// bonus: id = y + c (where c is the constant)
					Identifier y = (Identifier) be.getLeft();
					if (sign > 0)
						cleanup.put(y, state.getState(y).add(id));
					else if (sign < 0)
						// this is effectively a subtraction
						cleanup.put(id, state.getState(y).add(y));
				}
			}
		}

		return new ValueEnvironment<>(state.lattice, cleanup);
	}

	@Override
	public ValueEnvironment<DefiniteIdSet> smallStepSemantics(
			ValueEnvironment<DefiniteIdSet> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// nothing to do
		return state;
	}

	@Override
	public Satisfiability satisfies(
			ValueEnvironment<DefiniteIdSet> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(expression instanceof BinaryExpression))
			return Satisfiability.UNKNOWN;

		BinaryExpression bexp = (BinaryExpression) expression;
		SymbolicExpression left = bexp.getLeft();
		SymbolicExpression right = bexp.getRight();
		BinaryOperator operator = bexp.getOperator();

		if (!(left instanceof Identifier && right instanceof Identifier))
			return Satisfiability.UNKNOWN;

		Identifier x = (Identifier) left;
		Identifier y = (Identifier) right;

		if (operator instanceof ComparisonLt) {
			return state.getState(x).contains(y) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
		} else if (operator instanceof ComparisonLe) {
			if (state.getState(x).contains(y))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator instanceof ComparisonGt) {
			return state.getState(y).contains(x) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
		} else if (operator instanceof ComparisonGe) {
			if (state.getState(y).contains(x))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<DefiniteIdSet> assume(
			ValueEnvironment<DefiniteIdSet> state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom() || !(expression instanceof BinaryExpression))
			return state;

		BinaryExpression bexp = (BinaryExpression) expression;
		SymbolicExpression left = bexp.getLeft();
		SymbolicExpression right = bexp.getRight();
		BinaryOperator operator = bexp.getOperator();

		if (!(left instanceof Identifier && right instanceof Identifier))
			return state;

		Identifier x = (Identifier) left;
		Identifier y = (Identifier) right;

		if (operator instanceof ComparisonLt) {
			// [[x < y]](s) = s[x -> s(x) U s(y) U {y}]
			DefiniteIdSet s_x = state.getState(x);
			DefiniteIdSet s_y = state.getState(y);
			DefiniteIdSet y_singleton = new DefiniteIdSet(Collections.singleton(y));
			DefiniteIdSet set = s_x.glb(s_y).glb(y_singleton);
			return state.putState(x, set);
		} else if (operator instanceof ComparisonEq) {
			// [[x == y]](s) = s[x,y -> s(x) U s(y)]
			DefiniteIdSet s_x = state.getState(x);
			DefiniteIdSet s_y = state.getState(y);
			DefiniteIdSet set = s_x.glb(s_y);
			return state.putState(x, set).putState(y, set);
		} else if (operator instanceof ComparisonLe) {
			// [[x <= y]](s) = s[x -> s(x) U s(y)]
			DefiniteIdSet s_x = state.getState(x);
			DefiniteIdSet s_y = state.getState(y);
			DefiniteIdSet set = s_x.glb(s_y);
			return state.putState(x, set);
		} else if (operator instanceof ComparisonGt) {
			// x > y --> y < x
			return assume(
					state,
					new BinaryExpression(
							expression.getStaticType(),
							right,
							left,
							ComparisonLt.INSTANCE,
							expression.getCodeLocation()),
					src,
					dest,
					oracle);
		} else if (operator instanceof ComparisonGe) {
			// x >= y --> y <= x
			return assume(
					state,
					new BinaryExpression(
							expression.getStaticType(),
							right,
							left,
							ComparisonLe.INSTANCE,
							expression.getCodeLocation()),
					src,
					dest,
					oracle);
		}

		return state;
	}

	@Override
	public ValueEnvironment<DefiniteIdSet> makeLattice() {
		return new ValueEnvironment<>(new DefiniteIdSet(Collections.emptySet(), true));
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<DefiniteIdSet> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Collections.emptySet();
		if (state.isBottom())
			return null;

		if ((e instanceof UnaryExpression && ((UnaryExpression) e).getOperator() == LogicalNegation.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalAnd.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalOr.INSTANCE)) {
			Satisfiability sat = satisfies(state, e, pp, oracle);
			if (sat == Satisfiability.SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
			else if (sat == Satisfiability.NOT_SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
			else if (sat == Satisfiability.UNKNOWN)
				return Collections.emptySet();
			else
				return null;
		}

		if (e instanceof BinaryExpression) {
			BinaryOperator operator = ((BinaryExpression) e).getOperator();
			if (operator == ComparisonEq.INSTANCE
					|| operator == ComparisonNe.INSTANCE
					|| operator == ComparisonLe.INSTANCE
					|| operator == ComparisonLt.INSTANCE
					|| operator == ComparisonGe.INSTANCE
					|| operator == ComparisonGt.INSTANCE) {
				Satisfiability sat = satisfies(state, e, pp, oracle);
				if (sat == Satisfiability.SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
				else if (sat == Satisfiability.NOT_SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
				else if (sat == Satisfiability.UNKNOWN)
					return Collections.emptySet();
				else
					return null;
			}
		}

		if (e instanceof Identifier) {
			Identifier id = (Identifier) e;
			Set<BinaryExpression> constraints = new HashSet<>();
			DefiniteIdSet set = state.getState(id);
			if (!set.isTop() && !set.isBottom())
				for (Identifier ub : state.getState(id).elements)
					constraints.add(new BinaryExpression(
							e.getStaticType(),
							ub,
							id,
							ComparisonGt.INSTANCE,
							e.getCodeLocation()));
			for (Entry<Identifier, DefiniteIdSet> entry : state)
				if (entry.getValue().contains(id))
					constraints.add(new BinaryExpression(
							e.getStaticType(),
							entry.getKey(),
							id,
							ComparisonLt.INSTANCE,
							e.getCodeLocation()));
			return constraints;
		}

		return Collections.emptySet();
	}
}
