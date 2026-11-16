package it.unive.lisa.program.language.hierarchytraversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SingleInheritanceTraversalStrategyTest {

	private static ClassUnit cls(
			Program p,
			String name) {
		return new ClassUnit(SyntheticLocation.INSTANCE, p, name, false);
	}

	private static InterfaceUnit iface(
			Program p,
			String name) {
		return new InterfaceUnit(SyntheticLocation.INSTANCE, p, name, false);
	}

	private static List<CompilationUnit> visit(
			CompilationUnit start) {
		List<CompilationUnit> result = new ArrayList<>();
		SingleInheritanceTraversalStrategy.INSTANCE.traverse(null, start).forEach(result::add);
		return result;
	}

	// regression test: the iterator used to re-add the "current" pointer to
	// its own work queue at every batch boundary, even though that unit had
	// already been popped and returned by an earlier next() call - so every
	// superclass past the first one was yielded twice
	@Test
	public void aLinearSuperclassChainIsVisitedWithoutDuplicates() {
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		ClassUnit c = cls(p, "C");
		ClassUnit b = cls(p, "B");
		ClassUnit a = cls(p, "A");
		b.addSuperclass(c);
		a.addSuperclass(b);

		List<CompilationUnit> visited = visit(a);
		assertEquals(List.of(a, b, c), visited);
		assertEquals(3, new HashSet<>(visited).size(), "no unit should be visited more than once");
	}

	@Test
	public void aFourLevelChainIsVisitedWithoutDuplicates() {
		// long enough to exercise more than one "batch boundary" advance
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		ClassUnit d = cls(p, "D");
		ClassUnit c = cls(p, "C");
		ClassUnit b = cls(p, "B");
		ClassUnit a = cls(p, "A");
		c.addSuperclass(d);
		b.addSuperclass(c);
		a.addSuperclass(b);

		List<CompilationUnit> visited = visit(a);
		assertEquals(List.of(a, b, c, d), visited);
		assertEquals(4, new HashSet<>(visited).size());
	}

	@Test
	public void interfacesAreVisitedAlongsideTheSuperclass() {
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		InterfaceUnit i1 = iface(p, "I1");
		ClassUnit b = cls(p, "B");
		ClassUnit a = cls(p, "A");
		a.addSuperclass(b);
		a.addInterface(i1);

		Set<CompilationUnit> visited = new HashSet<>(visit(a));
		assertEquals(Set.of(a, b, i1), visited);
	}

	@Test
	public void aDiamondSharedAncestorIsVisitedOnlyOnce() {
		// A extends B and implements I1; B also implements I1: I1 is
		// reachable through two paths and must be reported only once
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		InterfaceUnit i1 = iface(p, "I1");
		ClassUnit b = cls(p, "B");
		b.addInterface(i1);
		ClassUnit a = cls(p, "A");
		a.addSuperclass(b);
		a.addInterface(i1);

		List<CompilationUnit> visited = visit(a);
		assertEquals(3, visited.size());
		assertTrue(visited.containsAll(List.of(a, b, i1)));
		assertEquals(3, new HashSet<>(visited).size());
	}

	@Test
	public void aUnitWithNoAncestorsYieldsOnlyItself() {
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		ClassUnit a = cls(p, "A");
		assertEquals(List.of(a), visit(a));
	}

}
