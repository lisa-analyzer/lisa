package it.unive.lisa.lattices.memory.allocations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class MemoryEnvWithFieldsTest {

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	private final ProgramPoint pp = new TestParameterProvider.FakePP();

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", loc);

	private final HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);

	@Test
	public void freshEnvironmentIsTop() {
		assertTrue(new MemoryEnvWithFields().isTop());
	}

	@Test
	public void bottomIsNotTop() {
		MemoryEnvWithFields bottom = new MemoryEnvWithFields().bottom();
		assertTrue(bottom.isBottom());
		assertFalse(bottom.isTop());
	}

	@Test
	public void lessOrEqualHoldsBetweenTopAndItself()
			throws SemanticException {
		MemoryEnvWithFields top = new MemoryEnvWithFields();
		assertTrue(top.lessOrEqual(top));
	}

	@Test
	public void bottomIsLessOrEqualThanTop()
			throws SemanticException {
		MemoryEnvWithFields env = new MemoryEnvWithFields();
		assertTrue(env.bottom().lessOrEqual(env));
	}

	@Test
	public void knowsIdentifierReflectsTheTrackedKeys()
			throws SemanticException {
		MemoryEnvWithFields env = new MemoryEnvWithFields();
		assertFalse(env.knowsIdentifier(x));

		MemoryEnvWithFields withX = env.putState(x, new AllocationSites(site));
		assertTrue(withX.knowsIdentifier(x));
	}

	@Test
	public void forgetIdentifierRemovesTheKeyAndItsFields()
			throws SemanticException {
		MemoryEnvWithFields env = new MemoryEnvWithFields().putState(x, new AllocationSites(site));
		var forgotten = env.forgetIdentifier(x, pp);
		assertFalse(forgotten.getLeft().knowsIdentifier(x));
	}

	@Test
	public void lubOfTopAndAnythingIsTop()
			throws SemanticException {
		MemoryEnvWithFields top = new MemoryEnvWithFields();
		MemoryEnvWithFields env = top.putState(x, new AllocationSites(site));
		assertTrue(top.lub(env).isTop());
		assertTrue(env.lub(top).isTop());
	}

	@Test
	public void lubOfBottomAndAnythingIsTheOther()
			throws SemanticException {
		MemoryEnvWithFields top = new MemoryEnvWithFields();
		MemoryEnvWithFields env = top.putState(x, new AllocationSites(site));
		MemoryEnvWithFields bottom = top.bottom();
		assertTrue(env.lub(bottom).equals(env) || env.lessOrEqual(env.lub(bottom)));
		assertTrue(bottom.lub(env).equals(env) || env.lessOrEqual(bottom.lub(env)));
	}

}
