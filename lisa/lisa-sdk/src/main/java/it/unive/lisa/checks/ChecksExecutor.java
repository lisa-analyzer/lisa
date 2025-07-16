package it.unive.lisa.checks;

import static it.unive.lisa.logging.IterationLogger.iterate;

import it.unive.lisa.program.Application;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
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
	 * @param <C>    the type of the checks to execute
	 * @param <T>    the type of the auxiliary tool used by the check
	 * @param tool   the auxiliary tool to be used during the checks execution
	 * @param app    the application to analyze
	 * @param checks the checks to execute
	 */
	public static <C extends Check<T>,
			T> void executeAll(
					T tool,
					Application app,
					Iterable<C> checks) {
		checks.forEach(c -> c.beforeExecution(tool));

		for (Program p : app.getPrograms())
			visitProgram(tool, p, checks);

		checks.forEach(c -> c.afterExecution(tool));
	}

	private static <T,
			C extends Check<T>> void visitProgram(
					T tool,
					Program program,
					Iterable<C> checks) {
		for (Global global : iterate(LOG, program.getGlobals(), "Analyzing program globals...", "Globals"))
			checks.forEach(c -> c.visitGlobal(tool, program, global, false));

		for (CodeMember cm : iterate(LOG, program.getCodeMembers(), "Analyzing program cfgs...", "CFGs"))
			if (cm instanceof CFG)
				checks.forEach(c -> ((CFG) cm).accept(c, tool));

		for (Unit unit : iterate(LOG, program.getUnits(), "Analyzing compilation units...", "Units"))
			checks.forEach(c -> visitUnit(tool, unit, c));
	}

	private static <C extends Check<T>,
			T> void visitUnit(
					T tool,
					Unit unit,
					C c) {
		if (!c.visitUnit(tool, unit))
			return;

		for (Global global : unit.getGlobals())
			c.visitGlobal(tool, unit, global, false);

		if (unit instanceof CompilationUnit)
			for (Global global : ((CompilationUnit) unit).getInstanceGlobals(false))
				c.visitGlobal(tool, unit, global, true);

		for (CodeMember cm : unit.getCodeMembers())
			if (cm instanceof CFG)
				((CFG) cm).accept(c, tool);

		if (unit instanceof CompilationUnit)
			for (CFG cfg : ((CompilationUnit) unit).getInstanceCFGs(false))
				cfg.accept(c, tool);
	}

}
