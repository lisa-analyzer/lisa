package it.unive.lisa.analysis.heap.pointbased;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.imp.types.IntType;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;

public class PointBasedHeapTest {
	private final ExternalSet<Type> untyped = Caches.types().mkSingletonSet(Untyped.INSTANCE);
	private final ExternalSet<Type> intType = Caches.types().mkSingletonSet(IntType.INSTANCE);

	private final CodeLocation loc1 = new SourceCodeLocation("fake", 1, 1);
	private final CodeLocation loc2 = new SourceCodeLocation("fake", 2, 2);

	private final AllocationSite alloc1 = new AllocationSite(untyped, loc1.getCodeLocation(), true, loc1);
	private final AllocationSite alloc2 = new AllocationSite(untyped, loc2.getCodeLocation(), true, loc2);

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

	private final CodeLocation fakeLocation = new SourceCodeLocation("fake", 0, 0);

	private final ProgramPoint fakeProgramPoint = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return fakeLocation;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	private final Variable x = new Variable(untyped, "x", fakeProgramPoint.getLocation());
	private final Variable y = new Variable(untyped, "y", fakeProgramPoint.getLocation());

	private final PointBasedHeap emptyHeap = new PointBasedHeap();
	private final PointBasedHeap topHeap = new PointBasedHeap().top();
	private final PointBasedHeap bottomHeap = new PointBasedHeap().bottom();

	private HeapEnvironment<
			AllocationSites> emptyHeapEnv = new HeapEnvironment<AllocationSites>(new AllocationSites());

