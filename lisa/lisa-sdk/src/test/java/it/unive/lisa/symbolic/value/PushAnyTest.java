package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class PushAnyTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void equalsComparesOnlyTheStaticType() {
		PushAny a = new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		PushAny b = new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new PushAny(VoidType.INSTANCE, SyntheticLocation.INSTANCE)));
	}

	@Test
	public void toStringIsPushany() {
		assertEquals("PUSHANY", new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE).toString());
	}

	@Test
	public void pushAndPopScopeAreNoOps() throws Exception {
		PushAny p = new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertSame(p, p.pushScope(TOKEN, null));
		assertSame(p, p.popScope(TOKEN, null));
	}

	@Test
	public void mightNeedRewritingFollowsTheStaticTypeRules() {
		assertFalse(new PushAny(VoidType.INSTANCE, SyntheticLocation.INSTANCE).mightNeedRewriting());
		assertTrue(new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE).mightNeedRewriting());
		assertTrue(new PushAny(NullType.INSTANCE, SyntheticLocation.INSTANCE).mightNeedRewriting());
	}

}
