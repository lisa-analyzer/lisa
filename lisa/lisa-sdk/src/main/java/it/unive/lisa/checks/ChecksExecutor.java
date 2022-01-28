package it.unive.lisa.checks;

import static it.unive.lisa.logging.IterationLogger.iterate;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.ImplementedCFG;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class that handles the execution of {@link Check}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ChecksExecutor {

	private static final Logger LOG = LogManager.getLogger(ChecksExecutor.class);

	private ChecksExecutor() {
		// this class is just a static holder
	}

	/**
	 * Executes all the given checks on the given inputs cfgs.
	 * 
	 * @param <C>     the type of the checks to execute
	 * @param <T>     the type of the auxiliary tool used by the check
	 * @param tool    the auxiliary tool to be used during the checks execution
	 * @param program the program to analyze
	 * @param checks  the checks to execute
	 */
	public static <C extends Check<T>, T> void executeAll(T tool, Program program,
			Iterable<C> checks) {
		checks.forEach(c -> c.beforeExecution(tool));

		for (Global global : iterate(LOG, program.getGlobals(), "Analyzing program globals...", "Globals"))
			checks.forEach(c -> c.visitGlobal(tool, program, global, false));

		for (ImplementedCFG cfg : iterate(LOG, program.getCFGs(), "Analyzing program cfgs...", "CFGs"))
			checks.forEach(c -> cfg.accept(c, tool));

		for (CompilationUnit unit : iterate(LOG, program.getUnits(), "Analyzing compilation units...", "Units"))
			checks.forEach(c -> visitUnit(tool, unit, c));

		checks.forEach(c -> c.afterExecution(tool));
	}

	private static <C extends Check<T>, T> void visitUnit(T tool, CompilationUnit unit, C c) {
		if (!c.visitCompilationUnit(tool, unit))
			return;

		for (Global global : unit.getGlobals())
			c.visitGlobal(tool, unit, global, false);

		for (Global global : unit.getInstanceGlobals(false))
			c.visitGlobal(tool, unit, global, true);

		for (ImplementedCFG cfg : unit.getCFGs())
			cfg.accept(c, tool);

		for (ImplementedCFG cfg : unit.getInstanceCFGs(false))
			cfg.accept(c, tool);
	}
}
