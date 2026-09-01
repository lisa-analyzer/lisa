package it.unive.lisa.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestCallGraph;
import it.unive.lisa.TestInterproceduralAnalysis;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class LiSAConfigurationTest {

	@Test
	public void toStringOmitsTheSemanticConfigurationFields() {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.callGraph = new TestCallGraph();
		conf.interproceduralAnalysis = new TestInterproceduralAnalysis<>();
		conf.analysis = new TestAbstractDomain();

		String s = conf.toString();
		// the field names themselves must not appear: their values are
		// intentionally excluded from both toString() and toPropertyBag()
		assertFalse(s.contains("callGraph"));
		assertFalse(s.contains("interproceduralAnalysis"));
		assertFalse(s.contains("analysis"));
		// but a genuinely unrelated field is still reported
		assertTrue(s.contains("workdir"));
	}

	@Test
	public void toPropertyBagOmitsTheSemanticConfigurationFields() {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.callGraph = new TestCallGraph();
		conf.interproceduralAnalysis = new TestInterproceduralAnalysis<>();
		conf.analysis = new TestAbstractDomain();

		Map<String, String> bag = conf.toPropertyBag();
		assertFalse(bag.containsKey("callGraph"));
		assertFalse(bag.containsKey("interproceduralAnalysis"));
		assertFalse(bag.containsKey("analysis"));
		assertTrue(bag.containsKey("workdir"));
		assertTrue(bag.containsKey("wideningThreshold"));
	}

	private static class NamedSyntacticCheck
			implements
			it.unive.lisa.checks.syntactic.SyntacticCheck {
	}

	@Test
	public void toPropertyBagReportsCollectionSizeAndElementNamesForChecks() {
		LiSAConfiguration conf = new LiSAConfiguration();
		// an anonymous class would report an empty simple name and defeat
		// this assertion, so a named one is used deliberately
		conf.syntacticChecks.add(new NamedSyntacticCheck());

		Map<String, String> bag = conf.toPropertyBag();
		assertTrue(bag.get("syntacticChecks").contains("NamedSyntacticCheck"));
	}

	@Test
	public void toPropertyBagNormalizesTheWorkdirToUnixSeparators() {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.workdir = "some\\windows\\path";
		Map<String, String> bag = conf.toPropertyBag();
		assertEquals("some/windows/path", bag.get("workdir"));
	}

	@Test
	public void equalsHoldsForTwoDefaultConfigurations() {
		// forwardFixpoint/backwardFixpoint each default to a FRESH
		// ForwardAscendingFixpoint/BackwardAscendingFixpoint instance per
		// LiSAConfiguration construction; ForwardCFGFixpoint/
		// BackwardCFGFixpoint define equals()/hashCode() based on the
		// concrete class (they only ever carry execution-scoped state, never
		// anything independently configurable), so two default instances of
		// the same fixpoint strategy - and thus two default configurations -
		// are equal
		LiSAConfiguration a = new LiSAConfiguration();
		LiSAConfiguration b = new LiSAConfiguration();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a.forwardFixpoint, b.forwardFixpoint);
		assertEquals(a.backwardFixpoint, b.backwardFixpoint);
	}

	@Test
	public void equalsDoesNotIgnoreTheSemanticFieldsUnlikeToStringAndToPropertyBag() {
		// CallGraph (like InterproceduralAnalysis/AbstractDomain) has no
		// custom equals() of its own, so two distinct instances - even two
		// freshly-built, still-empty ones of the same concrete type - fall
		// back to reference identity and make the configurations unequal,
		// even though toString()/toPropertyBag() deliberately skip this
		// field and would still render these two configurations identically
		LiSAConfiguration a = new LiSAConfiguration();
		LiSAConfiguration b = new LiSAConfiguration();
		a.callGraph = new TestCallGraph();
		b.callGraph = new TestCallGraph();
		assertFalse(a.equals(b));
	}

}
