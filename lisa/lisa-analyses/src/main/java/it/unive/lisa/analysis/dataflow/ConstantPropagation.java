package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.*;

/**
 * An implementation of the overflow-insensitive constant propagation dataflow
 * analysis, that focuses only on integers.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ConstantPropagation
		extends
		DataflowDomain<DefiniteSet<ConstantPropagation.CP>, ConstantPropagation.CP> {

	@Override
	public DefiniteSet<CP> makeLattice() {
		return new DefiniteSet<>();
	}

	private static class Evaluator
			implements
			ExpressionVisitor<Integer> {

		@Override
		public Integer visit(
				HeapExpression expression,
				Integer[] subExpressions,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				AccessChild expression,
				Integer receiver,
				Integer child,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				MemoryAllocation expression,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				HeapReference expression,
				Integer arg,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				HeapDereference expression,
				Integer arg,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				ValueExpression expression,
				Integer[] subExpressions,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				UnaryExpression expression,
				Integer arg,
				Object... params)
				throws SemanticException {
			if (arg == null || expression.getOperator() != NumericNegation.INSTANCE)
				return null;
			return -arg;
		}

		@Override
		public Integer visit(
				BinaryExpression expression,
				Integer left,
				Integer right,
				Object... params)
				throws SemanticException {
			if (right == null || left == null)
				return null;

			if (expression.getOperator() instanceof AdditionOperator)
				return left + right;
			if (expression.getOperator() instanceof DivisionOperator)
				return left == 0 ? null : (int) left / right;
			if (expression.getOperator() instanceof ModuloOperator)
				return right == 0 ? null : left % right;
			if (expression.getOperator() instanceof MultiplicationOperator)
				return left * right;
			if (expression.getOperator() instanceof SubtractionOperator)
				return left - right;

			return null;
		}

		@Override
		public Integer visit(
				TernaryExpression expression,
				Integer left,
				Integer middle,
				Integer right,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(VariadicExpression expression, Integer[] values, Map<String, Integer[]> variadicValues, Object... params) throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				Skip expression,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				PushAny expression,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				PushInv expression,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				NullConstant expression,
				Object... params)
				throws SemanticException {
			return null;
		}

		@Override
		public Integer visit(
				Constant expression,
				Object... params)
				throws SemanticException {
			Constant c = (Constant) expression;
			return c.getValue() instanceof Integer ? (Integer) c.getValue() : null;
		}

		@Override
		public Integer visit(
				Identifier expression,
				Object... params)
				throws SemanticException {
			@SuppressWarnings("unchecked")
			DefiniteSet<CP> domain = (DefiniteSet<CP>) params[0];
			for (CP cp : domain.getDataflowElements())
				if (cp.id.equals(expression))
					return cp.constant;

			return null;
		}

	}

	@Override
	public Set<CP> gen(
			DefiniteSet<CP> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		Set<CP> gen = new HashSet<>();

		Integer v = expression.accept(new Evaluator(), state);
		if (v != null)
			gen.add(new CP(id, v));

		return gen;
	}

	@Override
	public Set<CP> gen(
			DefiniteSet<CP> state,
			ValueExpression expression,
			ProgramPoint pp) {
		return Collections.emptySet();
	}

	@Override
	public Set<CP> kill(
			DefiniteSet<CP> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp) {
		Set<CP> result = new HashSet<>();

		for (CP cp : state.getDataflowElements())
			if (cp.id.equals(id))
				result.add(cp);

		return result;
	}

	@Override
	public Set<CP> kill(
			DefiniteSet<CP> state,
			ValueExpression expression,
			ProgramPoint pp) {
		return Collections.emptySet();
	}

	/**
	 * A constant propagation dataflow element, that is, a pair of an identifier
	 * and a constant value that is available at a given program point.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class CP
			implements
			DataflowElement<CP> {

		private final Identifier id;

		private final Integer constant;

		private CP(
				Identifier id,
				Integer v) {
			this.id = id;
			this.constant = v;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((constant == null) ? 0 : constant.hashCode());
			return result;
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CP other = (CP) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (constant == null) {
				if (other.constant != null)
					return false;
			} else if (!constant.equals(other.constant))
				return false;
			return true;
		}

		@Override
		public StructuredRepresentation representation() {
			return new ListRepresentation(new StringRepresentation(id), new StringRepresentation(constant));
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		@Override
		public Collection<Identifier> getInvolvedIdentifiers() {
			return Collections.singleton(id);
		}

		@Override
		public CP pushScope(
				ScopeToken scope,
				ProgramPoint pp)
				throws SemanticException {
			return new CP((Identifier) id.pushScope(scope, pp), constant);
		}

		@Override
		public CP popScope(
				ScopeToken scope,
				ProgramPoint pp)
				throws SemanticException {
			if (!id.canBeScoped())
				return this;

			SymbolicExpression popped = id.popScope(scope, pp);
			if (popped == null)
				return null;

			return new CP((Identifier) popped, constant);
		}

		@Override
		public CP replaceIdentifier(
				Identifier source,
				Identifier target) {
			SymbolicExpression e = id.replace(source, target);
			if (e == id)
				return this;
			return new CP((Identifier) e, constant);
		}

	}

}
