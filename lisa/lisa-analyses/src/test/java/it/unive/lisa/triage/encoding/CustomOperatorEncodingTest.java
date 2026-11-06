package it.unive.lisa.triage.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.IndividualWarningVerifier;
import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.triage.FirstViolationVerifier;
import it.unive.lisa.triage.bmc.BoundedModelChecker;
import it.unive.lisa.triage.pdr.PdrVerifier;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for the encoding of operators that are not defined by LiSA (e.g., by a
 * frontend), through a subclass of {@link ExpressionEncoder} passed to the
 * verifiers with an {@link ExpressionEncoderFactory}. Tests are skipped if z3
 * is not available.
 */
public class CustomOperatorEncodingTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("test", 0, 0);

	/**
	 * An operator that LiSA does not know, yielding the maximum of two numbers
	 * (as, e.g., {@code Math.max} in Java).
	 */
	public static class TestMax
			implements
			BinaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final TestMax INSTANCE = new TestMax();

		private TestMax() {
		}

		@Override
		public String toString() {
			return "testmax";
		}

		@Override
		public Set<Type> typeInference(
				TypeSystem types,
				Set<Type> left,
				Set<Type> right) {
			return Set.of(Int32Type.INSTANCE);
		}
	}

	/**
	 * An encoder that also knows {@link TestMax}, as a frontend would define.
	 */
	public static class TestEncoder
			extends
			ExpressionEncoder {

		/**
		 * Builds the encoder.
		 *
		 * @param sorts   the policy deciding the sorts
		 * @param symbols the provider of the constants
		 */
		public TestEncoder(
				SortPolicy sorts,
				SymbolTable symbols) {
			super(sorts, symbols);
		}

		@Override
		protected Term encodeBinary(
				BinaryExpression expression,
				Term left,
				Term right)
				throws SemanticException {
			if (expression.getOperator() instanceof TestMax)
				// exact over integers, unknown function over other values
				return arithmetic(expression, List.of(left, right),
						args -> Term.ite(Term.app(">=", Sort.BOOL, args.get(0), args.get(1)), args.get(0),
								args.get(1)));
			return super.encodeBinary(expression, left, right);
		}

		@Override
		protected Term encodeUnary(
				UnaryExpression expression,
				Term arg)
				throws SemanticException {
			if (expression.getOperator() instanceof TestIsDigit) {
				// encoded only over ASCII characters
				Term c = coerce(arg, Sort.INT, expression);
				Term ascii = Term.and(Term.app(">=", Sort.BOOL, c, Term.integer(0)),
						Term.app("<", Sort.BOOL, c, Term.integer(128)));
				Term digit = Term.and(Term.app(">=", Sort.BOOL, c, Term.integer('0')),
						Term.app("<=", Sort.BOOL, c, Term.integer('9')));
				return guarded(ascii, digit, List.of(c), expression);
			}
			return super.encodeUnary(expression, arg);
		}
	}

	/**
	 * An operator that LiSA does not know, testing whether a character is a
	 * digit (as, e.g., {@code Character.isDigit} in Java).
	 */
	public static class TestIsDigit
			implements
			UnaryOperator {

		/**
		 * The singleton instance of this class.
		 */
		public static final TestIsDigit INSTANCE = new TestIsDigit();

		private TestIsDigit() {
		}

		@Override
		public String toString() {
			return "testisdigit";
		}

		@Override
		public Set<Type> typeInference(
				TypeSystem types,
				Set<Type> argument) {
			return Set.of(BoolType.INSTANCE);
		}
	}

	// encoder-level tests

	private static final Variable X = new Variable(Int32Type.INSTANCE, "x", LOC);

	private static final Variable V = new Variable(Untyped.INSTANCE, "v", LOC);

	private SmtSolver solver;

	private int fresh;

	/**
	 * A symbol table declaring constants on the fly, and counting the fresh
	 * ones.
	 */
	private class Symbols
			implements
			SymbolTable {

		private final Map<Identifier, Term> constants = new HashMap<>();

		@Override
		public Term constant(
				Identifier id,
				Sort sort) {
			return constants.computeIfAbsent(id, i -> solver.declare(i.getName(), sort));
		}

		@Override
		public Term fresh(
				Sort sort,
				SymbolicExpression origin) {
			return solver.declare("fresh" + (fresh++), sort);
		}
	}

	private ExpressionEncoder encoder(
			ExpressionEncoderFactory factory) {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		if (solver == null) {
			solver = SmtLibProcessSolver.z3();
			solver.declareDatatype(ValueSort.DATATYPE);
		}
		Map<Identifier, Set<Type>> types = new HashMap<>();
		types.put(X, Set.of(Int32Type.INSTANCE));
		types.put(V, Set.of(Int32Type.INSTANCE, StringType.INSTANCE));
		return factory.create(new SortPolicy(types), new Symbols());
	}

	@AfterEach
	public void teardown() {
		if (solver != null)
			solver.close();
		solver = null;
	}

	private static Constant integer(
			int value) {
		return new Constant(Int32Type.INSTANCE, value, LOC);
	}

	private static BinaryExpression max(
			SymbolicExpression left,
			SymbolicExpression right) {
		return new BinaryExpression(Int32Type.INSTANCE, left, right, TestMax.INSTANCE, LOC);
	}

	private static BinaryExpression eq(
			SymbolicExpression left,
			SymbolicExpression right) {
		return new BinaryExpression(BoolType.INSTANCE, left, right, ComparisonEq.INSTANCE, LOC);
	}

	private SmtResult check(
			Term term) {
		solver.push();
		solver.assertTerm(term);
		SmtResult res = solver.check();
		solver.pop();
		return res;
	}

	@Test
	public void defaultEncoderHavocsUnknownOperators() throws SemanticException {
		ExpressionEncoder encoder = encoder(ExpressionEncoderFactory.DEFAULT);
		// max(x, 7) is unconstrained: it might be smaller than 7
		Term t = encoder.encodeCondition(eq(max(X, integer(7)), integer(3)));
		assertTrue(fresh > 0, "No fresh value for an unknown operator");
		assertEquals(SmtResult.SAT, check(t));
	}

	@Test
	public void customEncoderIsExact() throws SemanticException {
		ExpressionEncoder encoder = encoder(TestEncoder::new);
		Term t = encoder.encodeCondition(eq(max(X, integer(7)), integer(3)));
		// no fresh value is used: the encoding is exact
		assertEquals(0, fresh);
		assertEquals(SmtResult.UNSAT, check(t));
		assertEquals(SmtResult.UNSAT,
				check(Term.not(encoder.encodeCondition(eq(max(integer(9), integer(7)), integer(9))))));
		// values are encoded exactly if they are integers
		Term v = encoder.encodeCondition(eq(V, integer(9)));
		assertEquals(SmtResult.UNSAT,
				check(Term.and(v, Term.not(encoder.encodeCondition(eq(max(V, integer(7)), integer(9)))))));
	}

	@Test
	public void partialEncodingsAreGuarded() throws SemanticException {
		ExpressionEncoder encoder = encoder(TestEncoder::new);
		// precise where the encoding is defined
		assertEquals(SmtResult.UNSAT, check(Term.not(encoder.encodeCondition(isDigit(integer('7'))))));
		assertEquals(SmtResult.UNSAT, check(encoder.encodeCondition(isDigit(integer('a')))));
		// unknown elsewhere
		assertEquals(SmtResult.SAT, check(encoder.encodeCondition(isDigit(integer(200)))));
		assertEquals(SmtResult.SAT, check(Term.not(encoder.encodeCondition(isDigit(integer(200))))));
	}

	private static UnaryExpression isDigit(
			SymbolicExpression arg) {
		return new UnaryExpression(BoolType.INSTANCE, arg, TestIsDigit.INSTANCE, LOC);
	}

	@Test
	public void customEncoderDelegatesOtherOperators() throws SemanticException {
		ExpressionEncoder encoder = encoder(TestEncoder::new);
		// operators known by LiSA are still encoded by the default encoder
		Term t = encoder.encodeCondition(eq(X, integer(7)));
		assertEquals(0, fresh);
		assertEquals(SmtResult.SAT, check(t));
		// other unknown operators are still unconstrained
		BinaryOperator unknown = new BinaryOperator() {
			@Override
			public Set<Type> typeInference(
					TypeSystem types,
					Set<Type> left,
					Set<Type> right) {
				return Set.of(Int32Type.INSTANCE);
			}
		};
		encoder.encode(new BinaryExpression(Int32Type.INSTANCE, X, X, unknown, LOC));
		assertTrue(fresh > 0, "No fresh value for an unknown operator");
	}

	// verifier-level tests

	/**
	 * {@code max} is a native method added to the class after parsing,
	 * implemented through {@link TestMax}.
	 */
	private static final String PROGRAM = "class test {\n"
			+ "  main(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def m = this.max(x, 7);\n"
			+ "      if (m == 9)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "}";

	private static CFG main;

	private static SemanticTool<?, ?> tool;

	/**
	 * The construct implementing the native {@code max} method.
	 */
	public static class Max
			extends
			TernaryExpression
			implements
			PluggableStatement {

		private Statement originating;

		/**
		 * Builds the construct.
		 *
		 * @param cfg      the cfg
		 * @param location the location
		 * @param params   the receiver and the two operands
		 *
		 * @return the construct
		 */
		public static Max build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new Max(cfg, location, params[0], params[1], params[2]);
		}

		/**
		 * Builds the construct.
		 *
		 * @param cfg      the cfg
		 * @param location the location
		 * @param receiver the receiver
		 * @param left     the first operand
		 * @param right    the second operand
		 */
		public Max(
				CFG cfg,
				CodeLocation location,
				Expression receiver,
				Expression left,
				Expression right) {
			super(cfg, location, "max", Int32Type.INSTANCE, receiver, left, right);
		}

		@Override
		public void setOriginatingStatement(
				Statement st) {
			originating = st;
		}

		@Override
		protected int compareSameClassAndParams(
				Statement o) {
			// no additional fields
			return 0;
		}

		@Override
		public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdTernarySemantics(
				InterproceduralAnalysis<A, D> interprocedural,
				AnalysisState<A> state,
				SymbolicExpression receiver,
				SymbolicExpression left,
				SymbolicExpression right,
				StatementStore<A> expressions)
				throws SemanticException {
			return interprocedural.getAnalysis().smallStepSemantics(
					state,
					new BinaryExpression(Int32Type.INSTANCE, left, right, TestMax.INSTANCE, getLocation()),
					interprocedural.getCurrentContext(),
					originating == null ? this : originating);
		}
	}

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			CustomOperatorEncodingTest.tool = tool;
		}
	}

	@BeforeAll
	public static void analyze() throws Exception {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		Program program = IMPFrontend.processText(PROGRAM);
		main = program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("main")).findFirst()
				.get();
		if (!program.getEntryPoints().contains(main))
			program.addEntryPoint(main);

		CompilationUnit unit = (CompilationUnit) main.getDescriptor().getUnit();
		SourceCodeLocation runtime = new SourceCodeLocation("runtime", 0, 0);
		NativeCFG max = new NativeCFG(
				new CodeMemberDescriptor(runtime, unit, true, "max", Int32Type.INSTANCE,
						new Parameter(runtime, "this", main.getDescriptor().getFormals()[0].getStaticType()),
						new Parameter(runtime, "a", Untyped.INSTANCE),
						new Parameter(runtime, "b", Untyped.INSTANCE)),
				Max.class);
		unit.addInstanceCodeMember(max);

		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		// calls are resolved only by analyses relying on the call graph
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.workdir = Files.createTempDirectory("custom-operators").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static VerificationResult verify(
			IndividualWarningVerifier verifier)
			throws SemanticException {
		verifier.init(tool);
		Statement st = main.getNodes().stream().filter(n -> n.toString().contains("w = 1")).findFirst().get();
		return verifier.verify(tool, new StatementMessage(st, "alarm", null, Violation.reach()));
	}

	private static String valueOf(
			VerificationResult result,
			String source) {
		for (ReproducingInput input : result.getReproducingInputs())
			if (input.getSource().startsWith(source))
				return input.getValue();
		throw new AssertionError("No input from " + source + " in " + result.getReproducingInputs());
	}

	@Test
	public void pdrWithDefaultEncoderIsNotConfirmed() throws SemanticException {
		VerificationResult result = verify(new PdrVerifier<>());
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
		assertTrue(result.getExplanation().contains("TestMax"), result.toString());
	}

	@Test
	public void pdrWithCustomEncoderIsConfirmed() throws SemanticException {
		VerificationResult result = verify(new PdrVerifier<>().withEncoder(TestEncoder::new));
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("9", valueOf(result, "parameter x of"));
	}

	@Test
	public void bmcWithDefaultEncoderIsNotConfirmed() throws SemanticException {
		VerificationResult result = verify(new BoundedModelChecker<>());
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
	}

	@Test
	public void bmcWithCustomEncoderIsConfirmed() throws SemanticException {
		VerificationResult result = verify(new BoundedModelChecker<>().withEncoder(TestEncoder::new));
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("9", valueOf(result, "parameter x of"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void firstViolationWithCustomEncoderIsConfirmed() throws SemanticException {
		Statement st = main.getNodes().stream().filter(n -> n.toString().contains("w = 1")).findFirst().get();
		StatementMessage warning = new StatementMessage(st, "alarm", null, Violation.reach());
		Map<StatementMessage, VerificationResult> results = new FirstViolationVerifier(
				(ExpressionEncoderFactory) TestEncoder::new).verify((SemanticTool) tool, List.of(warning));
		assertEquals(VerificationOutcome.CONFIRMED, results.get(warning).getOutcome(), results.toString());
	}
}
