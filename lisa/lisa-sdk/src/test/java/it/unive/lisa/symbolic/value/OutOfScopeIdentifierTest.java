package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class OutOfScopeIdentifierTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);
	private static final ScopeToken OTHER_TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private static ScopeToken callToken() {
		ClassUnit unit = new ClassUnit(
				SyntheticLocation.INSTANCE,
				new Program(new TestLanguageFeatures(), new TestTypeSystem()),
				"unit",
				false);
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "foo"));
		Call call = new OpenCall(
				cfg, SyntheticLocation.INSTANCE, Call.CallType.STATIC, "q", "target");
		return new ScopeToken(call);
	}

	@Test
	public void nameCombinesScopeAndInnerName() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		OutOfScopeIdentifier o = new OutOfScopeIdentifier(v, TOKEN, SyntheticLocation.INSTANCE);
		assertEquals(TOKEN + ":x", o.getName());
		assertEquals(o.getName(), o.toString());
	}

	@Test
	public void popScopeWithMatchingTokenReturnsTheInnerIdentifier() throws Exception {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		OutOfScopeIdentifier o = new OutOfScopeIdentifier(v, TOKEN, SyntheticLocation.INSTANCE);
		assertSame(v, o.popScope(TOKEN, null));
	}

	@Test
	public void popScopeWithDifferentTokenReturnsNull() throws Exception {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		OutOfScopeIdentifier o = new OutOfScopeIdentifier(v, TOKEN, SyntheticLocation.INSTANCE);
		assertNull(o.popScope(OTHER_TOKEN, null));
	}

	@Test
	public void pushScopeWrapsAgainWithTheNewToken() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		OutOfScopeIdentifier o = new OutOfScopeIdentifier(v, TOKEN, SyntheticLocation.INSTANCE);
		SymbolicExpression pushed = o.pushScope(OTHER_TOKEN, null);
		assertTrue(pushed instanceof OutOfScopeIdentifier);
		OutOfScopeIdentifier nested = (OutOfScopeIdentifier) pushed;
		assertSame(OTHER_TOKEN, nested.getScope());
		assertEquals(OTHER_TOKEN + ":" + o.getName(), nested.getName());
	}

	@Test
	public void isScopedByCallIsTrueWhenTheImmediateScoperIsACall() {
		ScopeToken callToken = callToken();
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		OutOfScopeIdentifier o = new OutOfScopeIdentifier(v, callToken, SyntheticLocation.INSTANCE);
		assertTrue(o.isScopedByCall());
	}

	@Test
	public void isScopedByCallIsFalseWhenTheScoperIsNotACall() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		OutOfScopeIdentifier o = new OutOfScopeIdentifier(v, TOKEN, SyntheticLocation.INSTANCE);
		assertFalse(o.isScopedByCall());
	}

	@Test
	public void isScopedByCallPropagatesThroughNestedOutOfScopeIdentifiers() {
		ScopeToken callToken = callToken();
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		OutOfScopeIdentifier innerScoped = new OutOfScopeIdentifier(v, callToken, SyntheticLocation.INSTANCE);
		OutOfScopeIdentifier outer = new OutOfScopeIdentifier(innerScoped, OTHER_TOKEN, SyntheticLocation.INSTANCE);
		assertTrue(outer.isScopedByCall());
	}

	@Test
	public void isInstrumentedReceiverDelegatesToTheInnerIdentifier() {
		InstrumentedReceiver rec = new InstrumentedReceiver(Untyped.INSTANCE, false, SyntheticLocation.INSTANCE);
		OutOfScopeIdentifier o = new OutOfScopeIdentifier(rec, TOKEN, SyntheticLocation.INSTANCE);
		assertTrue(o.isInstrumentedReceiver());
	}

}
