package it.unive.lisa.checks.syntactic;

import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * A check that inspects the syntactic structure of the program to report
 * warnings about its structure. The inspection is performed by providing
 * callbacks that will be invoked by LiSA while visiting the program structure.
 * A syntactic check is supposed to perform on each syntactic element in
 * isolation, potentially in parallel. This means that implementers of this
 * interface should take care of sharing data between different callback calls
 * <i>only</i> through thread-safe data structures.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SyntacticCheck extends GraphVisitor<CFG, Statement, Edge, CheckTool> {

	/**
	 * Callback invoked only once before the beginning of the inspection of the
	 * program. Can be used to setup common data structures.
	 * 
	 * @param tool the auxiliary tool that this check can use during the
	 *                 execution
	 */
	void beforeExecution(CheckTool tool);

	/**
	 * Callback invoked only once after the end of the inspection of the
	 * program. Can be used to perform cleanups or to report summary warnings.
	 * 
	 * @param tool the auxiliary tool that this check can use during the
	 *                 execution
	 */
	void afterExecution(CheckTool tool);
}
