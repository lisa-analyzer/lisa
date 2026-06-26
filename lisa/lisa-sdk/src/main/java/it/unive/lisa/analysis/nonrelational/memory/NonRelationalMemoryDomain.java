package it.unive.lisa.analysis.nonrelational.memory;

import it.unive.lisa.analysis.memory.MemoryDomain;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link NonRelationalDomain} that focuses on the values of program
 * variables. Instances of this class use {@link MemoryEnvironment}s as lattice
 * elements, and are able to process any {@link SymbolicExpression}.
 * Transformers also return substitutions in the form of lits of
 * {@link MemoryReplacement}s, that <b>must</b> be processed in their order of
 * appearance. Each substitution maps {@link Identifier}s in the pre-state to
 * {@link Identifier}s in the post state. If no substitution needs to be
 * applied, an empty list is generated.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the lattice used as values in the environments
 */
public interface NonRelationalMemoryDomain<L extends MemoryValue<L>>
		extends
		MemoryDomain<MemoryEnvironment<L>>,
		NonRelationalDomain<L,
				Pair<MemoryEnvironment<L>, List<MemoryReplacement>>,
				MemoryEnvironment<L>,
				SymbolicExpression> {
}
