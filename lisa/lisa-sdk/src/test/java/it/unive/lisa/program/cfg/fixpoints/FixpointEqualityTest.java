package it.unive.lisa.program.cfg.fixpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.cfg.fixpoints.backward.BackwardAscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardDescendingGLBFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardAscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardDescendingGLBFixpoint;
import it.unive.lisa.program.cfg.fixpoints.optforward.OptimizedForwardAscendingFixpoint;
import org.junit.jupiter.api.Test;

// covers ForwardCFGFixpoint/BackwardCFGFixpoint's equals()/hashCode(): every
// concrete fixpoint algorithm's no-arg constructor builds a stateless
// "strategy selector" placeholder (see e.g. ForwardAscendingFixpoint's own
// javadoc), which is exactly what LiSAConfiguration#forwardFixpoint and
// friends hold by default - two such placeholders of the same concrete class
// must be equal for LiSAConfiguration's own (inherited) equals()/hashCode()
// to behave sensibly on default configurations
public class FixpointEqualityTest {

	@Test
	public void twoInstancesOfTheSameConcreteClassAreEqual() {
		assertEquals(new ForwardAscendingFixpoint<>(), new ForwardAscendingFixpoint<>());
		assertEquals(new ForwardAscendingFixpoint<>().hashCode(), new ForwardAscendingFixpoint<>().hashCode());
		assertEquals(new BackwardAscendingFixpoint<>(), new BackwardAscendingFixpoint<>());
	}

	@Test
	public void instancesOfDifferentAlgorithmsAreNotEqual() {
		assertNotEquals(new ForwardAscendingFixpoint<>(), new ForwardDescendingGLBFixpoint<>());
	}

	@Test
	public void forwardAndBackwardVariantsOfTheSameAlgorithmAreNotEqual() {
		assertNotEquals(new ForwardAscendingFixpoint<>(), new BackwardAscendingFixpoint<>());
		assertNotEquals(new ForwardDescendingGLBFixpoint<>(), new BackwardDescendingGLBFixpoint<>());
	}

	@Test
	public void optimizedAndUnoptimizedVariantsAreNotEqual() {
		assertNotEquals(new ForwardAscendingFixpoint<>(), new OptimizedForwardAscendingFixpoint<>());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsIsReflexiveAndNullAndOtherTypeSafe() {
		ForwardAscendingFixpoint<?, ?> f = new ForwardAscendingFixpoint<>();
		assertTrue(f.equals(f));
		assertFalse(f.equals(null));
		assertFalse(f.equals("not a fixpoint"));
	}

}
