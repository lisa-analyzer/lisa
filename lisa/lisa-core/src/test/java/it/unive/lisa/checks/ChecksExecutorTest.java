package it.unive.lisa.checks;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.io.IOException;
import org.junit.Test;

public class ChecksExecutorTest extends AnalysisTestExecutor {

	private static class VariableI implements SyntacticCheck {

		@Override
		public void beforeExecution(CheckTool tool) {
		}

		@Override
		public void afterExecution(CheckTool tool) {
		}

		@Override
		public boolean visit(CheckTool tool, CFG graph, Statement node) {
			if (node instanceof VariableRef && ((VariableRef) node).getName().equals("i"))
				tool.warnOn(node, "Found variable i");
			return true;
		}

		@Override
		public boolean visit(CheckTool tool, CFG g) {
			return true;
		}

		@Override
		public boolean visit(CheckTool tool, CFG graph, Edge edge) {
			return true;
		}

		@Override
		public void visitGlobal(CheckTool tool, Unit unit, Global global, boolean instance) {
		}

		@Override
		public boolean visitUnit(CheckTool tool, Unit unit) {
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
