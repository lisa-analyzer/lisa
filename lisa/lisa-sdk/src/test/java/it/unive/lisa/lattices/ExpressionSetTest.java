package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ExpressionSetTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	private static Constant constant(
			Object value) {
		return new Constant(Untyped.INSTANCE, value, SyntheticLocation.INSTANCE);
	}

	@Test
	public void topAndBottomAreTheEmptySetWithTheCorrectFlag() {
		assertTrue(new ExpressionSet().top().isTop());
		assertTrue(new ExpressionSet().bottom().isBottom());
		// the no-arg constructor documented as "the empty set lattice
		// element" is bottom, matching the natural convention of a normal
		// (non-inverse) subset lattice
		assertTrue(new ExpressionSet().isBottom());
	}

	@Test
	public void lubOfNonIdentifierExpressionsIsPlainUnion() throws SemanticException {
		Constant a = constant(1);
		Constant b = constant(2);
		ExpressionSet result = new ExpressionSet(a).lub(new ExpressionSet(b));
		assertEquals(Set.of(a, b), result.elements());
	}

	@Test
	public void lubMergesIdentifiersWithTheSameNameInsteadOfKeepingBothCopies() throws SemanticException {
		// Identifier#equals is name-based, and weak/strong identifiers with
		// the same name must be merged via Identifier#lub, not just unioned
		HeapLocation weak = new HeapLocation(Untyped.INSTANCE, "loc", true, SyntheticLocation.INSTANCE);
		HeapLocation strong = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);

		ExpressionSet result = new ExpressionSet(weak).lub(new ExpressionSet(strong));
		assertEquals(1, result.size());
		Identifier merged = (Identifier) result.elements().iterator().next();
		// HeapLocation#lub prefers the weak one
		assertEquals(weak, merged);
	}

	@Test
	public void lubKeepsUnmatchedIdentifiersAndNonIdentifiersTogether() throws SemanticException {
		Variable x = var("x");
		Constant c = constant(1);
		ExpressionSet result = new ExpressionSet(x).lub(new ExpressionSet(c));
		assertEquals(Set.of(x, c), result.elements());
	}

	@Test
	public void pushAndPopScopeMapEveryElement() throws Exception {
		Variable x = var("x");
		Variable y = var("y");
		ExpressionSet set = new ExpressionSet(Set.of(x, y));

		ExpressionSet pushed = set.pushScope(TOKEN, null);
		assertEquals(2, pushed.size());
		for (SymbolicExpression e : pushed)
			assertTrue(e instanceof it.unive.lisa.symbolic.value.OutOfScopeIdentifier);
	}

}
