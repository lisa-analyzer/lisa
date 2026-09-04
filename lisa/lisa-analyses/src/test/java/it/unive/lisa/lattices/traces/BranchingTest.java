package it.unive.lisa.lattices.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import org.junit.jupiter.api.Test;

public class BranchingTest {

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
	public void isTrueBranchReflectsTheConstructorArgument() {
		assertTrue(new Branching(pp1, true).isTrueBranch());
		assertFalse(new Branching(pp1, false).isTrueBranch());
	}

	@Test
	public void getProgramPointReflectsTheConstructorArgument() {
		assertEquals(pp1, new Branching(pp1, true).getProgramPoint());
	}

	@Test
	public void tokensWithSameProgramPointAndBranchAreEqual() {
		Branching a = new Branching(pp1, true);
		Branching b = new Branching(pp1, true);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void tokensWithDifferentBranchAreNotEqual() {
		assertNotEquals(new Branching(pp1, true), new Branching(pp1, false));
	}

	@Test
	public void tokensWithDifferentProgramPointAreNotEqual() {
		assertNotEquals(new Branching(pp1, true), new Branching(pp2, true));
	}

	@Test
	public void aBranchingIsNeverEqualToALoopToken() {
		// same program point, but a branch traversal and a loop traversal are
		// fundamentally different kinds of trace tokens
		assertNotEquals(new Branching(pp1, true), new LoopIteration(pp1, 0));
		assertNotEquals(new Branching(pp1, true), new LoopSummary(pp1));
	}

	@Test
	public void aTokenIsNotEqualToNullOrAForeignType() {
		Branching a = new Branching(pp1, true);
		assertNotEquals(a, null);
		assertNotEquals(a, "not a token");
	}

}
