package it.unive.lisa.checks;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.io.IOException;
import org.junit.Test;

public class ChecksExecutorTest extends AnalysisTestExecutor {

	private static class VariableI implements SyntacticCheck {

		@Override
		public boolean visit(CheckTool tool, CFG graph, Statement node) {
			if (node instanceof VariableRef && ((VariableRef) node).getName().equals("i"))
				tool.warnOn(node, "Found variable i");
			return true;
		}
	}

	@Test
	public void testSyntacticChecks() throws IOException, ParsingException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.syntacticChecks.add(new VariableI());
		perform("syntactic", "expressions.imp", conf);
	}
}
