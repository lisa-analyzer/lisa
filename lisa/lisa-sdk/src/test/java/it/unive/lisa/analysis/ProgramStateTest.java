package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ProgramStateTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG cfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "u", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	private static Identifier id(
			CFG cfg,
			String name) {
		return new VariableRef(cfg, LOC, name).getIdentifier();
	}

	@Test
	public void getStateAndGetComputedExpressionsReturnConstructorArguments() {
		MarkerAbstractLattice lattice = new MarkerAbstractLattice();
		ExpressionSet exp = new ExpressionSet(new Skip(LOC));
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(lattice, exp);
		assertSame(lattice, ps.getState());
		assertEquals(exp, ps.getComputedExpressions());
		assertTrue(ps.getFixpointInformation().isEmpty());
	}

	@Test
	public void singleExpressionConstructorWrapsItIntoASet() {
		Skip skip = new Skip(LOC);
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), skip);
		assertEquals(new ExpressionSet(skip), ps.getComputedExpressions());
	}

	@Test
	public void withComputedExpressionReplacesOnlyTheExpressions() {
		MarkerAbstractLattice lattice = new MarkerAbstractLattice(Set.of());
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(lattice, new Skip(LOC));
		Skip other = new Skip(LOC);
		ProgramState<MarkerAbstractLattice> updated = ps.withComputedExpression(other);
		assertSame(lattice, updated.getState());
		assertEquals(new ExpressionSet(other), updated.getComputedExpressions());
	}

	@Test
	public void storeInfoIsAStrongUpdate() {
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), new Skip(LOC));
		ProgramState<MarkerAbstractLattice> updated = ps.storeInfo("k", new GenericSetLattice<>("x"));
		assertEquals(new GenericSetLattice<>("x"), updated.getInfo("k"));
	}

	@Test
	public void weakStoreInfoLubsWithThePreviousValue()
			throws SemanticException {
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), new Skip(LOC))
				.storeInfo("k", new GenericSetLattice<>("x"));
		ProgramState<MarkerAbstractLattice> updated = ps.weakStoreInfo("k", new GenericSetLattice<>("y"));
		assertEquals(new GenericSetLattice<>(Set.of("x", "y")), updated.getInfo("k"));
	}

	@Test
	public void clearInfoRemovesEveryEntry() {
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), new Skip(LOC))
				.storeInfo("k", new GenericSetLattice<>("x"));
		assertTrue(ps.clearInfo().getFixpointInformation().isBottom());
	}

	@Test
	public void isTopRequiresBothStateAndExpressionsToBeTop() {
		ProgramState<MarkerAbstractLattice> allTop = new ProgramState<>(new MarkerAbstractLattice().top(),
				new ExpressionSet().top());
		assertTrue(allTop.isTop());

		ProgramState<MarkerAbstractLattice> onlyStateTop = new ProgramState<>(new MarkerAbstractLattice().top(),
				new ExpressionSet(new Skip(LOC)));
		assertFalse(onlyStateTop.isTop());
	}

	@Test
	public void topPreservesTheInfo() {
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), new ExpressionSet())
				.storeInfo("k", new GenericSetLattice<>("a"));
		ProgramState<MarkerAbstractLattice> top = ps.top();
		assertTrue(top.isTop());
		assertEquals(new GenericSetLattice<>("a"), top.getInfo("k"));
	}

	@Test
	public void latticeOperationsWithTopKeepTheInfoOfBothOperands()
			throws SemanticException {
		// regression test: the lattice operations used to shortcut on top
		// states, discarding the information of the other operand
		ProgramState<MarkerAbstractLattice> withInfo = new ProgramState<>(new MarkerAbstractLattice(),
				new ExpressionSet()).storeInfo("k", new GenericSetLattice<>("a"));
		ProgramState<MarkerAbstractLattice> top = new ProgramState<>(new MarkerAbstractLattice().top(),
				new ExpressionSet().top()).storeInfo("j", new GenericSetLattice<>("b"));

		for (ProgramState<MarkerAbstractLattice> res : List.of(
				top.lub(withInfo),
				withInfo.lub(top),
				top.upchain(withInfo),
				withInfo.upchain(top),
				top.widening(withInfo),
				withInfo.widening(top))) {
			assertTrue(res.isTop());
			assertEquals(new GenericSetLattice<>("a"), res.getInfo("k"));
			assertEquals(new GenericSetLattice<>("b"), res.getInfo("j"));
		}

		// k is missing from top's info, and j from withInfo's one
		assertFalse(withInfo.lessOrEqual(top));
		assertFalse(top.lessOrEqual(withInfo));
		assertTrue(withInfo.lessOrEqual(withInfo.lub(top)));
	}

	@Test
	public void bottomDiscardsTheInfo()
			throws SemanticException {
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), new ExpressionSet())
				.storeInfo("k", new GenericSetLattice<>("a"));
		ProgramState<MarkerAbstractLattice> bottom = ps.bottom();
		assertTrue(bottom.isBottom());
		assertTrue(bottom.getFixpointInformation().isBottom());

		// regression test: a state derived from a bottom one (e.g., by the
		// semantics of a statement, that sets the computed expressions) is
		// unreachable, and its info must not be joined with the one of
		// reachable states
		ProgramState<MarkerAbstractLattice> derived = bottom.withComputedExpression(new Skip(LOC));
		assertFalse(derived.isBottom());
		ProgramState<MarkerAbstractLattice> other = new ProgramState<>(new MarkerAbstractLattice(),
				new ExpressionSet()).storeInfo("k", new GenericSetLattice<>("b"));
		assertEquals(new GenericSetLattice<>("b"), derived.lub(other).getInfo("k"));
		assertEquals(new GenericSetLattice<>("b"), other.lub(derived).getInfo("k"));
	}

	@Test
	public void isBottomRequiresBothStateAndExpressionsToBeBottom() {
		ProgramState<MarkerAbstractLattice> allBottom = new ProgramState<>(new MarkerAbstractLattice().bottom(),
				new ExpressionSet().bottom());
		assertTrue(allBottom.isBottom());

		ProgramState<MarkerAbstractLattice> onlyExprBottom = new ProgramState<>(new MarkerAbstractLattice(),
				new ExpressionSet().bottom());
		assertFalse(onlyExprBottom.isBottom());
	}

	@Test
	public void lubCombinesStateExpressionsAndInfo()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		Identifier y = id(cfg, "y");
		ProgramState<MarkerAbstractLattice> left = new ProgramState<>(new MarkerAbstractLattice(Set.of(x)),
				new ExpressionSet()).storeInfo("k", new GenericSetLattice<>("a"));
		ProgramState<MarkerAbstractLattice> right = new ProgramState<>(new MarkerAbstractLattice(Set.of(y)),
				new ExpressionSet()).storeInfo("k", new GenericSetLattice<>("b"));

		ProgramState<MarkerAbstractLattice> lub = left.lub(right);
		assertEquals(Set.of(x, y), lub.getState().known);
		assertEquals(new GenericSetLattice<>(Set.of("a", "b")), lub.getInfo("k"));
	}

	@Test
	public void lessOrEqualDelegatesToStateExpressionsAndInfo()
			throws SemanticException {
		CFG cfg = cfg();
		Identifier x = id(cfg, "x");
		ProgramState<MarkerAbstractLattice> smaller = new ProgramState<>(new MarkerAbstractLattice(Set.of()),
				new ExpressionSet());
		ProgramState<MarkerAbstractLattice> bigger = new ProgramState<>(new MarkerAbstractLattice(Set.of(x)),
				new ExpressionSet());
		assertTrue(smaller.lessOrEqual(bigger));
		assertFalse(bigger.lessOrEqual(smaller));
	}

	@Test
	public void forgetIdentifierDelegatesToTheWrappedState()
			throws SemanticException {
		CFG cfg = cfg();
		Statement pp = new NoOp(cfg, LOC);
		Identifier x = id(cfg, "x");
		Identifier y = id(cfg, "y");
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(Set.of(x, y)),
				new ExpressionSet());
		ProgramState<MarkerAbstractLattice> updated = ps.forgetIdentifier(x, pp);
		assertFalse(updated.getState().knowsIdentifier(x));
		assertTrue(updated.getState().knowsIdentifier(y));
	}

	@Test
	public void forgetIdentifiersOnEmptyOrNullCollectionIsANoOp()
			throws SemanticException {
		CFG cfg = cfg();
		Statement pp = new NoOp(cfg, LOC);
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), new ExpressionSet());
		assertSame(ps, ps.forgetIdentifiers(null, pp));
		assertSame(ps, ps.forgetIdentifiers(List.of(), pp));
	}

	@Test
	public void pushAndPopScopeDelegateToBothStateAndExpressions()
			throws SemanticException {
		CFG cfg = cfg();
		Statement pp = new NoOp(cfg, LOC);
		ScopeToken token = new ScopeToken(pp);
		Identifier v = id(cfg, "x");
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), v);

		ProgramState<MarkerAbstractLattice> pushed = ps.pushScope(token, pp);
		assertEquals("push:" + token, pushed.getState().lastOperation);
		// the computed expression (a scopable identifier) must have been
		// pushed too, and stays an identifier
		assertTrue(pushed.getComputedExpressions().elements.iterator().next() instanceof Identifier);

		ProgramState<MarkerAbstractLattice> popped = pushed.popScope(token, pp);
		assertEquals("pop:" + token, popped.getState().lastOperation);
	}

	@Test
	public void withTopMemoryValuesAndTypesDelegateToTheWrappedState() {
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), new ExpressionSet());
		assertEquals("withTopMemory", ps.withTopMemory().getState().lastOperation);
		assertEquals("withTopValues", ps.withTopValues().getState().lastOperation);
		assertEquals("withTopTypes", ps.withTopTypes().getState().lastOperation);
	}

	@Test
	public void getAllLatticeInstancesDelegatesToTheWrappedState() {
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(), new ExpressionSet());
		assertTrue(ps.getAllLatticeInstances(MarkerAbstractLattice.class).contains(ps.getState()));
	}

	@Test
	public void representationDumpsStateExpressionsAndInfoWhenPresent() {
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(),
				new ExpressionSet(new Skip(LOC))).storeInfo("k", new GenericSetLattice<>("x"));
		String repr = ps.toString();
		assertTrue(repr.contains("state"));
		assertTrue(repr.contains("expressions"));
		assertTrue(repr.contains("info"));
	}

	@Test
	public void representationOmitsInfoWhenEmpty() {
		ProgramState<MarkerAbstractLattice> ps = new ProgramState<>(new MarkerAbstractLattice(),
				new ExpressionSet(new Skip(LOC)));
		assertFalse(ps.toString().contains("info"));
	}

}
