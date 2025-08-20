package it.unive.lisa.lattices.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionInverseSet;
import it.unive.lisa.analysis.string.SubstringDomain;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class SubstringsTest {

	private final Identifier y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);

	private final Identifier x = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final Identifier z = new Variable(StringType.INSTANCE, "z", SyntheticLocation.INSTANCE);

	private final Identifier w = new Variable(StringType.INSTANCE, "w", SyntheticLocation.INSTANCE);

	private final ValueExpression a = new Constant(StringType.INSTANCE, "a", SyntheticLocation.INSTANCE);

	private final ValueExpression c = new Constant(StringType.INSTANCE, "c", SyntheticLocation.INSTANCE);

	private final SubstringDomain domain = new SubstringDomain();

	private final Substrings valA;

	private final Substrings valB;

	private final Substrings valC;

	private final Substrings valD;

	private final Substrings valF;

	private final Substrings TOP;

	private final Substrings BOTTOM;

	private final ValueExpression XConcatA = new BinaryExpression(
			StringType.INSTANCE,
			x,
			a,
			StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);

	private final ProgramPoint pp = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return null;
		}

	};

	public SubstringsTest() {
		Map<Identifier, ExpressionInverseSet> mapA = new HashMap<>();
		Set<SymbolicExpression> setA = new HashSet<>();
		setA.add(y);
		setA.add(w);
		mapA.put(x, new ExpressionInverseSet(setA));

		Map<Identifier, ExpressionInverseSet> mapB = new HashMap<>();
		Set<SymbolicExpression> setB = new HashSet<>();
		Set<SymbolicExpression> setB2 = new HashSet<>();
		setB.add(z);
		setB2.add(w);
		mapB.put(y, new ExpressionInverseSet(setB));
		mapB.put(x, new ExpressionInverseSet(setB2));

		Map<Identifier, ExpressionInverseSet> mapC = new HashMap<>();
		Set<SymbolicExpression> setC = new HashSet<>();
		setC.add(z);
		mapC.put(x, new ExpressionInverseSet(setC));

		Map<Identifier, ExpressionInverseSet> mapD = new HashMap<>();
		Set<SymbolicExpression> setD = new HashSet<>();
		setD.add(w);
		mapD.put(y, new ExpressionInverseSet(setD));

		Map<Identifier, ExpressionInverseSet> mapE = new HashMap<>();
		Set<SymbolicExpression> setE1 = new HashSet<>();
		Set<SymbolicExpression> setE2 = new HashSet<>();
		setE1.add(y);
		setE2.add(x);
		mapE.put(x, new ExpressionInverseSet(setE1));
		mapE.put(y, new ExpressionInverseSet(setE2));

		Map<Identifier, ExpressionInverseSet> mapF = new HashMap<>();
		Set<SymbolicExpression> setF1 = new HashSet<>();
		Set<SymbolicExpression> setF2 = new HashSet<>();
		setF1.add(y);
		setF1.add(c);
		setF2.add(z);
		mapF.put(x, new ExpressionInverseSet(setF1));
		mapF.put(w, new ExpressionInverseSet(setF2));

		valA = new Substrings(new ExpressionInverseSet(), mapA);
		valB = new Substrings(new ExpressionInverseSet(), mapB);
		valC = new Substrings(new ExpressionInverseSet(), mapC);
		valD = new Substrings(new ExpressionInverseSet(), mapD);
		valF = new Substrings(new ExpressionInverseSet(), mapF);

		TOP = new Substrings().top();
		BOTTOM = new Substrings().bottom();
	}

	@Test
	public void testConstructor() {
		Substrings first = new Substrings();

		Map<Identifier, ExpressionInverseSet> f = new HashMap<>();

		Set<SymbolicExpression> set = new HashSet<>();

		set.add(y);
		ExpressionInverseSet eis = new ExpressionInverseSet(set);

		f.put(x, eis);

		Substrings second = new Substrings(new ExpressionInverseSet(), f);

		assertTrue(first.isBottom());
		assertTrue(second.getState(x).contains(y));

		assertTrue(new Substrings().top().isTop());
		assertTrue(new Substrings().bottom().isBottom());
	}

	@Test
	public void testForgetIdentifier()
			throws SemanticException {
		Map<Identifier, ExpressionInverseSet> f = new HashMap<>();

		Set<SymbolicExpression> set = new HashSet<>();
		Identifier y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);
		Identifier x = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);

		set.add(y);
		ExpressionInverseSet eis = new ExpressionInverseSet(set);

		f.put(x, eis);

		Substrings domain = new Substrings(new ExpressionInverseSet(), f);

		assertFalse(domain.forgetIdentifier(x, pp).knowsIdentifier(x));
	}

	@Test
	public void testForgetIdentifier2()
			throws SemanticException {
		Substrings s = domain.assign(valA, y, XConcatA, null, null);
		s = s.forgetIdentifier(x, pp);
		assertEquals(s.getState(x), new ExpressionInverseSet().top());
		assertFalse(s.getState(y).contains(x));
		assertFalse(s.getState(y).contains(XConcatA));
		assertFalse(s.knowsIdentifier(x));
	}

	@Test
	public void testForgetIdentifier3()
			throws SemanticException {
		Substrings s = domain.assign(valD, y, XConcatA, null, null);
		s = s.forgetIdentifier(x, pp);
		assertEquals(s.getState(x), new ExpressionInverseSet().top());
		assertFalse(s.getState(y).contains(x));
		assertFalse(s.getState(y).contains(XConcatA));
		assertFalse(s.knowsIdentifier(x));
	}

	@Test
	public void testForgetIdentifiersIf()
			throws SemanticException {
		Substrings s = valF.forgetIdentifiersIf((
				id) -> id.getName().equals("w") || id.getName().equals("y"), pp);
		assertEquals(s.getState(w), new ExpressionInverseSet().top());
		assertFalse(s.getState(x).contains(y));
	}

	@Test
	public void testKnowsIdentifier()
			throws SemanticException {
		assertTrue(valA.knowsIdentifier(x));
		assertTrue(valA.knowsIdentifier(y));
		assertFalse(valA.knowsIdentifier(z));
	}

	@Test
	public void testGlb1()
			throws SemanticException {
		Substrings glb = valA.glb(valB);

		assertTrue(glb.getState(x).contains(y));
		assertTrue(glb.getState(y).contains(z));
		assertTrue(glb.getState(x).contains(w));
		assertTrue(glb.getState(x).contains(z));

	}

	@Test
	public void testGlb2()
			throws SemanticException {
		Substrings glb = valA.glb(TOP);

		assertEquals(glb, valA);
	}

	@Test
	public void testGlb3()
			throws SemanticException {
		Substrings glb = BOTTOM.glb(valA);

		assertEquals(glb, BOTTOM);
	}

	@Test
	public void testGlb4()
			throws SemanticException {
		Substrings glb = valC.glb(valD);

		assertTrue(glb.getState(x).contains(z));
		assertTrue(glb.getState(y).contains(w));
	}

	@Test
	public void testLub1()
			throws SemanticException {
		Substrings lub = valA.lub(valB);

		assertFalse(lub.getState(x).contains(y));
		assertFalse(lub.getState(y).contains(z));
		assertEquals(lub.getState(y), lub.stateOfUnknown(y));
		assertTrue(lub.getState(x).contains(w));
	}

	@Test
	public void testLub2()
			throws SemanticException {
		Substrings lub = TOP.lub(valA);

		assertEquals(lub, TOP);
	}

	@Test
	public void testLub3()
			throws SemanticException {
		Substrings lub = BOTTOM.lub(valA);

		assertEquals(lub, valA);
	}

	@Test
	public void testLub4()
			throws SemanticException {
		Substrings lub = valC.lub(valD);

		assertEquals(lub, BOTTOM);
	}

	@Test
	public void testLub5()
			throws SemanticException {
		Substrings lub = valA.lub(valC);

		assertEquals(lub, BOTTOM);
	}

}
