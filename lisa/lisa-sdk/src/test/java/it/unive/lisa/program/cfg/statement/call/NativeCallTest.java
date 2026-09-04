package it.unive.lisa.program.cfg.statement.call;

import static it.unive.lisa.program.cfg.statement.call.CallFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.call.CallFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.type.Untyped;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NativeCallTest {

	private static NativeCFG nativeCfg(
			String name) {
		CFG holder = newCfg(name);
		return new NativeCFG(holder.getDescriptor(), FakeConstruct.class);
	}

	@Test
	public void constructorRejectsNullTargets() {
		CFG cfg = newCfg("caller");
		assertThrows(NullPointerException.class,
				() -> new NativeCall(cfg, LOC, CallType.STATIC, null, "foo", (List<NativeCFG>) null));
	}

	@Test
	public void withNoTargetsTheCommonReturnTypeIsUntyped() {
		NativeCall call = new NativeCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of());
		assertSame(Untyped.INSTANCE, call.getStaticType());
	}

	@Test
	public void getTargetedConstructsAndGetTargetsExposeTheSameContent() {
		NativeCFG t1 = nativeCfg("t1");
		NativeCFG t2 = nativeCfg("t2");
		NativeCall call = new NativeCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1, t2));
		assertEquals(Set.of(t1, t2), Set.copyOf(call.getTargetedConstructs()));
		assertEquals(Set.of(t1, t2), Set.copyOf(call.getTargets()));
	}

	@Test
	public void toStringIncludesTheNumberOfTargets() {
		NativeCFG t1 = nativeCfg("t1");
		NativeCall call = new NativeCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1));
		assertTrue(call.toString().startsWith("[1 targets]"));
	}

	@Test
	public void equalsAndHashCodeIncludeTheTargets() {
		NativeCFG t1 = nativeCfg("t1");
		CFG caller = newCfg("caller");
		NativeCall a = new NativeCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1));
		NativeCall b = new NativeCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void removeFirstParameterTruncatesParametersButKeepsTheSameTargets() {
		NativeCFG t1 = nativeCfg("t1");
		CFG caller = newCfg("caller");
		VariableRef receiver = new VariableRef(caller, LOC, "recv");
		VariableRef arg = new VariableRef(caller, LOC, "arg");
		NativeCall call = new NativeCall(caller, LOC, CallType.UNKNOWN, null, "foo", List.of(t1), receiver, arg);

		TruncatedParamsCall truncated = call.removeFirstParameter();
		assertEquals(1, truncated.getParameters().length);
		assertSame(arg, truncated.getParameters()[0]);
		assertEquals(Set.of(t1), Set.copyOf(((NativeCall) truncated.getInnerCall()).getTargetedConstructs()));
	}

	@Test
	public void aNativeCallWithAtLeastOneTargetIsNeverConsideredVoidEvenWithoutRunningItsSemantics() {
		// NativeCFGs always rewrite to an expression and leave a value on the
		// stack (see Call#returnsVoid's NativeCall branch), so this holds
		// regardless of what the pluggable construct itself would compute
		NativeCFG t1 = nativeCfg("t1");
		NativeCall call = new NativeCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1));
		assertFalse(call.returnsVoid(null));
	}

	@Test
	public void aVoidStaticTypeStillMakesTheCallVoidRegardlessOfTargets() {
		CFG caller = newCfg("caller");
		// getCommonReturnType of a single VoidType target is VoidType itself
		CodeMemberDescriptor voidDescriptor = new CodeMemberDescriptor(
				LOC, caller.getDescriptor().getUnit(), false, "voidTarget", it.unive.lisa.type.VoidType.INSTANCE);
		NativeCFG voidTarget = new NativeCFG(voidDescriptor, FakeConstruct.class);
		NativeCall voidCall = new NativeCall(caller, LOC, CallType.STATIC, null, "foo", List.of(voidTarget));
		assertTrue(voidCall.returnsVoid(null));
	}

}
