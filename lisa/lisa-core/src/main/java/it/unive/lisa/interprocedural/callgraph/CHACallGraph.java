package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Set;

/**
 * A call graph constructed following the Class Hierarchy Analysis as defined
 * in: Frank Tip and Jens Palsberg. 2000. Scalable propagation-based call graph
 * construction algorithms. In Proceedings of the 15th ACM SIGPLAN conference on
 * Object-oriented programming, systems, languages, and applications (OOPSLA
 * '00). Association for Computing Machinery, New York, NY, USA, 281–293.
 * DOI:https://doi.org/10.1145/353171.353190
 *
 * @author <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 */
public class CHACallGraph extends BaseCallGraph {

	@Override
	public Collection<Type> getPossibleTypesOfReceiver(Expression receiver, Set<Type> types) {
		return receiver.getStaticType().allInstances(receiver.getProgram().getTypes());
	}

}
