package it.unive.lisa.analysis.dataflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.ReachingDefinitions.RD;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ReachingDefinitionsTest {

	private final Type intType = Int32Type.INSTANCE;

	private final CodeLocation loc1 = new SourceCodeLocation("fake", 1, 1);
	private final CodeLocation loc2 = new SourceCodeLocation("fake", 2, 2);
	private final CodeLocation loc3 = new SourceCodeLocation("fake", 3, 3);

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

	private final ProgramPoint pp3 = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return loc3;
		}

		@Override
		public CFG getCFG() {
			return TestParameterProvider.cfg;
		}
	};

	private final Variable x = new Variable(intType, "x", loc1);
	private final Variable y = new Variable(intType, "y", loc1);

	private final ReachingDefinitions domain = new ReachingDefinitions();

	private final PossibleSet<RD> emptyState = domain.makeLattice();

	private Constant c(
			int v,
			ProgramPoint at) {
		return new Constant(intType, v, at.getLocation());
	}

	@Test
	public void assigningAnIdentifierGeneratesADefinitionAtThatProgramPoint()
			throws SemanticException {
		Set<RD> gen = domain.gen(emptyState, x, c(1, pp1), pp1);
		assertEquals(1, gen.size());
	}

	@Test
	public void generatedDefinitionDoesNotDependOnTheAssignedExpression()
			throws SemanticException {
		Set<RD> genWithConstant = domain.gen(emptyState, x, c(1, pp1), pp1);
		Set<RD> genWithOtherConstant = domain.gen(emptyState, x, c(99, pp1), pp1);
		assertEquals(genWithConstant, genWithOtherConstant);
	}

	@Test
	public void nonAssigningEvaluationNeverGeneratesADefinition()
			throws SemanticException {
		assertEquals(Set.of(), domain.gen(emptyState, c(1, pp1), pp1));
	}

	@Test
	public void killingReassignedIdentifierRemovesDefinitionsRegardlessOfTheirOriginalProgramPoint()
			throws SemanticException {
		RD xAtPp1 = domain.gen(emptyState, x, c(1, pp1), pp1).iterator().next();
		RD yAtPp2 = domain.gen(emptyState, y, c(2, pp2), pp2).iterator().next();
		PossibleSet<RD> state = emptyState.update(Set.of(), Set.of(xAtPp1, yAtPp2));

		// x is redefined at pp3: its old definition (at pp1) must be killed,
		// no matter where it originally came from
		Set<RD> killed = domain.kill(state, x, c(3, pp3), pp3);
		assertEquals(Set.of(xAtPp1), killed);
	}

	@Test
	public void nonAssigningEvaluationNeverKillsAnything()
			throws SemanticException {
		RD xAtPp1 = domain.gen(emptyState, x, c(1, pp1), pp1).iterator().next();
		PossibleSet<RD> state = emptyState.update(Set.of(), Set.of(xAtPp1));

		assertEquals(Set.of(), domain.kill(state, c(1, pp1), pp1));
	}

	@Test
	public void twoDefinitionsOfTheSameIdentifierAtDifferentProgramPointsAreDistinct()
			throws SemanticException {
		// this matters for "may" semantics: after a branch merge, two
		// definitions of the same identifier coming from different program
		// points must both survive as separate reaching-definition facts
		RD xAtPp1 = domain.gen(emptyState, x, c(1, pp1), pp1).iterator().next();
		RD xAtPp2 = domain.gen(emptyState, x, c(1, pp2), pp2).iterator().next();
		assertTrue(!xAtPp1.equals(xAtPp2));
	}

	@Test
	public void twoDefinitionsOfTheSameIdentifierAtTheSameProgramPointAreEqual()
			throws SemanticException {
		RD first = domain.gen(emptyState, x, c(1, pp1), pp1).iterator().next();
		RD second = domain.gen(emptyState, x, c(99, pp1), pp1).iterator().next();
		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode());
	}

	@Test
	public void pushingThenPoppingTheSameScopeRestoresTheOriginalIdentifier()
			throws SemanticException {
		RD rd = domain.gen(emptyState, x, c(1, pp1), pp1).iterator().next();
		ScopeToken token = new ScopeToken(() -> loc1);

		RD pushed = rd.pushScope(token, pp1);
		assertTrue(!pushed.getInvolvedIdentifiers().contains(x));

		RD popped = pushed.popScope(token, pp1);
		assertEquals(rd, popped);
	}

	@Test
	public void sequentialAssignmentsAccumulateThenReplaceReachingDefinitions()
			throws SemanticException {
		// x = 1 (at pp1); y = 2 (at pp2); x = 3 (at pp3)
		PossibleSet<RD> state = emptyState;
		state = state.update(domain.kill(state, x, c(1, pp1), pp1), domain.gen(state, x, c(1, pp1), pp1));
		state = state.update(domain.kill(state, y, c(2, pp2), pp2), domain.gen(state, y, c(2, pp2), pp2));
		assertEquals(2, state.getDataflowElements().size());

		state = state.update(domain.kill(state, x, c(3, pp3), pp3), domain.gen(state, x, c(3, pp3), pp3));

		RD xAtPp3 = domain.gen(emptyState, x, c(3, pp3), pp3).iterator().next();
		RD yAtPp2 = domain.gen(emptyState, y, c(2, pp2), pp2).iterator().next();
		assertEquals(Set.of(xAtPp3, yAtPp2), state.getDataflowElements());
	}

	@Test
	public void mergingTwoBranchesUnionsTheirReachingDefinitions()
			throws SemanticException {
		// if (...) { x = 1 at pp1 } else { x = 1 at pp2 }: after the merge,
		// both definitions of x may reach the join point
		RD xAtPp1 = domain.gen(emptyState, x, c(1, pp1), pp1).iterator().next();
		RD xAtPp2 = domain.gen(emptyState, x, c(1, pp2), pp2).iterator().next();
		PossibleSet<RD> branch1 = emptyState.update(Set.of(), Set.of(xAtPp1));
		PossibleSet<RD> branch2 = emptyState.update(Set.of(), Set.of(xAtPp2));

		PossibleSet<RD> merged = branch1.lub(branch2);
		assertEquals(Set.of(xAtPp1, xAtPp2), merged.getDataflowElements());
	}

}
