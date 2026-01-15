package it.unive.lisa.analysis.heap.pointbased;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.heap.allocations.AllocationSite;
import it.unive.lisa.lattices.heap.allocations.AllocationSites;
import it.unive.lisa.lattices.heap.allocations.HeapAllocationSite;
import it.unive.lisa.lattices.heap.allocations.StackAllocationSite;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
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
import org.junit.Test;

public class PointBasedHeapTest {

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

	private final PointBasedHeap heap = new PointBasedHeap();

	private final HeapEnvironment<AllocationSites> emptyHeap = new PointBasedHeap().makeLattice();

	private final HeapEnvironment<AllocationSites> topHeap = emptyHeap.top();

	private final HeapEnvironment<AllocationSites> bottomHeap = emptyHeap.bottom();

	private final HeapEnvironment<
			AllocationSites> emptyHeapEnv = new HeapEnvironment<AllocationSites>(new AllocationSites());

	@Test
	public void testAssign()
			throws SemanticException {
		Constant one = new Constant(Int32Type.INSTANCE, 1, loc1);
		Constant zero = new Constant(Int32Type.INSTANCE, 0, loc1);
		Pair<HeapEnvironment<AllocationSites>,
				List<HeapReplacement>> assignResult = heap.assign(topHeap, x, one, pp1, fakeOracle);

		// constants do not affect heap abstract domain
		assertEquals(topHeap, assignResult.getLeft());

		assignResult = heap.assign(
				topHeap,
				x,
				new BinaryExpression(intType, one, zero, NumericNonOverflowingAdd.INSTANCE, loc1),
				pp1,
				fakeOracle);

		// binary expressions do not affect heap abstract domain
		assertEquals(topHeap, assignResult.getLeft());

		// from empty environment, assignment x = *(pp1, fakeOracle)
		// expected: x -> pp1
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xAssign = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1));
		HeapEnvironment<AllocationSites> expectedEnv = emptyHeapEnv.putState(x, xSites);
		assertEquals(expectedEnv, xAssign.getLeft());

		// from x -> pp1, assignment x = *(pp2, fakeOracle)
		// expected: x -> pp2
		Pair<HeapEnvironment<AllocationSites>,
				List<HeapReplacement>> actual = heap.assign(
						xAssign.getLeft(),
						x,
						new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2),
						pp2,
						fakeOracle);

		xSites = new AllocationSites(Collections.singleton(alloc2));
		expectedEnv = emptyHeapEnv.putState(x, xSites);
		assertEquals(expectedEnv, actual.getLeft());
	}

	@Test
	public void testSmallStepSemantic()
			throws SemanticException {
		// The current implementation of semanticsOf returns this;
		// we test the method checking that rewriting a heap expression
		// in this and in its semanticsOf results produced the same
		// result.

		// 1. Heap allocation
		HeapExpression heapExpression = new MemoryAllocation(untyped, loc1, new Annotations());

		// from topState
		Pair<HeapEnvironment<AllocationSites>,
				List<HeapReplacement>> sss = heap.semanticsOf(topHeap, heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(topHeap, heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// from bottomState
		sss = heap.semanticsOf(bottomHeap, heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(bottomHeap, heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// from x -> pp1
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xToLoc1 = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		sss = heap.semanticsOf(xToLoc1.getLeft(), heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(xToLoc1.getLeft(), heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// 2. Heap reference
		heapExpression = new HeapReference(untyped, new MemoryAllocation(untyped, loc1, new Annotations()), loc1);

		// from topState
		sss = heap.semanticsOf(topHeap, heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(topHeap, heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// from bottomState
		sss = heap.semanticsOf(bottomHeap, heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(bottomHeap, heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// from x -> pp1
		sss = heap.semanticsOf(xToLoc1.getLeft(), heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(xToLoc1.getLeft(), heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// 3. Access child
		heapExpression = new AccessChild(untyped, x, y, loc1);

		// from topState
		sss = heap.semanticsOf(topHeap, heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(topHeap, heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// from bottomState
		sss = heap.semanticsOf(bottomHeap, heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(bottomHeap, heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// from x -> pp1
		sss = heap.semanticsOf(xToLoc1.getLeft(), heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(xToLoc1.getLeft(), heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// 4. Heap dereference
		heapExpression = new HeapDereference(
				untyped,
				new HeapReference(untyped, new MemoryAllocation(untyped, loc1, new Annotations()), loc1),
				loc1);

		// from topState
		sss = heap.semanticsOf(topHeap, heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(topHeap, heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// from bottomState
		sss = heap.semanticsOf(bottomHeap, heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(bottomHeap, heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));

		// from x -> pp1
		sss = heap.semanticsOf(xToLoc1.getLeft(), heapExpression, pp1, fakeOracle);
		assertEquals(
				heap.rewrite(xToLoc1.getLeft(), heapExpression, pp1, fakeOracle),
				heap.rewrite(sss.getLeft(), heapExpression, pp1, fakeOracle));
	}

	@Test
	public void testLub()
			throws SemanticException {
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xToLoc1 = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xToLoc2 = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> yToLoc2 = heap
				.assign(topHeap, y, new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		// top lub <any heap> or <any heap> lub top = top
		assertTrue(topHeap.lub(topHeap).isTop());
		assertTrue(topHeap.lub(xToLoc1.getLeft()).isTop());
		assertTrue(xToLoc1.getLeft().lub(topHeap).isTop());

		// <any heap> lub bottom or bottom lub <any heap> = <any heap>
		assertTrue(bottomHeap.lub(bottomHeap).isBottom());
		assertEquals(xToLoc1.getLeft(), xToLoc1.getLeft().lub(bottomHeap));
		assertEquals(xToLoc1.getLeft(), bottomHeap.lub(xToLoc1.getLeft()));

		// <any heap> lub <any heap> = <any heap>
		assertEquals(xToLoc1.getLeft(), xToLoc1.getLeft().lub(xToLoc1.getLeft()));

		// x -> pp1 lub y -> pp1 = x -> pp1, y -> pp1
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1));
		AllocationSites ySites = new AllocationSites(Collections.singleton(alloc2));

		HeapEnvironment<AllocationSites> expectedEnv = emptyHeapEnv.putState(x, xSites);
		expectedEnv = expectedEnv.putState(y, ySites);

		assertEquals(expectedEnv, xToLoc1.getLeft().lub(yToLoc2.getLeft()));

		// x -> pp1 lub x -> pp2 = x -> pp1,pp2
		HashSet<AllocationSite> xSet = new HashSet<>();
		xSet.add(alloc1);
		xSet.add(alloc2);
		xSites = new AllocationSites().mk(xSet);
		expectedEnv = emptyHeapEnv.putState(x, xSites);

		assertEquals(expectedEnv, xToLoc1.getLeft().lub(xToLoc2.getLeft()));
	}

	@Test
	public void testWidening()
			throws SemanticException {
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xToLoc1 = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xToLoc2 = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> yToLoc2 = heap
				.assign(topHeap, y, new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		// top lub <any heap> or <any heap> lub top = top
		assertTrue(topHeap.widening(topHeap).isTop());
		assertTrue(topHeap.widening(xToLoc1.getLeft()).isTop());
		assertTrue(xToLoc1.getLeft().widening(topHeap).isTop());

		// <any heap> lub bottom or bottom lub <any heap> = <any heap>
		assertTrue(bottomHeap.widening(bottomHeap).isBottom());
		assertEquals(xToLoc1.getLeft(), xToLoc1.getLeft().widening(bottomHeap));
		assertEquals(xToLoc1.getLeft(), bottomHeap.widening(xToLoc1.getLeft()));

		// <any heap> lub <any heap> = <any heap>
		assertEquals(xToLoc1.getLeft(), xToLoc1.getLeft().widening(xToLoc1.getLeft()));

		// x -> pp1 lub y -> pp1 = x -> pp1, y -> pp1
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1));
		AllocationSites ySites = new AllocationSites(Collections.singleton(alloc2));

		HeapEnvironment<AllocationSites> expectedEnv = emptyHeapEnv.putState(x, xSites);
		expectedEnv = expectedEnv.putState(y, ySites);

		assertEquals(expectedEnv, xToLoc1.getLeft().widening(yToLoc2.getLeft()));

		// x -> pp1 lub x -> pp2 = x -> pp1,pp2
		HashSet<AllocationSite> xSet = new HashSet<>();
		xSet.add(alloc1);
		xSet.add(alloc2);
		xSites = new AllocationSites().mk(xSet);
		expectedEnv = emptyHeapEnv.putState(x, xSites);

		assertEquals(expectedEnv, xToLoc1.getLeft().widening(xToLoc2.getLeft()));
	}

	@Test
	public void testLessOrEquals()
			throws SemanticException {
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xAssign = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> yAssign = heap
				.assign(topHeap, y, new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);

		// <any heap> <= top
		assertTrue(topHeap.lessOrEqual(topHeap));
		assertTrue(bottomHeap.lessOrEqual(topHeap));
		assertTrue(xAssign.getLeft().lessOrEqual(topHeap));

		// bottom <= <any heap>
		assertTrue(bottomHeap.lessOrEqual(bottomHeap));
		assertTrue(bottomHeap.lessOrEqual(xAssign.getLeft()));

		// <any heap> <= <any heap>
		assertTrue(xAssign.getLeft().lessOrEqual(xAssign.getLeft()));

		// x -> pp1 </= y -> pp2
		assertFalse(xAssign.getLeft().lessOrEqual(yAssign.getLeft()));
		assertFalse(yAssign.getLeft().lessOrEqual(xAssign.getLeft()));

		HeapEnvironment<AllocationSites> xyAssign = xAssign.getLeft().lub(yAssign.getLeft());

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
		assertEquals(topHeap, topHeap.forgetIdentifier(x, pp1).getLeft());
		assertEquals(bottomHeap, bottomHeap.forgetIdentifier(x, pp1).getLeft());

		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> result = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		assertEquals(emptyHeap, result.getLeft().forgetIdentifier(x, pp1).getLeft());
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

		assertEquals(topHeap, topHeap.pushScope(token, pp1).getLeft());
		assertEquals(bottomHeap, bottomHeap.pushScope(token, pp1).getLeft());

		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xAssign = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		Pair<HeapEnvironment<AllocationSites>,
				List<HeapReplacement>> xPushedScopeAssign = heap.assign(
						topHeap,
						new OutOfScopeIdentifier(x, token, loc1),
						new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1),
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

		assertEquals(topHeap, topHeap.popScope(token, pp1).getLeft());
		assertEquals(bottomHeap, bottomHeap.popScope(token, pp1).getLeft());

		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xAssign = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);

		Pair<HeapEnvironment<AllocationSites>,
				List<HeapReplacement>> xScopedAssign = heap.assign(
						topHeap,
						(Identifier) x.pushScope(token, pp1),
						new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1),
						pp1,
						fakeOracle);

		// [scoped]x -> pp1 popScope = x -> pp1
		assertEquals(xAssign.getLeft(), xScopedAssign.getLeft().popScope(token, pp1).getLeft());

		// x -> pp1 popScope = empty environment
		assertEquals(emptyHeap, xAssign.getLeft().popScope(token, pp1).getLeft());
	}

	@Test
	public void testAccessChildRewrite()
			throws SemanticException {
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xAssign = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		// x.y rewritten in x -> pp1 = pp1
		AccessChild accessChild = new AccessChild(untyped, x, y, loc1);

		ExpressionSet expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, heap.rewrite(xAssign.getLeft(), accessChild, pp1, fakeOracle));

		// y.x rewritten in x -> pp1 = empty set
		accessChild = new AccessChild(untyped, y, x, loc1);
		assertEquals(new ExpressionSet(), heap.rewrite(xAssign.getLeft(), accessChild, pp1, fakeOracle));
	}

	@Test
	public void testIdentifierRewrite()
			throws SemanticException {
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xAssign = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		// x rewritten in x -> pp1 = pp1
		ExpressionSet expectedRewritten = new ExpressionSet(x);
		assertEquals(expectedRewritten, heap.rewrite(xAssign.getLeft(), x, pp1, fakeOracle));

		// y rewritten in x -> pp1 = {y}
		assertEquals(new ExpressionSet(y), heap.rewrite(xAssign.getLeft(), y, pp1, fakeOracle));
	}

	@Test
	public void testHeapDereferenceRewrite()
			throws SemanticException {
		// *(&(new loc(pp1, fakeOracle)) rewritten in top -> pp1
		HeapDereference deref = new HeapDereference(
				untyped,
				new HeapReference(untyped, new MemoryAllocation(untyped, loc1, new Annotations()), loc1),
				loc1);

		ExpressionSet expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, heap.rewrite(topHeap, deref, pp1, fakeOracle));

		// *(x) rewritten in x -> pp1 -> pp1
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xAssign = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		deref = new HeapDereference(untyped, x, loc1);
		expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, heap.rewrite(xAssign.getLeft(), deref, pp1, fakeOracle));

		// *(y) rewritten in x -> pp1 -> empty set
		AllocationSite expectedUnknownAlloc = new StackAllocationSite(untyped, "unknown@y", true, loc1);
		deref = new HeapDereference(untyped, y, loc1);
		expectedRewritten = new ExpressionSet(expectedUnknownAlloc);
		assertEquals(expectedRewritten, heap.rewrite(xAssign.getLeft(), deref, pp1, fakeOracle));
	}

	@Test
	public void testIssue300()
			throws SemanticException {
		// ((type) x).f rewritten in x -> pp1 -> pp1
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xAssign = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		SymbolicExpression e = new AccessChild(
				intType,
				new BinaryExpression(
						untyped,
						x,
						new Constant(new TypeTokenType(Collections.singleton(intType)), intType, loc1),
						TypeConv.INSTANCE,
						loc1),
				new Constant(intType, 1, loc1),
				loc1);
		ExpressionSet expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, heap.rewrite(xAssign.getLeft(), e, pp1, fakeOracle));
	}

	@Test
	public void testConvOfAlloc()
			throws SemanticException {
		// (ref(alloc) conv-as type) rewritten in &alloc.loc
		SymbolicExpression e = new BinaryExpression(
				untyped,
				new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1),
				new Constant(new TypeTokenType(Collections.singleton(intType)), intType, loc1),
				TypeConv.INSTANCE,
				loc1);
		Pair<HeapEnvironment<AllocationSites>,
				List<HeapReplacement>> xAssign = heap.assign(topHeap, x, e, pp1, fakeOracle);
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1));
		assertEquals(emptyHeapEnv.putState(x, xSites), xAssign.getLeft());
	}

}
