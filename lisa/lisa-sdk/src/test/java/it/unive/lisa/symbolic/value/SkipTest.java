package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class SkipTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void staticTypeIsAlwaysVoid() {
		assertSame(VoidType.INSTANCE, new Skip(SyntheticLocation.INSTANCE).getStaticType());
	}

	@Test
	public void toStringIsSkip() {
		assertEquals("skip", new Skip(SyntheticLocation.INSTANCE).toString());
	}

	@Test
	public void twoSkipsAreEqualRegardlessOfLocation() {
		Skip a = new Skip(SyntheticLocation.INSTANCE);
		Skip b = new Skip(new SourceCodeLocation("f", 1, 0));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void pushAndPopScopeAreNoOps() throws Exception {
		Skip s = new Skip(SyntheticLocation.INSTANCE);
		assertSame(s, s.pushScope(TOKEN, null));
		assertSame(s, s.popScope(TOKEN, null));
	}

	@Test
	public void mightNeedRewritingIsAlwaysFalse() {
		assertFalse(new Skip(SyntheticLocation.INSTANCE).mightNeedRewriting());
	}

}
