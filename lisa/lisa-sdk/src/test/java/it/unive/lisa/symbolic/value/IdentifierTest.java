package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class IdentifierTest {

	@Test
	public void equalsAndHashCodeAreBasedOnNameOnlyIgnoringType() {
		// documented: "variables should be uniquely identified by their
		// name, regardless of their type"
		Variable a = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		Variable b = new Variable(VoidType.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		Variable c = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);
		assertFalse(a.equals(c));
	}

	@Test
	public void equalsRequiresTheExactSameConcreteClass() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		GlobalVariable g = new GlobalVariable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertFalse(v.equals(g));
	}

	@Test
	public void lubOfEqualIdentifiersReturnsThis() throws SemanticException {
		Variable a = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		Variable b = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertSame(a, a.lub(b));
	}

	@Test
	public void lubOfDifferentIdentifiersThrows() {
		Variable a = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		Variable b = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);
		assertThrows(SemanticException.class, () -> a.lub(b));
	}

	@Test
	public void mightNeedRewritingIsFalseForOrdinaryValueTypesAndTrueForInMemoryOrUntyped() {
		Variable typed = new Variable(VoidType.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertFalse(typed.mightNeedRewriting());

		Variable untyped = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertTrue(untyped.mightNeedRewriting());

		Variable inMemory = new Variable(NullType.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertTrue(inMemory.mightNeedRewriting());
	}

	@Test
	public void removeTypingExpressionsIsIdentity() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertSame(v, v.removeTypingExpressions());
	}

	@Test
	public void defaultsForIsInstrumentedReceiverAndIsScopedByCallAndCanBeAssigned() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertFalse(v.isInstrumentedReceiver());
		assertFalse(v.isScopedByCall());
		assertTrue(v.canBeAssigned());
	}

	@Test
	public void isWeakReflectsConstructorArgument() {
		HeapLocation weak = new HeapLocation(Untyped.INSTANCE, "loc", true, SyntheticLocation.INSTANCE);
		HeapLocation strong = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		assertTrue(weak.isWeak());
		assertFalse(strong.isWeak());
	}

	@Test
	public void annotationsCanBeAddedAndListed() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertTrue(v.getAnnotationList().isEmpty());
		it.unive.lisa.program.annotations.Annotation ann = new it.unive.lisa.program.annotations.Annotation("ann1");
		v.addAnnotation(ann);
		assertTrue(v.getAnnotationList().contains(ann));
	}

}
