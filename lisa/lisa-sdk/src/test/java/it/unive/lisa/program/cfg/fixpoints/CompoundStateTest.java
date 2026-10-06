package it.unive.lisa.program.cfg.fixpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.ListRepresentation;
import org.junit.jupiter.api.Test;

// CompoundState is a two-field product Lattice (postState + intermediateStates):
// these tests check that every Lattice operation is forwarded to the right
// field of the right operand, since AnalysisState/StatementStore's own
// semantics are already covered by their own test suites
public class CompoundStateTest {

	// TestAbstractState does not override equals()/hashCode() (identity
	// semantics), so a single shared instance is reused across every state()
	// call: otherwise two states built from the same varName would never be
	// equal to each other, making the equals()/hashCode() tests meaningless
	private static final TestAbstractState LATTICE = new TestAbstractState();

	private static AnalysisState<TestAbstractState> state(
			String varName) {
		ProgramState<TestAbstractState> content = new ProgramState<>(
				LATTICE, new ExpressionSet(new Variable(Untyped.INSTANCE, varName, SyntheticLocation.INSTANCE)));
		// AnalysisState's single-arg constructor discards its argument's
		// content and builds the TOP state instead (see its own body: "top =
		// lattice.top()") - withExecution() is what actually stores content
		return new AnalysisState<>(content).withExecution(content);
	}

	private static StatementStore<TestAbstractState> store(
			String varName) {
		return new StatementStore<>(state(varName));
	}

	@Test
	public void ofExposesBothComponentsAsGivenFields() {
		AnalysisState<TestAbstractState> post = state("x");
		StatementStore<TestAbstractState> inter = store("y");
		CompoundState<TestAbstractState> cs = CompoundState.of(post, inter);
		assertEquals(post, cs.postState);
		assertEquals(inter, cs.intermediateStates);
	}

