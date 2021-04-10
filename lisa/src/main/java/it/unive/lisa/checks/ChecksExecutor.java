package it.unive.lisa.checks;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.cfg.CFG;

/**
 * Utility class that handles the execution of {@link Check}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ChecksExecutor {

	private static final Logger log = LogManager.getLogger(ChecksExecutor.class);

	/**
	 * Executes all the given checks on the given inputs cfgs.
	 * 
	 * @param tool   the auxiliary tool to be used during the checks execution
	 * @param inputs the cfgs to analyze
	 * @param checks the checks to execute
	 */
	public static <C extends Check<T>, T> void executeAll(T tool, Collection<CFG> inputs,
			Collection<C> checks) {
		checks.forEach(c -> c.beforeExecution(tool));
		for (CFG cfg : IterationLogger.iterate(log, inputs, "Analyzing CFGs...", "CFGs"))
			checks.forEach(c -> cfg.accept(c, tool));
		checks.forEach(c -> c.afterExecution(tool));
	}
}
