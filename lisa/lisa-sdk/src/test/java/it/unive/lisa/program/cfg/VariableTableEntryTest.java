package it.unive.lisa.program.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class VariableTableEntryTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG cfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "u", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	@Test
	public void nameOnlyConstructorDefaultsToUntypedAndUnboundedScope() {
		VariableTableEntry entry = new VariableTableEntry(LOC, 0, "x");
		assertSame(Untyped.INSTANCE, entry.getStaticType());
		assertNull(entry.getScopeStart());
		assertNull(entry.getScopeEnd());
	}

	@Test
	public void createReferenceUsesTheEntrysNameTypeAndTheCfgDescriptorsLocation() {
		CFG cfg = cfg();
		VariableTableEntry entry = new VariableTableEntry(LOC, 0, null, null, "x", Untyped.INSTANCE);
		VariableRef ref = entry.createReference(cfg);
		assertEquals("x", ref.getName());
		assertEquals(Untyped.INSTANCE, ref.getStaticType());
		assertEquals(cfg.getDescriptor().getLocation(), ref.getLocation());
	}

	@Test
	public void setScopeStartAndEndAreReflectedByGetters() {
		CFG cfg = cfg();
		Statement start = new NoOp(cfg, LOC);
		Statement end = new NoOp(cfg, LOC);
		VariableTableEntry entry = new VariableTableEntry(LOC, 0, "x");

		entry.setScopeStart(start);
		entry.setScopeEnd(end);

		assertSame(start, entry.getScopeStart());
		assertSame(end, entry.getScopeEnd());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnAllFields() {
		VariableTableEntry a = new VariableTableEntry(LOC, 0, "x");
		VariableTableEntry b = new VariableTableEntry(LOC, 0, "x");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		VariableTableEntry differentIndex = new VariableTableEntry(LOC, 1, "x");
		assertFalse(a.equals(differentIndex));

		VariableTableEntry differentName = new VariableTableEntry(LOC, 0, "y");
		assertFalse(a.equals(differentName));
	}

	@Test
	public void setIndexUpdatesTheIndex() {
		VariableTableEntry entry = new VariableTableEntry(LOC, 0, "x");
		entry.setIndex(3);
		assertEquals(3, entry.getIndex());
	}

}
