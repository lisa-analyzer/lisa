package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * An implementation of the reaching definition dataflow analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReachingDefinitions
		implements
		DataflowElement<PossibleDataflowDomain<ReachingDefinitions>, ReachingDefinitions> {

	private final Identifier variable;

	private final ProgramPoint programPoint;

	/**
	 * Builds an empty reaching definition object.
	 */
	public ReachingDefinitions() {
		this(null, null);
	}

	/**
	 * Builds a new reaching definition object.
	 * 
	 * @param variable the variable being defined
	 * @param pp       the location where the definition happens
	 */
	public ReachingDefinitions(
			Identifier variable,
			ProgramPoint pp) {
		this.programPoint = pp;
		this.variable = variable;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((programPoint == null) ? 0 : programPoint.hashCode());
		result = prime * result + ((variable == null) ? 0 : variable.hashCode());
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
		ReachingDefinitions other = (ReachingDefinitions) obj;
		if (programPoint == null) {
			if (other.programPoint != null)
				return false;
		} else if (!programPoint.equals(other.programPoint))
			return false;
		if (variable == null) {
			if (other.variable != null)
				return false;
		} else if (!variable.equals(other.variable))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public Collection<Identifier> getInvolvedIdentifiers() {
		return Collections.singleton(variable);
	}

	@Override
	public Collection<ReachingDefinitions> gen(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			PossibleDataflowDomain<ReachingDefinitions> domain) {
		return Collections.singleton(new ReachingDefinitions(id, pp));
	}

	@Override
	public Collection<ReachingDefinitions> gen(
			ValueExpression expression,
			ProgramPoint pp,
			PossibleDataflowDomain<ReachingDefinitions> domain) {
		return Collections.emptyList();
	}

	@Override
	public Collection<ReachingDefinitions> kill(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			PossibleDataflowDomain<ReachingDefinitions> domain) {
		Collection<ReachingDefinitions> result = new HashSet<>();

		for (ReachingDefinitions rd : domain.getDataflowElements())
			if (rd.variable.equals(id))
				result.add(rd);

		return result;
	}

	@Override
	public Collection<ReachingDefinitions> kill(
			ValueExpression expression,
			ProgramPoint pp,
			PossibleDataflowDomain<ReachingDefinitions> domain) {
		return Collections.emptyList();
	}

	@Override
	public StructuredRepresentation representation() {
		return new ListRepresentation(new StringRepresentation(variable), new StringRepresentation(programPoint));
	}

	@Override
	public ReachingDefinitions pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		return new ReachingDefinitions((Identifier) variable.pushScope(scope, pp), programPoint);
	}

	@Override
	public ReachingDefinitions popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		if (!variable.canBeScoped())
			return this;

		SymbolicExpression popped = variable.popScope(scope, pp);
		if (popped == null)
			return null;

		return new ReachingDefinitions((Identifier) popped, programPoint);
	}
}
