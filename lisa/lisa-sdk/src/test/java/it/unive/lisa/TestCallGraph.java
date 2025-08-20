package it.unive.lisa;

import it.unive.lisa.interprocedural.callgraph.BaseCallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Set;

public class TestCallGraph
		extends
		BaseCallGraph {

	@Override
	public Collection<Type> getPossibleTypesOfReceiver(
			Expression receiver,
			Set<Type> types)
			throws CallResolutionException {
		return receiver.getStaticType().allInstances(receiver.getProgram().getTypes());
	}

}
