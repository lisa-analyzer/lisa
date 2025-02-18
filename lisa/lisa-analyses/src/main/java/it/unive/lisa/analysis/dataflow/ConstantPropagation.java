package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of the overflow-insensitive constant propagation dataflow
 * analysis, that focuses only on integers.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ConstantPropagation
		implements
		DataflowElement<DefiniteDataflowDomain<ConstantPropagation>, ConstantPropagation> {

	private final Identifier id;
	private final Integer constant;

	/**
	 * Builds an empty constant propagation object.
	 */
	public ConstantPropagation() {
		this(null, null);
	}

	/**
	 * Builds the new constant propagation object.
	 * 
	 * @param id the constant variable
	 * @param v  the constant value
	 */
	public ConstantPropagation(
			Identifier id,
			Integer v) {
		this.id = id;
		this.constant = v;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public Collection<Identifier> getInvolvedIdentifiers() {
		return Collections.singleton(id);
	}

	private static Integer eval(
			SymbolicExpression e,
			DefiniteDataflowDomain<ConstantPropagation> domain) {

		if (e instanceof Constant) {
			Constant c = (Constant) e;
			return c.getValue() instanceof Integer ? (Integer) c.getValue() : null;
		}

		if (e instanceof Identifier) {
			for (ConstantPropagation cp : domain.getDataflowElements())
				if (cp.id.equals(e))
					return cp.constant;

			return null;
		}

		if (e instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) e;
			Integer i = eval(unary.getExpression(), domain);

			if (i == null)
				return i;

			if (unary.getOperator() == NumericNegation.INSTANCE)
				return -i;
		}

		if (e instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) e;
			Integer right = eval(binary.getRight(), domain);
			Integer left = eval(binary.getLeft(), domain);

			if (right == null || left == null)
				return null;

			if (binary.getOperator() instanceof AdditionOperator)
				return left + right;
			if (binary.getOperator() instanceof DivisionOperator)
				return left == 0 ? null : (int) left / right;
			if (binary.getOperator() instanceof ModuloOperator)
				return right == 0 ? null : left % right;
			if (binary.getOperator() instanceof MultiplicationOperator)
				return left * right;
			if (binary.getOperator() instanceof SubtractionOperator)
				return left - right;
		}

		return null;
	}

	@Override
	public Collection<ConstantPropagation> gen(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			DefiniteDataflowDomain<ConstantPropagation> domain) {
		Set<ConstantPropagation> gen = new HashSet<>();

		Integer v = eval(expression, domain);
		if (v != null)
			gen.add(new ConstantPropagation(id, v));

		return gen;
	}

	@Override
	public Collection<ConstantPropagation> gen(
			ValueExpression expression,
			ProgramPoint pp,
			DefiniteDataflowDomain<ConstantPropagation> domain) {
		return Collections.emptyList();
	}

	@Override
	public Collection<ConstantPropagation> kill(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			DefiniteDataflowDomain<ConstantPropagation> domain) {
		Collection<ConstantPropagation> result = new HashSet<>();

		for (ConstantPropagation cp : domain.getDataflowElements())
			if (cp.id.equals(id))
				result.add(cp);

		return result;
	}

	@Override
	public Collection<ConstantPropagation> kill(
			ValueExpression expression,
			ProgramPoint pp,
			DefiniteDataflowDomain<ConstantPropagation> domain) {
		return Collections.emptyList();
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
		ConstantPropagation other = (ConstantPropagation) obj;
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
	public ConstantPropagation pushScope(
			ScopeToken scope)
			throws SemanticException {
		return new ConstantPropagation((Identifier) id.pushScope(scope), constant);
	}

	@Override
	public ConstantPropagation popScope(
			ScopeToken scope)
			throws SemanticException {
		if (!id.canBeScoped())
			return this;

		SymbolicExpression popped = id.popScope(scope);
		if (popped == null)
			return null;

		return new ConstantPropagation((Identifier) popped, constant);
	}
}
