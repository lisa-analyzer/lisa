package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class GlobalVariableTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void canBeScopedIsFalse() {
		assertFalse(new GlobalVariable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE).canBeScoped());
	}

	@Test
	public void pushAndPopScopeAreNoOps() throws Exception {
		GlobalVariable g = new GlobalVariable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertSame(g, g.pushScope(TOKEN, null));
		assertSame(g, g.popScope(TOKEN, null));
	}

}
