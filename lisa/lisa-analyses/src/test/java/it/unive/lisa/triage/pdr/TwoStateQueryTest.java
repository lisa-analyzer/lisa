package it.unive.lisa.triage.pdr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.AlarmModelBuilder;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.SymbolicStore;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Term;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for the two-state queries that property-directed reachability issues on
 * single transitions of an {@link AlarmModel}, encoded through
 * {@link Unrolling} with {@link Cube}s and {@link Lemma}s. Tests are skipped if
 * z3 is not available.
 */
public class TwoStateQueryTest {

	private static final String PROGRAM = "class test {\n"
			+ "  shallow(x) {\n"
			+ "    def y = 2;\n"
			+ "    if (x is int)\n"
			+ "      if (x == 42)\n"
			+ "        y = 1;\n"
			+ "    return y;\n"
			+ "  }\n"
			+ "}";

	private static Program program;

	private static SemanticTool<?, ?> tool;

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			TwoStateQueryTest.tool = tool;
		}

	}

	@BeforeAll
	public static void analyze() throws Exception {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		program = IMPFrontend.processText(PROGRAM);
		for (CFG cfg : program.getAllCFGs())
			if (!program.getEntryPoints().contains(cfg))
				program.addEntryPoint(cfg);
		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.workdir = Files.createTempDirectory("pdr").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static AlarmModel model() throws SemanticException {
		CFG cfg = program.getAllCFGs().iterator().next();
		Statement ret = cfg.getNodes().stream().filter(n -> n.toString().contains("return")).findFirst().get();
		SemanticTool t = tool;
		AnalyzedCFG result = (AnalyzedCFG) t.getResultOf(cfg).iterator().next();
		return AlarmModelBuilder.build(t, result, ret, Violation.reach(), SymbolicStore.DEFAULT_MAX_CASES);
	}

	private static Identifier find(
			AlarmModel model,
			String name) {
		return model.getVocabulary().stream().filter(i -> i.getName().equals(name)).findFirst().get();
	}

	// the true edge of the guard x == 42
	private static EdgeTransition trueEdge(
			AlarmModel model) {
		for (Statement loc : model.getLocations())
			for (EdgeTransition e : model.getTransitions(loc))
				if (e.getEdge() instanceof TrueEdge && e.getEdge().getSource().toString().contains("42"))
					return e;
		throw new AssertionError("No true edge");
	}

	// is the cube reachable at the destination of the edge, in one step from
	// its source, given the lemma at the source?
	private static SmtResult query(
			Unrolling u,
			SmtSolver s,
			EdgeTransition edge,
			Lemma lemma,
			Cube cube)
			throws SemanticException {
		Statement src = edge.getEdge().getSource();
		List<Term> query = List.of(u.at(0, src), u.seeds(src, 0),
				lemma == null ? Term.TRUE : lemma.at(u, 0), u.transition(0, edge), cube.at(u, 1, "#cube"));
		s.push();
		for (Term t : query)
			s.assertTerm(t);
		return s.check();
	}

	private static Cube is(
			Identifier id,
			int value) {
		return new Cube(List.of(new BinaryExpression(BoolType.INSTANCE, id,
				new Constant(Int32Type.INSTANCE, value, SyntheticLocation.INSTANCE), ComparisonEq.INSTANCE,
				SyntheticLocation.INSTANCE)));
	}

	@Test
	public void singleEdgeQueries() throws SemanticException {
		AlarmModel model = model();
		Identifier x = find(model, "x");
		EdgeTransition edge = trueEdge(model);
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			Unrolling u = new Unrolling(model, s);

			// the guard prevents x = 41 after the true edge
			assertEquals(SmtResult.UNSAT, query(u, s, edge, null, is(x, 41)));
			s.pop();

			// x = 42 is possible
			assertEquals(SmtResult.SAT, query(u, s, edge, null, is(x, 42)));
			s.pop();

			// blocking x = 42 at the source blocks the successor too
			assertEquals(SmtResult.UNSAT, query(u, s, edge, is(x, 42).negate(), is(x, 42)));
			s.pop();

			// dropping the literal yields the empty cube, that is reachable
			// through the edge unless the lemma holds at the source
			Cube empty = is(x, 42).without(is(x, 42).getLiterals().iterator().next());
			assertEquals(new Cube(List.of()), empty);
			assertEquals(SmtResult.SAT, query(u, s, edge, null, empty));
			s.pop();
			assertEquals(SmtResult.UNSAT, query(u, s, edge, is(x, 42).negate(), empty));
			s.pop();
		}
	}
}
