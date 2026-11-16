package it.unive.lisa.analysis.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.heap.AllocatedTypes;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class TypeBasedHeapTest {

	private final TypeBasedHeap heap = new TypeBasedHeap();

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	private final ProgramPoint pp = new TestParameterProvider.FakePP();

	private final SemanticOracle oracle = new TestParameterProvider.FakeOracle();

	@Test
	public void makeLatticeIsAnEmptySetOfTypes() {
		assertTrue(heap.makeLattice().isTop());
	}

	@Test
	public void allocationsOfTheSameTypeAreRewrittenToTheSameLocation()
			throws SemanticException {
		// NullType is one of the few types marked as "in-memory" (see
		// Type#isInMemoryType): only those actually produce a heap location
		MemoryAllocation firstAlloc = new MemoryAllocation(NullType.INSTANCE, loc);
		MemoryAllocation secondAlloc = new MemoryAllocation(
				NullType.INSTANCE, new SourceCodeLocation("fake", 2, 2));

		ExpressionSet first = heap.rewriteMemoryAllocation(firstAlloc, new AllocatedTypes(), pp, oracle);
		ExpressionSet second = heap.rewriteMemoryAllocation(secondAlloc, new AllocatedTypes(), pp, oracle);

		assertEquals(1, first.size());
		assertEquals(1, second.size());
		// same type -> same abstract location, regardless of where each was
		// allocated: this is exactly what "type-based" abstraction means
		assertEquals(first.elements.iterator().next(), second.elements.iterator().next());
	}

	@Test
	public void allocatingANonInMemoryTypeProducesNoLocation()
			throws SemanticException {
		// Int32Type is not an in-memory type: a value of this type is never
		// heap-allocated, so rewriting must not invent a location for it
		ExpressionSet intAlloc = heap.rewriteMemoryAllocation(
				new MemoryAllocation(Int32Type.INSTANCE, loc), new AllocatedTypes(), pp, oracle);
		assertEquals(0, intAlloc.size());
	}

	@Test
	public void assignDoesNotAffectTheHeap()
			throws SemanticException {
		var result = heap.assign(
				new AllocatedTypes(), new Variable(Untyped.INSTANCE, "x", loc),
				new MemoryAllocation(Int32Type.INSTANCE, loc), pp, oracle);
		assertEquals(new AllocatedTypes(), result.getLeft());
	}

	@Test
	public void aliasIsNotSatisfiedWhenRuntimeTypesAreDisjoint()
			throws SemanticException {
		Variable intVar = new Variable(Int32Type.INSTANCE, "x", loc);
		Variable nullVar = new Variable(NullType.INSTANCE, "y", loc);
		// FakeOracle#getRuntimeTypesOf returns the static type of the
		// expression, so an int-typed and a null-typed identifier have no
		// runtime type in common and can never be aliases
		assertEquals(
				Satisfiability.NOT_SATISFIED,
				heap.alias(new AllocatedTypes(), intVar, nullVar, pp, oracle));
	}

	@Test
	public void aliasIsUnknownWhenTypesOverlap()
			throws SemanticException {
		Variable intVar1 = new Variable(Int32Type.INSTANCE, "x", loc);
		Variable intVar2 = new Variable(Int32Type.INSTANCE, "y", loc);
		assertEquals(
				Satisfiability.UNKNOWN,
				heap.alias(new AllocatedTypes(), intVar1, intVar2, pp, oracle));
	}

	@Test
	public void aliasOnBottomStateIsBottom()
			throws SemanticException {
		Variable x = new Variable(Int32Type.INSTANCE, "x", loc);
		assertEquals(
				Satisfiability.BOTTOM,
				heap.alias(new AllocatedTypes().bottom(), x, x, pp, oracle));
	}

}
