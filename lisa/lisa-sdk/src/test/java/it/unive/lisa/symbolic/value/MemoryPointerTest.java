package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class MemoryPointerTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void nameIsDerivedFromTheReferencedLocation() {
		HeapLocation loc = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		MemoryPointer p = new MemoryPointer(Untyped.INSTANCE, loc, SyntheticLocation.INSTANCE);
		assertEquals("loc", p.getName());
		assertSame(loc, p.getReferencedLocation());
	}

	@Test
	public void toStringIsAmpersandName() {
		HeapLocation loc = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		MemoryPointer p = new MemoryPointer(Untyped.INSTANCE, loc, SyntheticLocation.INSTANCE);
		assertEquals("&loc", p.toString());
	}

	@Test
	public void canBeScopedIsTrue() {
		HeapLocation loc = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		assertTrue(new MemoryPointer(Untyped.INSTANCE, loc, SyntheticLocation.INSTANCE).canBeScoped());
	}

	@Test
	public void pushScopeWrapsInAnOutOfScopeIdentifierAndPopScopeIsAlwaysNull() throws Exception {
		HeapLocation loc = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		MemoryPointer p = new MemoryPointer(Untyped.INSTANCE, loc, SyntheticLocation.INSTANCE);

		SymbolicExpression pushed = p.pushScope(TOKEN, null);
		assertTrue(pushed instanceof OutOfScopeIdentifier);

		assertNull(p.popScope(TOKEN, null));
	}

}
