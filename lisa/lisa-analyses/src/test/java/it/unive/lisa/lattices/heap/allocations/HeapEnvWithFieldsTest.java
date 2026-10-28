package it.unive.lisa.lattices.heap.allocations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class HeapEnvWithFieldsTest {

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	private final ProgramPoint pp = new TestParameterProvider.FakePP();

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", loc);

	private final HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);

	@Test
	public void freshEnvironmentIsTop() {
		assertTrue(new HeapEnvWithFields().isTop());
	}

	@Test
	public void bottomIsNotTop() {
		HeapEnvWithFields bottom = new HeapEnvWithFields().bottom();
		assertTrue(bottom.isBottom());
		assertFalse(bottom.isTop());
	}

	@Test
	public void lessOrEqualHoldsBetweenTopAndItself()
			throws SemanticException {
		HeapEnvWithFields top = new HeapEnvWithFields();
		assertTrue(top.lessOrEqual(top));
	}

	@Test
	public void bottomIsLessOrEqualThanTop()
			throws SemanticException {
		HeapEnvWithFields env = new HeapEnvWithFields();
		assertTrue(env.bottom().lessOrEqual(env));
	}

	@Test
	public void knowsIdentifierReflectsTheTrackedKeys()
			throws SemanticException {
		HeapEnvWithFields env = new HeapEnvWithFields();
		assertFalse(env.knowsIdentifier(x));

		HeapEnvWithFields withX = env.putState(x, new AllocationSites(site));
		assertTrue(withX.knowsIdentifier(x));
	}

	@Test
	public void forgetIdentifierRemovesTheKeyAndItsFields()
			throws SemanticException {
		HeapEnvWithFields env = new HeapEnvWithFields().putState(x, new AllocationSites(site));
		var forgotten = env.forgetIdentifier(x, pp);
		assertFalse(forgotten.getLeft().knowsIdentifier(x));
	}

	@Test
	public void lubOfTopAndAnythingIsTop()
			throws SemanticException {
		HeapEnvWithFields top = new HeapEnvWithFields();
		HeapEnvWithFields env = top.putState(x, new AllocationSites(site));
		assertTrue(top.lub(env).isTop());
		assertTrue(env.lub(top).isTop());
	}

	@Test
	public void lubOfBottomAndAnythingIsTheOther()
			throws SemanticException {
		HeapEnvWithFields top = new HeapEnvWithFields();
		HeapEnvWithFields env = top.putState(x, new AllocationSites(site));
		HeapEnvWithFields bottom = top.bottom();
		assertTrue(env.lub(bottom).equals(env) || env.lessOrEqual(env.lub(bottom)));
		assertTrue(bottom.lub(env).equals(env) || env.lessOrEqual(bottom.lub(env)));
	}

	private HeapEnvWithFields withFields(
			HeapEnvWithFields env,
			ExpressionSet siteFields)
			throws SemanticException {
		GenericMapLattice<AllocationSite, ExpressionSet> fields = env.fields.putState(site, siteFields);
		return new HeapEnvWithFields(env.lattice, env.function, fields);
	}

	@Test
	public void wideningKeepsTheFieldsOfBothStates()
			throws SemanticException {
		HeapEnvWithFields base = new HeapEnvWithFields().putState(x, new AllocationSites(site));
		Constant f = new Constant(Untyped.INSTANCE, "f", loc);
		ExpressionSet siteFields = new ExpressionSet(f);
		HeapEnvWithFields noFields = withFields(base, new ExpressionSet());
		HeapEnvWithFields someFields = withFields(base, siteFields);

		HeapEnvWithFields widen = noFields.widening(someFields);
		assertEquals(siteFields, widen.fields.getState(site));
		assertTrue(someFields.lessOrEqual(widen));
	}

	@Test
	public void narrowingCombinesTheFieldsOfBothStates()
			throws SemanticException {
		HeapEnvWithFields base = new HeapEnvWithFields().putState(x, new AllocationSites(site));
		Constant f = new Constant(Untyped.INSTANCE, "f", loc);
		Constant g = new Constant(Untyped.INSTANCE, "g", loc);
		ExpressionSet fg = new ExpressionSet(Set.of(f, g));
		ExpressionSet onlyF = new ExpressionSet(f);
		HeapEnvWithFields big = withFields(base, fg);
		HeapEnvWithFields small = withFields(base, onlyF);

		HeapEnvWithFields narrow = big.narrowing(small);
		assertEquals(fg.narrowing(onlyF), narrow.fields.getState(site));
	}

}
