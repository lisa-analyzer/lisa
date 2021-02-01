package it.unive.lisa.analysis.dataflow;

import java.util.Collection;

import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public interface DataflowElement<D extends DataflowDomain<D, E>, E extends DataflowElement<D, E>> {
	
	Identifier getIdentifier();

	Collection<E> gen(Identifier id, ValueExpression expression, ProgramPoint pp, D domain);
	
	Collection<Identifier> kill(Identifier id, ValueExpression expression, ProgramPoint pp, D domain);
}
