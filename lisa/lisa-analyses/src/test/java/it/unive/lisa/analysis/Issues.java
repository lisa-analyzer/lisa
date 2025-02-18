package it.unive.lisa.analysis;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.dataflow.PossibleDataflowDomain;
import it.unive.lisa.analysis.dataflow.ReachingDefinitions;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
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
import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.program.language.hierarchytraversal.HierarcyTraversalStrategy;
import it.unive.lisa.program.language.hierarchytraversal.SingleInheritanceTraversalStrategy;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.program.language.parameterassignment.PythonLikeAssigningStrategy;
import it.unive.lisa.program.language.resolution.JavaLikeMatchingStrategy;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.language.validation.BaseValidationLogic;
import it.unive.lisa.program.language.validation.ProgramValidationLogic;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Arrays;
import org.junit.Test;

public class Issues {

	@Test
	public void issue322() {
		var program = buildProgram();

		LiSAConfiguration conf = new DefaultConfiguration();
		var valueDomain = new PossibleDataflowDomain<>(new ReachingDefinitions());
		var heapDomain = DefaultConfiguration.defaultHeapDomain();
		var typeDomain = DefaultConfiguration.defaultTypeDomain();
		conf.abstractState = DefaultConfiguration.simpleState(heapDomain, valueDomain, typeDomain);
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		conf.optimize = false; // Simplify collecting results
		conf.analysisGraphs = GraphType.HTML;
		conf.serializeInputs = false;
		conf.serializeResults = true;
		conf.jsonOutput = true;
		conf.workdir = AnalysisTestExecutor.ACTUAL_RESULTS_DIR + "/issue322";

		LiSA lisa = new LiSA(conf);
		lisa.run(program);

		CFG main = program.getEntryPoints().iterator().next();
		for (AnalyzedCFG<?> result : conf.interproceduralAnalysis.getAnalysisResultsOf(main)) {
			for (Statement node : main.getNodes()) {
				AnalysisState<?> analysisState = result.getAnalysisStateAfter(node);
				SimpleAbstractState<?, ?, ?> state = (SimpleAbstractState<?, ?, ?>) analysisState.getState();
				PossibleDataflowDomain<?> valueState = (PossibleDataflowDomain<?>) state.getValueState();
				System.out.println(String.format("After %s, a reaching definition is %s", node, valueState));
			}
		}
	}

	private int line = 1;

	private Program buildProgram() {
		Program program = new Program(new ExampleFeatures(), new ExampleTypeSystem());
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

	/** Adapted from IMPFeatures */
	private static class ExampleFeatures extends LanguageFeatures {

		@Override
		public ParameterMatchingStrategy getMatchingStrategy() {
			return JavaLikeMatchingStrategy.INSTANCE;
		}

		@Override
		public HierarcyTraversalStrategy getTraversalStrategy() {
			return SingleInheritanceTraversalStrategy.INSTANCE;
		}

		@Override
		public ParameterAssigningStrategy getAssigningStrategy() {
			return PythonLikeAssigningStrategy.INSTANCE;
		}

		@Override
		public ProgramValidationLogic getProgramValidationLogic() {
			return new BaseValidationLogic();
		}
	}

	/** Adapted from IMPTypeSystem */
	private static class ExampleTypeSystem extends TypeSystem {

		@Override
		public BooleanType getBooleanType() {
			return BoolType.INSTANCE;
		}

		@Override
		public StringType getStringType() {
			return StringType.INSTANCE;
		}

		@Override
		public NumericType getIntegerType() {
			return Int32Type.INSTANCE;
		}

		@Override
		public boolean canBeReferenced(
				Type type) {
			return type.isInMemoryType();
		}
	}
}
