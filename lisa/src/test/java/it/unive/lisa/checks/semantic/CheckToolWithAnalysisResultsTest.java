package it.unive.lisa.checks.semantic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.impl.heap.MonolithicHeap;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.checks.warnings.CFGDescriptorWarning;
import it.unive.lisa.checks.warnings.CFGWarning;
import it.unive.lisa.checks.warnings.ExpressionWarning;
import it.unive.lisa.checks.warnings.GlobalWarning;
import it.unive.lisa.checks.warnings.StatementWarning;
import it.unive.lisa.checks.warnings.UnitWarning;
import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Literal;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.types.IntType;

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
		CheckToolWithAnalysisResults<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
				ValueEnvironment<Sign>> tool = new CheckToolWithAnalysisResults<>(Map.of());
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, null, "foo"));
		exp.add(build(tool, cfg, "foo"));
		exp.add(build(tool, descriptor, "foo"));
		exp.add(build(tool, global, "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new Literal(cfg, new SourceCodeLocation("fake", 4, 0), 5, IntType.INSTANCE), "foo"));
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
				new CheckToolWithAnalysisResults<>(tool, Map.of()).getWarnings()));
	}

	@Test
	public void testSimpleFill() {
		CheckToolWithAnalysisResults<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
				ValueEnvironment<Sign>> tool = new CheckToolWithAnalysisResults<>(Map.of());
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, null, "foo"));
		exp.add(build(tool, cfg, "foo"));
		exp.add(build(tool, descriptor, "foo"));
		exp.add(build(tool, global, "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new Literal(cfg, new SourceCodeLocation("fake", 4, 0), 5, IntType.INSTANCE), "foo"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

	@Test
	public void testDisjointWarnings() {
		CheckToolWithAnalysisResults<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
				ValueEnvironment<Sign>> tool = new CheckToolWithAnalysisResults<>(Map.of());
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

	@Test
	public void testDuplicateWarnings() {
		CheckToolWithAnalysisResults<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
				ValueEnvironment<Sign>> tool = new CheckToolWithAnalysisResults<>(Map.of());
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
		AnalysisState<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
				ValueEnvironment<Sign>> singleton = new AnalysisState<>(
						new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign())),
						new ExpressionSet<>());
		NoOp noop = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));
		CFGWithAnalysisResults<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
				ValueEnvironment<Sign>> res1 = new CFGWithAnalysisResults<>(cfg, singleton,
						Map.of(noop, singleton.bottom()), Map.of(noop, singleton.bottom()));

		noop = new NoOp(cfg2, new SourceCodeLocation("fake", 30, 0));
		CFGWithAnalysisResults<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
				ValueEnvironment<Sign>> res2 = new CFGWithAnalysisResults<>(cfg2, singleton,
						Map.of(noop, singleton.bottom()), Map.of(noop, singleton.bottom()));

		CheckToolWithAnalysisResults<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
				ValueEnvironment<
						Sign>> tool = new CheckToolWithAnalysisResults<>(
								Map.of(cfg, Collections.singleton(res1), cfg2, Collections.singleton(res2)));

		assertEquals(res1, tool.getResultOf(cfg).iterator().next());
		assertEquals(res2, tool.getResultOf(cfg2).iterator().next());
	}
}