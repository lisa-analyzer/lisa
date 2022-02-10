package it.unive.lisa.program;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.program.cfg.SignatureCFG;
import org.junit.Test;

public class HieararchyComputationTest {

	private static Unit findUnit(Program prog, String name) {
		Unit unit = prog.getUnits().stream().filter(u -> u.getName().equals(name)).findFirst().get();
		assertNotNull("'" + name + "' unit not found", unit);
		return unit;
	}

	private static ImplementedCFG findCFG(CompilationUnit unit, String name) {
		ImplementedCFG cfg = unit.getInstanceCFGs(false).stream().filter(c -> c.getDescriptor().getName().equals(name))
				.findFirst()
				.get();
		assertNotNull("'" + unit.getName() + "' unit does not contain cfg '" + name + "'", cfg);
		return cfg;
	}

	private static CFG findCFG(InterfaceUnit unit, String name) {
		CFG cfg = unit.getInstanceCFGs(false).stream().filter(c -> c.getDescriptor().getName().equals(name))
				.findFirst()
				.get();
		assertNotNull("'" + unit.getName() + "' unit does not contain cfg '" + name + "'", cfg);
		return cfg;
	}

	private static void isInstance(UnitWithSuperUnits sup, Unit unit) {
		assertTrue("'" + unit.getName() + "' is not among '" + sup.getName() + "' instances",
				sup.getInstances().contains(unit));
		if (sup != unit) {
			if (unit instanceof CompilationUnit)
				assertTrue("'" + sup.getName() + "' is not among '" + unit.getName() + "' superunits",
						((CompilationUnit) unit).isInstanceOf(sup));
			else
				assertTrue("'" + sup.getName() + "' is not among '" + unit.getName() + "' superunits",
						((InterfaceUnit) unit).isInstanceOf((InterfaceUnit) sup));
		}
	}

	private static void notInstance(CompilationUnit sup, CompilationUnit unit) {
		assertFalse("'" + unit.getName() + "' is among '" + sup.getName() + "' instances",
				sup.getInstances().contains(unit));
		if (sup != unit)
			assertFalse("'" + sup.getName() + "' is among '" + unit.getName() + "' superunits",
					unit.getSuperUnits().contains(sup));
	}

	private static void overrides(CFG sup, CFG cfg) {
		assertTrue(
				"'" + sup.getDescriptor().getFullName() + "' is not overridden by '"
						+ cfg.getDescriptor().getFullName() + "'",
				sup.getDescriptor().overriddenBy().contains(cfg));
		assertTrue(
				"'" + sup.getDescriptor().getFullName() + "' does not override '" + cfg.getDescriptor().getFullName()
						+ "'",
				cfg.getDescriptor().overrides().contains(sup));
	}

	private static void notOverrides(ImplementedCFG sup, ImplementedCFG cfg) {
		assertFalse(
				"'" + sup.getDescriptor().getFullName() + "' is overridden by '"
						+ cfg.getDescriptor().getFullName() + "'",
				sup.getDescriptor().overriddenBy().contains(cfg));
		assertFalse(
				"'" + sup.getDescriptor().getFullName() + "' overrides '" + cfg.getDescriptor().getFullName()
						+ "'",
				cfg.getDescriptor().overrides().contains(sup));
	}

