package it.unive.lisa.checks.semantic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.outputs.warnings.CFGDescriptorWarning;
import it.unive.lisa.outputs.warnings.CFGWarning;
import it.unive.lisa.outputs.warnings.ExpressionWarning;
import it.unive.lisa.outputs.warnings.GlobalWarning;
import it.unive.lisa.outputs.warnings.StatementWarning;
import it.unive.lisa.outputs.warnings.UnitWarning;
import it.unive.lisa.outputs.warnings.Warning;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.file.FileManager;

public class CheckToolWithAnalysisResultsTest {

	private static final ClassUnit unit = new ClassUnit(
			new SourceCodeLocation("fake", 1, 0),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"fake",
			false);

	private static final Global global = new Global(new SourceCodeLocation("fake", 15, 0), unit, "fake", false);

	private static final CodeMemberDescriptor descriptor = new CodeMemberDescriptor(
			new SourceCodeLocation("fake", 2, 0),
			unit,
			false,
			"foo");

	private static final CFG cfg = new CFG(descriptor);

	private static final CodeMemberDescriptor descriptor2 = new CodeMemberDescriptor(
			new SourceCodeLocation("fake", 10, 0),
			unit,
			false,
			"faa");

	private static final CFG cfg2 = new CFG(descriptor2);

	private static final CallGraph fakeCallGraph = new CallGraph() {

		@Override
		public void registerCall(
				CFGCall call) {
		}

		@Override
		public void init(
				Application app)
				throws CallGraphConstructionException {
			super.init(app);
		}

		@Override
		public Collection<CodeMember> getCallers(
				CodeMember cm) {
			return null;
		}

		@Override
		public Collection<CodeMember> getCallees(
				CodeMember cm) {
			return null;
		}

		@Override
		public Collection<Call> getCallSites(
				CodeMember cm) {
			return null;
		}

		@Override
		public Call resolve(
				UnresolvedCall call,
				Set<Type>[] types,
				SymbolAliasing aliasing)
				throws CallResolutionException {
			return null;
		}

		@Override
		public Collection<Collection<CodeMember>> getRecursions() {
			return null;
		}

		@Override
		public Collection<Collection<CodeMember>> getRecursionsContaining(
				CodeMember cm) {
			return null;
		}

	};

	private static Warning build(
			CheckTool tool,
			Object target,
			String message) {
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
		} else if (target instanceof CodeMemberDescriptor) {
			tool.warnOn((CodeMemberDescriptor) target, message);
			return new CFGDescriptorWarning((CodeMemberDescriptor) target, message);
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
		CheckToolWithAnalysisResults<TestAbstractState,
				TestAbstractDomain> tool = new CheckToolWithAnalysisResults<>(
						new LiSAConfiguration(),
						new FileManager("foo"),
						Map.of(),
						fakeCallGraph,
						new Analysis<>(new TestAbstractDomain()));
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, null, "foo"));
		exp.add(build(tool, cfg, "foo"));
		exp.add(build(tool, descriptor, "foo"));
		exp.add(build(tool, global, "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new VariableRef(cfg, new SourceCodeLocation("fake", 4, 0), "x"), "foo"));
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
		assertTrue(
				"Wrong set of warnings",
				CollectionUtils.isEqualCollection(
						exp,
						new CheckToolWithAnalysisResults<>(
								tool,
								Map.of(),
								fakeCallGraph,
								new Analysis<>(new TestAbstractDomain())).getWarnings()));
	}

	@Test
	public void testSimpleFill() {
		CheckToolWithAnalysisResults<TestAbstractState,
				TestAbstractDomain> tool = new CheckToolWithAnalysisResults<>(
						new LiSAConfiguration(),
						new FileManager("foo"),
						Map.of(),
						fakeCallGraph,
						new Analysis<>(new TestAbstractDomain()));
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, null, "foo"));
		exp.add(build(tool, cfg, "foo"));
		exp.add(build(tool, descriptor, "foo"));
		exp.add(build(tool, global, "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new VariableRef(cfg, new SourceCodeLocation("fake", 4, 0), "x"), "foo"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

	@Test
	public void testDisjointWarnings() {
		CheckToolWithAnalysisResults<TestAbstractState,
				TestAbstractDomain> tool = new CheckToolWithAnalysisResults<>(
						new LiSAConfiguration(),
						new FileManager("foo"),
						Map.of(),
						fakeCallGraph,
						new Analysis<>(new TestAbstractDomain()));
		Collection<Warning> exp = new HashSet<>();

		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "foo"));
		exp.add(build(tool, new NoOp(cfg, new SourceCodeLocation("fake", 5, 0)), "faa"));

		assertTrue("Wrong set of warnings", CollectionUtils.isEqualCollection(exp, tool.getWarnings()));
	}

	@Test
	public void testDuplicateWarnings() {
		CheckToolWithAnalysisResults<TestAbstractState,
				TestAbstractDomain> tool = new CheckToolWithAnalysisResults<>(
						new LiSAConfiguration(),
						new FileManager("foo"),
						Map.of(),
						fakeCallGraph,
						new Analysis<>(new TestAbstractDomain()));
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
		AnalysisState<TestAbstractState> singleton = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		NoOp noop = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));
		AnalyzedCFG<TestAbstractState> res1 = new AnalyzedCFG<>(
				cfg,
				new UniqueScope<>(),
				singleton,
				Map.of(noop, singleton.bottom()),
				Map.of(noop, singleton.bottom()));

		noop = new NoOp(cfg2, new SourceCodeLocation("fake", 30, 0));
		AnalyzedCFG<TestAbstractState> res2 = new AnalyzedCFG<>(
				cfg2,
				new UniqueScope<>(),
				singleton,
				Map.of(noop, singleton.bottom()),
				Map.of(noop, singleton.bottom()));

		CheckToolWithAnalysisResults<TestAbstractState,
				TestAbstractDomain> tool = new CheckToolWithAnalysisResults<>(
						new LiSAConfiguration(),
						new FileManager("foo"),
						Map.of(cfg, Collections.singleton(res1), cfg2, Collections.singleton(res2)),
						fakeCallGraph,
						new Analysis<>(new TestAbstractDomain()));

		assertEquals(res1, tool.getResultOf(cfg).iterator().next());
		assertEquals(res2, tool.getResultOf(cfg2).iterator().next());
	}

}
