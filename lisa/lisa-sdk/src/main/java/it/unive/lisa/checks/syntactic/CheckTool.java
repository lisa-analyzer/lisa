package it.unive.lisa.checks.syntactic;

import it.unive.lisa.checks.warnings.CFGDescriptorWarning;
import it.unive.lisa.checks.warnings.CFGWarning;
import it.unive.lisa.checks.warnings.ExpressionWarning;
import it.unive.lisa.checks.warnings.GlobalWarning;
import it.unive.lisa.checks.warnings.StatementWarning;
import it.unive.lisa.checks.warnings.UnitWarning;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.file.FileManager;
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
	 * The configuration of the analysis
	 */
	private final LiSAConfiguration configuration;

	/**
	 * The file manager of the analysis
	 */
	private final FileManager fileManager;

	/**
	 * Build the tool.
	 * 
	 * @param configuration the configuration of the analysis
	 * @param fileManager   the file manager of the analysis
	 */
	public CheckTool(
			LiSAConfiguration configuration,
			FileManager fileManager) {
		warnings = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.configuration = configuration;
		this.fileManager = fileManager;
	}

	/**
	 * Yields the {@link LiSAConfiguration} of the current analysis.
	 * 
	 * @return the configuration
	 */
	public LiSAConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Yields the {@link FileManager} of the current analysis.
	 * 
	 * @return the file manager
	 */
	public FileManager getFileManager() {
		return fileManager;
	}

	/**
	 * Build the tool, shallow-copying the set of warnings from the given one.
	 * 
	 * @param other the original tool to copy
	 */
	protected CheckTool(
			CheckTool other) {
		this(other.configuration, other.fileManager);
		warnings.addAll(other.warnings);
	}

	/**
	 * Reports a new warning that is meant to be a generic warning on the
	 * program. For warnings related to one of the components of the program
	 * (e.g., a CFG, a statement, ...) rely on the other methods provided by
	 * this class.
	 * 
	 * @param message the message of the warning
	 */
	public void warn(
			String message) {
		warnings.add(new Warning(message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * given unit.
	 * 
	 * @param unit    the unit to warn on
	 * @param message the message of the warning
	 */
	public void warnOn(
			Unit unit,
			String message) {
		warnings.add(new UnitWarning(unit, message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * given global.
	 * 
	 * @param unit    the unit containing the global to warn on
	 * @param global  the global to warn on
	 * @param message the message of the warning
	 */
	public void warnOn(
			Unit unit,
			Global global,
			String message) {
		warnings.add(new GlobalWarning(unit, global, message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * given cfg.
	 * 
	 * @param cfg     the cfg to warn on
	 * @param message the message of the warning
	 */
	public void warnOn(
			CFG cfg,
			String message) {
		warnings.add(new CFGWarning(cfg, message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * cfg represented by the given descriptor.
	 * 
	 * @param descriptor the descriptor cfg to warn on
	 * @param message    the message of the warning
	 */
	public void warnOn(
			CodeMemberDescriptor descriptor,
			String message) {
		warnings.add(new CFGDescriptorWarning(descriptor, message));
	}

	/**
	 * Reports a new warning with the given message on the given statement. If
	 * {@code statement} is an instance of {@link Expression}, then
	 * {@link #warnOn(Expression, String)} is invoked.
	 * 
	 * @param statement the statement to warn on
	 * @param message   the message of the warning
	 */
	public void warnOn(
			Statement statement,
			String message) {
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
	public void warnOn(
			Expression expression,
			String message) {
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
