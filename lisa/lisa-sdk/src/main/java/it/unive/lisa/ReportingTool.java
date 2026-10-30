package it.unive.lisa;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.outputs.messages.CFGDescriptorMessage;
import it.unive.lisa.outputs.messages.CFGMessage;
import it.unive.lisa.outputs.messages.ExpressionMessage;
import it.unive.lisa.outputs.messages.GlobalMessage;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.outputs.messages.UnitMessage;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.file.FileManager;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An auxiliary tool that can be used by analysis components during their
 * execution. It provides reporting capabilities, file management capabilities,
 * as well as access to the configuration.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReportingTool {

	/**
	 * The collection of generated warnings
	 */
	private final Collection<Message> warnings;

	/**
	 * The collection of generated notices
	 */
	private final Collection<Message> notices;

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
	public ReportingTool(
			LiSAConfiguration configuration,
			FileManager fileManager) {
		warnings = Collections.newSetFromMap(new ConcurrentHashMap<>());
		notices = Collections.newSetFromMap(new ConcurrentHashMap<>());
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
	 * Build the tool, shallow-copying the sets of warnings and notices from the
	 * given one.
	 * 
	 * @param other the original tool to copy
	 */
	protected ReportingTool(
			ReportingTool other) {
		this(other.configuration, other.fileManager);
		warnings.addAll(other.warnings);
		notices.addAll(other.notices);
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
		warnings.add(new Message(message));
	}

	/**
	 * Reports a new warning that is meant to be a generic warning on the
	 * program. For warnings related to one of the components of the program
	 * (e.g., a CFG, a statement, ...) rely on the other methods provided by
	 * this class.
	 * 
	 * @param message           the message of the warning
	 * @param definite          whether or not the warning is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the warning (can be {@code null} if no
	 *                              inputs are known)
	 */
	public void warn(
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		warnings.add(new Message(message, definite, reproducingInputs));
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
		warnings.add(new UnitMessage(unit, message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * given unit.
	 * 
	 * @param unit              the unit to warn on
	 * @param message           the message of the warning
	 * @param definite          whether or not the warning is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the warning (can be {@code null} if no
	 *                              inputs are known)
	 */
	public void warnOn(
			Unit unit,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		warnings.add(new UnitMessage(unit, message, definite, reproducingInputs));
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
		warnings.add(new GlobalMessage(unit, global, message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * given global.
	 * 
	 * @param unit              the unit containing the global to warn on
	 * @param global            the global to warn on
	 * @param message           the message of the warning
	 * @param definite          whether or not the warning is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the warning (can be {@code null} if no
	 *                              inputs are known)
	 */
	public void warnOn(
			Unit unit,
			Global global,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		warnings.add(new GlobalMessage(unit, global, message, definite, reproducingInputs));
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
		warnings.add(new CFGMessage(cfg, message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * given cfg.
	 * 
	 * @param cfg               the cfg to warn on
	 * @param message           the message of the warning
	 * @param definite          whether or not the warning is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the warning (can be {@code null} if no
	 *                              inputs are known)
	 */
	public void warnOn(
			CFG cfg,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		warnings.add(new CFGMessage(cfg, message, definite, reproducingInputs));
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
		warnings.add(new CFGDescriptorMessage(descriptor, message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the
	 * cfg represented by the given descriptor.
	 * 
	 * @param descriptor        the descriptor cfg to warn on
	 * @param message           the message of the warning
	 * @param definite          whether or not the warning is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the warning (can be {@code null} if no
	 *                              inputs are known)
	 */
	public void warnOn(
			CodeMemberDescriptor descriptor,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		warnings.add(new CFGDescriptorMessage(descriptor, message, definite, reproducingInputs));
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
			warnings.add(new StatementMessage(statement, message));
	}

	/**
	 * Reports a new warning with the given message on the given statement. If
	 * {@code statement} is an instance of {@link Expression}, then
	 * {@link #warnOn(Expression, String, boolean, List)} is invoked.
	 * 
	 * @param statement         the statement to warn on
	 * @param message           the message of the warning
	 * @param definite          whether or not the warning is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the warning (can be {@code null} if no
	 *                              inputs are known)
	 */
	public void warnOn(
			Statement statement,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		if (statement instanceof Expression)
			warnOn((Expression) statement, message, definite, reproducingInputs);
		else
			warnings.add(new StatementMessage(statement, message, definite, reproducingInputs));
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
		warnings.add(new ExpressionMessage(expression, message));
	}

	/**
	 * Reports a new warning with the given message on the given expression.
	 * 
	 * @param expression        the expression to warn on
	 * @param message           the message of the warning
	 * @param definite          whether or not the warning is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the warning (can be {@code null} if no
	 *                              inputs are known)
	 */
	public void warnOn(
			Expression expression,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		warnings.add(new ExpressionMessage(expression, message, definite, reproducingInputs));
	}

	/**
	 * Reports the given warning.
	 *
	 * @param warning the warning to report
	 */
	protected void addWarning(
			Message warning) {
		warnings.add(warning);
	}

	/**
	 * Returns an <b>unmodifiable</b> view of the warnings that have been
	 * generated up to now using this tool.
	 * 
	 * @return a view of the warnings
	 */
	public Collection<Message> getWarnings() {
		return Collections.unmodifiableCollection(warnings);
	}

	/**
	 * Reports a new notice that is meant to be a generic notice on the program.
	 * For notices related to one of the components of the program (e.g., a CFG,
	 * a statement, ...) rely on the other methods provided by this class.
	 * 
	 * @param message the message of the notice
	 */
	public void notice(
			String message) {
		notices.add(new Message(message));
	}

	/**
	 * Reports a new notice that is meant to be a generic notice on the program.
	 * For notices related to one of the components of the program (e.g., a CFG,
	 * a statement, ...) rely on the other methods provided by this class.
	 * 
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void notice(
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		notices.add(new Message(message, definite, reproducingInputs));
	}

	/**
	 * Reports a new notice with the given message on the declaration of the
	 * given unit.
	 * 
	 * @param unit    the unit to notice on
	 * @param message the message of the notice
	 */
	public void noticeOn(
			Unit unit,
			String message) {
		notices.add(new UnitMessage(unit, message));
	}

	/**
	 * Reports a new notice with the given message on the declaration of the
	 * given unit.
	 * 
	 * @param unit              the unit to notice on
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void noticeOn(
			Unit unit,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		notices.add(new UnitMessage(unit, message, definite, reproducingInputs));
	}

	/**
	 * Reports a new notice with the given message on the declaration of the
	 * given global.
	 * 
	 * @param unit    the unit containing the global to notice on
	 * @param global  the global to notice on
	 * @param message the message of the notice
	 */
	public void noticeOn(
			Unit unit,
			Global global,
			String message) {
		notices.add(new GlobalMessage(unit, global, message));
	}

	/**
	 * Reports a new notice with the given message on the declaration of the
	 * given global.
	 * 
	 * @param unit              the unit containing the global to notice on
	 * @param global            the global to notice on
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void noticeOn(
			Unit unit,
			Global global,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		notices.add(new GlobalMessage(unit, global, message, definite, reproducingInputs));
	}

	/**
	 * Reports a new notice with the given message on the declaration of the
	 * given cfg.
	 * 
	 * @param cfg     the cfg to notice on
	 * @param message the message of the notice
	 */
	public void noticeOn(
			CFG cfg,
			String message) {
		notices.add(new CFGMessage(cfg, message));
	}

	/**
	 * Reports a new notice with the given message on the declaration of the
	 * given cfg.
	 * 
	 * @param cfg               the cfg to notice on
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void noticeOn(
			CFG cfg,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		notices.add(new CFGMessage(cfg, message, definite, reproducingInputs));
	}

	/**
	 * Reports a new notice with the given message on the declaration of the cfg
	 * represented by the given descriptor.
	 * 
	 * @param descriptor the descriptor cfg to notice on
	 * @param message    the message of the notice
	 */
	public void noticeOn(
			CodeMemberDescriptor descriptor,
			String message) {
		notices.add(new CFGDescriptorMessage(descriptor, message));
	}

	/**
	 * Reports a new notice with the given message on the declaration of the cfg
	 * represented by the given descriptor.
	 * 
	 * @param descriptor        the descriptor cfg to notice on
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void noticeOn(
			CodeMemberDescriptor descriptor,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		notices.add(new CFGDescriptorMessage(descriptor, message, definite, reproducingInputs));
	}

	/**
	 * Reports a new notice with the given message on the given statement. If
	 * {@code statement} is an instance of {@link Expression}, then
	 * {@link #noticeOn(Expression, String)} is invoked.
	 * 
	 * @param statement the statement to notice on
	 * @param message   the message of the notice
	 */
	public void noticeOn(
			Statement statement,
			String message) {
		if (statement instanceof Expression)
			noticeOn((Expression) statement, message);
		else
			notices.add(new StatementMessage(statement, message));
	}

	/**
	 * Reports a new notice with the given message on the given statement. If
	 * {@code statement} is an instance of {@link Expression}, then
	 * {@link #noticeOn(Expression, String, boolean, List)} is invoked.
	 * 
	 * @param statement         the statement to notice on
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void noticeOn(
			Statement statement,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		if (statement instanceof Expression)
			noticeOn((Expression) statement, message, definite, reproducingInputs);
		else
			notices.add(new StatementMessage(statement, message, definite, reproducingInputs));
	}

	/**
	 * Reports a new notice with the given message on the given expression.
	 * 
	 * @param expression the expression to notice on
	 * @param message    the message of the notice
	 */
	public void noticeOn(
			Expression expression,
			String message) {
		notices.add(new ExpressionMessage(expression, message));
	}

	/**
	 * Reports a new notice with the given message on the given expression.
	 * 
	 * @param expression        the expression to notice on
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void noticeOn(
			Expression expression,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		notices.add(new ExpressionMessage(expression, message, definite, reproducingInputs));
	}

	/**
	 * Reports the given notice.
	 *
	 * @param notice the notice to report
	 */
	protected void addNotice(
			Message notice) {
		notices.add(notice);
	}

	/**
	 * Returns an <b>unmodifiable</b> view of the notices that have been
	 * generated up to now using this tool.
	 * 
	 * @return a view of the notices
	 */
	public Collection<Message> getNotices() {
		return Collections.unmodifiableCollection(notices);
	}

}