	@Test
	public void testSingle() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/single.imp", false);
		prog.validateAndFinalize();
		// we just check that no exception is thrown
	}

	@Test
	public void testSimpleInheritance() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/simple-inheritance.imp", false);
		prog.validateAndFinalize();

		CompilationUnit first = (CompilationUnit) findUnit(prog, "first");
		CompilationUnit second = (CompilationUnit) findUnit(prog, "second");

		isInstance(first, first);
		isInstance(second, second);
		isInstance(first, second);
		notInstance(second, first);

		ImplementedCFG fooFirst = findCFG(first, "foo");
		ImplementedCFG fooSecond = findCFG(second, "foo");

		overrides(fooFirst, fooSecond);
	}

	@Test(expected = ProgramValidationException.class)
	public void testFinalCfg() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/final-cfg.imp", false);
		prog.validateAndFinalize();
	}

	@Test(expected = ProgramValidationException.class)
	public void testTree() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/tree.imp", false);
		prog.validateAndFinalize();
	}

	@Test
	public void testTreeSanitized() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/tree-sanitized.imp", false);
		prog.validateAndFinalize();

		CompilationUnit first = (CompilationUnit) findUnit(prog, "first");
		CompilationUnit second = (CompilationUnit) findUnit(prog, "second");
		CompilationUnit third = (CompilationUnit) findUnit(prog, "third");
		CompilationUnit fourth = (CompilationUnit) findUnit(prog, "fourth");
		CompilationUnit fifth = (CompilationUnit) findUnit(prog, "fifth");
		CompilationUnit sixth = (CompilationUnit) findUnit(prog, "sixth");

		isInstance(first, first);
		isInstance(second, second);
		isInstance(third, third);
		isInstance(fourth, fourth);
		isInstance(fifth, fifth);
		isInstance(sixth, sixth);

		isInstance(first, second);
		isInstance(first, third);
		isInstance(first, fourth);
		isInstance(first, fifth);
		isInstance(first, sixth);
		notInstance(second, first);
		notInstance(third, first);
		notInstance(fourth, first);
		notInstance(fifth, first);
		notInstance(sixth, first);

		isInstance(second, fourth);
		isInstance(second, fifth);
		notInstance(fourth, second);
		notInstance(fifth, second);

		notInstance(fourth, fifth);
		notInstance(fifth, fourth);

		isInstance(third, sixth);
		notInstance(sixth, third);
		notInstance(third, second);
		notInstance(third, fourth);
		notInstance(third, fifth);
		notInstance(second, third);
		notInstance(fourth, third);
		notInstance(fifth, third);

		ImplementedCFG fooFirst = findCFG(first, "foo");
		ImplementedCFG fooSecond = findCFG(second, "foo");
		ImplementedCFG fooThird = findCFG(third, "foo");
		ImplementedCFG fooFourth = findCFG(fourth, "foo");
		ImplementedCFG fooFifth = findCFG(fifth, "foo");

		overrides(fooFirst, fooSecond);
		overrides(fooFirst, fooThird);
		overrides(fooFirst, fooFourth);
		overrides(fooFirst, fooFifth);
		overrides(fooSecond, fooFourth);
		overrides(fooSecond, fooFifth);
		notOverrides(fooFourth, fooFifth);
		notOverrides(fooFifth, fooFourth);
	}

	@Test
	public void testSkipOne() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/skip-one.imp", false);
		prog.validateAndFinalize();

		CompilationUnit first = (CompilationUnit) findUnit(prog, "first");
		CompilationUnit second = (CompilationUnit) findUnit(prog, "second");
		CompilationUnit third = (CompilationUnit) findUnit(prog, "third");

		isInstance(first, first);
		isInstance(second, second);
		isInstance(third, third);
		isInstance(first, third);
		isInstance(second, third);

		ImplementedCFG fooFirst = findCFG(first, "foo");
		ImplementedCFG fooThird = findCFG(third, "foo");

		overrides(fooFirst, fooThird);
	}

	@Test
	public void testSimpleInterfaces() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/simple-interfaces.imp", false);
		prog.validateAndFinalize();

		CompilationUnit first = (CompilationUnit) findUnit(prog, "first");
		InterfaceUnit i = (InterfaceUnit) findUnit(prog, "i");
		InterfaceUnit j = (InterfaceUnit) findUnit(prog, "j");

		findCFG(first, "foo");
		ImplementedCFG aFirst = findCFG(first, "a");
		ImplementedCFG bFirst = findCFG(first, "b");
		ImplementedCFG cFirst = findCFG(first, "c");

		SignatureCFG aJ = (SignatureCFG) findCFG(j, "a");
		SignatureCFG bI = (SignatureCFG) findCFG(i, "b");
		SignatureCFG cI = (SignatureCFG) findCFG(i, "c");

		overrides(aJ, aFirst);
		overrides(bI, bFirst);
		overrides(cI, cFirst);
	}

	@Test
	public void testMultiInterfaces() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/multi-interfaces.imp", false);
		prog.validateAndFinalize();

		CompilationUnit first = (CompilationUnit) findUnit(prog, "first");
		InterfaceUnit i = (InterfaceUnit) findUnit(prog, "i");
		InterfaceUnit j = (InterfaceUnit) findUnit(prog, "j");
		InterfaceUnit k = (InterfaceUnit) findUnit(prog, "k");

		findCFG(first, "foo");
		ImplementedCFG aFirst = findCFG(first, "a");
		ImplementedCFG bFirst = findCFG(first, "b");
		ImplementedCFG cFirst = findCFG(first, "c");

		SignatureCFG aI = (SignatureCFG) findCFG(i, "a");
		SignatureCFG bJ = (SignatureCFG) findCFG(j, "b");
		SignatureCFG cK = (SignatureCFG) findCFG(k, "c");

		overrides(aI, aFirst);
		overrides(bJ, bFirst);
		overrides(cK, cFirst);

		isInstance(i, first);
		isInstance(j, first);
		isInstance(k, first);
	}

	@Test
	public void testDefaultMethodsInterface() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/default-methods-interface.imp",
				false);
		prog.validateAndFinalize();

		CompilationUnit first = (CompilationUnit) findUnit(prog, "first");
		InterfaceUnit i = (InterfaceUnit) findUnit(prog, "i");
		InterfaceUnit j = (InterfaceUnit) findUnit(prog, "j");

		findCFG(j, "a");
		ImplementedCFG bFirst = findCFG(first, "b");
		ImplementedCFG cFirst = findCFG(first, "c");

		SignatureCFG bI = (SignatureCFG) findCFG(i, "b");
		SignatureCFG cI = (SignatureCFG) findCFG(i, "c");
		findCFG(i, "d");

		overrides(bI, bFirst);
		overrides(cI, cFirst);

		isInstance(i, first);
		isInstance(j, i);
		isInstance(j, first);
	}

	@Test(expected = ProgramValidationException.class)
	public void testInterfaces() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/interfaces.imp", false);
		prog.validateAndFinalize();
	}

	@Test
	public void testOverridingDefaultMethods() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/overriding-default-method.imp",
				false);
		prog.validateAndFinalize();

		CompilationUnit first = (CompilationUnit) findUnit(prog, "first");
		InterfaceUnit i = (InterfaceUnit) findUnit(prog, "i");
		InterfaceUnit j = (InterfaceUnit) findUnit(prog, "j");

		findCFG(j, "a");
		ImplementedCFG bJ = (ImplementedCFG) findCFG(j, "b");

		SignatureCFG cI = (SignatureCFG) findCFG(i, "c");
		findCFG(i, "d");

		ImplementedCFG bFirst = findCFG(first, "b");
		ImplementedCFG cFirst = findCFG(first, "c");

		overrides(bJ, bFirst);
		overrides(cI, cFirst);

		isInstance(i, first);
		isInstance(j, i);
		isInstance(j, first);
	}
}
