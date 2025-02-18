package it.unive.lisa.cron;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.dataflow.AvailableExpressions;
import it.unive.lisa.analysis.dataflow.ConstantPropagation;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.analysis.dataflow.Liveness;
import it.unive.lisa.analysis.dataflow.PossibleDataflowDomain;
import it.unive.lisa.analysis.dataflow.ReachingDefinitions;
import it.unive.lisa.imp.IMPFeatures;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.interprocedural.BackwardModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.global.AccessGlobal;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import java.util.Arrays;
import org.junit.Test;

public class DataflowAnalysesTest extends AnalysisTestExecutor {

	@Test
	public void testAvailableExpressions() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new DefiniteDataflowDomain<>(new AvailableExpressions()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "dataflow/ae";
		conf.programFile = "ae.imp";
		perform(conf);
	}

	@Test
	public void testConstantPropagation() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new DefiniteDataflowDomain<>(new ConstantPropagation()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "dataflow/cp";
		conf.programFile = "cp.imp";
		perform(conf);
	}

	@Test
	public void testReachingDefinitions() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new PossibleDataflowDomain<>(new ReachingDefinitions()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "dataflow/rd";
		conf.programFile = "rd.imp";
		perform(conf);
	}

	@Test
	public void testLiveness() {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.interproceduralAnalysis = new BackwardModularWorstCaseAnalysis<>();
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new PossibleDataflowDomain<>(new Liveness()),
				DefaultConfiguration.defaultTypeDomain());
		conf.testDir = "dataflow/liveness";
		conf.programFile = "liveness.imp";
		conf.compareWithOptimization = false;
		perform(conf);
	}

	@Test
	public void testIssue322() {
		Program program = buildProgram();

		CronConfiguration conf = new CronConfiguration();
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new PossibleDataflowDomain<>(new ReachingDefinitions()),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.optimize = false;
		conf.serializeResults = true;
		conf.testDir = "dataflow/issue322";

		perform(conf, program);
	}

	private int line = 1;

	private Program buildProgram() {
		Program program = new Program(new IMPFeatures(), new IMPTypeSystem());
		Global g = new Global(getNewLocation(), program, "g", false);
		program.addGlobal(g);
		String calleeName = "foo";

		// Build main CFG
		var cfg1 = new CFG(new CodeMemberDescriptor(getNewLocation(), program, false, "main"));
		Statement s1 = buildAssign(cfg1, buildAccess(cfg1, g), buildInt32Literal(cfg1, 1));
		Statement s3 = buildUnresolvedCall(cfg1, calleeName);
		Statement s5 = buildAssign(cfg1, buildAccess(cfg1, g), buildInt32Literal(cfg1, 3));
		Statement s7 = buildRet(cfg1);
		addAsSequence(cfg1, s1, s3, s5, s7);

		// Build callee CFG
		var cfg2 = new CFG(new CodeMemberDescriptor(getNewLocation(), program, false, calleeName));
		Statement s8 = buildAssign(cfg2, buildAccess(cfg2, g), buildInt32Literal(cfg2, 2));
		Statement s9 = buildRet(cfg2);
		addAsSequence(cfg2, s8, s9);

		program.addCodeMember(cfg1);
		program.addCodeMember(cfg2);
		program.addEntryPoint(cfg1);

		return program;
	}

	private void addAsSequence(
			CFG cfg,
			Statement... statements) {
		Statement prev = null;
		for (Statement s : Arrays.asList(statements)) {
			cfg.addNode(s, prev == null);
			if (prev != null) {
				cfg.addEdge(new SequentialEdge(prev, s));
			}
			prev = s;
		}
	}

	private Statement buildAssign(
			CFG cfg,
			Expression target,
			Expression source) {
		return new Assignment(cfg, getNewLocation(), target, source);
	}

	private Statement buildUnresolvedCall(
			CFG cfg,
			String targetName) {
		return new UnresolvedCall(cfg, getNewLocation(), CallType.STATIC, (String) null, targetName);
	}

	private Statement buildRet(
			CFG cfg) {
		return new Ret(cfg, getNewLocation());
	}

	private Expression buildInt32Literal(
			CFG cfg,
			int value) {
		return new Int32Literal(cfg, getNewLocation(), value);
	}

	private Expression buildAccess(
			CFG cfg,
			Global g) {
		return new AccessGlobal(cfg, getNewLocation(), g.getContainer(), g);
	}

	private CodeLocation getNewLocation() {
		return new SourceCodeLocation("program", line++, 1);
	}
}