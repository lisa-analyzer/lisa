package it.unive.lisa.analysis.dataflow.impl;

import java.util.Collection;
import java.util.HashSet;

import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.PossibleForwardDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

// An element of the reaching definitions dataflow domain.
public class ReachingDefinitions 
	implements DataflowElement< // instances of this class represent a single element of the dataflow domain
		PossibleForwardDataflowDomain<ReachingDefinitions>, // the dataflow domain to be used with this element is 
															// a PossibleForwardDataflowDomain containing
															// instances of ReachingDefinitions
		ReachingDefinitions // the concrete type of dataflow elements, that must be the same of this class
	> {

	// the variable defined
	private final Identifier variable;
	// the program point where the variable has been defined
	private final ProgramPoint programPoint;
	
	// this constructor will be used when creating the abstract state
	public ReachingDefinitions() {
		this(null, null);
	}
	
	// this constructor is what we actually use to create dataflow elements 
	private ReachingDefinitions(Identifier variable, ProgramPoint pp) {
		this.programPoint = pp;
		this.variable = variable;
	}

	// instances of this class will end up in collections, so it is a good practice to 
	// implement equals and hashCode methods
	
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
	
	// we want to pretty print information inside the .dot files, so we redefine the 
	// toString method
	
	@Override
	public String toString() {
		return "(" + variable + "," + programPoint + ")"; 
	}

	// the variable we are referring to
	@Override
	public Identifier getIdentifier() {
		return this.variable;
	}

	// the gen function: which elements are we generating when we are performing an assignment
	@Override
	public Collection<ReachingDefinitions> gen(Identifier id, ValueExpression expression, ProgramPoint pp,
			PossibleForwardDataflowDomain<ReachingDefinitions> domain) {
		ReachingDefinitions generated = new ReachingDefinitions(id, pp);
		Collection<ReachingDefinitions> result = new HashSet<>();
		result.add(generated);
		return result;
	}

	// the kill function: which variables are we killing when we perform an assignment
	@Override
	public Collection<Identifier> kill(Identifier id, ValueExpression expression, ProgramPoint pp,
			PossibleForwardDataflowDomain<ReachingDefinitions> domain) {
		Collection<Identifier> result = new HashSet<>();
		result.add(id);
		return result;
	}
}
