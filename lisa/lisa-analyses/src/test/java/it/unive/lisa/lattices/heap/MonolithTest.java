package it.unive.lisa.lattices.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class MonolithTest {

	private final ProgramPoint pp = new TestParameterProvider.FakePP();

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", pp.getLocation());

	@Test
	public void topAndBottomAreDistinctSingletons() {
		assertTrue(Monolith.SINGLETON.isTop());
		assertFalse(Monolith.SINGLETON.isBottom());
		assertTrue(Monolith.BOTTOM.isBottom());
		assertFalse(Monolith.BOTTOM.isTop());
		assertEquals(Monolith.SINGLETON, Monolith.SINGLETON.top());
		assertEquals(Monolith.BOTTOM, Monolith.SINGLETON.bottom());
	}

	@Test
	public void bottomIsLessOrEqualThanEverything()
			throws SemanticException {
		assertTrue(Monolith.BOTTOM.lessOrEqual(Monolith.BOTTOM));
		assertTrue(Monolith.BOTTOM.lessOrEqual(Monolith.SINGLETON));
	}

	@Test
	public void singletonIsNotLessOrEqualThanBottom()
			throws SemanticException {
		assertFalse(Monolith.SINGLETON.lessOrEqual(Monolith.BOTTOM));
		assertTrue(Monolith.SINGLETON.lessOrEqual(Monolith.SINGLETON));
	}

	@Test
	public void lubIsSingletonUnlessBothAreBottom()
			throws SemanticException {
		assertEquals(Monolith.BOTTOM, Monolith.BOTTOM.lub(Monolith.BOTTOM));
		assertEquals(Monolith.SINGLETON, Monolith.BOTTOM.lub(Monolith.SINGLETON));
		assertEquals(Monolith.SINGLETON, Monolith.SINGLETON.lub(Monolith.BOTTOM));
		assertEquals(Monolith.SINGLETON, Monolith.SINGLETON.lub(Monolith.SINGLETON));
	}

	@Test
	public void glbIsBottomUnlessBothAreSingleton()
			throws SemanticException {
		assertEquals(Monolith.SINGLETON, Monolith.SINGLETON.glb(Monolith.SINGLETON));
		assertEquals(Monolith.BOTTOM, Monolith.SINGLETON.glb(Monolith.BOTTOM));
		assertEquals(Monolith.BOTTOM, Monolith.BOTTOM.glb(Monolith.SINGLETON));
		assertEquals(Monolith.BOTTOM, Monolith.BOTTOM.glb(Monolith.BOTTOM));
	}

	@Test
	public void scopeOperationsAndIdentifierTrackingAreNoOps()
			throws SemanticException {
		assertFalse(Monolith.SINGLETON.knowsIdentifier(x));

		Pair<Monolith, List<HeapReplacement>> forgotten = Monolith.SINGLETON.forgetIdentifier(x, pp);
		assertEquals(Monolith.SINGLETON, forgotten.getLeft());
		assertTrue(forgotten.getRight().isEmpty());

		Pair<Monolith, List<HeapReplacement>> forgottenMany = Monolith.SINGLETON
				.forgetIdentifiers(List.of(x), pp);
		assertEquals(Monolith.SINGLETON, forgottenMany.getLeft());
		assertTrue(forgottenMany.getRight().isEmpty());

		Pair<Monolith, List<HeapReplacement>> forgottenIf = Monolith.SINGLETON
				.forgetIdentifiersIf(id -> true, pp);
		assertEquals(Monolith.SINGLETON, forgottenIf.getLeft());
		assertTrue(forgottenIf.getRight().isEmpty());
	}

	@Test
	public void expandYieldsExactlyTheGivenReplacement()
			throws SemanticException {
		HeapReplacement base = new HeapReplacement().withSource(x);
		assertEquals(List.of(base), Monolith.SINGLETON.expand(base));
	}

}
