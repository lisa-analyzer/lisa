package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.emptyState;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.interprocedural;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.store;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import org.junit.jupiter.api.Test;

public class NaryStatementTest {

	private static class RecordingNaryStatement
			extends
			NaryStatement {

		RecordingNaryStatement(
				CFG cfg,
				CodeLocation loc,
				Expression... subs) {
			super(cfg, loc, "nary", subs);
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
	public void gettersExposeConstructorArgumentsAndSetParentOnSubExpressions() {
		CFG cfg = newCfg();
		VariableRef x = new VariableRef(cfg, LOC, "x");
		RecordingNaryStatement n = new RecordingNaryStatement(cfg, LOC, x);
		assertEquals("nary", n.getConstructName());
		assertSame(x, n.getSubExpressions()[0]);
		assertSame(n, x.getParentStatement());
		assertEquals("nary x", n.toString());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnConstructNameAndSubExpressions() {
		CFG cfg = newCfg();
		RecordingNaryStatement a = new RecordingNaryStatement(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		RecordingNaryStatement b = new RecordingNaryStatement(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		RecordingNaryStatement differentSub = new RecordingNaryStatement(cfg, LOC, new VariableRef(cfg, LOC, "y"));
		assertFalse(a.equals(differentSub));
	}

	@Test
	public void acceptVisitsSubNodesBeforeOrAfterTheNodeDependingOnTheVisitor() {
		CFG cfg = newCfg();
		VariableRef x = new VariableRef(cfg, LOC, "x");
		RecordingNaryStatement n = new RecordingNaryStatement(cfg, LOC, x);

		final boolean[] nodeSeenAfterSub = { false };
		n.accept(new GraphVisitor<CFG, Statement, Edge, Object>() {
			@Override
			public boolean visitSubNodesFirst() {
				return true;
			}

			@Override
			public boolean visit(
					Object tool,
					CFG graph,
					Statement node) {
				if (node == n)
					nodeSeenAfterSub[0] = true;
				return true;
			}
		}, null);
		assertTrue(nodeSeenAfterSub[0]);
	}

	@Test
	public void getStatementEvaluatedBeforeAndAfterAtTheBoundaries() {
		CFG cfg = newCfg();
		VariableRef first = new VariableRef(cfg, LOC, "a");
		VariableRef last = new VariableRef(cfg, LOC, "b");
		RecordingNaryStatement n = new RecordingNaryStatement(cfg, LOC, first, last);

		assertNull(n.getStatementEvaluatedBefore(first));
		assertSame(last, n.getStatementEvaluatedAfter(first));
		assertSame(first, n.getStatementEvaluatedBefore(last));
		assertSame(n, n.getStatementEvaluatedAfter(last));
	}

	@Test
	public void forwardSemanticsEvaluatesSubExpressionsAndForgetsTheirMetaVariables()
			throws Exception {
		CFG cfg = newCfg();
		VariableRef x = new VariableRef(cfg, LOC, "x");
		VariableRef y = new VariableRef(cfg, LOC, "y");
		RecordingNaryStatement n = new RecordingNaryStatement(cfg, LOC, x, y);

		Identifier fromX = new Variable(Untyped.INSTANCE, "metaX", LOC);
		x.getMetaVariables().add(fromX);

		AnalysisState<TestAbstractState> state = emptyState();
		AnalysisState<TestAbstractState> result = n.forwardSemantics(state, interprocedural(), store(state));

		// unlike NaryExpression (whose result is still nested inside something
		// else and thus keeps propagating meta variables upward), a statement
		// is
		// a "final" consumer: it forgets sub-expression meta variables via
		// AnalysisState#forgetIdentifiers instead of collecting them
		assertNotNull(result);
	}

}
