package it.unive.lisa.checks.syntactic;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Assignment;
import it.unive.lisa.cfg.statement.Call;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.cfg.statement.Throw;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.logging.IterationLogger;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
			processCFG(tool, cfg, checks);
		checks.forEach(c -> c.afterExecution(tool));
	}

	private static void processCFG(CheckTool tool, CFG cfg, Collection<SyntacticCheck> checks) {
		checks.forEach(c -> c.visitCFGDescriptor(tool, cfg.getDescriptor()));

		for (Statement st : cfg.getNodes())
			if (st instanceof Expression)
				processExpression(tool, checks, (Expression) st);
			else
				processStatement(tool, checks, st);
	}

	private static void processStatement(CheckTool tool, Collection<SyntacticCheck> checks, Statement st) {
		checks.forEach(c -> c.visitStatement(tool, st));

		if (st instanceof Return)
			processExpression(tool, checks, ((Return) st).getExpression());
		else if (st instanceof Throw)
			processExpression(tool, checks, ((Throw) st).getExpression());
	}

	private static void processExpression(CheckTool tool, Collection<SyntacticCheck> checks, Expression expression) {
		checks.forEach(c -> c.visitExpression(tool, expression));

		if (expression instanceof Assignment) {
			processExpression(tool, checks, ((Assignment) expression).getTarget());
			processExpression(tool, checks, ((Assignment) expression).getExpression());
		} else if (expression instanceof Call)
			for (Expression param : ((Call) expression).getParameters())
				processExpression(tool, checks, param);
	}
}
