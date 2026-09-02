package it.unive.lisa.program.cfg.statement.call;

import static it.unive.lisa.program.cfg.statement.call.CallFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.call.CallFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CFGCallTest {

	@Test
	public void constructorRejectsNullTargetsAndNullTargetElements() {
		CFG cfg = newCfg("caller");
		assertThrows(NullPointerException.class,
				() -> new CFGCall(cfg, LOC, CallType.STATIC, null, "foo", null));
		java.util.List<CFG> withNull = new java.util.ArrayList<>();
		withNull.add(null);
		assertThrows(NullPointerException.class,
				() -> new CFGCall(cfg, LOC, CallType.STATIC, null, "foo", withNull));
	}

	@Test
	public void withNoTargetsTheCommonReturnTypeIsUntyped() {
		CFG cfg = newCfg("caller");
		CFGCall call = new CFGCall(cfg, LOC, CallType.STATIC, null, "foo", List.of());
		assertSame(Untyped.INSTANCE, call.getStaticType());
	}

	@Test
	public void whenATargetsTypeIsAssignableToTheRunningResultTheResultIsUnchanged() {
		FakeType narrow = new FakeType("narrow", (
				self,
				other) -> true,
				(
						self,
						other) -> {
					throw new AssertionError("commonSupertype should not be needed here");
				});
		FakeType wide = new FakeType("wide", (
				self,
				other) -> false,
				(
						self,
						other) -> {
					throw new AssertionError("commonSupertype should not be needed here");
				});
		// wide first (becomes the running result), then narrow (assignable to
		// wide) - result must stay "wide"
		CFG t1 = newCfg("t1", wide);
		CFG t2 = newCfg("t2", narrow);
		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1, t2));
		assertSame(wide, call.getStaticType());
	}

	@Test
	public void whenTheRunningResultIsAssignableToATargetsTypeTheResultWidens() {
		FakeType wide = new FakeType("wide", (
				self,
				other) -> false,
				(
						self,
						other) -> {
					throw new AssertionError("commonSupertype should not be needed here");
				});
		FakeType narrow = new FakeType("narrow", (
				self,
				other) -> other == wide,
				(
						self,
						other) -> {
					throw new AssertionError("commonSupertype should not be needed here");
				});
		// narrow first (becomes the running result, assignable to wide), then
		// wide - result must widen to "wide"
		CFG t1 = newCfg("t1", narrow);
		CFG t2 = newCfg("t2", wide);
		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1, t2));
		assertSame(wide, call.getStaticType());
	}

	@Test
	public void whenTwoTargetTypesAreIncomparableTheCommonSupertypeIsUsed() {
		FakeType joined = new FakeType("joined", (
				self,
				other) -> false,
				(
						self,
						other) -> null);
		FakeType a = new FakeType("a", (
				self,
				other) -> false,
				(
						self,
						other) -> joined);
		FakeType b = new FakeType("b", (
				self,
				other) -> false,
				(
						self,
						other) -> joined);
		CFG t1 = newCfg("t1", a);
		CFG t2 = newCfg("t2", b);
		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1, t2));
		assertSame(joined, call.getStaticType());
	}

	@Test
	public void reachingAnUntypedTargetStopsTheJoinEarly() {
		FakeType neverVisited = new FakeType("neverVisited", (
				self,
				other) -> {
			throw new AssertionError("must not be reached after Untyped short-circuits");
		}, (
				self,
				other) -> {
			throw new AssertionError("must not be reached after Untyped short-circuits");
		});
		CFG t1 = newCfg("t1", Untyped.INSTANCE);
		CFG t2 = newCfg("t2", neverVisited);
		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1, t2));
		assertTrue(call.getStaticType().isUntyped());
	}

	@Test
	public void getTargetedCFGsAndGetTargetsExposeTheSameContent() {
		CFG t1 = newCfg("t1");
		CFG t2 = newCfg("t2");
		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1, t2));
		assertEquals(Set.of(t1, t2), Set.copyOf(call.getTargetedCFGs()));
		assertEquals(Set.of(t1, t2), Set.copyOf(call.getTargets()));
	}

	@Test
	public void equalsAndHashCodeIncludeTheTargets() {
		CFG t1 = newCfg("t1");
		CFG caller = newCfg("caller");
		CFGCall a = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1));
		CFGCall b = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		CFGCall differentTargets = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(newCfg("other")));
		assertNotEquals(a, differentTargets);
	}

	@Test
	public void toStringIncludesTheNumberOfTargets() {
		CFG t1 = newCfg("t1");
		CFG t2 = newCfg("t2");
		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1, t2));
		assertTrue(call.toString().startsWith("[2 targets]"));
	}

	@Test
	public void getMetaVariablePropagatesAnnotationsFromEveryTarget() {
		CFG t1 = newCfg("t1");
		t1.getDescriptor().addAnnotation(new Annotation("First"));
		CFG t2 = newCfg("t2");
		t2.getDescriptor().addAnnotation(new Annotation("Second"));
		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1, t2));

		Variable meta = (Variable) call.getMetaVariable();
		assertTrue(meta.getName().startsWith("call_ret_value@"));
		List<String> names = meta.getAnnotations().getAnnotations().stream()
				.map(Annotation::getAnnotationName).sorted().toList();
		assertEquals(List.of("First", "Second"), names);
	}

	@Test
	public void removeFirstParameterTruncatesParametersButKeepsTheSameTargets() {
		CFG t1 = newCfg("t1");
		CFG caller = newCfg("caller");
		VariableRef receiver = new VariableRef(caller, LOC, "recv");
		VariableRef arg = new VariableRef(caller, LOC, "arg");
		CFGCall call = new CFGCall(caller, LOC, CallType.UNKNOWN, null, "foo", List.of(t1), receiver, arg);

		TruncatedParamsCall truncated = call.removeFirstParameter();
		assertEquals(1, truncated.getParameters().length);
		assertSame(arg, truncated.getParameters()[0]);
		assertEquals(Set.of(t1), Set.copyOf(((CFGCall) truncated.getInnerCall()).getTargetedCFGs()));
	}

	@Test
	public void voidReturningTargetMakesTheCallVoid() {
		CFG t1 = newCfg("t1", VoidType.INSTANCE);
		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1));
		assertTrue(call.returnsVoid(null));
	}

	@Test
	public void untypedCallWithNoNormalExitpointReturningAValueIsConsideredVoid() {
		// t1 has no statements at all, so getNormalExitpoints() is empty and no
		// exitpoint is a MetaVariableCreator - Call#returnsVoid must then say
		// "void" for this CFGCall
		CFG t1 = newCfg("t1");
		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1));
		assertTrue(call.returnsVoid(null));
	}

	@Test
	public void untypedCallWithAReturnExitpointIsNotVoid() {
		CFG t1 = newCfg("t1");
		VariableRef retExpr = new VariableRef(t1, LOC, "x");
		it.unive.lisa.program.cfg.statement.Return ret = new it.unive.lisa.program.cfg.statement.Return(
				t1, LOC, retExpr);
		t1.addNode(ret, true);

		CFGCall call = new CFGCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo", List.of(t1));
		assertFalse(call.returnsVoid(null));
	}

}
