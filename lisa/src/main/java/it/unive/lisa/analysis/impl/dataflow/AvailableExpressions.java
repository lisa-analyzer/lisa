package it.unive.lisa.analysis.impl.dataflow;

import java.util.Collection;
import java.util.HashSet;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteForwardDataflowDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.PairRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * An implementation of the available expressions dataflow analysis, that
 * focuses only on the expressions that are stored into some variable.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AvailableExpressions
		implements DataflowElement<DefiniteForwardDataflowDomain<AvailableExpressions>, AvailableExpressions> {

	private final Identifier id;
	private final ValueExpression expression;

	/**
	 * Builds an empty available expressions object.
	 */
	public AvailableExpressions() {
		this(null, null);
	}

	private AvailableExpressions(Identifier id, ValueExpression expression) {
		this.id = id;
		this.expression = expression;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public Identifier getIdentifier() {
		return id;
	}

	private Collection<Identifier> getIdentifierOperands(SymbolicExpression expression) {
		Collection<Identifier> result = new HashSet<>();

		if (expression instanceof Identifier)
			result.add((Identifier) expression);

		if (expression instanceof UnaryExpression)
			result.addAll(getIdentifierOperands(((UnaryExpression) expression).getExpression()));

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			result.addAll(getIdentifierOperands(binary.getLeft()));
			result.addAll(getIdentifierOperands(binary.getRight()));
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;
			result.addAll(getIdentifierOperands(ternary.getLeft()));
			result.addAll(getIdentifierOperands(ternary.getMiddle()));
			result.addAll(getIdentifierOperands(ternary.getRight()));
		}

		return result;
	}

	@Override
	public Collection<AvailableExpressions> gen(Identifier id, ValueExpression expression, ProgramPoint pp,
			DefiniteForwardDataflowDomain<AvailableExpressions> domain) {
		Collection<AvailableExpressions> result = new HashSet<>();
		result.add(new AvailableExpressions(id, expression));
		return result;
	}

	@Override
	public Collection<Identifier> kill(Identifier id, ValueExpression expression, ProgramPoint pp,
			DefiniteForwardDataflowDomain<AvailableExpressions> domain) {
		Collection<Identifier> result = new HashSet<>();
		result.add(id);

		for (AvailableExpressions ae : domain.getDataflowElements()) {
			Collection<Identifier> ids = getIdentifierOperands(ae.expression);

			if (ids.contains(id))
				result.add(ae.getIdentifier());
		}

		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		return !id.getDynamicType().isPointerType();
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		return !expression.getDynamicType().isPointerType();
	}

	@Override
	public DomainRepresentation representation() {
		return new PairRepresentation(new StringRepresentation(id), new StringRepresentation(expression));
	}

	@Override
	public AvailableExpressions pushScope(ScopeToken scope) throws SemanticException {
		return new AvailableExpressions((Identifier) id.pushScope(scope),
				(ValueExpression) expression.pushScope(scope));
	}

	@Override
	public AvailableExpressions popScope(ScopeToken scope) throws SemanticException {
		if (!(id instanceof OutOfScopeIdentifier))
			return this;

		return new AvailableExpressions(((OutOfScopeIdentifier) id).popScope(scope),
				(ValueExpression) expression.popScope(scope));
	}
}