package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.triage.encoding.ExpressionEncoder;
import it.unive.lisa.triage.encoding.SortPolicy;
import it.unive.lisa.triage.encoding.SymbolTable;
import it.unive.lisa.triage.encoding.ValueSort;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Untyped;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TransitionExtractor}: the transitions extracted from the
 * results of an analysis of an IMP program are checked semantically with z3
 * (tests are skipped if z3 is not available).
 */
public class TransitionExtractorTest {

	private static final String PROGRAM = "class A { ~A() {} }\n"
			+ "class test {\n"
			+ "  main(x, s) {\n"
			+ "    def y = x + 1;\n"
			+ "    if (y > 10)\n"
			+ "      y = 0;\n"
			+ "    else\n"
			+ "      y = y * 2;\n"
			+ "    def t = s + \"a\";\n"
			+ "    return y;\n"
			+ "  }\n"
			+ "  loop(n) {\n"
			+ "    def i = 0;\n"
			+ "    while (i < n)\n"
			+ "      i = i + 1;\n"
			+ "    return i;\n"
			+ "  }\n"
			+ "  thrower(x) {\n"
			+ "    def r = 0;\n"
			+ "    try {\n"
			+ "      throw new A();\n"
			+ "    } catch (A e) {\n"
			+ "      r = 2;\n"
			+ "    }\n"
			+ "    return r;\n"
			+ "  }\n"
			+ "}";

	private static Program program;

	private static SemanticTool<?, ?> tool;

	private SmtSolver solver;

	private ExpressionEncoder encoder;

