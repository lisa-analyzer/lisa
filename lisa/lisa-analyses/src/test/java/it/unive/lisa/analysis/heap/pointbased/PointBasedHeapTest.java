package it.unive.lisa.analysis.heap.pointbased;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
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
			return null;
		}
	};

	private final ProgramPoint pp2 = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return loc2;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	private final SemanticOracle fakeOracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final Variable x = new Variable(untyped, "x", pp1.getLocation());
	private final Variable y = new Variable(untyped, "y", pp1.getLocation());

	private final PointBasedHeap emptyHeap = new PointBasedHeap();
	private final PointBasedHeap topHeap = new PointBasedHeap().top();
	private final PointBasedHeap bottomHeap = new PointBasedHeap().bottom();

	private final HeapEnvironment<
			AllocationSites> emptyHeapEnv = new HeapEnvironment<AllocationSites>(new AllocationSites());

	@Test
	public void testAssign() throws SemanticException {
		Constant one = new Constant(Int32Type.INSTANCE, 1, loc1);
		Constant zero = new Constant(Int32Type.INSTANCE, 0, loc1);
		PointBasedHeap assignResult = topHeap.assign(x, one, pp1, fakeOracle);

		// constants do not affect heap abstract domain
		assertTrue(topHeap.equalUpToSubs(assignResult));

		assignResult = topHeap.assign(x,
				new BinaryExpression(intType, one, zero, NumericNonOverflowingAdd.INSTANCE, loc1),
				pp1, fakeOracle);

		// binary expressions do not affect heap abstract domain
		assertTrue(topHeap.equalUpToSubs(assignResult));

		// from empty environment, assignment x = *(pp1, fakeOracle)
		// expected: x -> pp1
		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);

		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1), false);
		HeapEnvironment<AllocationSites> expectedEnv = emptyHeapEnv.putState(x, xSites);
		assertTrue(new PointBasedHeap(expectedEnv).equalUpToSubs(xAssign));

		// from x -> pp1, assignment x = *(pp2, fakeOracle)
		// expected: x -> pp2
		PointBasedHeap actual = xAssign.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc2), loc2),
				pp2, fakeOracle);

		xSites = new AllocationSites(Collections.singleton(alloc2), false);
		expectedEnv = emptyHeapEnv.putState(x, xSites);
		assertTrue(new PointBasedHeap(expectedEnv).equalUpToSubs(actual));
	}

	@Test
	public void testSmallStepSemantic() throws SemanticException {
		// The current implementation of semanticsOf returns this;
		// we test the method checking that rewriting a heap expression
		// in this and in its semanticsOf results produced the same
		// result.

		// 1. Heap allocation
		HeapExpression heapExpression = new MemoryAllocation(untyped, loc1, new Annotations());

		// from topState
		PointBasedHeap sss = topHeap.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				topHeap.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// from bottomState
		sss = bottomHeap.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				bottomHeap.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// from x -> pp1
		PointBasedHeap xToLoc1 = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);
		sss = xToLoc1.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				xToLoc1.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// 2. Heap reference
		heapExpression = new HeapReference(untyped,
				new MemoryAllocation(untyped, loc1, new Annotations()), loc1);

		// from topState
		sss = topHeap.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				topHeap.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// from bottomState
		sss = bottomHeap.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				bottomHeap.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// from x -> pp1
		sss = xToLoc1.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				xToLoc1.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// 3. Access child
		heapExpression = new AccessChild(untyped, x, y, loc1);

		// from topState
		sss = topHeap.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				topHeap.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// from bottomState
		sss = bottomHeap.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				bottomHeap.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// from x -> pp1
		sss = xToLoc1.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				xToLoc1.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// 4. Heap dereference
		heapExpression = new HeapDereference(untyped, new HeapReference(untyped,
				new MemoryAllocation(untyped, loc1, new Annotations()), loc1), loc1);

		// from topState
		sss = topHeap.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				topHeap.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// from bottomState
		sss = bottomHeap.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				bottomHeap.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));

		// from x -> pp1
		sss = xToLoc1.semanticsOf(heapExpression, pp1, fakeOracle);
		assertEquals(
				xToLoc1.rewrite(heapExpression, pp1, fakeOracle),
				sss.rewrite(heapExpression, pp1, fakeOracle));
	}

	@Test
	public void testLub() throws SemanticException {
		PointBasedHeap xToLoc1 = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);

		PointBasedHeap xToLoc2 = topHeap.assign(x,
				new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2),
				pp2, fakeOracle);

		PointBasedHeap yToLoc2 = topHeap.assign(y,
				new HeapReference(untyped, new MemoryAllocation(untyped, loc2), loc2),
				pp2, fakeOracle);

		// top lub <any heap> or <any heap> lub top = top
		assertTrue(topHeap.lub(xToLoc1).isTop());
		assertTrue(xToLoc1.lub(topHeap).isTop());
		assertTrue(topHeap.lub(topHeap).isTop());

		// <any heap> lub bottom or bottom lub <any heap> = <any heap>
		assertEquals(xToLoc1, xToLoc1.lub(bottomHeap));
		assertEquals(xToLoc1, bottomHeap.lub(xToLoc1));
		assertTrue(bottomHeap.lub(bottomHeap).isBottom());

		// <any heap> lub <any heap> = <any heap>
		assertEquals(xToLoc1, xToLoc1.lub(xToLoc1));

		// x -> pp1 lub y -> pp1 = x -> pp1, y -> pp1
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1), false);
		AllocationSites ySites = new AllocationSites(Collections.singleton(alloc2), false);

		HeapEnvironment<AllocationSites> expectedEnv = emptyHeapEnv.putState(x, xSites);
		expectedEnv = expectedEnv.putState(y, ySites);

		assertEquals(new PointBasedHeap(expectedEnv), xToLoc1.lub(yToLoc2));

		// x -> pp1 lub x -> pp2 = x -> pp1,pp2
		HashSet<AllocationSite> xSet = new HashSet<>();
		xSet.add(alloc1);
		xSet.add(alloc2);
		xSites = new AllocationSites().mk(xSet);
		expectedEnv = emptyHeapEnv.putState(x, xSites);

		assertEquals(new PointBasedHeap(expectedEnv), xToLoc1.lub(xToLoc2));
	}

	@Test
	public void testWidening() throws SemanticException {
		PointBasedHeap xToLoc1 = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);

		PointBasedHeap xToLoc2 = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc2), loc2),
				pp2, fakeOracle);

		PointBasedHeap yToLoc2 = topHeap.assign(y,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc2), loc2),
				pp2, fakeOracle);

		// top lub <any heap> or <any heap> lub top = top
		assertTrue(topHeap.widening(xToLoc1).isTop());
		assertTrue(xToLoc1.widening(topHeap).isTop());
		assertTrue(topHeap.widening(topHeap).isTop());

		// <any heap> lub bottom or bottom lub <any heap> = <any heap>
		assertEquals(xToLoc1, xToLoc1.widening(bottomHeap));
		assertEquals(xToLoc1, bottomHeap.widening(xToLoc1));
		assertTrue(bottomHeap.widening(bottomHeap).isBottom());

		// <any heap> lub <any heap> = <any heap>
		assertEquals(xToLoc1, xToLoc1.widening(xToLoc1));

		// x -> pp1 lub y -> pp1 = x -> pp1, y -> pp1
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1), false);
		AllocationSites ySites = new AllocationSites(Collections.singleton(alloc2), false);

		HeapEnvironment<AllocationSites> expectedEnv = emptyHeapEnv.putState(x, xSites);
		expectedEnv = expectedEnv.putState(y, ySites);

		assertEquals(new PointBasedHeap(expectedEnv), xToLoc1.widening(yToLoc2));

		// x -> pp1 lub x -> pp2 = x -> pp1,pp2
		HashSet<AllocationSite> xSet = new HashSet<>();
		xSet.add(alloc1);
		xSet.add(alloc2);
		xSites = new AllocationSites().mk(xSet);
		expectedEnv = emptyHeapEnv.putState(x, xSites);

		assertEquals(new PointBasedHeap(expectedEnv), xToLoc1.widening(xToLoc2));
	}

	@Test
	public void testLessOrEquals() throws SemanticException {

		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);

		PointBasedHeap yAssign = topHeap.assign(y,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc2), loc2),
				pp2, fakeOracle);

		// <any heap> <= top
		assertTrue(topHeap.lessOrEqual(topHeap));
		assertTrue(xAssign.lessOrEqual(topHeap));
		assertTrue(bottomHeap.lessOrEqual(topHeap));

		// bottom <= <any heap>
		assertTrue(bottomHeap.lessOrEqual(bottomHeap));
		assertTrue(bottomHeap.lessOrEqual(xAssign));

		// <any heap> <= <any heap>
		assertTrue(xAssign.lessOrEqual(xAssign));

		// x -> pp1 </= y -> pp2
		assertFalse(xAssign.lessOrEqual(yAssign));
		assertFalse(yAssign.lessOrEqual(xAssign));

		PointBasedHeap xyAssign = xAssign.lub(yAssign);

		// x -> pp1 <= x -> pp1 y -> pp2
		// y -> pp2 <= x -> pp1 y -> pp2
		// x -> pp1 y -> pp2 <= x -> pp1 y -> pp2
		assertTrue(xAssign.lessOrEqual(xyAssign));
		assertTrue(yAssign.lessOrEqual(xyAssign));
		assertTrue(xyAssign.lessOrEqual(xyAssign));
	}

	@Test
	public void testForgetIdentifier() throws SemanticException {
		PointBasedHeap result = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);

		assertTrue(emptyHeap.equalUpToSubs(result.forgetIdentifier(x, pp1)));
		assertTrue(result.equalUpToSubs(result.forgetIdentifier(y, pp1)));

		assertTrue(topHeap.equalUpToSubs(topHeap.forgetIdentifier(x, pp1)));
		assertTrue(bottomHeap.equalUpToSubs(bottomHeap.forgetIdentifier(x, pp1)));
	}

	@Test
	public void testPushScope() throws SemanticException {
		ScopeToken token = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				return new SourceCodeLocation("fake", 0, 0);
			}
		});

		assertTrue(topHeap.equalUpToSubs(topHeap.pushScope(token, pp1)));
		assertTrue(bottomHeap.equalUpToSubs(bottomHeap.pushScope(token, pp1)));

		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);
		PointBasedHeap xPushedScopeAssign = topHeap.assign(
				new OutOfScopeIdentifier(x, token, loc1),
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);

		// x -> pp1 pushScope = [out-of-scope-id]x -> pp1
		assertTrue(xPushedScopeAssign.equalUpToSubs(xAssign.pushScope(token, pp1)));
	}

	@Test
	public void testPopScope() throws SemanticException {
		ScopeToken token = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				return new SourceCodeLocation("fake", 0, 0);
			}
		});

		assertTrue(topHeap.equalUpToSubs(topHeap.popScope(token, pp1)));
		assertTrue(bottomHeap.equalUpToSubs(bottomHeap.popScope(token, pp1)));

		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);

		PointBasedHeap xScopedAssign = topHeap.assign((Identifier) x.pushScope(token, pp1),
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);

		// [scoped]x -> pp1 popScope = x -> pp1
		assertTrue(xAssign.equalUpToSubs(xScopedAssign.popScope(token, pp1)));

		// x -> pp1 popScope = empty environment
		assertTrue(emptyHeap.equalUpToSubs(xAssign.popScope(token, pp1)));
	}

	@Test
	public void testAccessChildRewrite() throws SemanticException {
		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);
		// x.y rewritten in x -> pp1 = pp1
		AccessChild accessChild = new AccessChild(untyped, x, y, loc1);

		ExpressionSet expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, xAssign.rewrite(accessChild, pp1, fakeOracle));

		// y.x rewritten in x -> pp1 = empty set
		accessChild = new AccessChild(untyped, y, x, loc1);
		assertEquals(new ExpressionSet(), xAssign.rewrite(accessChild, pp1, fakeOracle));
	}

	@Test
	public void testIdentifierRewrite() throws SemanticException {
		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);
		// x rewritten in x -> pp1 = pp1
		ExpressionSet expectedRewritten = new ExpressionSet(x);
		assertEquals(expectedRewritten, xAssign.rewrite(x, pp1, fakeOracle));

		// y rewritten in x -> pp1 = {y}
		assertEquals(new ExpressionSet(y), xAssign.rewrite(y, pp1, fakeOracle));
	}

	@Test
	public void testHeapDereferenceRewrite() throws SemanticException {
		// *(&(new loc(pp1, fakeOracle)) rewritten in top -> pp1
		HeapDereference deref = new HeapDereference(untyped, new HeapReference(untyped,
				new MemoryAllocation(untyped, loc1, new Annotations()), loc1), loc1);

		ExpressionSet expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, topHeap.rewrite(deref, pp1, fakeOracle));

		// *(x) rewritten in x -> pp1 -> pp1
		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);
		deref = new HeapDereference(untyped, x, loc1);
		expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, xAssign.rewrite(deref, pp1, fakeOracle));

		// *(y) rewritten in x -> pp1 -> empty set
		AllocationSite expectedUnknownAlloc = new StackAllocationSite(untyped, "unknown@y", true, loc1);
		deref = new HeapDereference(untyped, y, loc1);
		expectedRewritten = new ExpressionSet(expectedUnknownAlloc);
		assertEquals(expectedRewritten, xAssign.rewrite(deref, pp1, fakeOracle));
	}

	@Test
	public void testIssue300() throws SemanticException {
		// ((type) x).f rewritten in x -> pp1 -> pp1
		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new MemoryAllocation(untyped, loc1), loc1),
				pp1, fakeOracle);
		SymbolicExpression e = new AccessChild(intType,
				new BinaryExpression(untyped,
						x,
						new Constant(new TypeTokenType(Collections.singleton(intType)), intType, loc1),
						TypeConv.INSTANCE,
						loc1),
				new Constant(intType, 1, loc1), loc1);
		ExpressionSet expectedRewritten = new ExpressionSet(alloc1);
		assertEquals(expectedRewritten, xAssign.rewrite(e, pp1, fakeOracle));
	}

	@Test
	public void testConvOfAlloc() throws SemanticException {
		// (ref(alloc) conv-as type) rewritten in &alloc.loc
		SymbolicExpression e = new BinaryExpression(untyped,
				new HeapReference(untyped, new MemoryAllocation(untyped, loc1), loc1),
				new Constant(new TypeTokenType(Collections.singleton(intType)), intType, loc1),
				TypeConv.INSTANCE,
				loc1);
		PointBasedHeap xAssign = topHeap.assign(x, e, pp1, fakeOracle);
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1), false);
		assertTrue(emptyHeapEnv.putState(x, xSites).equalUpToSubs(xAssign.heapEnv));
	}
}
