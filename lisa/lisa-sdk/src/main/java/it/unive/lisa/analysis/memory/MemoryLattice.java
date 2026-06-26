package it.unive.lisa.analysis.memory;

import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link DomainLattice} that tracks information about the structure of the
 * memory of a program. Implementations are tasked with modeling points-to
 * relations, aliasing, reachability, and other memory-related properties.
 * Operations that modify this lattice, such as storing or deleting values, also
 * return a list of {@link MemoryReplacement}s, which describe materialization,
 * summarization and deletion of memory locations. {@link MemoryReplacement}s
 * <b>must</b> be processed in their order of appearance. Each substitution maps
 * {@link Identifier}s in the pre-state to {@link Identifier}s in the post
 * state. If no substitution needs to be applied, an empty list is generated.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete type of {@link MemoryLattice}
 */
public interface MemoryLattice<L extends MemoryLattice<L>>
		extends
		DomainLattice<L, Pair<L, List<MemoryReplacement>>> {

	/**
	 * Expands the given memory replacement {@code base} by adding all locations
	 * that are reachable <b>only</b> from the sources of {@code base},
	 * effectively performing a garbage collection operation.
	 * 
	 * @param base the starting point for the expansion of the replacement
	 * 
	 * @return the expanded list of memory replacements
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	List<MemoryReplacement> expand(
			MemoryReplacement base)
			throws SemanticException;

}
