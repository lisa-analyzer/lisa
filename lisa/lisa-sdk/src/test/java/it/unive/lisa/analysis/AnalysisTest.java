package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AnalysisTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG cfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "u", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	private static Identifier id(
			CFG cfg,
			String name) {
		return new VariableRef(cfg, LOC, name).getVariable();
	}

	private static ProgramState<MarkerAbstractLattice> ps(
			Identifier... known) {
		return new ProgramState<>(new MarkerAbstractLattice(Set.of(known)), new ExpressionSet());
	}

	private static ProgramState<MarkerAbstractLattice> psWithExpr(
			Set<Identifier> known,
			SymbolicExpression expr) {
		return new ProgramState<>(new MarkerAbstractLattice(known), new ExpressionSet(expr));
	}

	/**
	 * An analysis state with a clean execution and every other continuation
	 * (halt, errors, smashed errors) at bottom, mirroring
	 * {@link Analysis#makeLattice()}'s entry state.
	 */
	private static AnalysisState<MarkerAbstractLattice> base(
			ProgramState<MarkerAbstractLattice> exec) {
		return new AnalysisState<>(exec).withExecution(exec).removeAllErrors(true);
	}

	// smashes exactly Untyped errors, leaving every other type non-smashed
	private static Analysis<MarkerAbstractLattice, MarkerAbstractDomain> smashingAnalysis() {
		return new Analysis<>(new MarkerAbstractDomain(), t -> t == Untyped.INSTANCE);
	}

	private static Analysis<MarkerAbstractLattice, MarkerAbstractDomain> analysis() {
		return new Analysis<>(new MarkerAbstractDomain());
	}

	// ------------------------------------------------------------------
	// assign / smallStepSemantics / assume / satisfies
	// ------------------------------------------------------------------

	@Test
	public void assignOnAnIdentifierUpdatesTheStateAndComputedExpression()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Statement pp = new NoOp(cfg, LOC);
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		AnalysisState<MarkerAbstractLattice> res = analysis().assign(state, x, x, pp);
		assertTrue(res.getExecutionState().knowsIdentifier(x));
		assertEquals(new ExpressionSet(x), res.getExecutionExpressions());
	}

	@Test
	public void assignOnANonIdentifierRewritesAndAssignsToTheRewrittenIdentifiers()
			throws SemanticException {
		CFG cfg = cfg();
		Statement pp = new NoOp(cfg, LOC);
		Skip nonIdentifier = new Skip(LOC);
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		AnalysisState<MarkerAbstractLattice> res = analysis().assign(state, nonIdentifier, nonIdentifier, pp);
		// the oracle rewrites any non-identifier to a fixed "rewritten"
		// variable, which must be the one that gets assigned
		Identifier rewritten = new Variable(Untyped.INSTANCE, "rewritten", pp.getLocation());
		assertTrue(res.getExecutionState().knowsIdentifier(rewritten));
		assertEquals(new ExpressionSet(rewritten), res.getExecutionExpressions());
	}

	@Test
	public void assignOnAnIdentifierWrappedAsASymbolicExpressionDelegatesToTheIdentifierOverload()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Statement pp = new NoOp(cfg, LOC);
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		SymbolicExpression xAsExpression = x;
		AnalysisState<MarkerAbstractLattice> res = analysis().assign(state, xAsExpression, x, pp);
		assertTrue(res.getExecutionState().knowsIdentifier(x));
	}

	@Test
	public void smallStepSemanticsSetsTheComputedExpressionToTheRawExpression()
			throws SemanticException {
		CFG cfg = cfg();
		Statement pp = new NoOp(cfg, LOC);
		Identifier x = id(cfg, "x");
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		AnalysisState<MarkerAbstractLattice> res = analysis().smallStepSemantics(state, x, pp);
		assertEquals(new ExpressionSet(x), res.getExecutionExpressions());
	}

	@Test
	public void assumeYieldsBottomExecutionWhenTheDomainDeemsItUnsatisfiable()
			throws SemanticException {
		CFG cfg = cfg();
		Statement pp = new NoOp(cfg, LOC);
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		AnalysisState<MarkerAbstractLattice> res = analysis().assume(state, new Skip(LOC), pp, pp);
		assertTrue(res.getExecution().isBottom());
	}

	@Test
	public void assumeKeepsTheStateAndExpressionsWhenSatisfiable()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Statement pp = new NoOp(cfg, LOC);
		AnalysisState<MarkerAbstractLattice> state = base(ps()).withExecutionExpression(x);
		AnalysisState<MarkerAbstractLattice> res = analysis().assume(state, x, pp, pp);
		assertFalse(res.getExecution().isBottom());
		assertEquals(new ExpressionSet(x), res.getExecutionExpressions());
	}

	@Test
	public void satisfiesDelegatesToTheDomain()
			throws SemanticException {
		CFG cfg = cfg();
		Statement pp = new NoOp(cfg, LOC);
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		// the marker oracle answers UNKNOWN for expressions it cannot judge;
		// satisfies falls back to the domain, which does not override the
		// default (UNKNOWN) satisfiability check
		assertEquals(Satisfiability.UNKNOWN, analysis().satisfies(state, new Skip(LOC), pp));
	}

	// ------------------------------------------------------------------
	// makeLattice
	// ------------------------------------------------------------------

	@Test
	public void makeLatticeBuildsTheEntryStateWithBottomContinuationsExceptExecution() {
		AnalysisState<MarkerAbstractLattice> entry = analysis().makeLattice();
		assertFalse(entry.getExecution().isBottom());
		assertTrue(entry.getHalt().isBottom());
		assertTrue(entry.getErrors().isBottom());
		assertTrue(entry.getSmashedErrors().isBottom());
		assertEquals(new ExpressionSet(new Skip(SyntheticLocation.INSTANCE)), entry.getExecutionExpressions());
	}

	// ------------------------------------------------------------------
	// moveExecutionToError / moveExecutionToHalting
	// ------------------------------------------------------------------

	@Test
	public void moveExecutionToErrorTracksANonSmashedErrorByDefault()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Statement pp = new NoOp(cfg, LOC);
		Error err = new Error(VoidType.INSTANCE, pp);
		AnalysisState<MarkerAbstractLattice> state = base(ps(x));
		AnalysisState<MarkerAbstractLattice> res = analysis().moveExecutionToError(state, err, pp);
		assertTrue(res.getExecution().isBottom());
		assertEquals(Set.of(err), res.getErrors().getKeys());
		assertTrue(res.getSmashedErrors().isBottom());
	}

	@Test
	public void moveExecutionToErrorSmashesTheErrorWhenThePredicateSaysSo()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Statement pp = new NoOp(cfg, LOC);
		Error err = new Error(Untyped.INSTANCE, pp);
		AnalysisState<MarkerAbstractLattice> state = base(ps(x));
		AnalysisState<MarkerAbstractLattice> res = smashingAnalysis().moveExecutionToError(state, err, pp);
		assertTrue(res.getExecution().isBottom());
		assertTrue(res.getErrors().isBottom());
		assertEquals(Set.of(pp), res.getSmashedErrors().getState(Untyped.INSTANCE).elements);
	}

	@Test
	public void moveExecutionToErrorIsANoOpWhenTheExecutionIsAlreadyBottom()
			throws SemanticException {
		CFG cfg = cfg();
		Statement pp = new NoOp(cfg, LOC);
		Error err = new Error(Untyped.INSTANCE, pp);
		AnalysisState<MarkerAbstractLattice> bottomExec = base(ps()).bottomExecution();
		assertSame(bottomExec, analysis().moveExecutionToError(bottomExec, err, pp));
	}

	@Test
	public void moveExecutionToHaltingLubsWithAnyPreviousHaltingState()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Identifier y = id(cfg, "y");
		Statement pp = new NoOp(cfg, LOC);
		AnalysisState<MarkerAbstractLattice> state = base(ps(x)).withHalt(ps(y));
		AnalysisState<MarkerAbstractLattice> res = analysis().moveExecutionToHalting(state, pp);
		assertTrue(res.getExecution().isBottom());
		assertEquals(Set.of(x, y), res.getHalt().getState().known);
	}

	// ------------------------------------------------------------------
	// moveErrorsToExecution
	// ------------------------------------------------------------------

	@Test
	public void moveErrorsToExecutionMergesCaughtErrorsIntoTheExecutionWithoutAVariable()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Statement thrower = new NoOp(cfg, LOC);
		ProtectedBlock block = new ProtectedBlock(thrower, thrower, List.of(thrower));
		Error err = new Error(Untyped.INSTANCE, thrower);
		AnalysisState<MarkerAbstractLattice> state = base(ps()).addError(err, ps(x));

		AnalysisState<MarkerAbstractLattice> res = analysis().moveErrorsToExecution(
				state, thrower, block, List.of(Untyped.INSTANCE), List.of(), null);

		assertEquals(Set.of(x), res.getExecutionState().known);
		assertEquals(new ExpressionSet(new Skip(SyntheticLocation.INSTANCE)), res.getExecutionExpressions());
		assertTrue(res.getErrors().isBottom());
	}

	@Test
	public void moveErrorsToExecutionAssignsTheCaughtValueToTheGivenVariable()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Identifier excValue = id(cfg, "excValue");
		Statement thrower = new NoOp(cfg, LOC);
		ProtectedBlock block = new ProtectedBlock(thrower, thrower, List.of(thrower));
		VariableRef excVar = new VariableRef(cfg, LOC, "e");
		Error err = new Error(Untyped.INSTANCE, thrower);
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addError(err, psWithExpr(Set.of(x), excValue));

		AnalysisState<MarkerAbstractLattice> res = analysis().moveErrorsToExecution(
				state, thrower, block, List.of(Untyped.INSTANCE), List.of(), excVar);

		assertEquals(Set.of(x, id(cfg, "e")), res.getExecutionState().known);
		assertEquals(new ExpressionSet(excVar.getVariable()), res.getExecutionExpressions());
	}

	@Test
	public void moveErrorsToExecutionFallsBackToATopValueWhenNoExceptionValueWasComputed()
			throws SemanticException {
		// exercises the branch where a caught error's state carries no
		// computed expression (e.g. the exception object itself was never
		// materialized): the variable must still end up known, bound to an
		// unconstrained (PushAny) value, rather than being silently dropped
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Statement thrower = new NoOp(cfg, LOC);
		ProtectedBlock block = new ProtectedBlock(thrower, thrower, List.of(thrower));
		VariableRef excVar = new VariableRef(cfg, LOC, "e");
		Error err = new Error(Untyped.INSTANCE, thrower);
		AnalysisState<MarkerAbstractLattice> state = base(ps()).addError(err, ps(x));

		AnalysisState<MarkerAbstractLattice> res = analysis().moveErrorsToExecution(
				state, thrower, block, List.of(Untyped.INSTANCE), List.of(), excVar);

		assertEquals(Set.of(x, id(cfg, "e")), res.getExecutionState().known);
	}

	@Test
	public void moveErrorsToExecutionYieldsBottomWhenNothingIsCaught()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Statement thrower = new NoOp(cfg, LOC);
		ProtectedBlock block = new ProtectedBlock(thrower, thrower, List.of(thrower));
		Error err = new Error(Untyped.INSTANCE, thrower);
		AnalysisState<MarkerAbstractLattice> state = base(ps()).addError(err, ps(x));

		AnalysisState<MarkerAbstractLattice> res = analysis().moveErrorsToExecution(
				state, thrower, block, List.of(VoidType.INSTANCE), List.of(), null);

		assertTrue(res.isBottom());
	}

	// ------------------------------------------------------------------
	// removeCaughtErrors
	// ------------------------------------------------------------------

	@Test
	public void removeCaughtErrorsOnlyRemovesTheTypesCaughtByAnOutgoingErrorEdge()
			throws SemanticException {
		CFG cfg = cfg();
		Statement source = new NoOp(cfg, LOC);
		Statement dest = new NoOp(cfg, LOC);
		Statement thrower = new NoOp(cfg, LOC);
		cfg.addNode(source, true);
		cfg.addNode(dest);

		ProtectedBlock block = new ProtectedBlock(thrower, thrower, List.of(thrower));
		cfg.addEdge(new ErrorEdge(source, dest, null, block, Untyped.INSTANCE));

		Identifier x = id(cfg, "x");
		Identifier y = id(cfg, "y");
		Error caught = new Error(Untyped.INSTANCE, thrower);
		Error uncaught = new Error(VoidType.INSTANCE, thrower);
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addError(caught, ps(x))
				.addError(uncaught, ps(y));

		AnalysisState<MarkerAbstractLattice> res = analysis().removeCaughtErrors(state, source);
		assertEquals(Set.of(uncaught), res.getErrors().getKeys());
	}

	@Test
	public void removeCaughtErrorsIsANoOpWhenThereAreNoOutgoingErrorEdges()
			throws SemanticException {
		CFG cfg = cfg();
		Statement source = new NoOp(cfg, LOC);
		cfg.addNode(source, true);
		Error err = new Error(Untyped.INSTANCE, source);
		AnalysisState<MarkerAbstractLattice> state = base(ps()).addError(err, ps(id(cfg, "x")));
		assertSame(state, analysis().removeCaughtErrors(state, source));
	}

	// ------------------------------------------------------------------
	// transferThrowers
	// ------------------------------------------------------------------

	@Test
	public void transferThrowersMovesThrowersWithinOriginToTheGivenTarget()
			throws SemanticException {
		CFG origin = cfg();
		Statement thrower = new NoOp(origin, LOC);
		origin.addNode(thrower, true);

		CFG caller = cfg();
		// a call that was never resolved from an UnresolvedCall (getSource()
		// is null): this reproduces the scenario that used to make
		// transferThrowers null out the target instead of keeping it as-is
		CFGCall callSite = new CFGCall(caller, LOC, CallType.STATIC, null, "callee", Set.of(origin));

		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addSmashedError(new Error(Untyped.INSTANCE, thrower), ps(id(origin, "x")));

		AnalysisState<MarkerAbstractLattice> res = analysis().transferThrowers(state, callSite, origin);

		assertEquals(Set.of(callSite), res.getSmashedErrors().getState(Untyped.INSTANCE).elements);
	}

	@Test
	public void transferThrowersLeavesThrowersOutsideOriginUntouched()
			throws SemanticException {
		CFG origin = cfg();
		CFG other = cfg();
		Statement outsideThrower = new NoOp(other, LOC);
		other.addNode(outsideThrower, true);

		CFG caller = cfg();
		CFGCall callSite = new CFGCall(caller, LOC, CallType.STATIC, null, "callee", Set.of(origin));

		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addSmashedError(new Error(Untyped.INSTANCE, outsideThrower), ps(id(other, "x")));

		AnalysisState<MarkerAbstractLattice> res = analysis().transferThrowers(state, callSite, origin);

		assertEquals(Set.of(outsideThrower), res.getSmashedErrors().getState(Untyped.INSTANCE).elements);
	}

	@Test
	public void transferThrowersIsANoOpOnTopOrBottomStates()
			throws SemanticException {
		CFG origin = cfg();
		CFGCall callSite = new CFGCall(cfg(), LOC, CallType.STATIC, null, "callee", Set.of(origin));
		AnalysisState<MarkerAbstractLattice> top = base(ps()).top();
		assertSame(top, analysis().transferThrowers(top, callSite, origin));
	}

	// ------------------------------------------------------------------
	// onCallReturn
	// ------------------------------------------------------------------

	@Test
	public void onCallReturnDelegatesToTheDomainAndKeepsCallResultExpressionsAndInfo()
			throws SemanticException {
		CFG cfg = cfg();
		Statement call = new NoOp(cfg, LOC);
		Identifier x = id(cfg, "x");
		AnalysisState<MarkerAbstractLattice> entryState = base(ps());
		AnalysisState<MarkerAbstractLattice> callres = base(ps()).withExecutionExpression(x);

		AnalysisState<MarkerAbstractLattice> res = analysis().onCallReturn(entryState, callres, call);

		assertEquals(new ExpressionSet(x), res.getExecutionExpressions());
	}

}
