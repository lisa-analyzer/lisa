package it.unive.lisa.program.language.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.AbstractClassUnit;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.AbstractCodeMember;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Ret;
import org.junit.jupiter.api.Test;

public class BaseValidationLogicTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static Program program() {
		return new Program(new TestLanguageFeatures(), new TestTypeSystem());
	}

	@Test
	public void validateRejectsAGlobalWhoseInstanceFlagDisagreesWithTheExpectedOne() {
		Program p = program();
		CodeUnit unit = new CodeUnit(LOC, p, "U");
		Global instanceGlobal = new Global(LOC, unit, "g", true);

		BaseValidationLogic logic = new BaseValidationLogic();
		ProgramValidationException ex = assertThrows(
				ProgramValidationException.class, () -> logic.validate(instanceGlobal, false));
		assertTrue(ex.getMessage().contains("g"));
	}

	@Test
	public void validateAcceptsAGlobalWhoseInstanceFlagMatchesTheExpectedOne()
			throws ProgramValidationException {
		Program p = program();
		CodeUnit unit = new CodeUnit(LOC, p, "U");
		Global nonInstanceGlobal = new Global(LOC, unit, "g", false);

		new BaseValidationLogic().validate(nonInstanceGlobal, false);
	}

	@Test
	public void validateAndFinalizeRejectsASealedAbstractClassUnit() {
		Program p = program();
		AbstractClassUnit unit = new AbstractClassUnit(LOC, p, "A", true);

		BaseValidationLogic logic = new BaseValidationLogic();
		assertThrows(ProgramValidationException.class, () -> logic.validateAndFinalize(unit));
	}

	@Test
	public void validateAndFinalizeAcceptsANonSealedAbstractClassUnit()
			throws ProgramValidationException {
		Program p = program();
		AbstractClassUnit unit = new AbstractClassUnit(LOC, p, "A", false);

		new BaseValidationLogic().validateAndFinalize(unit);
	}

	@Test
	public void validateAndFinalizeRejectsAnInstantiableClassWithAnAbstractCodeMember() {
		Program p = program();
		ClassUnit unit = new ClassUnit(LOC, p, "C", false);
		AbstractCodeMember abstractMember = new AbstractCodeMember(
				new CodeMemberDescriptor(LOC, unit, true, "m"));
		unit.addInstanceCodeMember(abstractMember);

		BaseValidationLogic logic = new BaseValidationLogic();
		ProgramValidationException ex = assertThrows(
				ProgramValidationException.class, () -> logic.validateAndFinalize(unit));
		assertTrue(ex.getMessage().contains("abstract"));
	}

	@Test
	public void validateAndFinalizeAcceptsAClassWithoutAbstractCodeMembers()
			throws ProgramValidationException {
		Program p = program();
		ClassUnit unit = new ClassUnit(LOC, p, "C", false);
		CFG cfg = new CFG(new CodeMemberDescriptor(LOC, unit, true, "m"));
		cfg.addNode(new Ret(cfg, LOC), true);
		unit.addInstanceCodeMember(cfg);

		new BaseValidationLogic().validateAndFinalize(unit);
	}

	@Test
	public void validateAndFinalizeRejectsAnInterfaceWithAnInstanceGlobal() {
		Program p = program();
		InterfaceUnit unit = new InterfaceUnit(LOC, p, "I", false);
		unit.addInstanceGlobal(new Global(LOC, unit, "g", true));

		BaseValidationLogic logic = new BaseValidationLogic();
		assertThrows(ProgramValidationException.class, () -> logic.validateAndFinalize(unit));
	}

	@Test
	public void validateAndFinalizeRejectsInheritanceFromASealedUnit() {
		Program p = program();
		ClassUnit sealedParent = new ClassUnit(LOC, p, "Sealed", true);
		ClassUnit child = new ClassUnit(LOC, p, "Child", false);
		child.addSuperclass(sealedParent);

		BaseValidationLogic logic = new BaseValidationLogic();
		ProgramValidationException ex = assertThrows(
				ProgramValidationException.class,
				() -> logic.validateAndFinalize((it.unive.lisa.program.CompilationUnit) child));
		assertTrue(ex.getMessage().contains("Sealed"));
	}

	// regression test: the "unknown entrypoints" diagnostic used to compute
	// entrypoints.retainAll(baseline) - the INTERSECTION with the program's
	// known cfgs - instead of the actual set difference, so the exception
	// reported the entrypoints that WERE valid rather than the ones that
	// were not part of the program
	@Test
	public void validateAndFinalizeReportsTheActuallyUnknownEntrypoints() {
		Program p = program();
		// mirrors the CFG owner/registration pattern used elsewhere in this
		// test suite (see BaseCallGraphTest): the cfg's owning unit must be
		// the same unit it is registered with, otherwise the earlier
		// per-member signature check (validate(member, false)) fails first
		CFG known = new CFG(new CodeMemberDescriptor(LOC, p, false, "known"));
		known.addNode(new Ret(known, LOC), true);
		p.addCodeMember(known);
		p.addEntryPoint(known);

		CFG unknown = new CFG(new CodeMemberDescriptor(LOC, p, false, "unknown"));
		unknown.addNode(new Ret(unknown, LOC), true);
		p.addEntryPoint(unknown);

		BaseValidationLogic logic = new BaseValidationLogic();
		ProgramValidationException ex = assertThrows(
				ProgramValidationException.class, () -> logic.validateAndFinalize(p));
		assertTrue(ex.getMessage().contains(unknown.toString()),
				"the message must name the actually unknown entrypoint");
		assertTrue(
				!ex.getMessage().contains(known.toString()),
				"the message must not report the entrypoint that IS part of the program");
	}

}
