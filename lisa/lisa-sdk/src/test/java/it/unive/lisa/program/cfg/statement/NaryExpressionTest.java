package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.emptyState;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.interprocedural;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.store;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.program.cfg.statement.evaluation.RightToLeftEvaluation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class NaryExpressionTest {

	private static class RecordingNary
			extends
			NaryExpression {

		RecordingNary(
				CFG cfg,
				CodeLocation loc,
				Expression... subs) {
			super(cfg, loc, "nary", subs);
		}

		RecordingNary(
				CFG cfg,
				CodeLocation loc,
				it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder order,
				Expression... subs) {
			super(cfg, loc, "nary", order, subs);
		}

		@Override
		protected int compareSameClassAndParams(
				Statement o) {
			return 0;
		}

		@Override
		public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
				InterproceduralAnalysis<A, D> interprocedural,
				AnalysisState<A> state,
				ExpressionSet[] params,
				StatementStore<A> expressions) {
			return state;
		}
	}

	@Test
	public void constructorRejectsNullSubExpressionsArrayOrElements() {
		CFG cfg = newCfg();
		assertThrows(NullPointerException.class, () -> new RecordingNary(cfg, LOC, (Expression[]) null));
		assertThrows(NullPointerException.class,
				() -> new RecordingNary(cfg, LOC, new VariableRef(cfg, LOC, "x"), null));
	}

	@Test
	public void gettersExposeConstructorArguments() {
		CFG cfg = newCfg();
		VariableRef x = new VariableRef(cfg, LOC, "x");
		VariableRef y = new VariableRef(cfg, LOC, "y");
		RecordingNary n = new RecordingNary(cfg, LOC, x, y);
		assertEquals("nary", n.getConstructName());
		assertEquals(2, n.getSubExpressions().length);
		assertSame(x, n.getSubExpressions()[0]);
		assertSame(y, n.getSubExpressions()[1]);
		assertSame(LeftToRightEvaluation.INSTANCE, n.getOrder());
		assertEquals("nary(x, y)", n.toString());
	}

	@Test
	public void constructingAnExpressionSetsItAsTheParentOfItsSubExpressions() {
		CFG cfg = newCfg();
		VariableRef x = new VariableRef(cfg, LOC, "x");
		RecordingNary n = new RecordingNary(cfg, LOC, x);
		assertSame(n, x.getParentStatement());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnConstructNameAndSubExpressions() {
		CFG cfg = newCfg();
		RecordingNary a = new RecordingNary(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		RecordingNary b = new RecordingNary(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		RecordingNary differentSub = new RecordingNary(cfg, LOC, new VariableRef(cfg, LOC, "y"));
		assertFalse(a.equals(differentSub));
	}

	@Test
	public void compareSameClassComparesLengthThenElementwiseBeforeDelegating() {
		CFG cfg = newCfg();
		RecordingNary short_ = new RecordingNary(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		RecordingNary long_ = new RecordingNary(cfg, LOC, new VariableRef(cfg, LOC, "x"),
				new VariableRef(cfg, LOC, "y"));
		assertTrue(short_.compareTo(long_) < 0);

		RecordingNary a = new RecordingNary(cfg, LOC, new VariableRef(cfg, LOC, "a"));
		RecordingNary z = new RecordingNary(cfg, LOC, new VariableRef(cfg, LOC, "z"));
		assertTrue(a.compareTo(z) < 0);
	}

	@Test
	public void acceptVisitsSubNodesBeforeOrAfterTheNodeDependingOnTheVisitor() {
		CFG cfg = newCfg();
		VariableRef x = new VariableRef(cfg, LOC, "x");
		VariableRef y = new VariableRef(cfg, LOC, "y");
		RecordingNary n = new RecordingNary(cfg, LOC, x, y);

		List<Statement> subFirst = new ArrayList<>();
		n.accept(new GraphVisitor<CFG, Statement, Edge, List<Statement>>() {
			@Override
			public boolean visitSubNodesFirst() {
				return true;
			}

			@Override
			public boolean visit(
					List<Statement> tool,
					CFG graph,
					Statement node) {
				tool.add(node);
				return true;
			}
		}, subFirst);
		assertEquals(List.of(x, y, n), subFirst);

		List<Statement> nodeFirst = new ArrayList<>();
		n.accept(new GraphVisitor<CFG, Statement, Edge, List<Statement>>() {
			@Override
			public boolean visitSubNodesFirst() {
				return false;
			}

			@Override
			public boolean visit(
					List<Statement> tool,
					CFG graph,
					Statement node) {
				tool.add(node);
				return true;
			}
		}, nodeFirst);
		assertEquals(List.of(n, x, y), nodeFirst);
	}

	@Test
	public void getStatementEvaluatedBeforeAndAfterFollowTheEvaluationOrderAtTheBoundaries() {
		CFG cfg = newCfg();
		VariableRef first = new VariableRef(cfg, LOC, "a");
		VariableRef middle = new VariableRef(cfg, LOC, "b");
		VariableRef last = new VariableRef(cfg, LOC, "c");
		RecordingNary n = new RecordingNary(cfg, LOC, LeftToRightEvaluation.INSTANCE, first, middle, last);

		// querying the expression itself: before -> last evaluated sub, after
		// -> null
		assertSame(last, n.getStatementEvaluatedBefore(n));
		assertNull(n.getStatementEvaluatedAfter(n));

		// first sub: nothing evaluated before it, the middle one is evaluated
		// after
		assertNull(n.getStatementEvaluatedBefore(first));
		assertSame(middle, n.getStatementEvaluatedAfter(first));

		// middle sub: first is before, last is after
		assertSame(first, n.getStatementEvaluatedBefore(middle));
		assertSame(last, n.getStatementEvaluatedAfter(middle));

		// last sub: middle is before, the expression itself is evaluated after
		assertSame(middle, n.getStatementEvaluatedBefore(last));
		assertSame(n, n.getStatementEvaluatedAfter(last));

		// an unrelated statement is not found among the sub-expressions
		VariableRef unrelated = new VariableRef(cfg, LOC, "z");
		assertNull(n.getStatementEvaluatedBefore(unrelated));
		assertNull(n.getStatementEvaluatedAfter(unrelated));
	}

	@Test
	public void getStatementEvaluatedBeforeAndAfterHonorRightToLeftEvaluation() {
		CFG cfg = newCfg();
		VariableRef left = new VariableRef(cfg, LOC, "a");
		VariableRef right = new VariableRef(cfg, LOC, "b");
		RecordingNary n = new RecordingNary(cfg, LOC, RightToLeftEvaluation.INSTANCE, left, right);

		// right-to-left: the rightmost sub-expression is evaluated first
		assertNull(n.getStatementEvaluatedBefore(right));
		assertSame(left, n.getStatementEvaluatedAfter(right));
		assertSame(right, n.getStatementEvaluatedBefore(left));
		assertSame(n, n.getStatementEvaluatedAfter(left));
	}

	@Test
	public void forwardSemanticsPropagatesSubExpressionMetaVariablesIntoItsOwnAndForgetsThem()
			throws Exception {
		CFG cfg = newCfg();
		VariableRef x = new VariableRef(cfg, LOC, "x");
		VariableRef y = new VariableRef(cfg, LOC, "y");
		RecordingNary n = new RecordingNary(cfg, LOC, x, y);

		Identifier fromX = new Variable(Untyped.INSTANCE, "metaX", LOC);
		x.getMetaVariables().add(fromX);

		AnalysisState<TestAbstractState> state = emptyState();
		n.forwardSemantics(state, interprocedural(), store(state));

		assertTrue(n.getMetaVariables().contains(fromX));
		assertTrue(x.getMetaVariables().isEmpty());
	}

}
