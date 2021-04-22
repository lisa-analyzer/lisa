package it.unive.lisa.analysis.impl.dataflow;

import java.util.Collection;
import java.util.Collections;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.PossibleForwardDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * An implementation of the reaching definition dataflow analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReachingDefinitions
		implements DataflowElement<PossibleForwardDataflowDomain<ReachingDefinitions>, ReachingDefinitions> {

	private final Identifier variable;

	private final ProgramPoint programPoint;

	/**
	 * Builds an empty reaching definition object.
	 */
	public ReachingDefinitions() {
		this(null, null);
	}

	private ReachingDefinitions(Identifier variable, ProgramPoint pp) {
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
	public boolean equals(Object obj) {
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
		return "(" + variable + "," + programPoint + ")";
	}

	@Override
	public Identifier getIdentifier() {
		return this.variable;
	}

	@Override
	public Collection<ReachingDefinitions> gen(Identifier id, ValueExpression expression, ProgramPoint pp,
			PossibleForwardDataflowDomain<ReachingDefinitions> domain) {
		return Collections.singleton(new ReachingDefinitions(id, pp));
	}

	@Override
	public Collection<Identifier> kill(Identifier id, ValueExpression expression, ProgramPoint pp,
			PossibleForwardDataflowDomain<ReachingDefinitions> domain) {
		return Collections.singleton(id);
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
	public ReachingDefinitions pushScope(ScopeToken scope) throws SemanticException {
		return new ReachingDefinitions((Identifier) variable.pushScope(scope), programPoint);
	}

	@Override
	public ReachingDefinitions popScope(ScopeToken scope) throws SemanticException {
		if (!(variable instanceof OutOfScopeIdentifier))
			return null;
		
		return new ReachingDefinitions(((OutOfScopeIdentifier) variable).popScope(scope), programPoint);
	}
}