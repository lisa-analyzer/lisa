package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SingleTypeLatticeTest {

	@Test
	public void topIsTheSingletonAndBottomIsDistinct() {
		assertSame(SingleTypeLattice.SINGLETON, SingleTypeLattice.SINGLETON.top());
		assertSame(SingleTypeLattice.BOTTOM, SingleTypeLattice.SINGLETON.bottom());
		assertTrue(SingleTypeLattice.SINGLETON.isTop());
		assertTrue(SingleTypeLattice.BOTTOM.isBottom());
	}

	@Test
	public void lessOrEqualFollowsTheTwoElementChain() throws SemanticException {
		assertTrue(SingleTypeLattice.BOTTOM.lessOrEqual(SingleTypeLattice.SINGLETON));
		assertFalse(SingleTypeLattice.SINGLETON.lessOrEqual(SingleTypeLattice.BOTTOM));
	}

	@Test
	public void lubAndGlbFollowTheTwoElementChain() throws SemanticException {
		assertSame(SingleTypeLattice.BOTTOM, SingleTypeLattice.BOTTOM.lub(SingleTypeLattice.BOTTOM));
		assertSame(SingleTypeLattice.SINGLETON, SingleTypeLattice.BOTTOM.lub(SingleTypeLattice.SINGLETON));
		assertSame(SingleTypeLattice.SINGLETON, SingleTypeLattice.SINGLETON.glb(SingleTypeLattice.SINGLETON));
		assertSame(SingleTypeLattice.BOTTOM, SingleTypeLattice.SINGLETON.glb(SingleTypeLattice.BOTTOM));
	}

	@Test
	public void alwaysModelsUntypedAsTheOnlyRuntimeType() {
		Set<Type> types = SingleTypeLattice.SINGLETON.getRuntimeTypes();
		assertEquals(Set.of(Untyped.INSTANCE), types);
		// even bottom exposes the same fixed answer: this lattice never
		// actually tracks real type information
		assertEquals(Set.of(Untyped.INSTANCE), SingleTypeLattice.BOTTOM.getRuntimeTypes());
	}

}