	private int fresh;

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			TransitionExtractorTest.tool = tool;
		}

	}

	@BeforeAll
	public static void analyze() throws Exception {
		program = IMPFrontend.processText(PROGRAM);
		for (CFG cfg : program.getAllCFGs())
			if (!program.getEntryPoints().contains(cfg))
				program.addEntryPoint(cfg);
		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.workdir = Files.createTempDirectory("transitions").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
	}

	@BeforeEach
	public void setup() {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		solver = SmtLibProcessSolver.z3();
		solver.declareDatatype(ValueSort.DATATYPE);
		Map<String, Term> constants = new HashMap<>();
		encoder = new ExpressionEncoder(new SortPolicy(Map.of()), new SymbolTable() {

			@Override
			public Term constant(
					Identifier id,
					Sort sort) {
				return constants.computeIfAbsent(id.getName(), n -> solver.declare(n, sort));
			}

			@Override
			public Term fresh(
					Sort sort,
					SymbolicExpression origin) {
				return solver.declare("fresh" + (fresh++), sort);
			}
		});
	}

	@AfterEach
	public void teardown() {
		if (solver != null)
			solver.close();
	}

	private static CFG cfg(
			String name) {
		return program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst()
				.get();
	}

	private static Statement node(
			CFG cfg,
			String text) {
		return cfg.getNodes().stream().filter(n -> n.toString().contains(text)).findFirst()
				.orElseThrow(() -> new AssertionError("No node containing '" + text + "' in " + cfg.getNodes()));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static NodeTransitions extract(
			CFG cfg,
			Statement node)
			throws SemanticException {
		SemanticTool t = tool;
		TransitionExtractor extractor = new TransitionExtractor(
				t,
				(AnalyzedCFG) t.getResultOf(cfg).iterator().next(),
				new FreshValueFactory(new InputSourceResolver(t)),
				SymbolicStore.DEFAULT_MAX_CASES);
		return extractor.extract(node);
	}

	private static Variable post(
			String name) {
		return new Variable(Untyped.INSTANCE, name + "'", SyntheticLocation.INSTANCE);
	}

	private static Variable pre(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	/**
	 * Encodes that the given variable of the pre-state is an integer: untyped
	 * IMP values might also be, e.g., floating point numbers, whose
	 * comparisons are not modeled.
	 */
	private Term isInteger(
			String name)
			throws SemanticException {
		Term t = encoder.encode(pre(name));
		return t.getSort().equals(ValueSort.SORT) ? ValueSort.is(t, Sort.INT) : Term.TRUE;
	}

	private static BinaryExpression eq(
			SymbolicExpression l,
			SymbolicExpression r) {
		return new BinaryExpression(BoolType.INSTANCE, l, r, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
	}

	private static Constant integer(
			int value) {
		return new Constant(Int32Type.INSTANCE, value, SyntheticLocation.INSTANCE);
	}

	private static Constant string(
			String value) {
		return new Constant(StringType.INSTANCE, value, SyntheticLocation.INSTANCE);
	}

	/**
	 * Encodes the given transition as a formula over the pre-state (plain
	 * names) and the post-state (primed names) of the given identifiers.
	 */
	private Term transition(
			SymbolicStore store,
			String... vocabulary)
			throws SemanticException {
		List<Term> cases = new ArrayList<>();
		for (SymbolicCase c : store.getCases()) {
			List<Term> conjuncts = new ArrayList<>();
			for (SymbolicExpression cond : c.getPathCondition())
				conjuncts.add(encoder.encodeCondition(cond));
			for (String name : vocabulary) {
				SymbolicExpression value = null;
				for (Map.Entry<Identifier, SymbolicExpression> e : c.getUpdates().entrySet())
					if (e.getKey().getName().equals(name))
						value = e.getValue();
				// updates are assignments, encoded as in the unrolling (not as
				// program equalities, that might not be reflexive)
				Term post = encoder.encode(post(name));
				if (value != null)
					conjuncts.add(Term.eq(post, encoder.encodeAs(value, post.getSort())));
				else if (!c.isUnconstrained(pre(name)))
					conjuncts.add(Term.eq(post, encoder.encodeAs(pre(name), post.getSort())));
			}
			cases.add(Term.and(conjuncts.toArray(Term[]::new)));
		}
		return Term.or(cases.toArray(Term[]::new));
	}

	private SmtResult check(
			Term... terms) {
		solver.push();
		for (Term t : terms)
			solver.assertTerm(t);
		SmtResult res = solver.check();
		solver.pop();
		return res;
	}

	private static EdgeTransition single(
			NodeTransitions transitions) {
		assertEquals(1, transitions.getOutgoing().size(), "Expected one outgoing transition: " + transitions);
		return transitions.getOutgoing().get(0);
	}

	@Test
	public void assignment() throws SemanticException {
		CFG main = cfg("main");
		NodeTransitions tr = extract(main, node(main, "+(x, 1)"));
		Term t = transition(single(tr).getStore(), "x", "y");
		// if x is 5, y becomes 6: it is surely not 7 (even if string
		// concatenation is also considered)
		assertEquals(SmtResult.SAT, check(t, isInteger("x"), encoder.encodeCondition(eq(pre("x"), integer(5))),
				encoder.encodeCondition(eq(post("y"), integer(6)))));
		assertEquals(SmtResult.UNSAT, check(t, isInteger("x"), encoder.encodeCondition(eq(pre("x"), integer(5))),
				encoder.encodeCondition(eq(post("y"), integer(7)))));
		// x is not modified
		assertEquals(SmtResult.UNSAT,
				check(t, isInteger("x"), Term.not(encoder.encodeCondition(eq(post("x"), pre("x"))))));
		// the state after the sub-expression is available
		Statement sum = ((NaryExpression) node(main, "+(x, 1)")).getSubExpressions()[1];
		assertNotNull(tr.getPartial(sum));
		assertNotNull(tr.getPartial(tr.getNode()));
		assertNull(tr.getExit());
	}

	@Test
	public void conditionalEdges() throws SemanticException {
		CFG main = cfg("main");
		NodeTransitions tr = extract(main, node(main, ">(y, 10)"));
		assertEquals(2, tr.getOutgoing().size());
		for (EdgeTransition e : tr.getOutgoing()) {
			Term t = transition(e.getStore(), "y");
			Term small = encoder.encodeCondition(eq(pre("y"), integer(3)));
			Term big = encoder.encodeCondition(eq(pre("y"), integer(11)));
			Term integer = isInteger("y");
			if (e.getEdge() instanceof TrueEdge) {
				assertEquals(SmtResult.UNSAT, check(t, integer, small));
				assertEquals(SmtResult.SAT, check(t, integer, big));
			} else {
				assertTrue(e.getEdge() instanceof FalseEdge);
				assertEquals(SmtResult.SAT, check(t, integer, small));
				assertEquals(SmtResult.UNSAT, check(t, integer, big));
			}
		}
	}

	@Test
	public void concatenation() throws SemanticException {
		CFG main = cfg("main");
		NodeTransitions tr = extract(main, node(main, "+(s, \"a\")"));
		Term t = transition(single(tr).getStore(), "s", "t");
		assertEquals(SmtResult.SAT, check(t, encoder.encodeCondition(eq(pre("s"), string("x"))),
				encoder.encodeCondition(eq(post("t"), string("xa")))));
	}

	@Test
	public void exit() throws SemanticException {
		CFG main = cfg("main");
		NodeTransitions tr = extract(main, node(main, "return"));
		assertNotNull(tr.getExit());
		assertTrue(tr.getOutgoing().isEmpty());
	}

	@Test
	public void loop() throws SemanticException {
		CFG loop = cfg("loop");
		NodeTransitions guard = extract(loop, node(loop, "<(i, n)"));
		assertEquals(2, guard.getOutgoing().size());
		NodeTransitions body = extract(loop, node(loop, "+(i, 1)"));
		Term t = transition(single(body).getStore(), "i", "n");
		// i' = i + 1 for integers
		assertEquals(SmtResult.UNSAT, check(t, isInteger("i"), encoder.encodeCondition(eq(pre("i"), integer(4))),
				Term.not(encoder.encodeCondition(eq(post("i"), integer(5))))));
	}

	@Test
	public void caughtErrors() throws SemanticException {
		CFG thrower = cfg("thrower");
		NodeTransitions tr = extract(thrower, node(thrower, "throw"));
		boolean caught = false;
		for (EdgeTransition e : tr.getOutgoing())
			if (e.getEdge() instanceof ErrorEdge)
				caught = true;
		assertTrue(caught, "No transition to the catch block: " + tr);
		// the error is raised by the node, even if it is caught afterwards
		assertEquals(1, tr.getErrors().size(), "Expected one raised error: " + tr.getErrors());
		assertEquals("A", tr.getErrors().keySet().iterator().next().getType().toString());
		// the throw does not terminate normally
		assertTrue(tr.getOutgoing().stream().allMatch(e -> e.getEdge() instanceof ErrorEdge));
	}
}
