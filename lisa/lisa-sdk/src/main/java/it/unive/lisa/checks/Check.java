package it.unive.lisa.checks;

import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * A check that inspects the syntactic structure of the program to report
 * warnings. The inspection is performed by providing callbacks that will be
 * invoked by LiSA while visiting the program structure. A check is supposed to
 * perform on each syntactic element in isolation, potentially in parallel. This
 * means that implementers of this interface should take care of sharing data
 * between different callback calls <i>only</i> through thread-safe data
 * structures.<br>
 * <br>
 * The check is parametric to the type {@code T} of the tool that will be used
 * during the inspection.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of tool used during the inspection
 */
public interface Check<T>
		extends
		GraphVisitor<CFG, Statement, Edge, T> {

	/**
	 * Callback invoked only once before the beginning of the inspection of the
	 * program. Can be used to setup common data structures. The default
	 * implementation does nothing.
	 * 
	 * @param tool the auxiliary tool that this check can use during the
	 *                 execution
	 */
	default void beforeExecution(
			T tool) {
	}

	/**
	 * Callback invoked only once after the end of the inspection of the
	 * program. Can be used to perform cleanups or to report summary warnings.
	 * The default implementation does nothing.
	 * 
	 * @param tool the auxiliary tool that this check can use during the
	 *                 execution
	 */
	default void afterExecution(
			T tool) {
	}

	/**
	 * Visits the given unit. The default implementation does nothing and
	 * returns {@code true}.
	 * 
	 * @param tool the auxiliary tool that this visitor can use
	 * @param unit the unit being visited
	 * 
	 * @return whether or not the visiting should continue when this call
	 *             returns. If this method returns {@code false}, all members of
	 *             the unit will not be visited.
	 */
	default boolean visitUnit(
			T tool,
			Unit unit) {
		return true;
	}

	/**
	 * Visits the given global. The default implementation does nothing.
	 * 
	 * @param tool     the auxiliary tool that this visitor can use
	 * @param unit     the unit where the global belongs
	 * @param global   the global being visited
	 * @param instance whether or not the global is an instance member of the
	 *                     unit
	 */
	default void visitGlobal(
			T tool,
			Unit unit,
			Global global,
			boolean instance) {
	}

}
