package it.unive.lisa.checks.semantic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestHeapDomain;
import it.unive.lisa.TestTypeDomain;
import it.unive.lisa.TestValueDomain;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.checks.warnings.CFGDescriptorWarning;
import it.unive.lisa.checks.warnings.CFGWarning;
import it.unive.lisa.checks.warnings.ExpressionWarning;
import it.unive.lisa.checks.warnings.GlobalWarning;
import it.unive.lisa.checks.warnings.StatementWarning;
import it.unive.lisa.checks.warnings.UnitWarning;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

public class CheckToolWithAnalysisResultsTest {

	private static final CompilationUnit unit = new CompilationUnit(new SourceCodeLocation("fake", 1, 0), "fake",
			false);
	private static final Global global = new Global(new SourceCodeLocation("fake", 15, 0), "fake");
	private static final CFGDescriptor descriptor = new CFGDescriptor(new SourceCodeLocation("fake", 2, 0), unit, false,
			"foo");
	private static final CFG cfg = new CFG(descriptor);
	private static final CFGDescriptor descriptor2 = new CFGDescriptor(new SourceCodeLocation("fake", 10, 0), unit,
			false,
			"faa");
	private static final CFG cfg2 = new CFG(descriptor2);

	private static final CallGraph fakeCallGraph = new CallGraph() {

		@Override
		public void registerCall(CFGCall call) {
		}

		@Override
		public void init(Program program) throws CallGraphConstructionException {
		}

		@Override
		public Collection<CodeMember> getCallers(CodeMember cm) {
			return null;
		}

		@Override
		public Collection<CodeMember> getCallees(CodeMember cm) {
			return null;
		}

		@Override
		public Collection<Call> getCallSites(CodeMember cm) {
			return null;
		}

		@Override
		public Call resolve(UnresolvedCall call, ExternalSet<Type>[] types, SymbolAliasing aliasing)
				throws CallResolutionException {
			return null;
		}
	};

	private static Warning build(CheckTool tool, Object target, String message) {
		if (target == null) {
			tool.warn(message);
			return new Warning(message);
		} else if (target instanceof Unit) {
			tool.warnOn((Unit) target, message);
			return new UnitWarning((Unit) target, message);
		} else if (target instanceof Global) {
			tool.warnOn(unit, (Global) target, message);
			return new GlobalWarning(unit, (Global) target, message);
		} else if (target instanceof CFG) {
			tool.warnOn((CFG) target, message);
			return new CFGWarning((CFG) target, message);
		} else if (target instanceof CFGDescriptor) {
			tool.warnOn((CFGDescriptor) target, message);
			return new CFGDescriptorWarning((CFGDescriptor) target, message);
		} else if (target instanceof Expression) {
			tool.warnOn((Expression) target, message);
			return new ExpressionWarning((Expression) target, message);
		} else if (target instanceof Statement) {
			tool.warnOn((Statement) target, message);
			return new StatementWarning((Statement) target, message);
		}
		return null;
	}

	@Test
	public void testCopy() {
		CheckToolWithAnalysisResults<TestAbstractState, TestHeapDomain,
				TestValueDomain, TestTypeDomain> tool = new CheckToolWithAnalysisResults<>(Map.of(), fakeCallGraph);
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, null, "foo"));
		exp.add(build(tool, cfg, "foo"));
		exp.add(build(tool, descriptor, "foo"));
		exp.add(build(tool, global, "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new Int32Literal(cfg, new SourceCodeLocation("fake", 4, 0), 5), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp,
				new CheckToolWithAnalysisResults<>(tool, Map.of(), fakeCallGraph).getWarnings()));
	}

	@Test
	public void testSimpleFill() {
		CheckToolWithAnalysisResults<TestAbstractState, TestHeapDomain,
				TestValueDomain, TestTypeDomain> tool = new CheckToolWithAnalysisResults<>(Map.of(), fakeCallGraph);
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, null, "foo"));
		exp.add(build(tool, cfg, "foo"));
		exp.add(build(tool, descriptor, "foo"));
		exp.add(build(tool, global, "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new Int32Literal(cfg, new SourceCodeLocation("fake", 4, 0), 5), "foo"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

	@Test
	public void testDisjointWarnings() {
		CheckToolWithAnalysisResults<TestAbstractState, TestHeapDomain,
				TestValueDomain, TestTypeDomain> tool = new CheckToolWithAnalysisResults<>(Map.of(), fakeCallGraph);
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

	@Test
	public void testDuplicateWarnings() {
		CheckToolWithAnalysisResults<TestAbstractState, TestHeapDomain,
				TestValueDomain, TestTypeDomain> tool = new CheckToolWithAnalysisResults<>(Map.of(), fakeCallGraph);
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

	@Test
	public void testResultRetrieval() {
		AnalysisState<TestAbstractState, TestHeapDomain,
				TestValueDomain, TestTypeDomain> singleton = new AnalysisState<>(
						new TestAbstractState(new TestHeapDomain(), new TestValueDomain()),
						new ExpressionSet<>());
		NoOp noop = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));
		CFGWithAnalysisResults<TestAbstractState, TestHeapDomain,
				TestValueDomain, TestTypeDomain> res1 = new CFGWithAnalysisResults<>(cfg, singleton,
						Map.of(noop, singleton.bottom()), Map.of(noop, singleton.bottom()));

		noop = new NoOp(cfg2, new SourceCodeLocation("fake", 30, 0));
		CFGWithAnalysisResults<TestAbstractState, TestHeapDomain,
				TestValueDomain, TestTypeDomain> res2 = new CFGWithAnalysisResults<>(cfg2, singleton,
						Map.of(noop, singleton.bottom()), Map.of(noop, singleton.bottom()));

		CheckToolWithAnalysisResults<TestAbstractState, TestHeapDomain,
				TestValueDomain, TestTypeDomain> tool = new CheckToolWithAnalysisResults<>(
						Map.of(cfg, Collections.singleton(res1), cfg2, Collections.singleton(res2)), fakeCallGraph);

		assertEquals(res1, tool.getResultOf(cfg).iterator().next());
		assertEquals(res2, tool.getResultOf(cfg2).iterator().next());
	}
}
