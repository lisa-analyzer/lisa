package it.unive.lisa.analysis.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class MemoryReplacementTest {

	private final Variable x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final Variable y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

	private final Variable z = new Variable(Untyped.INSTANCE, "z", SyntheticLocation.INSTANCE);

	@Test
	public void testEmptyReplacementHasNoSourcesOrTargets() {
		MemoryReplacement rep = new MemoryReplacement();
		assertTrue(rep.getSources().isEmpty());
		assertTrue(rep.getTargets().isEmpty());
		assertTrue(rep.getIdsToForget().isEmpty());
	}

	@Test
	public void testWithSourceAndWithTargetAreFluentAndAccumulate() {
		MemoryReplacement rep = new MemoryReplacement().withSource(x).withSource(y).withTarget(z);
		assertEquals(Set.of(x, y), rep.getSources());
		assertEquals(Set.of(z), rep.getTargets());
	}

	@Test
	public void testIdsToForgetAreSourcesMinusTargets() {
		// x -> y is a strong substitution (x is not also a target), so x must
		// be forgotten once the replacement has been applied
		MemoryReplacement rep = new MemoryReplacement();
		rep.addSource(x);
		rep.addTarget(y);
		assertEquals(Set.of(x), rep.getIdsToForget());
	}

	@Test
	public void testIdsToForgetIsEmptyWhenSourceIsAlsoATarget() {
		// x -> {x, y} is a weak substitution (x is also a target), so x must
		// survive the replacement
		MemoryReplacement rep = new MemoryReplacement();
		rep.addSource(x);
		rep.addTarget(x);
		rep.addTarget(y);
		assertTrue(rep.getIdsToForget().isEmpty());
	}

	@Test
	public void testIdsToForgetDoesNotMutateSources() {
		MemoryReplacement rep = new MemoryReplacement();
		rep.addSource(x);
		rep.addTarget(y);
		rep.getIdsToForget();
		// getIdsToForget must operate on a defensive copy
		assertEquals(Set.of(x), rep.getSources());
	}

	@Test
	public void testEqualsAndHashCodeDependOnlyOnSourcesAndTargets() {
		MemoryReplacement rep1 = new MemoryReplacement().withSource(x).withTarget(y);
		MemoryReplacement rep2 = new MemoryReplacement().withSource(x).withTarget(y);
		MemoryReplacement rep3 = new MemoryReplacement().withSource(x).withTarget(z);

		assertEquals(rep1, rep2);
		assertEquals(rep1.hashCode(), rep2.hashCode());
		assertFalse(rep1.equals(rep3));
	}

	@Test
	public void testToStringMentionsSourcesAndTargets() {
		MemoryReplacement rep = new MemoryReplacement().withSource(x).withTarget(y);
		String repr = rep.toString();
		assertTrue(repr.contains(x.toString()));
		assertTrue(repr.contains(y.toString()));
	}

	@Test
	public void testGetSourcesAndTargetsReturnLiveViews() {
		// addSource/addTarget after retrieving the sets must be visible,
		// confirming that getSources()/getTargets() do not return copies
		MemoryReplacement rep = new MemoryReplacement();
		Set<Identifier> sources = rep.getSources();
		rep.addSource(x);
		assertTrue(sources.contains(x));
	}

}
