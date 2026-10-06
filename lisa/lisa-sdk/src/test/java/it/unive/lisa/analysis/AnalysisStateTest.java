package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AnalysisStateTest {

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

	/**
	 * An analysis state whose normal execution is {@code exec} and every other
	 * continuation (halt, errors, smashed errors) is bottom, mirroring the
	 * entry state produced by {@link Analysis#makeLattice()}.
	 */
	private static AnalysisState<MarkerAbstractLattice> base(
			ProgramState<MarkerAbstractLattice> exec) {
		return new AnalysisState<>(exec).withExecution(exec).removeAllErrors(true);
	}

	// ------------------------------------------------------------------
	// constructor
	// ------------------------------------------------------------------

	@Test
	public void constructorDiscardsTheGivenStateAndBuildsTop() {
		ProgramState<MarkerAbstractLattice> concrete = ps(id(cfg(), "x"));
		AnalysisState<MarkerAbstractLattice> state = new AnalysisState<>(concrete);
		// this is the documented (if surprising) contract: the constructor
		// derives only the concrete type from its argument, and always
		// produces the top analysis state
		assertTrue(state.isTop());
		assertNotEquals(concrete, state.getExecution());
	}

	@Test
	public void withExecutionThenRestoresTheConcreteState() {
		ProgramState<MarkerAbstractLattice> concrete = ps(id(cfg(), "x"));
		AnalysisState<MarkerAbstractLattice> state = new AnalysisState<>(concrete).withExecution(concrete);
		assertSame(concrete, state.getExecution());
		assertFalse(state.isTop());
	}

	// ------------------------------------------------------------------
	// execution / halt / expressions / fixpoint info
	// ------------------------------------------------------------------

	@Test
	public void withExecutionAndWithHaltOnlyChangeTheirOwnContinuation() {
		ProgramState<MarkerAbstractLattice> exec = ps();
		AnalysisState<MarkerAbstractLattice> state = base(exec);
		ProgramState<MarkerAbstractLattice> newHalt = ps(id(cfg(), "h"));
		AnalysisState<MarkerAbstractLattice> withHalt = state.withHalt(newHalt);
		assertSame(exec, withHalt.getExecution());
		assertSame(newHalt, withHalt.getHalt());
	}

	@Test
	public void executionExpressionsAreStoredAndRetrievedFromTheExecutionState()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		AnalysisState<MarkerAbstractLattice> withExpr = state.withExecutionExpression(x);
		assertEquals(new ExpressionSet(x), withExpr.getExecutionExpressions());
	}

	@Test
	public void storeExecutionInfoIsAStrongUpdate() {
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		AnalysisState<MarkerAbstractLattice> updated = state.storeExecutionInfo("k", new GenericSetLattice<>("x"));
		assertEquals(new GenericSetLattice<>("x"), updated.getExecutionInfo("k"));
		assertEquals(new GenericSetLattice<>("x"), updated.getExecutionInfo("k", GenericSetLattice.class));
	}

	@Test
	public void weakStoreExecutionInfoLubsWithThePreviousValue()
			throws SemanticException {
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.storeExecutionInfo("k", new GenericSetLattice<>("x"));
		AnalysisState<MarkerAbstractLattice> updated = state.weakStoreExecutionInfo("k", new GenericSetLattice<>("y"));
		assertEquals(new GenericSetLattice<>(Set.of("x", "y")), updated.getExecutionInfo("k"));
	}

	@Test
	public void clearExecutionInfoRemovesEveryEntry() {
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.storeExecutionInfo("k", new GenericSetLattice<>("x"));
		assertTrue(state.clearExecutionInfo().getExecutionInformation().isBottom());
	}

	// ------------------------------------------------------------------
	// error tracking
	// ------------------------------------------------------------------

	@Test
	public void freshBaseStateHasNoErrors() {
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		assertTrue(state.getErrors().isBottom());
		assertTrue(state.getSmashedErrors().isBottom());
		assertTrue(state.getSmashedErrorsState().isBottom());
		assertTrue(state.getHalt().isBottom());
	}

	@Test
	public void addErrorAddsANewEntry()
			throws SemanticException {
		CFG cfg = cfg();
		Statement thrower = new NoOp(cfg, LOC);
		Error err = new Error(Untyped.INSTANCE, thrower);
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		AnalysisState<MarkerAbstractLattice> updated = state.addError(err, ps(id(cfg, "x")));
		assertEquals(Set.of(err), updated.getErrors().getKeys());
	}

	@Test
	public void addErrorOnABottomStateDoesNothing()
			throws SemanticException {
		CFG cfg = cfg();
		Error err = new Error(Untyped.INSTANCE, new NoOp(cfg, LOC));
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		assertSame(state, state.addError(err, ps().bottom()));
	}

	@Test
	public void addErrorLubsTheStateOfAnAlreadyPresentError()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Identifier y = id(cfg, "y");
		Error err = new Error(Untyped.INSTANCE, new NoOp(cfg, LOC));
		AnalysisState<MarkerAbstractLattice> state = base(ps()).addError(err, ps(x)).addError(err, ps(y));
		assertEquals(Set.of(x, y), state.getErrors().getState(err).getState().known);
	}

	@Test
	public void addErrorsMergesTheGivenMapIntoTheExistingOne()
			throws SemanticException {
		CFG cfg = cfg();
		Error e1 = new Error(Untyped.INSTANCE, new NoOp(cfg, LOC));
		Error e2 = new Error(VoidType.INSTANCE, new NoOp(cfg, LOC));
		AnalysisState<MarkerAbstractLattice> state = base(ps()).addError(e1, ps(id(cfg, "x")));
		AnalysisState<MarkerAbstractLattice> updated = state.addErrors(Map.of(e2, ps(id(cfg, "y"))));
		assertEquals(Set.of(e1, e2), updated.getErrors().getKeys());
	}

	@Test
	public void removeErrorsOnlyRemovesTheGivenOnes()
			throws SemanticException {
		CFG cfg = cfg();
		Error e1 = new Error(Untyped.INSTANCE, new NoOp(cfg, LOC));
		Error e2 = new Error(VoidType.INSTANCE, new NoOp(cfg, LOC));
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addError(e1, ps(id(cfg, "x")))
				.addError(e2, ps(id(cfg, "y")));
		AnalysisState<MarkerAbstractLattice> updated = state.removeErrors(Set.of(e1));
		assertEquals(Set.of(e2), updated.getErrors().getKeys());
	}

	@Test
	public void removeAllErrorsClearsEverythingButOptionallyKeepsHalt()
			throws SemanticException {
		CFG cfg = cfg();
		Error e1 = new Error(Untyped.INSTANCE, new NoOp(cfg, LOC));
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addError(e1, ps(id(cfg, "x")))
				.withHalt(ps(id(cfg, "h")));

		AnalysisState<MarkerAbstractLattice> keepHalt = state.removeAllErrors(false);
		assertTrue(keepHalt.getErrors().isBottom());
		assertFalse(keepHalt.getHalt().isBottom());

		AnalysisState<MarkerAbstractLattice> dropHalt = state.removeAllErrors(true);
		assertTrue(dropHalt.getHalt().isBottom());
	}

	@Test
	public void addSmashedErrorAccumulatesThrowersForTheSameType()
			throws SemanticException {
		CFG cfg = cfg();
		Statement t1 = new NoOp(cfg, LOC);
		Statement t2 = new Return(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addSmashedError(new Error(Untyped.INSTANCE, t1), ps(id(cfg, "x")))
				.addSmashedError(new Error(Untyped.INSTANCE, t2), ps(id(cfg, "y")));

		GenericSetLattice<Statement> throwers = state.getSmashedErrors().getState(Untyped.INSTANCE);
		assertEquals(Set.of(t1, t2), throwers.elements);
		assertEquals(Set.of(id(cfg, "x"), id(cfg, "y")), state.getSmashedErrorsState().getState().known);
	}

	@Test
	public void addSmashedErrorsMergesEveryTypeInTheGivenMap()
			throws SemanticException {
		CFG cfg = cfg();
		Statement t1 = new NoOp(cfg, LOC);
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addSmashedErrors(Map.of(Untyped.INSTANCE, Set.of(t1)), ps(id(cfg, "x")));
		assertEquals(Set.of(t1), state.getSmashedErrors().getState(Untyped.INSTANCE).elements);
	}

	@Test
	public void removeSmashedErrorsDropsOnlyTheGivenThrowersAndKeepsOthers()
			throws SemanticException {
		CFG cfg = cfg();
		Statement t1 = new NoOp(cfg, LOC);
		Statement t2 = new Return(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addSmashedError(new Error(Untyped.INSTANCE, t1), ps(id(cfg, "x")))
				.addSmashedError(new Error(Untyped.INSTANCE, t2), ps(id(cfg, "y")));

		AnalysisState<MarkerAbstractLattice> updated = state
				.removeSmashedErrors(Map.of(Untyped.INSTANCE, Set.of(t1)));
		assertEquals(Set.of(t2), updated.getSmashedErrors().getState(Untyped.INSTANCE).elements);
	}

	@Test
	public void removeSmashedErrorsClearsTheStateTooWhenNoTypeSurvives()
			throws SemanticException {
		CFG cfg = cfg();
		Statement t1 = new NoOp(cfg, LOC);
		AnalysisState<MarkerAbstractLattice> state = base(ps())
				.addSmashedError(new Error(Untyped.INSTANCE, t1), ps(id(cfg, "x")));

		AnalysisState<MarkerAbstractLattice> updated = state
				.removeSmashedErrors(Map.of(Untyped.INSTANCE, Set.of(t1)));
		assertTrue(updated.getSmashedErrors().isBottom());
		assertTrue(updated.getSmashedErrorsState().isBottom());
	}

	// ------------------------------------------------------------------
	// scopes
	// ------------------------------------------------------------------

	@Test
	public void pushScopeAffectsExecutionAndSmashedErrorsStateButLubsPerErrorEntries()
			throws SemanticException {
		CFG cfg = cfg();
		Statement pp = new NoOp(cfg, LOC);
		ScopeToken token = new ScopeToken(pp);
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		AnalysisState<MarkerAbstractLattice> pushed = state.pushScope(token, pp);
		assertEquals("push:" + token, pushed.getExecution().getState().lastOperation);
	}

	// ------------------------------------------------------------------
	// top / bottom
	// ------------------------------------------------------------------

	@Test
	public void topAndBottomRequireAllContinuationsToAgree() {
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		assertFalse(state.isTop());
		assertFalse(state.isBottom());
	}

	@Test
	public void topExecutionOnlyTouchesTheExecutionContinuation() {
		AnalysisState<MarkerAbstractLattice> state = base(ps(id(cfg(), "x")));
		AnalysisState<MarkerAbstractLattice> topExec = state.topExecution();
		assertTrue(topExec.getExecution().isTop());
		assertEquals(state.getHalt(), topExec.getHalt());
	}

	@Test
	public void bottomExecutionOnlyTouchesTheExecutionContinuation() {
		AnalysisState<MarkerAbstractLattice> state = base(ps(id(cfg(), "x")));
		AnalysisState<MarkerAbstractLattice> bottomExec = state.bottomExecution();
		assertTrue(bottomExec.getExecution().isBottom());
	}

	// ------------------------------------------------------------------
	// forgetIdentifier*
	// ------------------------------------------------------------------

	@Test
	public void forgetIdentifierOnlyAffectsTheExecutionContinuation()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Identifier y = id(cfg, "y");
		AnalysisState<MarkerAbstractLattice> state = base(ps(x, y));
		AnalysisState<MarkerAbstractLattice> updated = state.forgetIdentifier(x, new NoOp(cfg, LOC));
		assertFalse(updated.knowsIdentifier(x));
		assertTrue(updated.knowsIdentifier(y));
	}

	@Test
	public void forgetIdentifierIsANoOpWhenExecutionIsTopOrBottom()
			throws SemanticException {
		CFG cfg = cfg();
		AnalysisState<MarkerAbstractLattice> topExec = base(ps()).topExecution();
		assertSame(topExec, topExec.forgetIdentifier(id(cfg, "x"), new NoOp(cfg, LOC)));
		AnalysisState<MarkerAbstractLattice> bottomExec = base(ps()).bottomExecution();
		assertSame(bottomExec, bottomExec.forgetIdentifier(id(cfg, "x"), new NoOp(cfg, LOC)));
	}

	@Test
	public void forgetIdentifiersOnNullOrEmptyCollectionIsANoOp()
			throws SemanticException {
		CFG cfg = cfg();
		AnalysisState<MarkerAbstractLattice> state = base(ps(id(cfg, "x")));
		assertSame(state, state.forgetIdentifiers(null, new NoOp(cfg, LOC)));
		assertSame(state, state.forgetIdentifiers(List.of(), new NoOp(cfg, LOC)));
	}

	// ------------------------------------------------------------------
	// withTop* / knowsIdentifier / getAllLatticeInstances
	// ------------------------------------------------------------------

	@Test
	public void withTopMemoryValuesAndTypesDelegateToTheExecutionState() {
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		assertEquals("withTopMemory", state.withTopMemory().getExecutionState().lastOperation);
		assertEquals("withTopValues", state.withTopValues().getExecutionState().lastOperation);
		assertEquals("withTopTypes", state.withTopTypes().getExecutionState().lastOperation);
	}

	@Test
	public void withTopMemoryIsANoOpWhenExecutionIsAlreadyTop() {
		AnalysisState<MarkerAbstractLattice> topExec = base(ps()).topExecution();
		assertSame(topExec, topExec.withTopMemory());
	}

	@Test
	public void knowsIdentifierDelegatesToTheExecutionState() {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		AnalysisState<MarkerAbstractLattice> state = base(ps(x));
		assertTrue(state.knowsIdentifier(x));
		assertFalse(state.knowsIdentifier(id(cfg, "y")));
	}

	@Test
	public void getAllLatticeInstancesDelegatesToTheExecutionState() {
		AnalysisState<MarkerAbstractLattice> state = base(ps());
		assertTrue(state.getAllLatticeInstances(MarkerAbstractLattice.class)
				.contains(state.getExecutionState()));
	}

	// ------------------------------------------------------------------
	// Error (nested class)
	// ------------------------------------------------------------------

	@Test
	public void errorConstructorKeepsAPlainStatementAsIs() {
		CFG cfg = cfg();
		Statement thrower = new NoOp(cfg, LOC);
		Error err = new Error(Untyped.INSTANCE, thrower);
		assertSame(thrower, err.getThrower());
	}

	@Test
	public void errorConstructorKeepsACallWithNoSourceAsIs() {
		CFG cfg = cfg();
		CFGCall call = new CFGCall(cfg, LOC, CallType.STATIC, null, "target", Set.of(cfg));
		Error err = new Error(Untyped.INSTANCE, call);
		assertSame(call, err.getThrower());
	}

	@Test
	public void errorConstructorFullyUnwindsChainsOfResolvedCalls() {
		CFG cfg = cfg();
		UnresolvedCall root = new UnresolvedCall(cfg, LOC, CallType.STATIC, null, "root");
		UnresolvedCall mid = new UnresolvedCall(cfg, LOC, CallType.STATIC, null, "mid");
		mid.setSource(root);
		CFGCall resolved = new CFGCall(mid, Set.of(cfg));
		resolved.setSource(mid);

		Error err = new Error(Untyped.INSTANCE, resolved);
		assertSame(root, err.getThrower());
	}

	@Test
	public void withThrowerKeepsTheTypeAndReplacesTheThrower() {
		CFG cfg = cfg();
		Error err = new Error(Untyped.INSTANCE, new NoOp(cfg, LOC));
		Statement newThrower = new Return(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		Error updated = err.withThrower(newThrower);
		assertEquals(Untyped.INSTANCE, updated.getType());
		assertSame(newThrower, updated.getThrower());
	}

	@Test
	public void errorEqualityIsBasedOnTypeAndThrower() {
		CFG cfg = cfg();
		Statement thrower = new NoOp(cfg, LOC);
		Error e1 = new Error(Untyped.INSTANCE, thrower);
		Error e2 = new Error(Untyped.INSTANCE, thrower);
		Error different = new Error(VoidType.INSTANCE, thrower);
		assertEquals(e1, e2);
		assertEquals(e1.hashCode(), e2.hashCode());
		assertNotEquals(e1, different);
	}

}
