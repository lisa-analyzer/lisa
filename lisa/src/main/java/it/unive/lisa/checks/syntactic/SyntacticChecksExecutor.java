package it.unive.lisa.checks.syntactic;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Assignment;
import it.unive.lisa.cfg.statement.Call;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.cfg.statement.Throw;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.logging.IterationLogger;

/**
 * Utility class that handles the execution of {@link SyntacticCheck}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SyntacticChecksExecutor {
	
	private static final Logger log = LogManager.getLogger(SyntacticChecksExecutor.class); 

	/**
	 * Executes all the given checks on the given inputs cfgs.
	 * 
	 * @param tool   the auxiliary tool to be used during the checks execution
	 * @param inputs the cfgs to analyze
	 * @param checks the checks to execute
	 */
	public static void executeAll(CheckTool tool, Collection<CFG> inputs, Collection<SyntacticCheck> checks) {
		checks.forEach(c -> c.beforeExecution(tool));
		for (CFG cfg : IterationLogger.iterate(log, inputs, "Analyzing CFGs...", "CFGs"))
			process(tool, cfg, checks);
		checks.forEach(c -> c.afterExecution(tool));
	}

	private static void process(CheckTool tool, CFG cfg, Collection<SyntacticCheck> checks) {
		checks.forEach(c -> c.visitCFGDescriptor(tool, cfg.getDescriptor()));

		for (Statement st : cfg.getNodes()) {
			checks.forEach(c -> c.visitStatement(tool, st));
			if (st instanceof Return)
				checks.forEach(c -> c.visitExpression(tool, ((Return) st).getExpression()));
			else if (st instanceof Throw)
				checks.forEach(c -> c.visitExpression(tool, ((Throw) st).getExpression()));
			else if (st instanceof Assignment) {
				checks.forEach(c -> c.visitExpression(tool, ((Assignment) st).getTarget()));
				checks.forEach(c -> c.visitExpression(tool, ((Assignment) st).getExpression()));
			} else if (st instanceof Call)
				for (Expression param : ((Call) st).getParameters())
					checks.forEach(c -> c.visitExpression(tool, param));
		}
	}
}
