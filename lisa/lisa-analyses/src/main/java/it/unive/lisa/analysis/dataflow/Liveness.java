package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An implementation of the liveness dataflow analysis, that determines which
 * values might be used later on in the program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Liveness
		implements
		DataflowElement<PossibleDataflowDomain<Liveness>, Liveness> {

	private final Identifier id;

	/**
	 * Builds an empty liveness element.
	 */
	public Liveness() {
		this(null);
	}

	/**
	 * Builds the liveness element for the specified id.
	 * 
	 * @param id the id
	 */
	public Liveness(
			Identifier id) {
		this.id = id;
	}

	private static Collection<Identifier> getIdentifierOperands(
			ValueExpression expression) {
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
	public Liveness pushScope(
			ScopeToken token)
			throws SemanticException {
		return new Liveness((Identifier) id.pushScope(token));
	}

	@Override
	public Liveness popScope(
			ScopeToken token)
			throws SemanticException {
		return new Liveness((Identifier) id.popScope(token));
	}

	@Override
	public Collection<Identifier> getInvolvedIdentifiers() {
		return Collections.singleton(id);
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(id);
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
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
		Liveness other = (Liveness) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public Collection<Liveness> gen(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			PossibleDataflowDomain<Liveness> domain)
			throws SemanticException {
		Collection<Identifier> ids = getIdentifierOperands(expression);
		return ids.stream().map(Liveness::new).collect(Collectors.toSet());
	}

	@Override
	public Collection<Liveness> gen(
			ValueExpression expression,
			ProgramPoint pp,
			PossibleDataflowDomain<Liveness> domain)
			throws SemanticException {
		Collection<Identifier> ids = getIdentifierOperands(expression);
		return ids.stream().map(Liveness::new).collect(Collectors.toSet());
	}

	@Override
	public Collection<Liveness> kill(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			PossibleDataflowDomain<Liveness> domain)
			throws SemanticException {
		return Collections.singleton(new Liveness(id));
	}

	@Override
	public Collection<Liveness> kill(
			ValueExpression expression,
			ProgramPoint pp,
			PossibleDataflowDomain<Liveness> domain)
			throws SemanticException {
		return Collections.emptySet();
	}
}
