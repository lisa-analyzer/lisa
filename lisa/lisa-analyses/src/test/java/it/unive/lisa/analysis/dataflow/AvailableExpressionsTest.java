package it.unive.lisa.analysis.dataflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.AvailableExpressions.AE;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Type;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AvailableExpressionsTest {

	private final Type intType = Int32Type.INSTANCE;

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

	private final Identifier x = new Variable(intType, "x", loc1);

	private final Identifier y = new Variable(intType, "y", loc1);

	private final Identifier z = new Variable(intType, "z", loc1);

	private final Identifier w = new Variable(intType, "w", loc1);

	private final AvailableExpressions ae = new AvailableExpressions();

	private final DefiniteSet<AE> empty = ae.makeLattice();

	private BinaryExpression sum(
			Identifier left,
			Identifier right) {
		return new BinaryExpression(intType, left, right, NumericNonOverflowingAdd.INSTANCE, loc1);
	}

	@Test
	public void testGenAssignmentOfNonTrivialExpressionIsAvailable()
			throws SemanticException {
		BinaryExpression expr = sum(x, y);
		Set<AE> assigning = ae.gen(empty, z, expr, pp1);
		Set<AE> nonAssigning = ae.gen(empty, expr, pp1);

		assertEquals(1, assigning.size());
		assertEquals(nonAssigning, assigning);
	}

	@Test
	public void testGenSelfReferentialAssignmentIsNotAvailable()
			throws SemanticException {
		// x = x + z: the computed expression involves the very variable being
		// reassigned, so it cannot be considered available right after the
		// assignment (its meaning changes as soon as x is overwritten).
		BinaryExpression expr = sum(x, z);
		Set<AE> gen = ae.gen(empty, x, expr, pp1);
		assertTrue(gen.isEmpty());
	}

	@Test
	public void testGenTrivialExpressionsAreNeverAvailable()
			throws SemanticException {
		assertTrue(ae.gen(empty, y, x, pp1).isEmpty());
		assertTrue(ae.gen(empty, y, new Constant(intType, 5, loc1), pp1).isEmpty());
		assertTrue(ae.gen(empty, y, new Skip(loc1), pp1).isEmpty());
		assertTrue(ae.gen(empty, y, new PushAny(intType, loc1), pp1).isEmpty());
	}

	@Test
	public void testGenNonAssigningExpressionIsAvailable()
			throws SemanticException {
		BinaryExpression expr = sum(x, y);
		Set<AE> gen = ae.gen(empty, expr, pp1);
		assertEquals(1, gen.size());
		assertEquals(new HashSet<>(java.util.Arrays.asList(x, y)), gen.iterator().next().getInvolvedIdentifiers());
	}

	@Test
	public void testKillRemovesOnlyExpressionsInvolvingReassignedVariable()
			throws SemanticException {
		Set<AE> xy = ae.gen(empty, sum(x, y), pp1);
		Set<AE> yz = ae.gen(empty, sum(y, z), pp1);
		Set<AE> both = new HashSet<>(xy);
		both.addAll(yz);
		DefiniteSet<AE> state = new DefiniteSet<>(both);

		assertEquals(xy, ae.kill(state, x, new Constant(intType, 1, loc2), pp2));
		assertEquals(both, ae.kill(state, y, new Constant(intType, 1, loc2), pp2));
		assertTrue(ae.kill(state, w, new Constant(intType, 1, loc2), pp2).isEmpty());
	}

	@Test
	public void testKillOnNonAssigningExpressionNeverKillsAnything()
			throws SemanticException {
		Set<AE> xy = ae.gen(empty, sum(x, y), pp1);
		DefiniteSet<AE> state = new DefiniteSet<>(xy);
		assertTrue(ae.kill(state, sum(x, y), pp2).isEmpty());
	}

	@Test
	public void testAvailableExpressionSurvivesUnrelatedAssignment()
			throws SemanticException {
		Set<AE> xy = ae.gen(empty, sum(x, y), pp1);
		DefiniteSet<AE> state = new DefiniteSet<>(xy);

		Constant five = new Constant(intType, 5, loc2);
		Set<AE> killed = ae.kill(state, w, five, pp2);
		Set<AE> generated = ae.gen(state, w, five, pp2);
		DefiniteSet<AE> after = state.update(killed, generated);

		assertEquals(state.getDataflowElements(), after.getDataflowElements());
	}

	@Test
	public void testAvailableExpressionIsInvalidatedByReassignment()
			throws SemanticException {
		Set<AE> xy = ae.gen(empty, sum(x, y), pp1);
		DefiniteSet<AE> state = new DefiniteSet<>(xy);

		Constant five = new Constant(intType, 5, loc2);
		Set<AE> killed = ae.kill(state, x, five, pp2);
		Set<AE> generated = ae.gen(state, x, five, pp2);
		DefiniteSet<AE> after = state.update(killed, generated);

		assertTrue(after.getDataflowElements().isEmpty());
	}

	@Test
	public void testInvolvedIdentifiersIgnoreConstants()
			throws SemanticException {
		BinaryExpression expr = new BinaryExpression(
				intType,
				x,
				new Constant(intType, 5, loc1),
				NumericNonOverflowingAdd.INSTANCE,
				loc1);
		Set<AE> gen = ae.gen(empty, expr, pp1);
		assertEquals(1, gen.size());
		assertEquals(java.util.Collections.singleton(x), gen.iterator().next().getInvolvedIdentifiers());
	}

	@Test
	public void testPushScopeThenPopScopeRoundTrips()
			throws SemanticException {
		ScopeToken token = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				return loc1;
			}
		});

		Set<AE> gen = ae.gen(empty, sum(x, y), pp1);
		AE elem = gen.iterator().next();

		AE pushed = elem.pushScope(token, pp1);
		AE popped = pushed.popScope(token, pp1);

		assertNotEquals(elem, pushed);
		assertEquals(elem, popped);
	}

}
