package it.unive.lisa.lattices.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import org.junit.jupiter.api.Test;

public class ExecutionTraceTest {

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
	public void pushAddsTheTokenAsTheNewHead() {
		ExecutionTrace trace = ExecutionTrace.EMPTY.push(new Branching(pp1, true));
		assertEquals(new Branching(pp1, true), trace.getHead());
	}

	@Test
	public void pushingSeveralTokensKeepsTheLastOneAsHead() {
		ExecutionTrace trace = ExecutionTrace.EMPTY
				.push(new Branching(pp1, true))
				.push(new LoopIteration(pp2, 0));
		assertEquals(new LoopIteration(pp2, 0), trace.getHead());
	}

	@Test
	public void popRemovesTheHeadAndUncoversThePreviousOne() {
		ExecutionTrace trace = ExecutionTrace.EMPTY
				.push(new Branching(pp1, true))
				.push(new LoopIteration(pp2, 0));
		ExecutionTrace popped = trace.pop();
		assertEquals(new Branching(pp1, true), popped.getHead());
	}

	@Test
	public void popOnAnEmptyTraceIsANoOp() {
		assertEquals(ExecutionTrace.EMPTY, ExecutionTrace.EMPTY.pop());
	}

	@Test
	public void pushDoesNotMutateTheOriginalTrace() {
		ExecutionTrace original = ExecutionTrace.EMPTY.push(new Branching(pp1, true));
		ExecutionTrace extended = original.push(new LoopIteration(pp2, 0));
		assertEquals(new Branching(pp1, true), original.getHead());
		assertEquals(new LoopIteration(pp2, 0), extended.getHead());
	}

	@Test
	public void numberOfBranchesCountsOnlyBranchingTokens() {
		ExecutionTrace trace = ExecutionTrace.EMPTY
				.push(new Branching(pp1, true))
				.push(new LoopIteration(pp2, 0))
				.push(new Branching(pp2, false))
				.push(new LoopSummary(pp1));
		assertEquals(2, trace.numberOfBranches());
	}

	@Test
	public void numberOfBranchesOnAnEmptyTraceIsZero() {
		assertEquals(0, ExecutionTrace.EMPTY.numberOfBranches());
	}

	@Test
	public void lastLoopTokenForFindsTheTopMostMatchingLoopToken() {
		ExecutionTrace trace = ExecutionTrace.EMPTY
				.push(new LoopIteration(pp1, 0))
				.push(new LoopIteration(pp1, 1))
				.push(new LoopIteration(pp1, 2));
		assertEquals(new LoopIteration(pp1, 2), trace.lastLoopTokenFor(pp1));
	}

	@Test
	public void lastLoopTokenForIgnoresBranchingTokensForTheSameGuard() {
		// Branching and loop tokens are unrelated kinds of guards: a
		// Branching token must never be returned by a lookup for loop tokens,
		// even if it happens to share the same program point
		ExecutionTrace trace = ExecutionTrace.EMPTY
				.push(new LoopIteration(pp1, 0))
				.push(new Branching(pp1, true));
		assertEquals(new LoopIteration(pp1, 0), trace.lastLoopTokenFor(pp1));
	}

	@Test
	public void lastLoopTokenForReturnsNullWhenNoTokenMatchesTheGuard() {
		ExecutionTrace trace = ExecutionTrace.EMPTY.push(new LoopIteration(pp2, 0));
		assertNull(trace.lastLoopTokenFor(pp1));
	}

	@Test
	public void lastLoopTokenForFindsALoopSummary() {
		ExecutionTrace trace = ExecutionTrace.EMPTY
				.push(new LoopIteration(pp1, 0))
				.push(new LoopSummary(pp1));
		assertEquals(new LoopSummary(pp1), trace.lastLoopTokenFor(pp1));
	}

	@Test
	public void tracesWithTheSameTokenSequenceAreEqual() {
		ExecutionTrace a = ExecutionTrace.EMPTY.push(new Branching(pp1, true)).push(new LoopIteration(pp2, 0));
		ExecutionTrace b = ExecutionTrace.EMPTY.push(new Branching(pp1, true)).push(new LoopIteration(pp2, 0));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void tracesWithTokensInADifferentOrderAreNotEqual() {
		ExecutionTrace a = ExecutionTrace.EMPTY.push(new Branching(pp1, true)).push(new LoopIteration(pp2, 0));
		ExecutionTrace b = ExecutionTrace.EMPTY.push(new LoopIteration(pp2, 0)).push(new Branching(pp1, true));
		assertNotEquals(a, b);
	}

	@Test
	public void anEmptyTraceIsOnlyEqualToAnotherEmptyTrace() {
		ExecutionTrace nonEmpty = ExecutionTrace.EMPTY.push(new Branching(pp1, true));
		assertNotEquals(ExecutionTrace.EMPTY, nonEmpty);
		assertEquals(ExecutionTrace.EMPTY, ExecutionTrace.EMPTY.push(new Branching(pp1, true)).pop());
	}

}
