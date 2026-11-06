package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.lattices.SingleValueLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class NoOpValuesTest {

	private static final ProgramPoint fake = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return null;
		}

		@Override
		public CFG getCFG() {
			return null;
		}

	};

	private final Variable x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final NoOpValues values = new NoOpValues();

	@Test
	public void testMakeLatticeYieldsTheSingleton() {
		assertSame(SingleValueLattice.SINGLETON, values.makeLattice());
	}

	@Test
	public void testAssignAndSmallStepAndAssumeLeaveStateUntouched()
			throws SemanticException {
		assertSame(SingleValueLattice.SINGLETON, values.assign(SingleValueLattice.SINGLETON, x, x, fake, null));
		assertSame(SingleValueLattice.SINGLETON,
				values.smallStepSemantics(SingleValueLattice.SINGLETON, x, fake, null));
		assertSame(SingleValueLattice.SINGLETON, values.assume(SingleValueLattice.SINGLETON, x, fake, fake, null));
	}

	@Test
	public void testCanProcessAlwaysReturnsTrue() {
		assertTrue(values.canProcess(x, fake, null));
	}

}
