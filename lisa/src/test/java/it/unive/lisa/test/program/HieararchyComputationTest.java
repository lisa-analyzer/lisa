package it.unive.lisa.test.program;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;
import org.junit.Test;

public class HieararchyComputationTest {

	private static CompilationUnit findUnit(Program prog, String name) {
		CompilationUnit unit = prog.getUnits().stream().filter(u -> u.getName().equals(name)).findFirst().get();
		assertNotNull("'" + name + "' unit not found", unit);
		return unit;
	}

	private static CFG findCFG(CompilationUnit unit, String name) {
		CFG cfg = unit.getInstanceCFGs(false).stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst()
				.get();
		assertNotNull("'" + unit.getName() + "' unit does not contain cfg '" + name + "'", cfg);
		return cfg;
	}

	private static void isInstance(CompilationUnit sup, CompilationUnit unit) {
		assertTrue("'" + unit.getName() + "' is not among '" + sup.getName() + "' instances",
				sup.getInstances().contains(unit));
		if (sup != unit)
			assertTrue("'" + sup.getName() + "' is not among '" + unit.getName() + "' superunits",
					unit.isInstanceOf(sup));
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

	private static void notOverrides(CFG sup, CFG cfg) {
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
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/single.imp");
		prog.validateAndFinalize();
		// we just check that no exception is thrown
	}

	@Test
	public void testSimpleInheritance() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/simple-inheritance.imp");
		prog.validateAndFinalize();

		CompilationUnit first = findUnit(prog, "first");
		CompilationUnit second = findUnit(prog, "second");

		isInstance(first, first);
		isInstance(second, second);
		isInstance(first, second);
		notInstance(second, first);

		CFG fooFirst = findCFG(first, "foo");
		CFG fooSecond = findCFG(second, "foo");

		overrides(fooFirst, fooSecond);
	}

	@Test(expected = ProgramValidationException.class)
	public void testFinalCfg() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/final-cfg.imp");
		prog.validateAndFinalize();
	}

	@Test(expected = ProgramValidationException.class)
	public void testTree() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/tree.imp");
		prog.validateAndFinalize();
	}

	@Test
	public void testTreeSanitized() throws ParsingException, ProgramValidationException {
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/tree-sanitized.imp");
		prog.validateAndFinalize();

		CompilationUnit first = findUnit(prog, "first");
		CompilationUnit second = findUnit(prog, "second");
		CompilationUnit third = findUnit(prog, "third");
		CompilationUnit fourth = findUnit(prog, "fourth");
		CompilationUnit fifth = findUnit(prog, "fifth");
		CompilationUnit sixth = findUnit(prog, "sixth");

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

		CFG fooFirst = findCFG(first, "foo");
		CFG fooSecond = findCFG(second, "foo");
		CFG fooThird = findCFG(third, "foo");
		CFG fooFourth = findCFG(fourth, "foo");
		CFG fooFifth = findCFG(fifth, "foo");

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
		Program prog = IMPFrontend.processFile("imp-testcases/program-finalization/skip-one.imp");
		prog.validateAndFinalize();

		CompilationUnit first = findUnit(prog, "first");
		CompilationUnit second = findUnit(prog, "second");
		CompilationUnit third = findUnit(prog, "third");

		isInstance(first, first);
		isInstance(second, second);
		isInstance(third, third);
		isInstance(first, third);
		isInstance(second, third);

		CFG fooFirst = findCFG(first, "foo");
		CFG fooThird = findCFG(third, "foo");

		overrides(fooFirst, fooThird);
	}
}
