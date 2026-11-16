package it.unive.lisa.lattices.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import org.junit.jupiter.api.Test;

public class TraceTokenTest {

	private final CodeLocation loc1 = new SourceCodeLocation("fake", 1, 1);

	private final CodeLocation loc2 = new SourceCodeLocation("fake", 2, 2);

	private final ProgramPoint pp1 = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return loc1;
		}

		@Override
		public CFG getCFG() {
			return TestParameterProvider.cfg;
		}
	};

	private final ProgramPoint pp2 = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return loc2;
		}

		@Override
		public CFG getCFG() {
			return TestParameterProvider.cfg;
		}
	};

	private static class MinimalToken
			extends
			TraceToken {

		MinimalToken(
				ProgramPoint pp) {
			super(pp);
		}
	}

	private static class OtherMinimalToken
			extends
			TraceToken {

		OtherMinimalToken(
				ProgramPoint pp) {
			super(pp);
		}
	}

	@Test
	public void getProgramPointReflectsTheConstructorArgument() {
		assertEquals(pp1, new MinimalToken(pp1).getProgramPoint());
	}

	@Test
	public void toStringDelegatesToTheProgramPointWhenNotOverridden() {
		// TraceToken's own toString (unlike
		// Branching/LoopIteration/LoopSummary,
		// which decorate it) is exactly the program point's toString
		assertEquals(pp1.toString(), new MinimalToken(pp1).toString());
	}

	@Test
	public void tokensOfTheSameConcreteClassWithTheSameProgramPointAreEqual() {
		MinimalToken a = new MinimalToken(pp1);
		MinimalToken b = new MinimalToken(pp1);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void tokensWithDifferentProgramPointsAreNotEqual() {
		assertNotEquals(new MinimalToken(pp1), new MinimalToken(pp2));
	}

	@Test
	public void tokensOfDifferentConcreteClassesAreNeverEqualEvenWithTheSameProgramPoint() {
		// the equals() contract explicitly checks getClass(), so two distinct
		// TraceToken subclasses sharing the same program point must not be
		// considered equal to one another
		assertNotEquals(new MinimalToken(pp1), new OtherMinimalToken(pp1));
	}

	@Test
	public void aTokenIsNotEqualToNullOrAForeignType() {
		MinimalToken a = new MinimalToken(pp1);
		assertNotEquals(a, null);
		assertNotEquals(a, "not a token");
	}

	@Test
	public void aTokenIsEqualToItself() {
		MinimalToken a = new MinimalToken(pp1);
		assertEquals(a, a);
	}

}
