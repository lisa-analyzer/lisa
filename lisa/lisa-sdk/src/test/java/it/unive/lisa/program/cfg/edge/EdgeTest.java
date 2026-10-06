package it.unive.lisa.program.cfg.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class EdgeTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG cfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "u", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	// ---- equals/hashCode/compareTo/isUnconditional/isErrorHandling ----

	@Test
	public void differentEdgeSubtypesWithTheSameEndpointsAreNotEqual() {
		CFG cfg = cfg();
		Statement a = new NoOp(cfg, LOC);
		Statement b = new NoOp(cfg, LOC);

		SequentialEdge seq = new SequentialEdge(a, b);
		TrueEdge tru = new TrueEdge(a, b);

		assertFalse(seq.equals(tru));
		assertFalse(tru.equals(seq));
	}

	@Test
	public void sameSubtypeAndEndpointsAreEqual() {
		CFG cfg = cfg();
		Statement a = new NoOp(cfg, LOC);
		Statement b = new NoOp(cfg, LOC);

		SequentialEdge first = new SequentialEdge(a, b);
		SequentialEdge second = new SequentialEdge(a, b);

		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode());
	}

	@Test
	public void newInstanceProducesTheSameConcreteSubtype() {
		CFG cfg = cfg();
		Statement a = new NoOp(cfg, LOC);
		Statement b = new NoOp(cfg, LOC);
		Statement c = new NoOp(cfg, LOC);

		TrueEdge original = new TrueEdge(a, b);
		Edge moved = original.newInstance(a, c);

		assertEquals(TrueEdge.class, moved.getClass());
		assertSame(a, moved.getSource());
		assertSame(c, moved.getDestination());
	}

	@Test
	public void onlySequentialEdgesAreUnconditional() {
		CFG cfg = cfg();
		Statement a = new NoOp(cfg, LOC);
		Statement b = new NoOp(cfg, LOC);

		assertTrue(new SequentialEdge(a, b).isUnconditional());
		assertFalse(new TrueEdge(a, b).isUnconditional());
		assertFalse(new FalseEdge(a, b).isUnconditional());
	}

	@Test
	public void onlyErrorEdgesAreErrorHandling() {
		CFG cfg = cfg();
		Statement a = new NoOp(cfg, LOC);
		Statement b = new NoOp(cfg, LOC);

		assertFalse(new SequentialEdge(a, b).isErrorHandling());
		assertFalse(new TrueEdge(a, b).isErrorHandling());
		assertFalse(new FalseEdge(a, b).isErrorHandling());
	}

	// ---- TrueEdge/FalseEdge assumption semantics ----

	/**
	 * A fake {@link AbstractDomain} that just records the expression it was
	 * asked to
	 * {@link #assume(RecordingState, SymbolicExpression, ProgramPoint, ProgramPoint)}
	 * without changing the state, so that tests can check exactly what
	 * expression {@link TrueEdge}/{@link FalseEdge} pass down.
	 */
	private static class RecordingDomain
			implements
			AbstractDomain<RecordingState> {

		SymbolicExpression lastAssumed;

		@Override
		public RecordingState assign(
				RecordingState state,
				Identifier id,
				SymbolicExpression expression,
				ProgramPoint pp) {
			return state;
		}

		@Override
		public RecordingState smallStepSemantics(
				RecordingState state,
				SymbolicExpression expression,
				ProgramPoint pp) {
			return state;
		}

		@Override
		public RecordingState assume(
				RecordingState state,
				SymbolicExpression expression,
				ProgramPoint src,
				ProgramPoint dest) {
			this.lastAssumed = expression;
			return state;
		}

		@Override
		public SemanticOracle makeOracle(
				RecordingState state) {
			return null;
		}

		@Override
		public RecordingState makeLattice() {
			return new RecordingState();
		}

		@Override
		public RecordingState onCallReturn(
				RecordingState entryState,
				RecordingState callres,
				ProgramPoint call) {
			return callres;
		}

		@Override
		public void setEventQueue(
				EventQueue queue) {
		}

	}

	private static class RecordingState
			implements
			it.unive.lisa.analysis.AbstractLattice<RecordingState> {

		@Override
		public it.unive.lisa.util.representation.StructuredRepresentation representation() {
			return new it.unive.lisa.util.representation.StringRepresentation("state");
		}

		@Override
		public RecordingState withTopMemory() {
			return this;
		}

		@Override
		public RecordingState withTopValues() {
			return this;
		}

		@Override
		public RecordingState withTopTypes() {
			return this;
		}

		@Override
		public boolean knowsIdentifier(
				Identifier id) {
			return false;
		}

		@Override
		public RecordingState forgetIdentifier(
				Identifier id,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public RecordingState forgetIdentifiersIf(
				java.util.function.Predicate<Identifier> test,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public RecordingState forgetIdentifiers(
				Iterable<Identifier> ids,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public boolean lessOrEqual(
				RecordingState other) {
			return true;
		}

		@Override
		public RecordingState lub(
				RecordingState other) {
			return this;
		}

		@Override
		public RecordingState upchain(
				RecordingState other) {
			return this;
		}

		@Override
		public RecordingState downchain(
				RecordingState other) {
			return this;
		}

		@Override
		public RecordingState top() {
			return this;
		}

		@Override
		public RecordingState bottom() {
			return this;
		}

		@Override
		public RecordingState pushScope(
				it.unive.lisa.analysis.ScopeToken token,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public RecordingState popScope(
				it.unive.lisa.analysis.ScopeToken token,
				ProgramPoint pp) {
			return this;
		}

	}

	private static AnalysisState<RecordingState> stateWithExpression(
			SymbolicExpression expr) {
		ProgramState<RecordingState> programState = new ProgramState<>(new RecordingState(), new ExpressionSet(expr));
		// the AnalysisState(ProgramState) constructor replaces the given
		// lattice with its own top() (see AnalysisState's javadoc/ctor), so
		// the execution state must be set afterwards via withExecution() to
		// actually carry the given expression
		return new AnalysisState<>(programState).withExecution(programState);
	}

	@Test
	public void trueEdgeAssumesTheExpressionAsIs() throws SemanticException {
		CFG cfg = cfg();
		Statement a = new NoOp(cfg, LOC);
		Statement b = new NoOp(cfg, LOC);
		TrueEdge edge = new TrueEdge(a, b);

		PushAny expr = new PushAny(Untyped.INSTANCE, LOC);
		RecordingDomain domain = new RecordingDomain();
		Analysis<RecordingState, RecordingDomain> analysis = new Analysis<>(domain);

		edge.traverseForward(stateWithExpression(expr), analysis);

		assertSame(expr, domain.lastAssumed);
	}

	@Test
	public void falseEdgeAssumesTheLogicalNegationOfTheExpression() throws SemanticException {
		CFG cfg = cfg();
		Statement a = new NoOp(cfg, LOC);
		Statement b = new NoOp(cfg, LOC);
		FalseEdge edge = new FalseEdge(a, b);

		PushAny expr = new PushAny(Untyped.INSTANCE, LOC);
		RecordingDomain domain = new RecordingDomain();
		Analysis<RecordingState, RecordingDomain> analysis = new Analysis<>(domain);

		edge.traverseForward(stateWithExpression(expr), analysis);

		assertTrue(domain.lastAssumed instanceof UnaryExpression);
		UnaryExpression negated = (UnaryExpression) domain.lastAssumed;
		assertSame(LogicalNegation.INSTANCE, negated.getOperator());
		assertSame(expr, negated.getExpression());
	}

	@Test
	public void sequentialEdgeDoesNotChangeTheState() throws SemanticException {
		CFG cfg = cfg();
		Statement a = new NoOp(cfg, LOC);
		Statement b = new NoOp(cfg, LOC);
		SequentialEdge edge = new SequentialEdge(a, b);

		AnalysisState<RecordingState> state = stateWithExpression(new PushAny(Untyped.INSTANCE, LOC));
		RecordingDomain domain = new RecordingDomain();
		Analysis<RecordingState, RecordingDomain> analysis = new Analysis<>(domain);

		AnalysisState<RecordingState> result = edge.traverseForward(state, analysis);

		assertSame(state, result);
	}

}
