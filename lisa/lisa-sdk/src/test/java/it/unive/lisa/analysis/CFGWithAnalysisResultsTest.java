package it.unive.lisa.analysis;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.TestHeapDomain;
import it.unive.lisa.TestTypeDomain;
import it.unive.lisa.TestValueDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import java.util.Map;
import org.junit.Test;

public class CFGWithAnalysisResultsTest {

	private static final CompilationUnit unit = new CompilationUnit(new SourceCodeLocation("unknown", 0, 0), "Testing",
			false);

	@Test
	public void testIssue189() throws SemanticException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CFGDescriptor(unknown, unit, false, "emptyIf"));
		Int32Literal constant = new Int32Literal(cfg, unknown, 5);
		Return ret = new Return(cfg, unknown, constant);
		cfg.addNode(ret, true);

		AnalysisState<SimpleAbstractState<TestHeapDomain, TestValueDomain, TestTypeDomain>, TestHeapDomain,
				TestValueDomain,
				TestTypeDomain> state = new AnalysisState<>(
						new SimpleAbstractState<>(new TestHeapDomain(), new TestValueDomain(), new TestTypeDomain()),
						new ExpressionSet<>());

		Map<Statement, AnalysisState<SimpleAbstractState<TestHeapDomain, TestValueDomain, TestTypeDomain>,
				TestHeapDomain, TestValueDomain, TestTypeDomain>> entries = Map.of(ret, state);
		Map<Statement, AnalysisState<SimpleAbstractState<TestHeapDomain, TestValueDomain, TestTypeDomain>,
				TestHeapDomain, TestValueDomain, TestTypeDomain>> results = Map.of(ret, state, constant, state);

		CFGWithAnalysisResults<SimpleAbstractState<TestHeapDomain, TestValueDomain, TestTypeDomain>, TestHeapDomain,
				TestValueDomain, TestTypeDomain> res = new CFGWithAnalysisResults<>(cfg, state, entries, results);

		assertEquals(state, res.getAnalysisStateAfter(ret));
		assertEquals(state, res.getAnalysisStateBefore(ret));
		assertEquals(state, res.getAnalysisStateAfter(constant));
		assertEquals(state, res.getAnalysisStateBefore(constant));
	}
}
