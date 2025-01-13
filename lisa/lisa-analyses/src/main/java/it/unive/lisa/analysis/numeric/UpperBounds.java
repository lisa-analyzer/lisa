package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Relational implementation of the upper bounds analysis of
 * <a href="https://doi.org/10.1016/j.scico.2009.04.004">this paper</a>.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UpperBounds
		extends
		Environment<UpperBounds, ValueExpression, UpperBounds.IdSet>
		implements
		ValueDomain<UpperBounds> {

	/**
	 * Builds a new instance of upper bounds.
	 */
	public UpperBounds() {
		super(new IdSet(Collections.emptySet()).top());
	}

	/**
	 * Builds a new instance of upper bounds.
	 * 
	 * @param lattice  the {@link IdSet} instance to use as singleton
	 * @param function the mapping for the upper bounds
	 */
	public UpperBounds(
			IdSet lattice,
			Map<Identifier, IdSet> function) {
		super(lattice, function);
	}

	@Override
	public UpperBounds mk(
			IdSet lattice,
			Map<Identifier, IdSet> function) {
		return new UpperBounds(lattice, function);
	}

	@Override
	public UpperBounds top() {
		return new UpperBounds(lattice.top(), null);
	}

	@Override
	public UpperBounds bottom() {
		return new UpperBounds(lattice.bottom(), null);
	}

	@Override
	public UpperBounds assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// cleanup: if a variable is reassigned, it can no longer be an
		// upperbound of other variables
		Map<Identifier, IdSet> cleanup = new HashMap<>();
		for (Map.Entry<Identifier, IdSet> entry : this) {
			if (entry.getKey().equals(id))
				continue;
			if (!entry.getValue().contains(id))
				cleanup.put(entry.getKey(), entry.getValue());

			Set<Identifier> copy = new HashSet<>(entry.getValue().elements);
			copy.remove(id);
			cleanup.put(entry.getKey(), new IdSet(copy));
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
						cleanup.put(id, getState(y).add(y));
					else if (sign < 0)
						// this is effectively an addition
						cleanup.put(y, getState(y).add(id));
				} else if (op instanceof AdditionOperator) {
					// bonus: id = y + c (where c is the constant)
					Identifier y = (Identifier) be.getLeft();
					if (sign > 0)
						cleanup.put(y, getState(y).add(id));
					else if (sign < 0)
						// this is effectively a subtraction
						cleanup.put(id, getState(y).add(y));
				}
			}
		}

		return new UpperBounds(lattice, cleanup);
	}

	@Override
	public UpperBounds smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// nothing to do
		return this;
	}

	@Override
	public Satisfiability satisfies(
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
			return Satisfiability.fromBoolean(getState(x).contains(y));
		} else if (operator instanceof ComparisonLe) {
			if (getState(x).contains(y))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator instanceof ComparisonGt) {
			return Satisfiability.fromBoolean(getState(y).contains(x));
		} else if (operator instanceof ComparisonGe) {
			if (getState(y).contains(x))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public UpperBounds assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(expression instanceof BinaryExpression))
			return this;

		BinaryExpression bexp = (BinaryExpression) expression;
		SymbolicExpression left = bexp.getLeft();
		SymbolicExpression right = bexp.getRight();
		BinaryOperator operator = bexp.getOperator();

		if (!(left instanceof Identifier && right instanceof Identifier))
			return this;

		Identifier x = (Identifier) left;
		Identifier y = (Identifier) right;

		if (operator instanceof ComparisonLt) {
			// [[x < y]](s) = s[x -> s(x) U s(y) U {y}]
			IdSet s_x = getState(x);
			IdSet s_y = getState(y);
			IdSet y_singleton = new IdSet(Collections.singleton(y));
			IdSet set = s_x.glb(s_y).glb(y_singleton);
			return putState(x, set);
		} else if (operator instanceof ComparisonEq) {
			// [[x == y]](s) = s[x,y -> s(x) U s(y)]
			IdSet s_x = getState(x);
			IdSet s_y = getState(y);
			IdSet set = s_x.glb(s_y);
			return putState(x, set).putState(y, set);
		} else if (operator instanceof ComparisonLe) {
			// [[x <= y]](s) = s[x -> s(x) U s(y)]
			IdSet s_x = getState(x);
			IdSet s_y = getState(y);
			IdSet set = s_x.glb(s_y);
			return putState(x, set);
		} else if (operator instanceof ComparisonGt) {
			// x > y --> y < x
			return assume(
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

		return this;
	}

	/**
	 * An {@link InverseSetLattice} of {@link Identifier}s. This class is made
	 * to be a {@link NonRelationalDomain} just to be used in conjunction with
	 * {@link UpperBounds}, which is an {@link Environment}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IdSet
			extends
			InverseSetLattice<IdSet, Identifier>
			implements
			NonRelationalDomain<IdSet, ValueExpression, UpperBounds> {

		/**
		 * Builds the lattice.
		 *
		 * @param elements the elements that are contained in the lattice
		 */
		public IdSet(
				Set<Identifier> elements) {
			super(elements, elements.isEmpty());
		}

		/**
		 * Builds the lattice.
		 *
		 * @param elements the elements that are contained in the lattice
		 * @param isTop    whether or not this is the top or bottom element of
		 *                     the lattice, valid only if the set of elements is
		 *                     empty
		 */
		public IdSet(
				Set<Identifier> elements,
				boolean isTop) {
			super(elements, isTop);
		}

		@Override
		public IdSet wideningAux(
				IdSet other)
				throws SemanticException {
			return other.elements.containsAll(elements) ? other : top();
		}

		@Override
		public IdSet top() {
			return new IdSet(Collections.emptySet(), true);
		}

		@Override
		public IdSet bottom() {
			return new IdSet(Collections.emptySet(), false);
		}

		@Override
		public IdSet mk(
				Set<Identifier> set) {
			return new IdSet(set);
		}

		/**
		 * Adds a new {@link Identifier} to this set. This method has no side
		 * effect: a new {@link IdSet} is created, modified and returned.
		 * 
		 * @param id the identifier to add
		 * 
		 * @return the new set
		 */
		public IdSet add(
				Identifier id) {
			Set<Identifier> res = new HashSet<>(elements);
			res.add(id);
			return new IdSet(res);
		}

		@Override
		public IdSet eval(
				ValueExpression expression,
				UpperBounds environment,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			// since this won't be really used as a non relational domain,
			// we don't care about the implementation of this method
			return top();
		}

		@Override
		public Satisfiability satisfies(
				ValueExpression expression,
				UpperBounds environment,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			// since this won't be really used as a non relational domain,
			// we don't care about the implementation of this method
			return Satisfiability.UNKNOWN;
		}

		@Override
		public UpperBounds assume(
				UpperBounds environment,
				ValueExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle)
				throws SemanticException {
			// since this won't be really used as a non relational domain,
			// we don't care about the implementation of this method
			return environment;
		}

		@Override
		public boolean canProcess(
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			// since this won't be really used as a non relational domain,
			// we don't care about the implementation of this method
			return true;
		}
	}
}