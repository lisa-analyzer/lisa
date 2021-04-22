package it.unive.lisa.checks;

import static it.unive.lisa.logging.IterationLogger.iterate;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
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
	 * @param <C>     the type of the checks to execute
	 * @param <T>     the type of the auxiliary tool used by the check
	 * @param tool    the auxiliary tool to be used during the checks execution
	 * @param program the program to analyze
	 * @param checks  the checks to execute
	 */
	public static <C extends Check<T>, T> void executeAll(T tool, Program program,
			Collection<C> checks) {
		checks.forEach(c -> c.beforeExecution(tool));

		for (Global global : iterate(log, program.getGlobals(), "Analyzing program globals...", "Globals"))
			checks.forEach(c -> c.visitGlobal(tool, program, global, false));

		for (CFG cfg : iterate(log, program.getCFGs(), "Analyzing program cfgs...", "CFGs"))
			checks.forEach(c -> cfg.accept(c, tool));

		for (CompilationUnit unit : iterate(log, program.getUnits(), "Analyzing compilation units...", "Units"))
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

		for (CFG cfg : unit.getCFGs())
			cfg.accept(c, tool);

		for (CFG cfg : unit.getInstanceCFGs(false))
			cfg.accept(c, tool);
	}
}
