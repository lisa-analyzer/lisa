package it.unive.lisa.conf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.cfg.fixpoints.backward.BackwardAscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardAscendingFixpoint;
import org.junit.jupiter.api.Test;

public class FixpointConfigurationTest {

	// no concrete OptimizedForwardFixpoint/OptimizedBackwardFixpoint exists
	// anywhere in this codebase; overriding isOptimized() on the real
	// concrete Ascending fixpoints is the minimal way to exercise the "true"
	// branch without reimplementing a fixpoint from scratch
	private static class OptimizedForward
			extends
			ForwardAscendingFixpoint<
					it.unive.lisa.TestAbstractState,
					it.unive.lisa.TestAbstractDomain> {

		@Override
		public boolean isOptimized() {
			return true;
		}
	}

	private static class OptimizedBackward
			extends
			BackwardAscendingFixpoint<
					it.unive.lisa.TestAbstractState,
					it.unive.lisa.TestAbstractDomain> {

		@Override
		public boolean isOptimized() {
			return true;
		}
	}

	@Test
	public void constructorCopiesAllFixpointRelatedFieldsFromTheParentConfiguration() {
		LiSAConfiguration parent = new LiSAConfiguration();
		parent.wideningThreshold = 7;
		parent.recursionWideningThreshold = 3;
		parent.glbThreshold = 2;
		parent.useWideningPoints = false;

		FixpointConfiguration<?, ?> conf = new FixpointConfiguration<>(parent);

		assertSame(parent.fixpointWorkingSet, conf.fixpointWorkingSet);
		assertSame(parent.hotspots, conf.hotspots);
		assertSame(parent.forwardFixpoint, conf.forwardFixpoint);
		assertSame(parent.forwardDescendingFixpoint, conf.forwardDescendingFixpoint);
		assertSame(parent.backwardFixpoint, conf.backwardFixpoint);
		assertSame(parent.backwardDescendingFixpoint, conf.backwardDescendingFixpoint);
		org.junit.jupiter.api.Assertions.assertEquals(7, conf.wideningThreshold);
		org.junit.jupiter.api.Assertions.assertEquals(3, conf.recursionWideningThreshold);
		org.junit.jupiter.api.Assertions.assertEquals(2, conf.glbThreshold);
		assertFalse(conf.useWideningPoints);
	}

	@Test
	public void usesOptimizedForwardFixpointIsFalseByDefault() {
		LiSAConfiguration parent = new LiSAConfiguration();
		FixpointConfiguration<?, ?> conf = new FixpointConfiguration<>(parent);
		assertFalse(conf.usesOptimizedForwardFixpoint());
	}

	@Test
	public void usesOptimizedForwardFixpointIsTrueWhenTheAscendingPhaseIsOptimized() {
		LiSAConfiguration parent = new LiSAConfiguration();
		parent.forwardFixpoint = new OptimizedForward();
		FixpointConfiguration<?, ?> conf = new FixpointConfiguration<>(parent);
		assertTrue(conf.usesOptimizedForwardFixpoint());
	}

	@Test
	public void usesOptimizedForwardFixpointIsTrueWhenOnlyTheDescendingPhaseIsOptimized() {
		LiSAConfiguration parent = new LiSAConfiguration();
		parent.forwardDescendingFixpoint = new OptimizedForward();
		FixpointConfiguration<?, ?> conf = new FixpointConfiguration<>(parent);
		assertTrue(conf.usesOptimizedForwardFixpoint());
	}

	@Test
	public void usesOptimizedForwardFixpointToleratesANullDescendingPhase() {
		LiSAConfiguration parent = new LiSAConfiguration();
		parent.forwardDescendingFixpoint = null;
		FixpointConfiguration<?, ?> conf = new FixpointConfiguration<>(parent);
		assertFalse(conf.usesOptimizedForwardFixpoint());
	}

	@Test
	public void usesOptimizedBackwardFixpointMirrorsTheForwardLogic() {
		LiSAConfiguration parent = new LiSAConfiguration();
		FixpointConfiguration<?, ?> defaultConf = new FixpointConfiguration<>(parent);
		assertFalse(defaultConf.usesOptimizedBackwardFixpoint());

		parent.backwardFixpoint = new OptimizedBackward();
		FixpointConfiguration<?, ?> optimizedConf = new FixpointConfiguration<>(parent);
		assertTrue(optimizedConf.usesOptimizedBackwardFixpoint());
	}

}
