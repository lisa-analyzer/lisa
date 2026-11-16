package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class PushInvTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void toStringIsPushinv() {
		assertEquals("PUSHINV", new PushInv(Untyped.INSTANCE, SyntheticLocation.INSTANCE).toString());
	}

	@Test
	public void pushAndPopScopeAreNoOps() throws Exception {
		PushInv p = new PushInv(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertSame(p, p.pushScope(TOKEN, null));
		assertSame(p, p.popScope(TOKEN, null));
	}

	@Test
	public void mightNeedRewritingIsAlwaysFalseUnlikePushAny() {
		// PushInv represents bottom: regardless of its static type it never
		// carries an actual value/memory reference to rewrite, unlike PushAny
		// whose mightNeedRewriting() depends on the static type
		assertFalse(new PushInv(VoidType.INSTANCE, SyntheticLocation.INSTANCE).mightNeedRewriting());
		assertFalse(new PushInv(Untyped.INSTANCE, SyntheticLocation.INSTANCE).mightNeedRewriting());
		assertFalse(new PushInv(NullType.INSTANCE, SyntheticLocation.INSTANCE).mightNeedRewriting());
	}

}
