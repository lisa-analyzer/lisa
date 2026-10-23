package it.unive.lisa.analysis.heap.pointbased;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.heap.allocations.AllocationSite;
import it.unive.lisa.lattices.heap.allocations.AllocationSites;
import it.unive.lisa.lattices.heap.allocations.HeapAllocationSite;
import it.unive.lisa.lattices.heap.allocations.NullAllocationSite;
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
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.InMemoryType;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

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

	// an empty function is normalized to top by FunctionalLattice (see its
	// two-arg constructor), so emptyHeap above is actually top: pointer
	// comparisons must be run against a state that carries at least one
	// (irrelevant) binding, otherwise satisfies/alias/isReachableFrom would
	// all short-circuit on their own "state.isTop()" checks
	private final HeapEnvironment<AllocationSites> nonTopHeap = emptyHeapEnv.putState(
			new Variable(untyped, "unrelated", loc1), new AllocationSites(Collections.singleton(alloc1)));

	private final AllocationSite strongAlloc1 = new HeapAllocationSite(untyped, "strong1", false, loc1);

	private final AllocationSite strongAlloc2 = new HeapAllocationSite(untyped, "strong2", false, loc2);

	private final Type inMemoryType = new InMemoryType() {

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other == this;
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return other == this ? this : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

	};

	private MemoryPointer pointerTo(
			AllocationSite site) {
		return new MemoryPointer(untyped, site, site.getCodeLocation());
	}

	private BinaryExpression eq(
			SymbolicExpression left,
			SymbolicExpression right) {
		return new BinaryExpression(untyped, left, right, ComparisonEq.INSTANCE, loc1);
	}

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

	@Test
	public void testSatisfiesEqualityBetweenPointers()
			throws SemanticException {
		// two pointers to the same strong site must be equal
		assertEquals(
				Satisfiability.SATISFIED,
				heap.satisfies(nonTopHeap, eq(pointerTo(strongAlloc1), pointerTo(strongAlloc1)), pp1, fakeOracle));

		// two pointers to different strong sites can never be equal
		assertEquals(
				Satisfiability.NOT_SATISFIED,
				heap.satisfies(nonTopHeap, eq(pointerTo(strongAlloc1), pointerTo(strongAlloc2)), pp1, fakeOracle));

		// a strong site can never be proven equal to a different weak one
		assertEquals(
				Satisfiability.UNKNOWN,
				heap.satisfies(nonTopHeap, eq(pointerTo(strongAlloc1), pointerTo(alloc1)), pp1, fakeOracle));

		// a weak site (possibly representing more than one runtime object at
		// once) can never be proven equal to itself either
		assertEquals(
				Satisfiability.UNKNOWN,
				heap.satisfies(nonTopHeap, eq(pointerTo(alloc1), pointerTo(alloc1)), pp1, fakeOracle));
	}

	@Test
	public void testSatisfiesEqualityWithNull()
			throws SemanticException {
		MemoryPointer nullPointer = new MemoryPointer(untyped, NullAllocationSite.INSTANCE, loc1);

		assertEquals(
				Satisfiability.SATISFIED,
				heap.satisfies(nonTopHeap, eq(nullPointer, nullPointer), pp1, fakeOracle));
		assertEquals(
				Satisfiability.NOT_SATISFIED,
				heap.satisfies(nonTopHeap, eq(nullPointer, pointerTo(strongAlloc1)), pp1, fakeOracle));
		assertEquals(
				Satisfiability.NOT_SATISFIED,
				heap.satisfies(nonTopHeap, eq(pointerTo(strongAlloc1), nullPointer), pp1, fakeOracle));
	}

	@Test
	public void testSatisfiesLogicalOperators()
			throws SemanticException {
		BinaryExpression sameStrong = eq(pointerTo(strongAlloc1), pointerTo(strongAlloc1));
		BinaryExpression diffStrong = eq(pointerTo(strongAlloc1), pointerTo(strongAlloc2));

		// negating a satisfied equality must yield not satisfied
		assertEquals(
				Satisfiability.NOT_SATISFIED,
				heap.satisfies(
						nonTopHeap,
						new UnaryExpression(untyped, sameStrong, LogicalNegation.INSTANCE, loc1),
						pp1,
						fakeOracle));

		// != is the negation of ==
		BinaryExpression ne = new BinaryExpression(
				untyped, pointerTo(strongAlloc1), pointerTo(strongAlloc1), ComparisonNe.INSTANCE, loc1);
		assertEquals(Satisfiability.NOT_SATISFIED, heap.satisfies(nonTopHeap, ne, pp1, fakeOracle));

		// AND is satisfied only if both sides are; OR is satisfied if either is
		BinaryExpression and = new BinaryExpression(untyped, sameStrong, diffStrong, LogicalAnd.INSTANCE, loc1);
		assertEquals(Satisfiability.NOT_SATISFIED, heap.satisfies(nonTopHeap, and, pp1, fakeOracle));

		BinaryExpression or = new BinaryExpression(untyped, sameStrong, diffStrong, LogicalOr.INSTANCE, loc1);
		assertEquals(Satisfiability.SATISFIED, heap.satisfies(nonTopHeap, or, pp1, fakeOracle));
	}

	@Test
	public void testSatisfiesEdgeCases()
			throws SemanticException {
		// an unconstrained (top) state cannot prove anything, regardless of
		// the expression being evaluated
		assertEquals(
				Satisfiability.UNKNOWN,
				heap.satisfies(topHeap, eq(pointerTo(strongAlloc1), pointerTo(strongAlloc1)), pp1, fakeOracle));

		// an expression that is neither a negation nor a binary comparison
		// falls back to unknown
		assertEquals(Satisfiability.UNKNOWN, heap.satisfies(nonTopHeap, x, pp1, fakeOracle));

		// comparing two non-pointer expressions can never be proven aliased
		assertEquals(
				Satisfiability.UNKNOWN,
				heap.satisfies(
						nonTopHeap,
						eq(new Constant(intType, 1, loc1), new Constant(intType, 2, loc1)),
						pp1,
						fakeOracle));
	}

	@Test
	public void testAssume()
			throws SemanticException {
		BinaryExpression sat = eq(pointerTo(strongAlloc1), pointerTo(strongAlloc1));
		BinaryExpression notSat = eq(pointerTo(strongAlloc1), pointerTo(strongAlloc2));
		BinaryExpression unknown = eq(pointerTo(strongAlloc1), pointerTo(alloc1));

		// a satisfied or unknown condition never restricts the state
		assertEquals(nonTopHeap, heap.assume(nonTopHeap, sat, pp1, pp1, fakeOracle).getLeft());
		assertEquals(nonTopHeap, heap.assume(nonTopHeap, unknown, pp1, pp1, fakeOracle).getLeft());

		// a condition that can never be satisfied makes the state unreachable
		assertTrue(heap.assume(nonTopHeap, notSat, pp1, pp1, fakeOracle).getLeft().isBottom());
	}

	@Test
	public void testAlias()
			throws SemanticException {
		assertEquals(
				Satisfiability.SATISFIED,
				heap.alias(nonTopHeap, pointerTo(strongAlloc1), pointerTo(strongAlloc1), pp1, fakeOracle));
		assertEquals(
				Satisfiability.NOT_SATISFIED,
				heap.alias(nonTopHeap, pointerTo(strongAlloc1), pointerTo(strongAlloc2), pp1, fakeOracle));

		// per its javadoc, both expressions must be pointers for this to be
		// satisfied: plain, unresolved variables can never be aliases
		assertEquals(Satisfiability.NOT_SATISFIED, heap.alias(nonTopHeap, x, y, pp1, fakeOracle));

		// an unconstrained state cannot prove or disprove aliasing
		assertEquals(
				Satisfiability.UNKNOWN,
				heap.alias(topHeap, pointerTo(strongAlloc1), pointerTo(strongAlloc1), pp1, fakeOracle));

		// an unreachable state is bottom
		assertEquals(
				Satisfiability.BOTTOM,
				heap.alias(bottomHeap, pointerTo(strongAlloc1), pointerTo(strongAlloc1), pp1, fakeOracle));

		// x pointing to two different sites can only be an alias of one of
		// them if it is unknown which one x actually refers to at runtime
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xToLoc1 = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xToLoc2 = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);
		HeapEnvironment<AllocationSites> multiTarget = xToLoc1.getLeft().lub(xToLoc2.getLeft());
		SymbolicExpression xAsPointer = new HeapReference(untyped, x, loc1);
		assertEquals(
				Satisfiability.UNKNOWN,
				heap.alias(multiTarget, xAsPointer, pointerTo(alloc1), pp1, fakeOracle));
	}

	@Test
	public void testIsReachableFromDirectMatch()
			throws SemanticException {
		// when x itself already denotes the target location, no traversal of
		// the heap graph is needed and the check succeeds immediately
		assertEquals(
				Satisfiability.SATISFIED,
				heap.isReachableFrom(nonTopHeap, pointerTo(strongAlloc1), pointerTo(strongAlloc1), pp1, fakeOracle));
		assertEquals(
				Satisfiability.UNKNOWN,
				heap.isReachableFrom(topHeap, pointerTo(strongAlloc1), pointerTo(strongAlloc1), pp1, fakeOracle));
		assertEquals(
				Satisfiability.BOTTOM,
				heap.isReachableFrom(bottomHeap, pointerTo(strongAlloc1), pointerTo(strongAlloc1), pp1, fakeOracle));
	}

	@Test
	public void testIsReachableFromRequiresActuallyTraversingTheHeapGraph()
			throws SemanticException {
		HeapEnvironment<AllocationSites> direct = emptyHeapEnv.putState(x,
				new AllocationSites(Collections.singleton(alloc1)));
		assertEquals(Satisfiability.SATISFIED, heap.isReachableFrom(direct, x, alloc1, pp1, fakeOracle));

		// x points to alloc1, which is unrelated to alloc2: no path exists
		assertEquals(Satisfiability.NOT_SATISFIED, heap.isReachableFrom(direct, x, alloc2, pp1, fakeOracle));

		// x -> alloc1 -> alloc2 (alloc1's own field points to alloc2): alloc2
		// is reachable transitively, through two hops
		HeapEnvironment<AllocationSites> transitive = direct.putState(alloc1,
				new AllocationSites(Collections.singleton(alloc2)));
		assertEquals(Satisfiability.SATISFIED, heap.isReachableFrom(transitive, x, alloc2, pp1, fakeOracle));
	}

	@Test
	public void testShallowCopyClonesTheSiteInsteadOfAliasingIt()
			throws SemanticException {
		StackAllocationSite site = new StackAllocationSite(untyped, "stack@src", true, loc1);
		List<HeapReplacement> replacements = new ArrayList<>();

		HeapEnvironment<AllocationSites> result = heap.shallowCopy(topHeap, x, site, replacements);

		AllocationSites xSites = result.getState(x);
		assertEquals(1, xSites.size());
		AllocationSite clone = xSites.iterator().next();
		assertFalse(clone.equals(site), "the site must be cloned, not aliased");

		// the replacement must record that the clone AND the original site
		// are both valid targets for whoever held a reference to the source
		assertEquals(1, replacements.size());
		assertEquals(Set.of(site), replacements.get(0).getSources());
		assertEquals(Set.of(clone, site), replacements.get(0).getTargets());
	}

	@Test
	public void testAssignThroughADereferencedPointerAliasesTheReferencedLocation()
			throws SemanticException {
		// *p = &(new loc2), where p already points to strongAlloc1: this must
		// make strongAlloc1 itself point to the new location, rather than
		// rebinding p
		MemoryPointer p = pointerTo(strongAlloc1);
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> result = heap.assign(
				topHeap, p, new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2, fakeOracle);

		AllocationSites expectedSites = new AllocationSites(Collections.singleton(alloc2));
		assertEquals(expectedSites, result.getLeft().getState(strongAlloc1));
	}

	@Test
	public void testStoreMergesValuesForWeakIdentifiers()
			throws SemanticException {
		StackAllocationSite weakField = new StackAllocationSite(untyped, "field@x", true, loc1);

		HeapEnvironment<AllocationSites> afterFirst = heap.store(topHeap, weakField, alloc1);
		HeapEnvironment<AllocationSites> afterSecond = heap.store(afterFirst, weakField, alloc2);

		AllocationSites expected = new AllocationSites(Collections.singleton(alloc1))
				.lub(new AllocationSites(Collections.singleton(alloc2)));
		assertEquals(expected, afterSecond.getState(weakField));
	}

	@Test
	public void testStoreSkipsAssignmentToTheNullIdentifier()
			throws SemanticException {
		assertTrue(heap.store(topHeap, NullAllocationSite.INSTANCE, alloc1).isBottom());
	}

	@Test
	public void testStoreNeverPersistsTheAllocationFlagOfTheStoredSite()
			throws SemanticException {
		// a site marked as "just allocated" must be normalized before being
		// stored, otherwise it would keep looking like a fresh allocation
		// every time it is retrieved from the environment
		HeapAllocationSite justAllocated = new HeapAllocationSite(untyped, "fresh", true, loc1);
		justAllocated.setAllocation(true);

		HeapEnvironment<AllocationSites> result = heap.store(topHeap, x, justAllocated);
		AllocationSite stored = result.getState(x).iterator().next();
		assertFalse(stored.isAllocation());
	}

	@Test
	public void testReassigningAStrongIdentifierReportsItsOldAllocationAsGarbage()
			throws SemanticException {
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> firstAssign = heap.assign(
				topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1, fakeOracle);

		// x is strong and was already bound to alloc1: rebinding it to a
		// different, unrelated allocation must report alloc1 (now
		// unreachable) so that callers can garbage collect it
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> secondAssign = heap.assign(
				firstAssign.getLeft(), x, new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
				fakeOracle);

		assertFalse(secondAssign.getRight().isEmpty());
		boolean alloc1Reported = secondAssign.getRight().stream()
				.anyMatch(r -> r.getSources().contains(alloc1) || r.getSources().contains(x));
		assertTrue(alloc1Reported, "reassigning x away from its only allocation must report it as garbage");
	}

	@Test
	public void testRewritePushAny()
			throws SemanticException {
		PushAny pointerPushAny = new PushAny(new ReferenceType(intType), loc1);
		ExpressionSet pointerResult = heap.rewritePushAny(pointerPushAny, topHeap, pp1, fakeOracle);
		assertEquals(1, pointerResult.elements().size());
		assertTrue(pointerResult.elements().iterator().next() instanceof MemoryPointer);

		PushAny inMemoryPushAny = new PushAny(inMemoryType, loc1);
		ExpressionSet inMemoryResult = heap.rewritePushAny(inMemoryPushAny, topHeap, pp1, fakeOracle);
		assertEquals(1, inMemoryResult.elements().size());
		assertTrue(inMemoryResult.elements().iterator().next() instanceof MemoryPointer);

		// a plain value type is left untouched: it is not something this
		// domain can allocate
		PushAny plainPushAny = new PushAny(intType, loc1);
		assertEquals(new ExpressionSet(plainPushAny), heap.rewritePushAny(plainPushAny, topHeap, pp1, fakeOracle));
	}

	@Test
	public void testRewriteNullConstant()
			throws SemanticException {
		ExpressionSet result = heap.rewriteNullConstant(new NullConstant(loc1), topHeap, pp1, fakeOracle);
		assertEquals(1, result.elements().size());
		SymbolicExpression rewritten = result.elements().iterator().next();
		assertTrue(rewritten instanceof MemoryPointer);
		assertEquals(NullAllocationSite.INSTANCE, ((MemoryPointer) rewritten).getReferencedLocation());
	}

	@Test
	public void testRewriteWithMultipleTargetsCoversAllOfThem()
			throws SemanticException {
		// once x may point to either alloc1 or alloc2, rewriting an
		// expression built on top of x must account for both, not just one
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xToLoc1 = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1), pp1,
						fakeOracle);
		Pair<HeapEnvironment<AllocationSites>, List<HeapReplacement>> xToLoc2 = heap
				.assign(topHeap, x, new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2), pp2,
						fakeOracle);
		HeapEnvironment<AllocationSites> multiTarget = xToLoc1.getLeft().lub(xToLoc2.getLeft());

		AccessChild accessChild = new AccessChild(untyped, x, y, loc1);
		ExpressionSet rewrittenAccess = heap.rewrite(multiTarget, accessChild, pp1, fakeOracle);
		assertEquals(2, rewrittenAccess.elements().size());

		HeapDereference deref = new HeapDereference(untyped, x, loc1);
		ExpressionSet rewrittenDeref = heap.rewrite(multiTarget, deref, pp1, fakeOracle);
		assertEquals(2, rewrittenDeref.elements().size());
	}

}
