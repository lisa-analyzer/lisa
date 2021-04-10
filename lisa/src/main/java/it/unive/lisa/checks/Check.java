package it.unive.lisa.checks;

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
 * @param <T> the type of tool used during the inspection
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface Check<T> extends GraphVisitor<CFG, Statement, Edge, T> {

	/**
	 * Callback invoked only once before the beginning of the inspection of the
	 * program. Can be used to setup common data structures.
	 * 
	 * @param tool the auxiliary tool that this check can use during the
	 *                 execution
	 */
	void beforeExecution(T tool);

	/**
	 * Callback invoked only once after the end of the inspection of the
	 * program. Can be used to perform cleanups or to report summary warnings.
	 * 
	 * @param tool the auxiliary tool that this check can use during the
	 *                 execution
	 */
	void afterExecution(T tool);
}
