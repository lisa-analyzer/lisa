package it.unive.lisa.checks.syntactic;

import it.unive.lisa.cfg.CFGDescriptor;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.checks.CheckTool;

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
public interface SyntacticCheck {

	/**
	 * Callback invoked only once before the beginning of the inspection of the
	 * program. Can be used to setup common data structures.
	 * 
	 * @param tool the auxiliary tool that this check can use during the execution
	 */
	void beforeExecution(CheckTool tool);

	/**
	 * Callback invoked when inspecting the descriptor of a CFG.
	 * 
	 * @param tool       the auxiliary tool that this check can use during the
	 *                   execution
	 * @param descriptor the descriptor that is currently being inspected
	 */
	void visitCFGDescriptor(CheckTool tool, CFGDescriptor descriptor);

	/**
	 * Callback invoked when inspecting a statement. It is guaranteed that the
	 * statement is not an instance of {@link Expression}, for which
	 * {@link #visitExpression(CheckTool, Expression)} will be invoked instead.
	 * 
	 * @param tool      the auxiliary tool that this check can use during the
	 *                  execution
	 * @param statement the statement that is currently being inspected
	 */
	void visitStatement(CheckTool tool, Statement statement);

	/**
	 * Callback invoked when inspecting the an expression.
	 * 
	 * @param tool       the auxiliary tool that this check can use during the
	 *                   execution
	 * @param expression the expression that is currently being inspected
	 */
	void visitExpression(CheckTool tool, Expression expression);

	/**
	 * Callback invoked only once after the end of the inspection of the program.
	 * Can be used to perform cleanups or to report summary warnings.
	 * 
	 * @param tool the auxiliary tool that this check can use during the execution
	 */
	void afterExecution(CheckTool tool);
}
