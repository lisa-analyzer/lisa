package it.unive.lisa.program.cfg.statement.call.traversal;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.statement.call.Call;

/**
 * A strategy for traversing hierarchies of {@link CompilationUnit} to search
 * for implementations of call targets. Depending on the call itself and on the
 * language, the order in which type hierarchies are traversed to find targets
 * of calls changes (e.g. languages with multiple inheritance have specific
 * algorithms for traversing it). Each strategy comes with a different
 * {@link #traverse(Call, CompilationUnit)} implementation that yields an
 * iterable containing (or generating on-the-fly) {@link CompilationUnit}s in
 * the order they should be visited.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface HierarcyTraversalStrategy {

	/**
	 * Yields an iterable containing (or generating on-the-fly) compilation
	 * units in the order they should be visited for traversing a type hierarchy
	 * to find call targets.
	 * 
	 * @param call  the call for which the traversal is requested
	 * @param start the unit where the traversal should start
	 * 
	 * @return an iterable that contains the units in order in which they must
	 *             be visited
	 */
	Iterable<CompilationUnit> traverse(Call call, CompilationUnit start);
}
