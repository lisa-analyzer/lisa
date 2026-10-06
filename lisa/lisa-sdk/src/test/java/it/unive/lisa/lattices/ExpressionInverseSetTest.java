package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ExpressionInverseSetTest {

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	private static Constant constant(
			Object value) {
		return new Constant(Untyped.INSTANCE, value, SyntheticLocation.INSTANCE);
	}

	@Test
	public void theNoArgConstructorIsBottomAndTheConstructorEmptySentinelMatchesIt() {
		assertTrue(new ExpressionInverseSet().isBottom());
		assertTrue(new ExpressionInverseSet().top().isTop());
	}

	@Test
	public void lubOfNonIdentifierExpressionsIsPlainIntersection() throws SemanticException {
		Constant a = constant(1);
		Constant b = constant(2);
		ExpressionInverseSet result = new ExpressionInverseSet(a).lub(new ExpressionInverseSet(b));
		// disjoint singletons: their intersection is empty, which for this
		// inverse-ordered lattice is top, not bottom
		assertTrue(result.isTop());
	}

	@Test
	public void lubKeepsCommonNonIdentifierExpressions() throws SemanticException {
		Constant a = constant(1);
		Constant b = constant(2);
		ExpressionInverseSet left = new ExpressionInverseSet(Set.of(a, b));
		ExpressionInverseSet right = new ExpressionInverseSet(Set.of(a));
		assertEquals(Set.of(a), left.lub(right).elements());
	}

	@Test
	public void lubMergesIdentifiersWithTheSameNameInsteadOfDroppingThem() throws SemanticException {
		HeapLocation weak = new HeapLocation(Untyped.INSTANCE, "loc", true, SyntheticLocation.INSTANCE);
		HeapLocation strong = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);

		ExpressionInverseSet result = new ExpressionInverseSet(weak).lub(new ExpressionInverseSet(strong));
		assertEquals(1, result.size());
		Identifier merged = (Identifier) result.elements().iterator().next();
		assertEquals(weak, merged);
	}

	@Test
	public void lubDropsUnmatchedNonIdentifierExpressions() throws SemanticException {
		// unlike ExpressionSet (union semantics), an unmatched non-identifier
		// expression present in only one side must be dropped, not kept
		Variable x = var("x");
		Constant c = constant(1);
		ExpressionInverseSet result = new ExpressionInverseSet(x).lub(new ExpressionInverseSet(c));
		assertTrue(result.elements().stream().noneMatch(e -> e.equals(c)));
	}

}
