package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * An implementation of the available expressions dataflow analysis, that
 * focuses only on the expressions that are stored into some variable.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AvailableExpressions
		implements DataflowElement<DefiniteForwardDataflowDomain<AvailableExpressions>, AvailableExpressions> {

	private final ValueExpression expression;

	/**
	 * Builds an empty available expressions object.
	 */
	public AvailableExpressions() {
		this(null);
	}

	/**
	 * Builds the available expressions object.
	 * 
	 * @param expression the expression of this element
	 */
	public AvailableExpressions(ValueExpression expression) {
		this.expression = expression;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public Collection<Identifier> getInvolvedIdentifiers() {
		return getIdentifierOperands(expression);
	}

	private static Collection<Identifier> getIdentifierOperands(ValueExpression expression) {
		Collection<Identifier> result = new HashSet<>();

		if (expression == null)
			return result;

		if (expression instanceof Identifier)
			result.add((Identifier) expression);

		if (expression instanceof UnaryExpression)
			result.addAll(getIdentifierOperands((ValueExpression) ((UnaryExpression) expression).getExpression()));

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			result.addAll(getIdentifierOperands((ValueExpression) binary.getLeft()));
			result.addAll(getIdentifierOperands((ValueExpression) binary.getRight()));
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;
			result.addAll(getIdentifierOperands((ValueExpression) ternary.getLeft()));
			result.addAll(getIdentifierOperands((ValueExpression) ternary.getMiddle()));
			result.addAll(getIdentifierOperands((ValueExpression) ternary.getRight()));
		}

		return result;
	}

	@Override
	public Collection<AvailableExpressions> gen(Identifier id, ValueExpression expression, ProgramPoint pp,
			DefiniteForwardDataflowDomain<AvailableExpressions> domain) {
		Collection<AvailableExpressions> result = new HashSet<>();
		AvailableExpressions ae = new AvailableExpressions(expression);
		if (!ae.getInvolvedIdentifiers().contains(id) && filter(expression))
			result.add(ae);
		return result;
	}

	@Override
	public Collection<AvailableExpressions> gen(ValueExpression expression, ProgramPoint pp,
			DefiniteForwardDataflowDomain<AvailableExpressions> domain) {
		Collection<AvailableExpressions> result = new HashSet<>();
		AvailableExpressions ae = new AvailableExpressions(expression);
		if (filter(expression))
			result.add(ae);
		return result;
	}

	private static boolean filter(ValueExpression expression) {
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
	public Collection<AvailableExpressions> kill(Identifier id, ValueExpression expression, ProgramPoint pp,
			DefiniteForwardDataflowDomain<AvailableExpressions> domain) {
		Collection<AvailableExpressions> result = new HashSet<>();

		for (AvailableExpressions ae : domain.getDataflowElements()) {
			Collection<Identifier> ids = getIdentifierOperands(ae.expression);

			if (ids.contains(id))
				result.add(ae);
		}

		return result;
	}

	@Override
	public Collection<AvailableExpressions> kill(ValueExpression expression, ProgramPoint pp,
			DefiniteForwardDataflowDomain<AvailableExpressions> domain) {
		return Collections.emptyList();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AvailableExpressions other = (AvailableExpressions) obj;
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
	public AvailableExpressions pushScope(ScopeToken scope) throws SemanticException {
		return new AvailableExpressions((ValueExpression) expression.pushScope(scope));
	}

	@Override
	public AvailableExpressions popScope(ScopeToken scope) throws SemanticException {
		return new AvailableExpressions((ValueExpression) expression.popScope(scope));
	}
}
