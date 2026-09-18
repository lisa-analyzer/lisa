package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Set;

/**
 * A call graph resolving dynamic dispatches through Rapid Type Analysis (RTA):
 * a call on a receiver statically typed {@code T} is resolved only against the
 * subtypes of {@code T} that are known to be actually instantiated somewhere in
 * the program (see {@link #getPossibleTypesOfReceiver(Expression, Set)}),
 * rather than against every possible subtype of {@code T} as
 * {@link CHACallGraph} does.
 *
 * @author <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 *
 * @see <a href="https://doi.org/10.1145/353171.353190">Frank Tip, Jens
 *          Palsberg. Scalable Propagation-Based Call Graph Construction
 *          Algorithms. In Proceedings of the 15th ACM SIGPLAN Conference on
 *          Object-Oriented Programming, Systems, Languages, and Applications
 *          (OOPSLA '00), pages 281-293, ACM, 2000.</a>
 */
public class RTACallGraph
		extends
		BaseCallGraph {

	@Override
	public Collection<Type> getPossibleTypesOfReceiver(
			Expression receiver,
			Set<Type> types) {
		return types;
	}

}
