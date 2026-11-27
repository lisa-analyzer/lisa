package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Set;

/**
 * A call graph resolving dynamic dispatches through Class Hierarchy Analysis
 * (CHA): a call on a receiver statically typed {@code T} is resolved against
 * every possible instance of {@code T} according to the class hierarchy alone
 * (see {@link #getPossibleTypesOfReceiver(Expression, Set)}), regardless of
 * whether those subtypes are ever actually instantiated in the program (see
 * {@link RTACallGraph} for an analysis that also prunes uninstantiated types).
 *
 * @author <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 *
 * @see <a href="https://doi.org/10.1145/353171.353190">Frank Tip, Jens
 *          Palsberg. Scalable Propagation-Based Call Graph Construction
 *          Algorithms. In Proceedings of the 15th ACM SIGPLAN Conference on
 *          Object-Oriented Programming, Systems, Languages, and Applications
 *          (OOPSLA '00), pages 281-293, ACM, 2000.</a>
 */
public class CHACallGraph
		extends
		BaseCallGraph {

	@Override
	public Collection<Type> getPossibleTypesOfReceiver(
			Expression receiver,
			Set<Type> types) {
		return receiver.getStaticType().allInstances(receiver.getProgram().getTypes());
	}

}
