package it.unive.lisa.checks;

import it.unive.lisa.checks.warnings.CFGDesccriptorWarning;
import it.unive.lisa.checks.warnings.CFGWarning;
import it.unive.lisa.checks.warnings.ExpressionWarning;
import it.unive.lisa.checks.warnings.StatementWarning;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An auxiliary tool that can be used by checks during their execution. It
 * provides reporting capabilities, as well as access to analysis singletons,
 * such as executed semantic analyses.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CheckTool {

	/**
	 * The collection of generated warnings
	 */
	private final Collection<Warning> warnings;

	/**
	 * Build the tool.
	 */
	public CheckTool() {
		warnings = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	/**
	 * Reports a new warning that is meant to be a generic warning on the
	 * program. For warnings related to one of the components of the program
	 * (e.g., a CFG, a statement, ...) rely on the other methods provided by
	 * this class.
	 * 
	 * @param message the message of the warning
	 */
	public void warn(String message) {
		warnings.add(new Warning(message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * given cfg.
	 * 
	 * @param cfg     the cfg to warn on
	 * @param message the message of the warning
	 */
	public void warnOn(CFG cfg, String message) {
		warnings.add(new CFGWarning(cfg, message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * cfg represented by the given descriptor.
	 * 
	 * @param descriptor the descriptor cfg to warn on
	 * @param message    the message of the warning
	 */
	public void warnOn(CFGDescriptor descriptor, String message) {
		warnings.add(new CFGDesccriptorWarning(descriptor, message));
	}

	/**
	 * Reports a new warning with the given message on the given statement. If
	 * {@code statement} is an instance of {@link Expression}, then
	 * {@link #warnOn(Expression, String)} is invoked.
	 * 
	 * @param statement the statement to warn on
	 * @param message   the message of the warning
	 */
	public void warnOn(Statement statement, String message) {
		if (statement instanceof Expression)
			warnOn((Expression) statement, message);
		else
			warnings.add(new StatementWarning(statement, message));
	}

	/**
	 * Reports a new warning with the given message on the given expression.
	 * 
	 * @param expression the expression to warn on
	 * @param message    the message of the warning
	 */
	public void warnOn(Expression expression, String message) {
		warnings.add(new ExpressionWarning(expression, message));
	}

	/**
	 * Returns an <b>unmodifiable</b> view of the warnings that have been
	 * generated up to now using this tool.
	 * 
	 * @return a view of the warnings
	 */
	public Collection<Warning> getWarnings() {
		return Collections.unmodifiableCollection(warnings);
	}
}
