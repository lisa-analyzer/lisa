package it.unive.lisa.lattices.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import org.junit.jupiter.api.Test;

public class LoopSummaryTest {

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

	@Test
	public void getProgramPointReflectsTheConstructorArgument() {
		assertEquals(pp1, new LoopSummary(pp1).getProgramPoint());
	}

	@Test
	public void summariesForTheSameProgramPointAreEqual() {
		// a LoopSummary carries no other state than the guard it summarizes,
		// so any two summaries of the same guard represent the same token
		LoopSummary a = new LoopSummary(pp1);
		LoopSummary b = new LoopSummary(pp1);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void summariesForDifferentProgramPointsAreNotEqual() {
		assertNotEquals(new LoopSummary(pp1), new LoopSummary(pp2));
	}

	@Test
	public void aSummaryIsNeverEqualToABranchingOrALoopIteration() {
		assertNotEquals(new LoopSummary(pp1), new Branching(pp1, true));
		assertNotEquals(new LoopSummary(pp1), new LoopIteration(pp1, 0));
	}

}
