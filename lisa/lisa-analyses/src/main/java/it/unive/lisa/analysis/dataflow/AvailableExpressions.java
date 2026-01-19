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
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.*;

/**
 * An implementation of the available expressions dataflow analysis, that
 * focuses only on the expressions that are stored into some variable.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AvailableExpressions
		extends
		DataflowDomain<DefiniteSet<AvailableExpressions.AE>, AvailableExpressions.AE> {

	@Override
	public DefiniteSet<AE> makeLattice() {
		return new DefiniteSet<>();
	}

	@Override
	public Set<AE> gen(
			DefiniteSet<AE> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp) {
		Set<AE> result = new HashSet<>();
		AE ae = new AE(expression);
		if (!ae.getInvolvedIdentifiers().contains(id) && filter(expression))
			result.add(ae);
		return result;
	}

	@Override
	public Set<AE> gen(
			DefiniteSet<AE> state,
			ValueExpression expression,
			ProgramPoint pp) {
		Set<AE> result = new HashSet<>();
		AE ae = new AE(expression);
		if (filter(expression))
			result.add(ae);
		return result;
	}

	private static boolean filter(
			ValueExpression expression) {
		if (expression instanceof Identifier)
			return false;
		if (expression instanceof Constant)
			return false;
		if (expression instanceof Skip)
			return false;
		if (expression instanceof PushAny)
			return false;
		return true;
	}

	@Override
	public Set<AE> kill(
			DefiniteSet<AE> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp) {
		Set<AE> result = new HashSet<>();

		for (AE ae : state.getDataflowElements()) {
			Collection<Identifier> ids = ae.getInvolvedIdentifiers();

			if (ids.contains(id))
				result.add(ae);
		}

		return result;
	}

	@Override
	public Set<AE> kill(
			DefiniteSet<AE> state,
			ValueExpression expression,
			ProgramPoint pp) {
		return Collections.emptySet();
	}

	/**
	 * An expression visitor that collects all the identifiers that appear in a
	 * given expression.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IDCollector
			implements
			ExpressionVisitor<Collection<Identifier>> {

		private final Collection<Identifier> result = new HashSet<>();

		@Override
		public Collection<Identifier> visit(
				HeapExpression expression,
				Collection<Identifier>[] subExpressions,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				AccessChild expression,
				Collection<Identifier> receiver,
				Collection<Identifier> child,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				MemoryAllocation expression,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				HeapReference expression,
				Collection<Identifier> arg,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				HeapDereference expression,
				Collection<Identifier> arg,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				ValueExpression expression,
				Collection<Identifier>[] subExpressions,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				UnaryExpression expression,
				Collection<Identifier> arg,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				BinaryExpression expression,
				Collection<Identifier> left,
				Collection<Identifier> right,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				TernaryExpression expression,
				Collection<Identifier> left,
				Collection<Identifier> middle,
				Collection<Identifier> right,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(VariadicExpression expression, Collection<Identifier>[] values, Object... params) throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				Skip expression,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				PushAny expression,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				PushInv expression,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				Constant expression,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				NullConstant expression,
				Object... params)
				throws SemanticException {
			return result;
		}

		@Override
		public Collection<Identifier> visit(
				Identifier expression,
				Object... params)
				throws SemanticException {
			result.add(expression);
			return result;
		}

	}

	/**
	 * An available expression dataflow element, that is, an expression that is
	 * available at a given program point.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class AE
			implements
			DataflowElement<AE> {

		private final ValueExpression expression;

		private AE(
				ValueExpression expression) {
			this.expression = expression;
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		@Override
		public Collection<Identifier> getInvolvedIdentifiers() {
			try {
				return expression.accept(new AvailableExpressions.IDCollector(), new Object[0]);
			} catch (SemanticException e) {
				return Collections.emptySet();
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((expression == null) ? 0 : expression.hashCode());
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
			AE other = (AE) obj;
			if (expression == null) {
				if (other.expression != null)
					return false;
			} else if (!expression.equals(other.expression))
				return false;
			return true;
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation(expression);
		}

		@Override
		public AE pushScope(
				ScopeToken scope,
				ProgramPoint pp)
				throws SemanticException {
			return new AE((ValueExpression) expression.pushScope(scope, pp));
		}

		@Override
		public AE popScope(
				ScopeToken scope,
				ProgramPoint pp)
				throws SemanticException {
			return new AE((ValueExpression) expression.popScope(scope, pp));
		}

		@Override
		public AE replaceIdentifier(
				Identifier source,
				Identifier target) {
			SymbolicExpression e = expression.replace(source, target);
			if (e == expression)
				return this;
			return new AE((ValueExpression) e);
		}

	}

}
