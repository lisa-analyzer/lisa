package it.unive.lisa.program.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class CodeMemberDescriptorTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static ClassUnit unit() {
		return new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "unit", false);
	}

	@Test
	public void formalsBecomeVariableTableEntriesInDeclarationOrder() {
		Parameter p0 = new Parameter(LOC, "a", Untyped.INSTANCE);
		Parameter p1 = new Parameter(LOC, "b", Untyped.INSTANCE);
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(LOC, unit(), false, "m", p0, p1);

		assertEquals(2, descriptor.getVariables().size());
		assertEquals("a", descriptor.getVariables().get(0).getName());
		assertEquals(0, descriptor.getVariables().get(0).getIndex());
		assertEquals("b", descriptor.getVariables().get(1).getName());
		assertEquals(1, descriptor.getVariables().get(1).getIndex());
	}

	@Test
	public void addVariableOverwritesIndexWithNextFreeSlot() {
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(LOC, unit(), false, "m");
		VariableTableEntry entry = new VariableTableEntry(LOC, 42, "x");

		descriptor.addVariable(entry);

		assertEquals(0, entry.getIndex());
	}

	@Test
	public void matchesSignatureRequiresSameNameInstanceAndAssignableFormals() {
		CodeMemberDescriptor reference = new CodeMemberDescriptor(
				LOC, unit(), true, "m", new Parameter(LOC, "a", Untyped.INSTANCE));

		CodeMemberDescriptor sameSignature = new CodeMemberDescriptor(
				LOC, unit(), true, "m", new Parameter(LOC, "a", Untyped.INSTANCE));
		assertTrue(sameSignature.matchesSignature(reference));

		CodeMemberDescriptor differentName = new CodeMemberDescriptor(
				LOC, unit(), true, "other", new Parameter(LOC, "a", Untyped.INSTANCE));
		assertFalse(differentName.matchesSignature(reference));

		CodeMemberDescriptor differentInstance = new CodeMemberDescriptor(
				LOC, unit(), false, "m", new Parameter(LOC, "a", Untyped.INSTANCE));
		assertFalse(differentInstance.matchesSignature(reference));

		CodeMemberDescriptor differentArity = new CodeMemberDescriptor(LOC, unit(), true, "m");
		assertFalse(differentArity.matchesSignature(reference));
	}

	@Test
	public void addControlFlowStructureRejectsASecondStructureOnTheSameCondition() {
		CFG cfg = new CFG(new CodeMemberDescriptor(LOC, unit(), false, "loop"));
		Statement condition = new NoOp(cfg, LOC);
		CodeMemberDescriptor descriptor = cfg.getDescriptor();

		descriptor.addControlFlowStructure(
				new IfThenElse(cfg.getNodeList(), condition, null, Collections.emptySet(), Collections.emptySet()));

		assertThrows(IllegalArgumentException.class,
				() -> descriptor.addControlFlowStructure(
						new IfThenElse(
								cfg.getNodeList(), condition, null, Collections.emptySet(),
								Collections.emptySet())));
	}

	@Test
	public void getAnnotationsOfReturnsEmptyAnnotationsWhenVariableIsNotFound() {
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(LOC, unit(), false, "m");
		CFG cfg = new CFG(descriptor);
		Statement st = new NoOp(cfg, LOC);

		assertTrue(descriptor.getAnnotationsOf("nonexistent", st).getAnnotations().isEmpty());
	}

	@Test
	public void signaturesReflectNameReturnTypeAndFormals() {
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(
				LOC, unit(), false, "m", new Parameter(LOC, "a", Untyped.INSTANCE));

		assertEquals("unit::m", descriptor.getFullName());
		assertEquals("unit::m(" + Untyped.INSTANCE + ")", descriptor.getSignature());
		assertEquals("unit::m(" + Untyped.INSTANCE + " a)", descriptor.getSignatureWithParNames());
		assertEquals(Untyped.INSTANCE + " unit::m(" + Untyped.INSTANCE + ")", descriptor.getFullSignature());
	}

}
