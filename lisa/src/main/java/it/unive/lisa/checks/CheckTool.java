package it.unive.lisa.checks;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFGDescriptor;
import it.unive.lisa.cfg.statement.Statement;

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
	 * Reports a new warning with the given message on the declaration of the given
	 * cfg.
	 * 
	 * @param cfg     the cfg to warn on
	 * @param message the message of the warning
	 */
	public void warn(CFG cfg, String message) {
		warnings.add(new Warning(cfg.getDescriptor().getSourceFile(), cfg.getDescriptor().getLine(),
				cfg.getDescriptor().getCol(), message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the cfg
	 * represented by the given descriptor.
	 * 
	 * @param descriptor the descriptor cfg to warn on
	 * @param message    the message of the warning
	 */
	public void warn(CFGDescriptor descriptor, String message) {
		warnings.add(new Warning(descriptor.getSourceFile(), descriptor.getLine(), descriptor.getCol(), message));
	}

	/**
	 * Reports a new warning with the given message on the declaration of the given
	 * statement.
	 * 
	 * @param statement the statement to warn on
	 * @param message   the message of the warning
	 */
	public void warn(Statement statement, String message) {
		warnings.add(new Warning(statement.getSourceFile(), statement.getLine(), statement.getCol(), message));
	}

	/**
	 * Returns an <b>unmodifiable</b> view of the warnings that have been generated
	 * up to now using this tool.
	 * 
	 * @return a view of the warnings
	 */
	public Collection<Warning> getWarnings() {
		return Collections.unmodifiableCollection(warnings);
	}
}
