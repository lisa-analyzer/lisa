package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.lattices.SingleTypeLattice;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NoOpTypesTest {

	private static final ClassUnit unit = new ClassUnit(
			new SourceCodeLocation("unknown", 0, 0),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"Testing",
			false);

	private static Statement mkStatement() {
		SourceCodeLocation loc = new SourceCodeLocation("unknown", 1, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(loc, unit, false, "m"));
		return new Ret(cfg, loc);
	}

	private final Variable x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final NoOpTypes types = new NoOpTypes();

	@Test
	public void testMakeLatticeYieldsTheSingleton() {
		assertSame(SingleTypeLattice.SINGLETON, types.makeLattice());
	}

	@Test
	public void testAssignAndSmallStepAndAssumeLeaveStateUntouched()
			throws SemanticException {
		Statement st = mkStatement();
		assertSame(SingleTypeLattice.SINGLETON, types.assign(SingleTypeLattice.SINGLETON, x, x, st, null));
		assertSame(SingleTypeLattice.SINGLETON, types.smallStepSemantics(SingleTypeLattice.SINGLETON, x, st, null));
		assertSame(SingleTypeLattice.SINGLETON, types.assume(SingleTypeLattice.SINGLETON, x, st, st, null));
	}

	@Test
	public void testGetRuntimeTypesOfReturnsEveryTypeRegisteredInTheProgram()
			throws SemanticException {
		Statement st = mkStatement();
		TestTypeSystem typeSystem = (TestTypeSystem) unit.getProgram().getTypes();
		Type registered = typeSystem.getBooleanType();
		typeSystem.registerType(registered);

		Set<Type> runtimeTypes = types.getRuntimeTypesOf(SingleTypeLattice.SINGLETON, x, st, null);

		assertTrue(runtimeTypes.contains(registered));
		assertEquals(typeSystem.getTypes(), runtimeTypes);
	}

	@Test
	public void testGetDynamicTypeOfIsAlwaysUntyped()
			throws SemanticException {
		Statement st = mkStatement();
		assertSame(Untyped.INSTANCE, types.getDynamicTypeOf(SingleTypeLattice.SINGLETON, x, st, null));
	}

}