	@Test
	public void equalsAndHashCodeAreBasedOnBothComponents() {
		CompoundState<TestAbstractState> a = CompoundState.of(state("x"), store("y"));
		CompoundState<TestAbstractState> b = CompoundState.of(state("x"), store("y"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		assertNotEquals(a, CompoundState.of(state("other"), store("y")));
		assertNotEquals(a, CompoundState.of(state("x"), store("other")));
	}

	@Test
	public void lessOrEqualRequiresBothComponentsToBeLessOrEqual() throws SemanticException {
		CompoundState<TestAbstractState> cs = CompoundState.of(state("x"), store("y"));
		AnalysisState<TestAbstractState> otherPost = state("x2");
		StatementStore<TestAbstractState> otherInter = store("y2");
		CompoundState<TestAbstractState> other = CompoundState.of(otherPost, otherInter);

		assertEquals(
				cs.postState.lessOrEqual(otherPost) && cs.intermediateStates.lessOrEqual(otherInter),
				cs.lessOrEqual(other));
	}

	@Test
	public void lubCombinesEachComponentIndependently() throws SemanticException {
		AnalysisState<TestAbstractState> post1 = state("x1");
		StatementStore<TestAbstractState> inter1 = store("y1");
		AnalysisState<TestAbstractState> post2 = state("x2");
		StatementStore<TestAbstractState> inter2 = store("y2");
		CompoundState<TestAbstractState> a = CompoundState.of(post1, inter1);
		CompoundState<TestAbstractState> b = CompoundState.of(post2, inter2);

		CompoundState<TestAbstractState> lub = a.lub(b);
		assertEquals(post1.lub(post2), lub.postState);
		assertEquals(inter1.lub(inter2), lub.intermediateStates);
	}

	@Test
	public void glbCombinesEachComponentIndependently() throws SemanticException {
		AnalysisState<TestAbstractState> post1 = state("x1");
		StatementStore<TestAbstractState> inter1 = store("y1");
		AnalysisState<TestAbstractState> post2 = state("x2");
		StatementStore<TestAbstractState> inter2 = store("y2");
		CompoundState<TestAbstractState> a = CompoundState.of(post1, inter1);
		CompoundState<TestAbstractState> b = CompoundState.of(post2, inter2);

		CompoundState<TestAbstractState> glb = a.glb(b);
		assertEquals(post1.glb(post2), glb.postState);
		assertEquals(inter1.glb(inter2), glb.intermediateStates);
	}

	@Test
	public void upchainAndDownchainCombineEachComponentIndependently() throws SemanticException {
		AnalysisState<TestAbstractState> post1 = state("x1");
		StatementStore<TestAbstractState> inter1 = store("y1");
		AnalysisState<TestAbstractState> post2 = state("x2");
		StatementStore<TestAbstractState> inter2 = store("y2");
		CompoundState<TestAbstractState> a = CompoundState.of(post1, inter1);
		CompoundState<TestAbstractState> b = CompoundState.of(post2, inter2);

		CompoundState<TestAbstractState> up = a.upchain(b);
		assertEquals(post1.upchain(post2), up.postState);
		assertEquals(inter1.upchain(inter2), up.intermediateStates);

		CompoundState<TestAbstractState> down = a.downchain(b);
		assertEquals(post1.downchain(post2), down.postState);
		assertEquals(inter1.downchain(inter2), down.intermediateStates);
	}

	@Test
	public void wideningAndNarrowingCombineEachComponentIndependently() throws SemanticException {
		AnalysisState<TestAbstractState> post1 = state("x1");
		StatementStore<TestAbstractState> inter1 = store("y1");
		AnalysisState<TestAbstractState> post2 = state("x2");
		StatementStore<TestAbstractState> inter2 = store("y2");
		CompoundState<TestAbstractState> a = CompoundState.of(post1, inter1);
		CompoundState<TestAbstractState> b = CompoundState.of(post2, inter2);

		CompoundState<TestAbstractState> widened = a.widening(b);
		assertEquals(post1.widening(post2), widened.postState);
		assertEquals(inter1.widening(inter2), widened.intermediateStates);

		CompoundState<TestAbstractState> narrowed = a.narrowing(b);
		assertEquals(post1.narrowing(post2), narrowed.postState);
		assertEquals(inter1.narrowing(inter2), narrowed.intermediateStates);
	}

	@Test
	public void topAndBottomCombineEachComponentIndependently() {
		AnalysisState<TestAbstractState> post = state("x");
		StatementStore<TestAbstractState> inter = store("y");
		CompoundState<TestAbstractState> cs = CompoundState.of(post, inter);

		assertEquals(CompoundState.of(post.top(), inter.top()), cs.top());
		assertEquals(CompoundState.of(post.bottom(), inter.bottom()), cs.bottom());
	}

	@Test
	public void isTopAndIsBottomRequireBothComponents() {
		AnalysisState<TestAbstractState> post = state("x");
		StatementStore<TestAbstractState> inter = store("y");
		CompoundState<TestAbstractState> cs = CompoundState.of(post, inter);

		assertEquals(post.isTop() && inter.isTop(), cs.isTop());
		assertEquals(post.isBottom() && inter.isBottom(), cs.isBottom());
	}

	@Test
	public void representationCombinesBothComponentsAsAListRepresentation() {
		AnalysisState<TestAbstractState> post = state("x");
		StatementStore<TestAbstractState> inter = store("y");
		CompoundState<TestAbstractState> cs = CompoundState.of(post, inter);

		assertTrue(cs.representation() instanceof ListRepresentation);
		assertEquals(
				new ListRepresentation(post.representation(), inter.representation()), cs.representation());
	}

	@Test
	public void toStringIncludesBothComponents() {
		AnalysisState<TestAbstractState> post = state("x");
		StatementStore<TestAbstractState> inter = store("y");
		CompoundState<TestAbstractState> cs = CompoundState.of(post, inter);

		assertFalse(cs.toString().isEmpty());
		assertTrue(cs.toString().contains(post.toString()));
	}

}
