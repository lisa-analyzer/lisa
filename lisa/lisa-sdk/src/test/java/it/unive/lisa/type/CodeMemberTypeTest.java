package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.AbstractCodeMember;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CodeMemberTypeTest {

	private static CodeMember member(
			String name) {
		ClassUnit unit = new ClassUnit(
				new SourceCodeLocation("fake", 1, 0),
				new Program(new TestLanguageFeatures(), new TestTypeSystem()),
				"fake",
				false);
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(
				new SourceCodeLocation("fake", 2, 0),
				unit,
				false,
				name);
		return new AbstractCodeMember(descriptor);
	}

	@Test
	public void getMembersReturnsTheConstructorArgument() {
		Set<CodeMember> members = Collections.singleton(member("foo"));
		assertEquals(members, new CodeMemberType(members).getMembers());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnTheMembersSetByValue() {
		CodeMember foo = member("foo");
		CodeMember bar = member("bar");

		Set<CodeMember> members1 = new HashSet<>(Arrays.asList(foo, bar));
		Set<CodeMember> members2 = new HashSet<>(Arrays.asList(bar, foo));
		CodeMemberType a = new CodeMemberType(members1);
		CodeMemberType b = new CodeMemberType(members2);

		assertNotSame(a, b);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		CodeMemberType c = new CodeMemberType(Collections.singleton(foo));
		assertFalse(a.equals(c));
	}

	@Test
	public void canBeAssignedToAnotherCodeMemberTypeOrUntyped() {
		CodeMemberType a = new CodeMemberType(Collections.singleton(member("foo")));
		CodeMemberType b = new CodeMemberType(Collections.singleton(member("bar")));
		assertTrue(a.canBeAssignedTo(b));
		assertTrue(a.canBeAssignedTo(Untyped.INSTANCE));
		assertFalse(a.canBeAssignedTo(VoidType.INSTANCE));
	}

	@Test
	public void commonSupertypeRecognizesDistinctButEqualCodeMemberTypesAsTheSameType() {
		// regression test, mirroring TypeTokenType: CodeMemberType is not a
		// singleton, so distinct instances wrapping the same set of members
		// must be recognized as the same type here, consistently with
		// equals()
		CodeMember foo = member("foo");
		CodeMember bar = member("bar");
		Set<CodeMember> members1 = new HashSet<>(Arrays.asList(foo, bar));
		Set<CodeMember> members2 = new HashSet<>(Arrays.asList(bar, foo));
		CodeMemberType a = new CodeMemberType(members1);
		CodeMemberType b = new CodeMemberType(members2);

		assertNotSame(a, b);
		assertSame(a, a.commonSupertype(b));
	}

	@Test
	public void commonSupertypeWithADifferentCodeMemberTypeIsUntyped() {
		CodeMemberType a = new CodeMemberType(Collections.singleton(member("foo")));
		CodeMemberType b = new CodeMemberType(Collections.singleton(member("bar")));
		assertSame(Untyped.INSTANCE, a.commonSupertype(b));
	}

	@Test
	public void allInstancesIsJustItself() {
		CodeMemberType t = new CodeMemberType(Collections.singleton(member("foo")));
		assertEquals(Collections.singleton(t), t.allInstances(new MinimalTypeSystem()));
	}

	@Test
	public void typeInterfaceDispatchesToCodeMemberType() {
		CodeMemberType t = new CodeMemberType(Collections.singleton(member("foo")));
		assertTrue(t.isCodeMemberType());
		assertSame(t, t.asCodeMemberType());
	}

}
