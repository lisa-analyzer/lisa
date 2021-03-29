package it.unive.lisa.analysis.dataflow.impl;

import java.util.Collection;
import java.util.Collections;

import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.PossibleForwardDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public class ReachingDefinitions
		implements DataflowElement<PossibleForwardDataflowDomain<ReachingDefinitions>, ReachingDefinitions> {

	private final Identifier variable;

	private final ProgramPoint programPoint;

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
}