package it.unive.lisa.lattices.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import org.junit.jupiter.api.Test;

public class LoopIterationTest {

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
	public void getIterationReflectsTheConstructorArgument() {
		assertEquals(0, new LoopIteration(pp1, 0).getIteration());
		assertEquals(7, new LoopIteration(pp1, 7).getIteration());
	}

	@Test
	public void getProgramPointReflectsTheConstructorArgument() {
		assertEquals(pp1, new LoopIteration(pp1, 0).getProgramPoint());
	}

	@Test
	public void tokensWithSameProgramPointAndIterationAreEqual() {
		LoopIteration a = new LoopIteration(pp1, 2);
		LoopIteration b = new LoopIteration(pp1, 2);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void tokensWithDifferentIterationAreNotEqual() {
		// distinct iterations of the same loop guard must be tracked as
		// distinct traces, otherwise per-iteration precision is lost
		assertNotEquals(new LoopIteration(pp1, 0), new LoopIteration(pp1, 1));
	}

	@Test
	public void tokensWithDifferentProgramPointAreNotEqual() {
		assertNotEquals(new LoopIteration(pp1, 0), new LoopIteration(pp2, 0));
	}

	@Test
	public void aLoopIterationIsNeverEqualToABranchingOrASummary() {
		assertNotEquals(new LoopIteration(pp1, 0), new Branching(pp1, true));
		assertNotEquals(new LoopIteration(pp1, 0), new LoopSummary(pp1));
	}

}