	@Test
	public void testAssign() throws SemanticException {
		Constant one = new Constant(IntType.INSTANCE, 1, loc1);
		Constant zero = new Constant(IntType.INSTANCE, 0, loc1);
		PointBasedHeap assignResult = topHeap.assign(x,
				one, fakeProgramPoint);

		// constants do not affect heap abstract domain
		assertEquals(assignResult, topHeap);

		assignResult = topHeap.assign(x,
				new BinaryExpression(intType, one, zero, BinaryOperator.NUMERIC_ADD, fakeLocation), fakeProgramPoint);

		// binary expressions do not affect heap abstract domain
		assertEquals(assignResult, topHeap);

		// from empty environment, assignment x = *(pp1)
		// expected: x -> pp1
		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);

		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1), false);
		HeapEnvironment<AllocationSites> expectedEnv = emptyHeapEnv.putState(x, xSites);
		assertEquals(xAssign, new PointBasedHeap(expectedEnv));

		// from x -> pp1, assignment x = *(pp2)
		// expected: x -> pp2
		PointBasedHeap actual = xAssign.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc2), loc2),
				pp2);

		xSites = new AllocationSites(Collections.singleton(alloc2), false);
		expectedEnv = emptyHeapEnv.putState(x, xSites);

		assertEquals(actual, new PointBasedHeap(expectedEnv));
	}

	@Test
	public void testSmallStepSemantic() throws SemanticException {
		// The current implementation of semanticsOf returns this;
		// we test the method checking that rewriting a heap expression
		// in this and in its semanticsOf results produced the same
		// result.

		// 1. Heap allocation
		HeapExpression heapExpression = new HeapAllocation(untyped, loc1);

		// from topState
		PointBasedHeap sss = topHeap.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), topHeap.rewrite(heapExpression, pp1));

		// from bottomState
		sss = bottomHeap.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), bottomHeap.rewrite(heapExpression, pp1));

		// from x -> pp1
		PointBasedHeap xToLoc1 = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);
		sss = xToLoc1.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), xToLoc1.rewrite(heapExpression, pp1));

		// 2. Heap reference
		heapExpression = new HeapReference(untyped,
				new HeapAllocation(untyped, loc1), loc1);

		// from topState
		sss = topHeap.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), topHeap.rewrite(heapExpression, pp1));

		// from bottomState
		sss = bottomHeap.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), bottomHeap.rewrite(heapExpression, pp1));

		// from x -> pp1
		sss = xToLoc1.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), xToLoc1.rewrite(heapExpression, pp1));

		// 3. Access child
		heapExpression = new AccessChild(untyped, x, y, loc1);

		// from topState
		sss = topHeap.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), topHeap.rewrite(heapExpression, pp1));

		// from bottomState
		sss = bottomHeap.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), bottomHeap.rewrite(heapExpression, pp1));

		// from x -> pp1
		sss = xToLoc1.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), xToLoc1.rewrite(heapExpression, pp1));

		// 4. Heap dereference
		heapExpression = new HeapDereference(untyped, new HeapReference(untyped,
				new HeapAllocation(untyped, loc1), loc1), loc1);

		// from topState
		sss = topHeap.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), topHeap.rewrite(heapExpression, pp1));

		// from bottomState
		sss = bottomHeap.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), bottomHeap.rewrite(heapExpression, pp1));

		// from x -> pp1
		sss = xToLoc1.semanticsOf(heapExpression, pp1);
		assertEquals(sss.rewrite(heapExpression, pp1), xToLoc1.rewrite(heapExpression, pp1));
	}

	@Test
	public void testLub() throws SemanticException {
		PointBasedHeap xToLoc1 = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);

		PointBasedHeap xToLoc2 = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc2), loc2),
				pp2);

		PointBasedHeap yToLoc2 = topHeap.assign(y,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc2), loc2),
				pp2);

		// top lub <any heap> or <any heap> lub top = top
		assertTrue(topHeap.lub(xToLoc1).isTop());
		assertTrue(xToLoc1.lub(topHeap).isTop());
		assertTrue(topHeap.lub(topHeap).isTop());

		// <any heap> lub bottom or bottom lub <any heap> = <any heap>
		assertEquals(xToLoc1.lub(bottomHeap), xToLoc1);
		assertEquals(bottomHeap.lub(xToLoc1), xToLoc1);
		assertTrue(bottomHeap.lub(bottomHeap).isBottom());

		// <any heap> lub <any heap> = <any heap>
		assertEquals(xToLoc1.lub(xToLoc1), xToLoc1);

		// x -> pp1 lub y -> pp1 = x -> pp1, y -> pp1
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1), false);
		AllocationSites ySites = new AllocationSites(Collections.singleton(alloc2), false);

		HeapEnvironment<AllocationSites> expectedEnv = emptyHeapEnv.putState(x, xSites);
		expectedEnv = expectedEnv.putState(y, ySites);

		assertEquals(xToLoc1.lub(yToLoc2), new PointBasedHeap(expectedEnv));

		// x -> pp1 lub x -> pp2 = x -> pp1,pp2
		HashSet<AllocationSite> xSet = new HashSet<>();
		xSet.add(alloc1);
		xSet.add(alloc2);
		xSites = new AllocationSites().mk(xSet);
		expectedEnv = emptyHeapEnv.putState(x, xSites);

		assertEquals(xToLoc1.lub(xToLoc2), new PointBasedHeap(expectedEnv));
	}

	@Test
	public void testWidening() throws SemanticException {
		PointBasedHeap xToLoc1 = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);

		PointBasedHeap xToLoc2 = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc2), loc2),
				pp2);

		PointBasedHeap yToLoc2 = topHeap.assign(y,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc2), loc2),
				pp2);

		// top lub <any heap> or <any heap> lub top = top
		assertTrue(topHeap.widening(xToLoc1).isTop());
		assertTrue(xToLoc1.widening(topHeap).isTop());
		assertTrue(topHeap.widening(topHeap).isTop());

		// <any heap> lub bottom or bottom lub <any heap> = <any heap>
		assertEquals(xToLoc1.widening(bottomHeap), xToLoc1);
		assertEquals(bottomHeap.widening(xToLoc1), xToLoc1);
		assertTrue(bottomHeap.widening(bottomHeap).isBottom());

		// <any heap> lub <any heap> = <any heap>
		assertEquals(xToLoc1.widening(xToLoc1), xToLoc1);

		// x -> pp1 lub y -> pp1 = x -> pp1, y -> pp1
		AllocationSites xSites = new AllocationSites(Collections.singleton(alloc1), false);
		AllocationSites ySites = new AllocationSites(Collections.singleton(alloc2), false);

		HeapEnvironment<AllocationSites> expectedEnv = emptyHeapEnv.putState(x, xSites);
		expectedEnv = expectedEnv.putState(y, ySites);

		assertEquals(xToLoc1.widening(yToLoc2), new PointBasedHeap(expectedEnv));

		// x -> pp1 lub x -> pp2 = x -> pp1,pp2
		HashSet<AllocationSite> xSet = new HashSet<>();
		xSet.add(alloc1);
		xSet.add(alloc2);
		xSites = new AllocationSites().mk(xSet);
		expectedEnv = emptyHeapEnv.putState(x, xSites);

		assertEquals(xToLoc1.widening(xToLoc2), new PointBasedHeap(expectedEnv));
	}

	@Test
	public void testLessOrEquals() throws SemanticException {

		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);

		PointBasedHeap yAssign = topHeap.assign(y,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc2), loc2),
				pp2);

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
						new HeapAllocation(untyped, loc1), loc1),
				pp1);

		assertEquals(result.forgetIdentifier(x), emptyHeap);
		assertEquals(result.forgetIdentifier(y), result);

		assertEquals(topHeap.forgetIdentifier(x), topHeap);
		assertEquals(bottomHeap.forgetIdentifier(x), bottomHeap);
	}

	@Test
	public void testRepresentation() throws SemanticException {
		assertEquals(topHeap.representation().toString(), "#TOP#");
		assertEquals(bottomHeap.representation().toString(), "_|_");
		assertEquals(emptyHeap.representation().toString(), "[]");

		PointBasedHeap result = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);

		assertEquals(result.representation().toString(), "[heap[w]:pp@'fake':1:1]");
	}

	@Test
	public void testPushScope() throws SemanticException {
		ScopeToken token = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				return new SourceCodeLocation("fake", 0, 0);
			}
		});

		assertEquals(topHeap.pushScope(token), topHeap);
		assertEquals(bottomHeap.pushScope(token), bottomHeap);

		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);
		PointBasedHeap xPushedScopeAssign = topHeap.assign(
				new OutOfScopeIdentifier(x, token, loc1),
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);

		// x -> pp1 pushScope = [out-of-scope-id]x -> pp1
		assertEquals(xAssign.pushScope(token), xPushedScopeAssign);
	}

	@Test
	public void testPopScope() throws SemanticException {
		ScopeToken token = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				return new SourceCodeLocation("fake", 0, 0);
			}
		});

		assertEquals(topHeap.popScope(token), topHeap);
		assertEquals(bottomHeap.popScope(token), bottomHeap);

		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);

		PointBasedHeap xScopedAssign = topHeap.assign((Identifier) x.pushScope(token),
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);

		// [scoped]x -> pp1 popScope = x -> pp1
		assertEquals(xScopedAssign.popScope(token), xAssign);

		// x -> pp1 popScope = empty environment
		assertEquals(xAssign.popScope(token), emptyHeap);
	}

	@Test
	public void testAccessChildRewrite() throws SemanticException {
		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), loc1),
				pp1);
		// x.y rewritten in x -> pp1 = pp1
		AccessChild accessChild = new AccessChild(untyped, x, y, loc1);

		ExpressionSet<ValueExpression> expectedRewritten = new ExpressionSet<ValueExpression>(alloc1);
		assertEquals(xAssign.rewrite(accessChild, fakeProgramPoint), expectedRewritten);

		// y.x rewritten in x -> pp1 = empty set
		accessChild = new AccessChild(untyped, y, x, loc1);
		assertEquals(xAssign.rewrite(accessChild, fakeProgramPoint), new ExpressionSet<ValueExpression>());
	}

	@Test
	public void testIdentifierRewrite() throws SemanticException {
		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), fakeLocation),
				pp1);
		// x rewritten in x -> pp1 = pp1
		ExpressionSet<ValueExpression> expectedRewritten = new ExpressionSet<ValueExpression>(
				new MemoryPointer(untyped, alloc1, fakeLocation));
		assertEquals(xAssign.rewrite(x, fakeProgramPoint), expectedRewritten);

		// y rewritten in x -> pp1 = {y}
		// TODO to verify
		assertEquals(xAssign.rewrite(y, fakeProgramPoint), new ExpressionSet<ValueExpression>(y));
	}

	@Test
	public void testHeapDereferenceRewrite() throws SemanticException {
		// *(&(new loc(pp1)) rewritten in top -> pp1
		HeapDereference deref = new HeapDereference(untyped, new HeapReference(untyped,
				new HeapAllocation(untyped, loc1), loc1), loc1);

		ExpressionSet<ValueExpression> expectedRewritten = new ExpressionSet<ValueExpression>(
				new MemoryPointer(untyped, alloc1, fakeLocation));
		assertEquals(topHeap.rewrite(deref, fakeProgramPoint), expectedRewritten);

		// *(x) rewritten in x -> pp1 -> pp1
		PointBasedHeap xAssign = topHeap.assign(x,
				new HeapReference(untyped,
						new HeapAllocation(untyped, loc1), fakeLocation),
				pp1);
		deref = new HeapDereference(untyped, x, loc1);
		expectedRewritten = new ExpressionSet<ValueExpression>(
				new MemoryPointer(untyped, alloc1, fakeLocation));
		assertEquals(xAssign.rewrite(deref, fakeProgramPoint), expectedRewritten);

		// *(y) rewritten in x -> pp1 -> empty set
		deref = new HeapDereference(untyped, y, loc1);
		expectedRewritten = new ExpressionSet<ValueExpression>();
		assertEquals(xAssign.rewrite(deref, fakeProgramPoint), expectedRewritten);
	}
}
