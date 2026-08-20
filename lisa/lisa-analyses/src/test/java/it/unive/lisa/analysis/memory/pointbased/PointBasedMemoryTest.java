package it.unive.lisa.analysis.memory.pointbased;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.analysis.nonrelational.memory.MemoryEnvironment;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.memory.allocations.AllocationSite;
import it.unive.lisa.lattices.memory.allocations.AllocationSites;
import it.unive.lisa.lattices.memory.allocations.HeapAllocationSite;
import it.unive.lisa.lattices.memory.allocations.StackAllocationSite;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.memory.GetAddress;
import it.unive.lisa.symbolic.memory.MemoryAllocation;
import it.unive.lisa.symbolic.memory.MemoryDereference;
import it.unive.lisa.symbolic.memory.MemoryExpression;
import it.unive.lisa.symbolic.memory.StaticAccess;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class PointBasedMemoryTest {

	private final Type untyped = Untyped.INSTANCE;

	private final Type intType = Int32Type.INSTANCE;

	private final CodeLocation loc1 = new SourceCodeLocation("fake", 1, 1);

	private final CodeLocation loc2 = new SourceCodeLocation("fake", 2, 2);

	private final AllocationSite alloc1 = new HeapAllocationSite(untyped, loc1.getCodeLocation(), true, loc1);

	private final AllocationSite alloc2 = new HeapAllocationSite(untyped, loc2.getCodeLocation(), true, loc2);

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

	private final SemanticOracle fakeOracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final Variable x = new Variable(untyped, "x", pp1.getLocation());

	private final Variable y = new Variable(untyped, "y", pp1.getLocation());

	private final PointBasedMemory memory = new PointBasedMemory();

	private final MemoryEnvironment<AllocationSites> emptyMemory = new PointBasedMemory().makeLattice();

	private final MemoryEnvironment<AllocationSites> topMemory = emptyMemory.top();

	private final MemoryEnvironment<AllocationSites> bottomMemory = emptyMemory.bottom();

	private final MemoryEnvironment<
			AllocationSites> emptyMemoryEnv = new MemoryEnvironment<AllocationSites>(new AllocationSites());

	@Test
	public void testAssign()
			throws SemanticException {
		Constant one = new Constant(Int32Type.INSTANCE, 1, loc1);
		Constant zero = new Constant(Int32Type.INSTANCE, 0, loc1);
		Pair<MemoryEnvironment<AllocationSites>,
				List<MemoryReplacement>> assignResult = memory.assign(topMemory, x, one, pp1, fakeOracle);

		// constants do not affect memory abstract domain
		assertEquals(topMemory, assignResult.getLeft());

		assignResult = memory.assign(
				topMemory,
				x,
				new BinaryExpression(intType, one, zero, NumericNonOverflowingAdd.INSTANCE, loc1),
				pp1,
				fakeOracle);

		// binary expressions do not affect memory abstract domain
		assertEquals(topMemory, assignResult.getLeft());

		// from empty environment, assignment x = *(pp1, fakeOracle)
		// expected: x -> pp1
		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xAssign = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1));
		MemoryEnvironment<AllocationSites> expectedEnv = emptyMemoryEnv.putState(x, xSites);
		assertEquals(expectedEnv, xAssign.getLeft());

		// from x -> pp1, assignment x = *(pp2, fakeOracle)
		// expected: x -> pp2
		Pair<MemoryEnvironment<AllocationSites>,
				List<MemoryReplacement>> actual = memory.assign(
						xAssign.getLeft(),
						x,
						new GetAddress(untyped, new MemoryAllocation(untyped, loc2), loc2),
						pp2,
						fakeOracle);

		xSites = new AllocationSites(Collections.singleton(alloc2));
		expectedEnv = emptyMemoryEnv.putState(x, xSites);
		assertEquals(expectedEnv, actual.getLeft());
	}

	@Test
	public void testSmallStepSemantic()
			throws SemanticException {
		// The current implementation of semanticsOf returns this;
		// we test the method checking that rewriting a memory expression
		// in this and in its semanticsOf results produced the same
		// result.

		// 1. Memory allocation
		MemoryExpression MemoryExpression = new MemoryAllocation(untyped, loc1, new Annotations());

		// from topState
		Pair<MemoryEnvironment<AllocationSites>,
				List<MemoryReplacement>> sss = memory.semanticsOf(topMemory, MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(topMemory, MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// from bottomState
		sss = memory.semanticsOf(bottomMemory, MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(bottomMemory, MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// from x -> pp1
		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xToLoc1 = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		sss = memory.semanticsOf(xToLoc1.getLeft(), MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(xToLoc1.getLeft(), MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// 2. Memory reference
		MemoryExpression = new GetAddress(untyped, new MemoryAllocation(untyped, loc1, new Annotations()), loc1);

		// from topState
		sss = memory.semanticsOf(topMemory, MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(topMemory, MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// from bottomState
		sss = memory.semanticsOf(bottomMemory, MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(bottomMemory, MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// from x -> pp1
		sss = memory.semanticsOf(xToLoc1.getLeft(), MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(xToLoc1.getLeft(), MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// 3. Access child
		MemoryExpression = new StaticAccess(untyped, x, y, loc1);

		// from topState
		sss = memory.semanticsOf(topMemory, MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(topMemory, MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// from bottomState
		sss = memory.semanticsOf(bottomMemory, MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(bottomMemory, MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// from x -> pp1
		sss = memory.semanticsOf(xToLoc1.getLeft(), MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(xToLoc1.getLeft(), MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// 4. Memory dereference
		MemoryExpression = new MemoryDereference(
				untyped,
				new GetAddress(untyped, new MemoryAllocation(untyped, loc1, new Annotations()), loc1),
				loc1);

		// from topState
		sss = memory.semanticsOf(topMemory, MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(topMemory, MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// from bottomState
		sss = memory.semanticsOf(bottomMemory, MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(bottomMemory, MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));

		// from x -> pp1
		sss = memory.semanticsOf(xToLoc1.getLeft(), MemoryExpression, pp1, fakeOracle);
		assertEquals(
				memory.rewrite(xToLoc1.getLeft(), MemoryExpression, pp1, fakeOracle),
				memory.rewrite(sss.getLeft(), MemoryExpression, pp1, fakeOracle));
	}

	@Test
	public void testLub()
			throws SemanticException {
		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xToLoc1 = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xToLoc2 = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> yToLoc2 = memory
				.assign(topMemory, y, new GetAddress(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		// top lub <any memory> or <any memory> lub top = top
		assertTrue(topMemory.lub(topMemory).isTop());
		assertTrue(topMemory.lub(xToLoc1.getLeft()).isTop());
		assertTrue(xToLoc1.getLeft().lub(topMemory).isTop());

		// <any memory> lub bottom or bottom lub <any memory> = <any memory>
		assertTrue(bottomMemory.lub(bottomMemory).isBottom());
		assertEquals(xToLoc1.getLeft(), xToLoc1.getLeft().lub(bottomMemory));
		assertEquals(xToLoc1.getLeft(), bottomMemory.lub(xToLoc1.getLeft()));

		// <any memory> lub <any memory> = <any memory>
		assertEquals(xToLoc1.getLeft(), xToLoc1.getLeft().lub(xToLoc1.getLeft()));

		// x -> pp1 lub y -> pp1 = x -> pp1, y -> pp1
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1));
		AllocationSites ySites = new AllocationSites(Collections.singleton(alloc2));

		MemoryEnvironment<AllocationSites> expectedEnv = emptyMemoryEnv.putState(x, xSites);
		expectedEnv = expectedEnv.putState(y, ySites);

		assertEquals(expectedEnv, xToLoc1.getLeft().lub(yToLoc2.getLeft()));

		// x -> pp1 lub x -> pp2 = x -> pp1,pp2
		HashSet<AllocationSite> xSet = new HashSet<>();
		xSet.add(alloc1);
		xSet.add(alloc2);
		xSites = new AllocationSites().mk(xSet);
		expectedEnv = emptyMemoryEnv.putState(x, xSites);

		assertEquals(expectedEnv, xToLoc1.getLeft().lub(xToLoc2.getLeft()));
	}

	@Test
	public void testWidening()
			throws SemanticException {
		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xToLoc1 = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xToLoc2 = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> yToLoc2 = memory
				.assign(topMemory, y, new GetAddress(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		// top lub <any memory> or <any memory> lub top = top
		assertTrue(topMemory.widening(topMemory).isTop());
		assertTrue(topMemory.widening(xToLoc1.getLeft()).isTop());
		assertTrue(xToLoc1.getLeft().widening(topMemory).isTop());

		// <any memory> lub bottom or bottom lub <any memory> = <any memory>
		assertTrue(bottomMemory.widening(bottomMemory).isBottom());
		assertEquals(xToLoc1.getLeft(), xToLoc1.getLeft().widening(bottomMemory));
		assertEquals(xToLoc1.getLeft(), bottomMemory.widening(xToLoc1.getLeft()));

		// <any memory> lub <any memory> = <any memory>
		assertEquals(xToLoc1.getLeft(), xToLoc1.getLeft().widening(xToLoc1.getLeft()));

		// x -> pp1 lub y -> pp1 = x -> pp1, y -> pp1
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1));
		AllocationSites ySites = new AllocationSites(Collections.singleton(alloc2));

		MemoryEnvironment<AllocationSites> expectedEnv = emptyMemoryEnv.putState(x, xSites);
		expectedEnv = expectedEnv.putState(y, ySites);

		assertEquals(expectedEnv, xToLoc1.getLeft().widening(yToLoc2.getLeft()));

		// x -> pp1 lub x -> pp2 = x -> pp1,pp2
		HashSet<AllocationSite> xSet = new HashSet<>();
		xSet.add(alloc1);
		xSet.add(alloc2);
		xSites = new AllocationSites().mk(xSet);
		expectedEnv = emptyMemoryEnv.putState(x, xSites);

		assertEquals(expectedEnv, xToLoc1.getLeft().widening(xToLoc2.getLeft()));
	}

	@Test
	public void testLessOrEquals()
			throws SemanticException {
		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xAssign = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> yAssign = memory
				.assign(topMemory, y, new GetAddress(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		// <any memory> <= top
		assertTrue(topMemory.lessOrEqual(topMemory));
		assertTrue(bottomMemory.lessOrEqual(topMemory));
		assertTrue(xAssign.getLeft().lessOrEqual(topMemory));

		// bottom <= <any memory>
		assertTrue(bottomMemory.lessOrEqual(bottomMemory));
		assertTrue(bottomMemory.lessOrEqual(xAssign.getLeft()));

		// <any memory> <= <any memory>
		assertTrue(xAssign.getLeft().lessOrEqual(xAssign.getLeft()));

		// x -> pp1 </= y -> pp2
		assertFalse(xAssign.getLeft().lessOrEqual(yAssign.getLeft()));
		assertFalse(yAssign.getLeft().lessOrEqual(xAssign.getLeft()));

		MemoryEnvironment<AllocationSites> xyAssign = xAssign.getLeft().lub(yAssign.getLeft());

		// x -> pp1 <= x -> pp1 y -> pp2
		// y -> pp2 <= x -> pp1 y -> pp2
		// x -> pp1 y -> pp2 <= x -> pp1 y -> pp2
		assertTrue(xAssign.getLeft().lessOrEqual(xyAssign));
		assertTrue(yAssign.getLeft().lessOrEqual(xyAssign));
		assertTrue(xyAssign.lessOrEqual(xyAssign));
	}

	@Test
	public void testForgetIdentifier()
			throws SemanticException {
		assertEquals(topMemory, topMemory.forgetIdentifier(x, pp1).getLeft());
		assertEquals(bottomMemory, bottomMemory.forgetIdentifier(x, pp1).getLeft());

		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> result = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		assertEquals(emptyMemory, result.getLeft().forgetIdentifier(x, pp1).getLeft());
		assertEquals(result.getLeft(), result.getLeft().forgetIdentifier(y, pp1).getLeft());
	}

	@Test
	public void testPushScope()
			throws SemanticException {
		ScopeToken token = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				return new SourceCodeLocation("fake", 0, 0);
			}

		});

		assertEquals(topMemory, topMemory.pushScope(token, pp1).getLeft());
		assertEquals(bottomMemory, bottomMemory.pushScope(token, pp1).getLeft());

		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xAssign = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		Pair<MemoryEnvironment<AllocationSites>,
				List<MemoryReplacement>> xPushedScopeAssign = memory.assign(
						topMemory,
						new OutOfScopeIdentifier(x, token, loc1),
						new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1),
						pp1,
						fakeOracle);

		// x -> pp1 pushScope = [out-of-scope-id]x -> pp1
		assertEquals(xPushedScopeAssign.getLeft(), xAssign.getLeft().pushScope(token, pp1).getLeft());
	}

	@Test
	public void testPopScope()
			throws SemanticException {
		ScopeToken token = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				return new SourceCodeLocation("fake", 0, 0);
			}

		});

		assertEquals(topMemory, topMemory.popScope(token, pp1).getLeft());
		assertEquals(bottomMemory, bottomMemory.popScope(token, pp1).getLeft());

		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xAssign = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		Pair<MemoryEnvironment<AllocationSites>,
				List<MemoryReplacement>> xScopedAssign = memory.assign(
						topMemory,
						(Identifier) x.pushScope(token, pp1),
						new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1),
						pp1,
						fakeOracle);

		// [scoped]x -> pp1 popScope = x -> pp1
		assertEquals(xAssign.getLeft(), xScopedAssign.getLeft().popScope(token, pp1).getLeft());

		// x -> pp1 popScope = empty environment
		assertEquals(emptyMemory, xAssign.getLeft().popScope(token, pp1).getLeft());
	}

	@Test
	public void testAccessChildRewrite()
			throws SemanticException {
		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xAssign = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		// x.y rewritten in x -> pp1 = pp1
		StaticAccess accessChild = new StaticAccess(untyped, x, y, loc1);

		ExpressionSet expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, memory.rewrite(xAssign.getLeft(), accessChild, pp1, fakeOracle));

		// y.x rewritten in x -> pp1 = empty set
		accessChild = new StaticAccess(untyped, y, x, loc1);
		assertEquals(new ExpressionSet(), memory.rewrite(xAssign.getLeft(), accessChild, pp1, fakeOracle));
	}

	@Test
	public void testIdentifierRewrite()
			throws SemanticException {
		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xAssign = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		// x rewritten in x -> pp1 = pp1
		ExpressionSet expectedRewritten = new ExpressionSet(x);
		assertEquals(expectedRewritten, memory.rewrite(xAssign.getLeft(), x, pp1, fakeOracle));

		// y rewritten in x -> pp1 = {y}
		assertEquals(new ExpressionSet(y), memory.rewrite(xAssign.getLeft(), y, pp1, fakeOracle));
	}

	@Test
	public void testMemoryDereferenceRewrite()
			throws SemanticException {
		// *(&(new loc(pp1, fakeOracle)) rewritten in top -> pp1
		MemoryDereference deref = new MemoryDereference(
				untyped,
				new GetAddress(untyped, new MemoryAllocation(untyped, loc1, new Annotations()), loc1),
				loc1);

		ExpressionSet expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, memory.rewrite(topMemory, deref, pp1, fakeOracle));

		// *(x) rewritten in x -> pp1 -> pp1
		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xAssign = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		deref = new MemoryDereference(untyped, x, loc1);
		expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, memory.rewrite(xAssign.getLeft(), deref, pp1, fakeOracle));

		// *(y) rewritten in x -> pp1 -> empty set
		AllocationSite expectedUnknownAlloc = new StackAllocationSite(untyped, "unknown@y", true, loc1);
		deref = new MemoryDereference(untyped, y, loc1);
		expectedRewritten = new ExpressionSet(expectedUnknownAlloc);
		assertEquals(expectedRewritten, memory.rewrite(xAssign.getLeft(), deref, pp1, fakeOracle));
	}

	@Test
	public void testIssue300()
			throws SemanticException {
		// ((type) x).f rewritten in x -> pp1 -> pp1
		Pair<MemoryEnvironment<AllocationSites>, List<MemoryReplacement>> xAssign = memory
				.assign(topMemory, x, new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		// TODO this must be converted to DynamicAccess
		SymbolicExpression e = new StaticAccess(
				intType,
				new BinaryExpression(
						untyped,
						x,
						new Constant(new TypeTokenType(Collections.singleton(intType)), intType, loc1),
						TypeConv.INSTANCE,
						loc1),
				new Variable(intType, "1", loc1),
				loc1);
		ExpressionSet expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, memory.rewrite(xAssign.getLeft(), e, pp1, fakeOracle));
	}

	@Test
	public void testConvOfAlloc()
			throws SemanticException {
		// (ref(alloc) conv-as type) rewritten in &alloc.loc
		SymbolicExpression e = new BinaryExpression(
				untyped,
				new GetAddress(untyped, new MemoryAllocation(untyped, loc1), loc1),
				new Constant(new TypeTokenType(Collections.singleton(intType)), intType, loc1),
				TypeConv.INSTANCE,
				loc1);
		Pair<MemoryEnvironment<AllocationSites>,
				List<MemoryReplacement>> xAssign = memory.assign(topMemory, x, e, pp1, fakeOracle);
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1));
		assertEquals(emptyMemoryEnv.putState(x, xSites), xAssign.getLeft());
	}

}
