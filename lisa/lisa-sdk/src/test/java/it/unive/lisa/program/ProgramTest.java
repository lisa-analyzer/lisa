package it.unive.lisa.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import org.junit.jupiter.api.Test;

public class ProgramTest {

	private static Program program() {
		return new Program(new TestLanguageFeatures(), new TestTypeSystem());
	}

	@Test
	public void aFreshProgramCannotBeInstantiatedAndIsItsOwnProgram() {
		Program p = program();
		assertFalse(p.canBeInstantiated());
		assertSame(p, p.getProgram());
		assertEquals(Program.PROGRAM_NAME, p.getName());
	}

	@Test
	public void addUnitRejectsAnotherProgramAsAUnit() {
		Program p = program();
		assertThrows(IllegalArgumentException.class, () -> p.addUnit(program()));
	}

	@Test
	public void addUnitDiscardsDuplicateNames() {
		Program p = program();
		ClassUnit first = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "u", false);
		ClassUnit second = new ClassUnit(new SourceCodeLocation("f", 2, 0), p, "u", false);
		assertTrue(p.addUnit(first));
		assertFalse(p.addUnit(second));
		assertSame(first, p.getUnit("u"));
	}

	@Test
	public void getAllCFGsFiltersCodeMembersRecursivelyToOnlyCFGs() {
		Program p = program();
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "u", false);
		p.addUnit(unit);

		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(new SourceCodeLocation("f", 1, 0), unit, false,
				"foo");
		CFG cfg = new CFG(descriptor);
		unit.addCodeMember(cfg);

		assertEquals(1, p.getAllCFGs().size());
		assertTrue(p.getAllCFGs().contains(cfg));
	}

	@Test
	public void entryPointsAreTrackedIndependentlyOfUnits() {
		Program p = program();
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "u", false);
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(new SourceCodeLocation("f", 1, 0), unit, false,
				"foo");
		CFG cfg = new CFG(descriptor);
		assertTrue(p.getEntryPoints().isEmpty());
		assertTrue(p.addEntryPoint(cfg));
		assertEquals(1, p.getEntryPoints().size());
	}

}
