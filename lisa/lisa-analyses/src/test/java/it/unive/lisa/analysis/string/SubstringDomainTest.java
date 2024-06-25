package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionInverseSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int16Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class SubstringDomainTest {

	Identifier y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);
	Identifier x = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);
	Identifier z = new Variable(StringType.INSTANCE, "z", SyntheticLocation.INSTANCE);
	Identifier w = new Variable(StringType.INSTANCE, "w", SyntheticLocation.INSTANCE);

	ValueExpression a = new Constant(StringType.INSTANCE, "a", SyntheticLocation.INSTANCE);
	ValueExpression b = new Constant(StringType.INSTANCE, "b", SyntheticLocation.INSTANCE);
	ValueExpression c = new Constant(StringType.INSTANCE, "c", SyntheticLocation.INSTANCE);
	ValueExpression ab = new Constant(StringType.INSTANCE, "ab", SyntheticLocation.INSTANCE);
	ValueExpression bc = new Constant(StringType.INSTANCE, "bc", SyntheticLocation.INSTANCE);
	ValueExpression abc = new Constant(StringType.INSTANCE, "abc", SyntheticLocation.INSTANCE);

	SubstringDomain domainA;
	SubstringDomain domainB;

	SubstringDomain domainC;
	SubstringDomain domainD;

	SubstringDomain domainE;

	SubstringDomain domainF;

	SubstringDomain TOP;
	SubstringDomain BOTTOM;

	ValueExpression XEqualsY = new BinaryExpression(BoolType.INSTANCE, x, y, StringEquals.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XEqualsW = new BinaryExpression(BoolType.INSTANCE, x, w, StringEquals.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression YSubstringOfX = new BinaryExpression(BoolType.INSTANCE, x, y, StringContains.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression WSubstringOfX = new BinaryExpression(BoolType.INSTANCE, x, w, StringContains.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XStartsWithY = new BinaryExpression(BoolType.INSTANCE, x, y, StringStartsWith.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XStartsWithW = new BinaryExpression(BoolType.INSTANCE, x, w, StringStartsWith.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XEndsWithY = new BinaryExpression(BoolType.INSTANCE, x, y, StringEndsWith.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XEndsWithW = new BinaryExpression(BoolType.INSTANCE, x, w, StringEndsWith.INSTANCE,
			SyntheticLocation.INSTANCE);

	ValueExpression invalid = new BinaryExpression(StringType.INSTANCE, x, XEqualsY, StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);

	ValueExpression XConcatY = new BinaryExpression(StringType.INSTANCE, x, y, StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression ZConcatX = new BinaryExpression(StringType.INSTANCE, z, x, StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression ZConcatXConcatY = new BinaryExpression(StringType.INSTANCE, z, XConcatY, StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);

	ValueExpression XConcatAB = new BinaryExpression(StringType.INSTANCE, x, ab, StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XConcatA = new BinaryExpression(StringType.INSTANCE, x, a, StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XConcatBC = new BinaryExpression(StringType.INSTANCE, x, bc, StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XConcatABC = new BinaryExpression(StringType.INSTANCE, x, abc, StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XConcatABCConcatY = new BinaryExpression(StringType.INSTANCE, XConcatABC, y, StringConcat.INSTANCE,
			SyntheticLocation.INSTANCE);

	ValueExpression CSubstringOfX = new BinaryExpression(BoolType.INSTANCE, x, c, StringContains.INSTANCE,
			SyntheticLocation.INSTANCE);
	ValueExpression XSubstringOfC = new BinaryExpression(BoolType.INSTANCE, c, x, StringContains.INSTANCE,
			SyntheticLocation.INSTANCE);

	public SubstringDomainTest() {
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

		domainA = new SubstringDomain(new ExpressionInverseSet(), mapA);
		domainB = new SubstringDomain(new ExpressionInverseSet(), mapB);
		domainC = new SubstringDomain(new ExpressionInverseSet(), mapC);
		domainD = new SubstringDomain(new ExpressionInverseSet(), mapD);
		domainE = new SubstringDomain(new ExpressionInverseSet(), mapE);
		domainF = new SubstringDomain(new ExpressionInverseSet(), mapF);

		TOP = new SubstringDomain().top();
		BOTTOM = new SubstringDomain().bottom();
		;
	}

	@Test
	public void testAssumeEmpty() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assumed = empty.assume(XEqualsY, null, null, null);
		assertTrue(assumed.getState(x).contains(y));
		assertTrue(assumed.getState(y).contains(x));
		assertFalse(assumed.getState(y).contains(y));
		assertFalse(assumed.getState(x).contains(x));
	}

	@Test
	public void testAssumeEmpty2() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assumed = empty.assume(XEndsWithY, null, null, null);
		assertTrue(assumed.getState(x).contains(y));
		assertFalse(assumed.getState(y).contains(x));
		assertFalse(assumed.getState(y).contains(y));
		assertFalse(assumed.getState(x).contains(x));
	}

	@Test
	public void testAssumeEmpty3() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assumed = empty.assume(XStartsWithY, null, null, null);
		assertTrue(assumed.getState(x).contains(y));
		assertFalse(assumed.getState(y).contains(x));
		assertFalse(assumed.getState(y).contains(y));
		assertFalse(assumed.getState(x).contains(x));
	}

	@Test
	public void testAssumeEmpty4() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assumed = empty.assume(YSubstringOfX, null, null, null);
		assertTrue(assumed.getState(x).contains(y));
		assertFalse(assumed.getState(y).contains(x));
		assertFalse(assumed.getState(y).contains(y));
		assertFalse(assumed.getState(x).contains(x));
	}

	@Test
	public void testAssumeEmpty5() throws SemanticException {
		ValueExpression orOperation = new BinaryExpression(BoolType.INSTANCE, WSubstringOfX, YSubstringOfX,
				LogicalOr.INSTANCE, SyntheticLocation.INSTANCE);
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assumed = empty.assume(orOperation, null, null, null);
		assertFalse(assumed.getState(x).contains(y));
		assertFalse(assumed.getState(y).contains(x));
		assertFalse(assumed.getState(y).contains(y));
		assertFalse(assumed.getState(x).contains(x));

		assertEquals(assumed, BOTTOM);
	}

	@Test
	public void testAssumeEmpty6() throws SemanticException {
		ValueExpression andOperation = new BinaryExpression(BoolType.INSTANCE, WSubstringOfX, YSubstringOfX,
				LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE);
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assumed = empty.assume(andOperation, null, null, null);
		assertTrue(assumed.getState(x).contains(y));
		assertFalse(assumed.getState(y).contains(x));
		assertFalse(assumed.getState(y).contains(w));
		assertTrue(assumed.getState(x).contains(w));
	}

	@Test
	public void testAssumeEmpty7() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assumed = empty.assume(XEqualsY, null, null, null);
		assertTrue(assumed.getState(x).contains(y));
		assertTrue(assumed.getState(y).contains(x));
	}

	@Test
	public void testAssume1() throws SemanticException {
		SubstringDomain assume1 = domainB.assume(YSubstringOfX, null, null, null);
		SubstringDomain assume2 = domainB.assume(XEndsWithY, null, null, null);
		SubstringDomain assume3 = domainB.assume(XStartsWithY, null, null, null);

		assertEquals(assume1, assume2);
		assertEquals(assume1, assume3);
		assertTrue(assume1.getState(x).contains(y));
		assertTrue(assume1.getState(x).contains(w));
		assertTrue(assume1.getState(x).contains(z));
		assertTrue(assume1.getState(y).contains(z));
	}

	@Test
	public void testAssume2() throws SemanticException {
		SubstringDomain assume = domainB.assume(XEqualsY, null, null, null);

		assertTrue(assume.getState(x).contains(y));
		assertTrue(assume.getState(x).contains(w));
		assertTrue(assume.getState(x).contains(z));
		assertTrue(assume.getState(y).contains(w));
		assertTrue(assume.getState(y).contains(x));
		assertTrue(assume.getState(y).contains(z));
	}

	@Test
	public void testAssume3() throws SemanticException {
		Identifier j = new Variable(StringType.INSTANCE, "j", SyntheticLocation.INSTANCE);
		ValueExpression JSubstringOfY = new BinaryExpression(BoolType.INSTANCE, y, j, StringContains.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression andOperation = new BinaryExpression(BoolType.INSTANCE, JSubstringOfY, YSubstringOfX,
				LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE);
		SubstringDomain assume = domainB.assume(andOperation, null, null, null);

		assertTrue(assume.getState(x).contains(w));
		assertTrue(assume.getState(x).contains(y));
		assertTrue(assume.getState(x).contains(z));
		assertTrue(assume.getState(x).contains(j));
		assertTrue(assume.getState(y).contains(j));
		assertTrue(assume.getState(y).contains(z));
		assertFalse(assume.getState(x).contains(x));
		assertFalse(assume.getState(y).contains(y));

	}

	@Test
	public void testAssume4() throws SemanticException {
		Identifier j = new Variable(StringType.INSTANCE, "j", SyntheticLocation.INSTANCE);
		ValueExpression JSubstringOfY = new BinaryExpression(BoolType.INSTANCE, y, j, StringContains.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression orOperation = new BinaryExpression(BoolType.INSTANCE, JSubstringOfY, YSubstringOfX,
				LogicalOr.INSTANCE, SyntheticLocation.INSTANCE);
		SubstringDomain assume = domainB.assume(orOperation, null, null, null);

		assertTrue(assume.getState(x).contains(w));
		assertFalse(assume.getState(x).contains(y));
		assertFalse(assume.getState(x).contains(z));
		assertFalse(assume.getState(x).contains(j));
		assertFalse(assume.getState(y).contains(j));
		assertTrue(assume.getState(y).contains(z));
		assertFalse(assume.getState(x).contains(x));
		assertFalse(assume.getState(y).contains(y));

	}

	@Test
	public void testAssume5() throws SemanticException {
		SubstringDomain assume = domainB.assume(CSubstringOfX, null, null, null);

		assertTrue(assume.getState(x).contains(c));
		assertTrue(assume.getState(x).contains(w));
		assertTrue(assume.getState(y).contains(z));
	}

	@Test
	public void testAssume6() throws SemanticException {
		SubstringDomain assume = domainF.assume(XSubstringOfC, null, null, null);

		assertEquals(assume, domainF);
	}

	@Test
	public void testSatisfies() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED, domainA.satisfies(YSubstringOfX, null, null));

		assertEquals(Satisfiability.SATISFIED, domainE.satisfies(XEqualsY, null, null));
		assertEquals(Satisfiability.SATISFIED, domainE.satisfies(YSubstringOfX, null, null));
		assertEquals(Satisfiability.UNKNOWN, domainE.satisfies(XEqualsW, null, null));

		assertEquals(Satisfiability.SATISFIED, domainE.satisfies(XStartsWithY, null, null));
		assertEquals(Satisfiability.UNKNOWN, domainA.satisfies(XStartsWithY, null, null));
		assertEquals(Satisfiability.SATISFIED, domainE.satisfies(XEndsWithY, null, null));
		assertEquals(Satisfiability.UNKNOWN, domainA.satisfies(XEndsWithY, null, null));

		ValueExpression andOperation = new BinaryExpression(BoolType.INSTANCE, WSubstringOfX, YSubstringOfX,
				LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE);
		ValueExpression orOperation1 = new BinaryExpression(BoolType.INSTANCE, WSubstringOfX, YSubstringOfX,
				LogicalOr.INSTANCE, SyntheticLocation.INSTANCE);
		ValueExpression orOperation2 = new BinaryExpression(BoolType.INSTANCE, YSubstringOfX, WSubstringOfX,
				LogicalOr.INSTANCE, SyntheticLocation.INSTANCE);

		assertEquals(Satisfiability.SATISFIED, domainA.satisfies(andOperation, null, null));
		assertEquals(Satisfiability.SATISFIED, domainE.satisfies(orOperation1, null, null));
		assertEquals(Satisfiability.SATISFIED, domainA.satisfies(orOperation1, null, null));
		assertEquals(Satisfiability.UNKNOWN, domainE.satisfies(andOperation, null, null));
		assertEquals(Satisfiability.UNKNOWN, domainD.satisfies(orOperation1, null, null));

		assertEquals(domainE.satisfies(orOperation2, null, null), domainE.satisfies(orOperation1, null, null));

		assertEquals(Satisfiability.SATISFIED, domainF.satisfies(CSubstringOfX, null, null));
		assertEquals(Satisfiability.UNKNOWN, domainF.satisfies(XSubstringOfC, null, null));
	}

	@Test
	public void testAssignEmptyDomain1() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, y, null, null);
		assertTrue(assigned.getState(x).contains(y));
	}

	@Test
	public void testAssignEmptyDomain2() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, XConcatY, null, null);
		assertTrue(assigned.getState(x).contains(y));
		assertFalse(assigned.getState(x).contains(x));
	}

	@Test
	public void testAssignEmptyDomain3() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, ZConcatXConcatY, null, null);
		assertTrue(assigned.getState(x).contains(y));
		assertFalse(assigned.getState(x).contains(x));
		assertTrue(assigned.getState(x).contains(z));
	}

	@Test
	public void testAssignEmptyDomain4() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, c, null, null);
		assertTrue(assigned.getState(x).contains(c));
	}

	@Test
	public void testAssignEmptyDomain5() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		Identifier j = new Variable(StringType.INSTANCE, "j", SyntheticLocation.INSTANCE);

		SubstringDomain assigned = empty.assign(j, c, null, null)
				.assign(w, j, null, null)
				.assign(y, x, null, null);

		assigned = assigned.assume(WSubstringOfX, null, null, null);

		assertTrue(assigned.getState(j).contains(c));
		assertTrue(assigned.getState(w).contains(c));
		assertTrue(assigned.getState(w).contains(j));
		assertTrue(assigned.getState(x).contains(w));
		assertTrue(assigned.getState(x).contains(c));
		assertTrue(assigned.getState(x).contains(j));
		assertTrue(assigned.getState(y).contains(x));
		assertTrue(assigned.getState(y).contains(w));
		assertTrue(assigned.getState(y).contains(c));
		assertTrue(assigned.getState(y).contains(j));
	}

	@Test
	public void testAssignEmptyDomain6() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();

		ValueExpression a = new Constant(StringType.INSTANCE, "a", SyntheticLocation.INSTANCE);
		ValueExpression b = new Constant(StringType.INSTANCE, "b", SyntheticLocation.INSTANCE);
		ValueExpression AConcatB = new BinaryExpression(StringType.INSTANCE, a, b, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain assigned = empty.assign(x, AConcatB, null, null);
		assigned = assigned.assign(y, a, null, null);

		assertTrue(assigned.getState(x).contains(y));

		assigned = assigned.assign(x, c, null, null);

		assertTrue(assigned.getState(y).contains(a));
		assertFalse(assigned.getState(x).contains(a));
		assertTrue(assigned.getState(x).contains(c));
	}

	@Test
	public void testAssignEmptyDomain7() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();

		ValueExpression a = new Constant(StringType.INSTANCE, "a", SyntheticLocation.INSTANCE);
		ValueExpression b = new Constant(StringType.INSTANCE, "b", SyntheticLocation.INSTANCE);
		ValueExpression AConcatB = new BinaryExpression(StringType.INSTANCE, a, b, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain assigned = empty.assign(x, AConcatB, null, null);
		assigned = assigned.assign(y, x, null, null);
		assigned = assigned.assign(x, c, null, null);

		assertTrue(assigned.getState(y).contains(a));
		assertTrue(assigned.getState(y).contains(b));
		assertFalse(assigned.getState(x).contains(a));
		assertTrue(assigned.getState(x).contains(c));
		assertFalse(assigned.getState(y).contains(x));
	}

	@Test
	public void testAssignEmptyDomain8() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();

		ValueExpression a = new Constant(StringType.INSTANCE, "a", SyntheticLocation.INSTANCE);
		ValueExpression b = new Constant(StringType.INSTANCE, "b", SyntheticLocation.INSTANCE);
		ValueExpression AConcatB = new BinaryExpression(StringType.INSTANCE, a, b, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueExpression XConcatC = new BinaryExpression(StringType.INSTANCE, x, c, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain assigned = empty.assign(x, AConcatB, null, null);
		assigned = assigned.assign(y, x, null, null);
		assigned = assigned.assign(x, XConcatC, null, null);

		assertTrue(assigned.getState(y).contains(a));
		assertTrue(assigned.getState(y).contains(b));
		assertTrue(assigned.getState(x).contains(a));
		assertTrue(assigned.getState(x).contains(c));
		assertFalse(assigned.getState(y).contains(c));
	}

	@Test
	public void testAssignEmptyDomain9() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();

		SubstringDomain assigned = empty.assign(x, c, null, null);
		assigned = assigned.assign(y, x, null, null);

	}

	@Test
	public void testAssign1() throws SemanticException {
		SubstringDomain assigned = domainA.assign(x, z, null, null);
		assertTrue(assigned.getState(x).contains(z));
		assertFalse(assigned.getState(x).contains(y));
	}

	@Test
	public void testAssign2() throws SemanticException {
		SubstringDomain assigned = domainA.assign(x, XConcatY, null, null);
		assertTrue(assigned.getState(x).contains(y));
		assertFalse(assigned.getState(x).contains(x));
		assertTrue(assigned.getState(x).contains(w));
	}

	@Test
	public void testAssign3() throws SemanticException {
		ValueExpression XConcatZ = new BinaryExpression(StringType.INSTANCE, x, z, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain assigned = domainA.assign(x, XConcatZ, null, null);
		assertTrue(assigned.getState(x).contains(y));
		assertFalse(assigned.getState(x).contains(x));
		assertTrue(assigned.getState(x).contains(w));
		assertTrue(assigned.getState(x).contains(z));
	}

	@Test
	public void testAssign4() throws SemanticException {
		SubstringDomain assigned = domainA.assign(x, y, null, null);
		assertTrue(assigned.getState(x).contains(y));
		assertFalse(assigned.getState(x).contains(w));
	}

	@Test
	public void testAssign5() throws SemanticException {
		SubstringDomain assigned = domainD.assign(x, XConcatY, null, null);
		assertTrue(assigned.getState(x).contains(y));
		assertTrue(assigned.getState(x).contains(w));
		assertTrue(assigned.getState(y).contains(w));
	}

	@Test
	public void testAssign6() throws SemanticException {
		ValueExpression WConcatY = new BinaryExpression(StringType.INSTANCE, w, y, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		SubstringDomain assigned = domainA.assign(z, WConcatY, null, null);
		assertTrue(assigned.getState(x).contains(y));
		assertTrue(assigned.getState(x).contains(w));
		assertTrue(assigned.getState(z).contains(w));
		assertTrue(assigned.getState(z).contains(y));
		assertFalse(assigned.getState(x).contains(z));
	}

	@Test
	public void testAssign7() throws SemanticException {
		SubstringDomain assigned = domainE.assign(y, z, null, null);
		assertFalse(assigned.getState(x).contains(y));
		assertFalse(assigned.getState(y).contains(x));
		assertTrue(assigned.getState(y).contains(z));

	}

	@Test
	public void testAssign8() throws SemanticException {
		ValueExpression XConcatZ = new BinaryExpression(StringType.INSTANCE, x, z, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		SubstringDomain assigned = domainA.assign(x, XConcatZ, null, null);
		assigned = assigned.assign(w, x, null, null);
		assertFalse(assigned.getState(x).contains(w));
		assertTrue(assigned.getState(x).contains(y));
		assertTrue(assigned.getState(x).contains(z));
		assertTrue(assigned.getState(w).contains(x));
		assertTrue(assigned.getState(w).contains(y));
		assertTrue(assigned.getState(w).contains(z));
	}

	@Test
	public void testAssign9() throws SemanticException {
		SubstringDomain assigned = new SubstringDomain().assign(x, w, null, null)
				.assign(y, x, null, null)
				.assign(x, ZConcatXConcatY, null, null);
		assertTrue(assigned.getState(x).contains(y));
		assertTrue(assigned.getState(x).contains(z));
		assertTrue(assigned.getState(x).contains(w));
		assertFalse(assigned.getState(y).contains(x));
		assertTrue(assigned.getState(y).contains(w));
	}

	@Test
	public void testAssign10() throws SemanticException {
		Identifier j = new Variable(StringType.INSTANCE, "j", SyntheticLocation.INSTANCE);

		SubstringDomain assigned = domainF.assign(j, x, null, null);

		assertTrue(assigned.getState(x).contains(c));
		assertTrue(assigned.getState(x).contains(y));
		assertTrue(assigned.getState(w).contains(z));
		assertTrue(assigned.getState(j).contains(x));
		assertTrue(assigned.getState(j).contains(c));
		assertTrue(assigned.getState(j).contains(y));
	}

	@Test
	public void testAssign11() throws SemanticException {
		SubstringDomain assigned = new SubstringDomain().assign(y, c, null, null).assign(x, y, null, null);

		assertTrue(assigned.getState(x).contains(y));
		assertTrue(assigned.getState(x).contains(c));
		assertTrue(assigned.getState(y).contains(c));
	}

	@Test
	public void testAssign12() throws SemanticException {
		SubstringDomain assigned = new SubstringDomain().assign(y, c, null, null).assign(x, c, null, null);

		assertTrue(assigned.getState(y).contains(x));
	}

	@Test
	public void testEmptyAssignComplex1() throws SemanticException {
		ValueExpression ZConcatY = new BinaryExpression(StringType.INSTANCE, z, y, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(w, ZConcatXConcatY, null, null);

		assertTrue(assigned.getState(w).contains(ZConcatX));
		assertTrue(assigned.getState(w).contains(XConcatY));
		assertTrue(assigned.getState(w).contains(ZConcatXConcatY));
		assertTrue(assigned.getState(w).contains(x));
		assertTrue(assigned.getState(w).contains(z));
		assertTrue(assigned.getState(w).contains(y));
		assertFalse(assigned.getState(w).contains(ZConcatY));
	}

	@Test
	public void testEmptyAssignComplex2() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(w, XConcatABC, null, null);

		assertTrue(assigned.getState(w).contains(XConcatABC));
		assertTrue(assigned.getState(w).contains(x));
		assertTrue(assigned.getState(w).contains(abc));
		assertTrue(assigned.getState(w).contains(ab));
		assertTrue(assigned.getState(w).contains(bc));
		assertTrue(assigned.getState(w).contains(a));
		assertTrue(assigned.getState(w).contains(b));
		assertTrue(assigned.getState(w).contains(XConcatA));
		assertTrue(assigned.getState(w).contains(XConcatAB));
		assertFalse(assigned.getState(w).contains(XConcatBC));

	}

	@Test
	public void testEmptyAssignComplex3() throws SemanticException {
		ValueExpression abcd = new Constant(StringType.INSTANCE, "abcd", SyntheticLocation.INSTANCE);
		ValueExpression ab = new Constant(StringType.INSTANCE, "ab", SyntheticLocation.INSTANCE);
		ValueExpression cd = new Constant(StringType.INSTANCE, "cd", SyntheticLocation.INSTANCE);
		ValueExpression ABConcatCD = new BinaryExpression(StringType.INSTANCE, ab, cd, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned1 = empty.assign(x, abcd, null, null);
		SubstringDomain assigned2 = empty.assign(x, ABConcatCD, null, null);

		assertEquals(assigned1, assigned2);
	}

	@Test
	public void testEmptyAssignComplex4() throws SemanticException {
		ValueExpression ab = new Constant(StringType.INSTANCE, "ab", SyntheticLocation.INSTANCE);
		ValueExpression cd = new Constant(StringType.INSTANCE, "cd", SyntheticLocation.INSTANCE);
		ValueExpression ABConcatY = new BinaryExpression(StringType.INSTANCE, ab, y, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueExpression ABConcatYConcatCD = new BinaryExpression(StringType.INSTANCE, ABConcatY, cd,
				StringConcat.INSTANCE, SyntheticLocation.INSTANCE);

		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, ABConcatYConcatCD, null, null);

		assertTrue(assigned.getState(x).size() == 15);
	}

	@Test
	public void testEmptyAssignComplex5() throws SemanticException {
		ValueExpression YConcatW = new BinaryExpression(StringType.INSTANCE, y, w, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueExpression YConcatWConcatAB = new BinaryExpression(StringType.INSTANCE, YConcatW, ab,
				StringConcat.INSTANCE, SyntheticLocation.INSTANCE);

		ValueExpression WConcatA = new BinaryExpression(StringType.INSTANCE, w, a, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, YConcatWConcatAB, null, null);

		assertTrue(assigned.getState(x).contains(y));
		assertTrue(assigned.getState(x).contains(w));
		assertTrue(assigned.getState(x).contains(a));
		assertTrue(assigned.getState(x).contains(b));
		assertTrue(assigned.getState(x).contains(ab));
		assertTrue(assigned.getState(x).contains(YConcatW));
		assertTrue(assigned.getState(x).contains(WConcatA));
	}

	@Test
	public void testEmptyAssignComplex6() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, c, null, null);
		assigned = assigned.assign(x, ZConcatX, null, null);

		assertTrue(assigned.getState(x).contains(c));

		assigned = assigned.assign(x, w, null, null);

		assertTrue(assigned.getState(x).contains(w) && assigned.getState(x).size() == 1);
	}

	@Test
	public void testEmptyAssignComplex7() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(y, ZConcatX, null, null);
		assigned = assigned.assign(x, c, null, null);

		assertFalse(assigned.getState(y).contains(x));
		assertFalse(assigned.getState(y).contains(ZConcatX));

	}

	@Test
	public void testEmptyInterAssignComplex() throws SemanticException {
		ValueExpression YConcatW = new BinaryExpression(StringType.INSTANCE, y, w, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueExpression YConcatWConcatAB = new BinaryExpression(StringType.INSTANCE, YConcatW, ab,
				StringConcat.INSTANCE, SyntheticLocation.INSTANCE);

		ValueExpression WConcatA = new BinaryExpression(StringType.INSTANCE, w, a, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, YConcatWConcatAB, null, null);
		assigned = assigned.assign(y, WConcatA, null, null);

		assertTrue(assigned.getState(x).contains(y));
	}

	@Test
	public void testEmptyInterAssignComplex2() throws SemanticException {
		SubstringDomain empty = new SubstringDomain();
		ValueExpression WConcatC = new BinaryExpression(StringType.INSTANCE, w, c, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain assigned = empty.assign(x, WConcatC, null, null);
		assigned = assigned.assign(x, ZConcatX, null, null);
		assigned = assigned.assign(y, ZConcatX, null, null);

		assertTrue(assigned.getState(y).contains(x));
		assertTrue(assigned.getState(y).contains(c));
		assertTrue(assigned.getState(y).contains(WConcatC));
	}

	@Test
	public void testAssumeComplex1() throws SemanticException {
		ValueExpression YConcatW = new BinaryExpression(StringType.INSTANCE, y, w, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueExpression XSubstringOfY = new BinaryExpression(BoolType.INSTANCE, y, x, StringContains.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, YConcatW, null, null);
		assigned = assigned.assume(XSubstringOfY, null, null, null);

		assertTrue(assigned.getState(y).contains(x));
		assertTrue(assigned.getState(y).contains(w));
		assertFalse(assigned.getState(y).contains(YConcatW));
	}

	@Test
	public void testAssumeComplex2() throws SemanticException {
		ValueExpression YConcatW = new BinaryExpression(StringType.INSTANCE, y, w, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueExpression YConcatWSubstringOfX = new BinaryExpression(BoolType.INSTANCE, x, YConcatW,
				StringContains.INSTANCE, SyntheticLocation.INSTANCE);

		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(z, YConcatW, null, null);
		assigned = assigned.assume(YConcatWSubstringOfX, null, null, null);

		assertEquals(assigned.getState(x), assigned.getState(z));
		assertFalse(assigned.getState(z).contains(x));
	}

	@Test
	public void testAssumeComplex3() throws SemanticException {
		ValueExpression XSubstringOfY = new BinaryExpression(BoolType.INSTANCE, y, x, StringContains.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain empty = new SubstringDomain();
		SubstringDomain assigned = empty.assign(x, abc, null, null);
		assigned = assigned.assume(XSubstringOfY, null, null, null);

		assertTrue(assigned.getState(y).contains(abc));
		assertTrue(assigned.getState(y).contains(x));
		assertFalse(assigned.getState(x).contains(y));

	}

	@Test
	public void testConstructor() {
		SubstringDomain first = new SubstringDomain();

		Map<Identifier, ExpressionInverseSet> f = new HashMap<>();

		Set<SymbolicExpression> set = new HashSet<>();

		set.add(y);
		ExpressionInverseSet eis = new ExpressionInverseSet(set);

		f.put(x, eis);

		SubstringDomain second = new SubstringDomain(new ExpressionInverseSet(), f);

		assertTrue(first.isBottom());
		assertTrue(second.getState(x).contains(y));

		assertTrue(new SubstringDomain().top().isTop());
		assertTrue(new SubstringDomain().bottom().isBottom());
	}

	@Test
	public void testForgetIdentifier() throws SemanticException {
		Map<Identifier, ExpressionInverseSet> f = new HashMap<>();

		Set<SymbolicExpression> set = new HashSet<>();
		Identifier y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);
		Identifier x = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);

		set.add(y);
		ExpressionInverseSet eis = new ExpressionInverseSet(set);

		f.put(x, eis);

		SubstringDomain domain = new SubstringDomain(new ExpressionInverseSet(), f);

		assertFalse(domain.forgetIdentifier(x).knowsIdentifier(x));
	}

	@Test
	public void testForgetIdentifier2() throws SemanticException {
		SubstringDomain s = domainA.assign(y, XConcatA, null, null);
		s = s.forgetIdentifier(x);
		assertEquals(s.getState(x), new ExpressionInverseSet().top());
		assertFalse(s.getState(y).contains(x));
		assertFalse(s.getState(y).contains(XConcatA));
		assertFalse(s.knowsIdentifier(x));
	}

	@Test
	public void testForgetIdentifier3() throws SemanticException {
		SubstringDomain s = domainD.assign(y, XConcatA, null, null);
		s = s.forgetIdentifier(x);
		assertEquals(s.getState(x), new ExpressionInverseSet().top());
		assertFalse(s.getState(y).contains(x));
		assertFalse(s.getState(y).contains(XConcatA));
		assertFalse(s.knowsIdentifier(x));
	}

	@Test
	public void testForgetIdentifiersIf() throws SemanticException {
		SubstringDomain s = domainF.forgetIdentifiersIf((
				id) -> id.getName().equals("w") || id.getName().equals("y"));
		assertEquals(s.getState(w), new ExpressionInverseSet().top());
		assertFalse(s.getState(x).contains(y));
	}

	@Test
	public void testKnowsIdentifier() throws SemanticException {
		assertTrue(domainA.knowsIdentifier(x));
		assertTrue(domainA.knowsIdentifier(y));
		assertFalse(domainA.knowsIdentifier(z));
	}

	@Test
	public void testGlb1() throws SemanticException {
		SubstringDomain glb = domainA.glb(domainB);

		assertTrue(glb.getState(x).contains(y));
		assertTrue(glb.getState(y).contains(z));
		assertTrue(glb.getState(x).contains(w));
		assertTrue(glb.getState(x).contains(z));

	}

	@Test
	public void testGlb2() throws SemanticException {
		SubstringDomain glb = domainA.glb(TOP);

		assertEquals(glb, domainA);
	}

	@Test
	public void testGlb3() throws SemanticException {
		SubstringDomain glb = BOTTOM.glb(domainA);

		assertEquals(glb, BOTTOM);
	}

	@Test
	public void testGlb4() throws SemanticException {
		SubstringDomain glb = domainC.glb(domainD);

		assertTrue(glb.getState(x).contains(z));
		assertTrue(glb.getState(y).contains(w));
	}

	@Test
	public void testLub1() throws SemanticException {
		SubstringDomain lub = domainA.lub(domainB);

		assertFalse(lub.getState(x).contains(y));
		assertFalse(lub.getState(y).contains(z));
		assertEquals(lub.getState(y), lub.stateOfUnknown(y));
		assertTrue(lub.getState(x).contains(w));
	}

	@Test
	public void testLub2() throws SemanticException {
		SubstringDomain lub = TOP.lub(domainA);

		assertEquals(lub, TOP);
	}

	@Test
	public void testLub3() throws SemanticException {
		SubstringDomain lub = BOTTOM.lub(domainA);

		assertEquals(lub, domainA);
	}

	@Test
	public void testLub4() throws SemanticException {
		SubstringDomain lub = domainC.lub(domainD);

		assertEquals(lub, BOTTOM);
	}

	@Test
	public void testLub5() throws SemanticException {
		SubstringDomain lub = domainA.lub(domainC);

		assertEquals(lub, BOTTOM);
	}

	@Test
	public void testExtr1() throws SemanticException {
		ValueExpression replaceYAC = new TernaryExpression(StringType.INSTANCE, y, a, c, StringReplace.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression ABConcatX = new BinaryExpression(StringType.INSTANCE, ab, x, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueExpression BConcatX = new BinaryExpression(StringType.INSTANCE, b, x, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression assignExpr = new BinaryExpression(StringType.INSTANCE, replaceYAC, ABConcatX,
				StringConcat.INSTANCE, SyntheticLocation.INSTANCE);

		SubstringDomain result = domainE.assign(z, assignExpr, null, null);

		assertTrue(result.getState(z).contains(ABConcatX));
		assertTrue(result.getState(z).contains(BConcatX));
	}

	@Test
	public void testExtr2() throws SemanticException {
		ValueExpression cb = new Constant(StringType.INSTANCE, "cb", SyntheticLocation.INSTANCE);

		ValueExpression replaceCbCA = new TernaryExpression(StringType.INSTANCE, cb, c, a, StringReplace.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression assignExpr = new BinaryExpression(StringType.INSTANCE, replaceCbCA, c, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain result = new SubstringDomain().assign(x, assignExpr, null, null);

		assertTrue(result.getState(x).contains(abc));
	}

	@Test
	public void testExtr3() throws SemanticException {
		ValueExpression zero = new Constant(Int16Type.INSTANCE, 0, SyntheticLocation.INSTANCE);
		ValueExpression two = new Constant(Int16Type.INSTANCE, 2, SyntheticLocation.INSTANCE);
		ValueExpression substringAbc02 = new TernaryExpression(StringType.INSTANCE, abc, zero, two,
				StringSubstring.INSTANCE, SyntheticLocation.INSTANCE);

		ValueExpression assignExpr = new BinaryExpression(StringType.INSTANCE, substringAbc02, c, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain result = new SubstringDomain().assign(x, assignExpr, null, null);

		assertTrue(result.getState(x).contains(abc));
	}

	@Test
	public void testExtr4() throws SemanticException {
		ValueExpression replaceXCA = new TernaryExpression(StringType.INSTANCE, x, c, a, StringReplace.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression assignExpr = new BinaryExpression(StringType.INSTANCE, replaceXCA, c, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain result = new SubstringDomain().assign(x, assignExpr, null, null);

		assertTrue(result.getState(x).contains(c));
	}

	@Test
	public void testExtr5() throws SemanticException {
		ValueExpression zero = new Constant(Int16Type.INSTANCE, 0, SyntheticLocation.INSTANCE);
		ValueExpression two = new Constant(Int16Type.INSTANCE, 2, SyntheticLocation.INSTANCE);
		ValueExpression substringY02 = new TernaryExpression(StringType.INSTANCE, y, zero, two,
				StringSubstring.INSTANCE, SyntheticLocation.INSTANCE);

		ValueExpression assignExpr = new BinaryExpression(StringType.INSTANCE, substringY02, c, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain result = new SubstringDomain().assign(x, assignExpr, null, null);

		assertTrue(result.getState(x).contains(c));
	}
	
	@Test
	public void testExtr6() throws SemanticException {
		Constant AB = new Constant(StringType.INSTANCE, "ab", SyntheticLocation.INSTANCE);
		Constant CD = new Constant(StringType.INSTANCE, "cd", SyntheticLocation.INSTANCE);
		ValueExpression ABConcatCD = new BinaryExpression(StringType.INSTANCE, AB, CD, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		
		ValueExpression one = new Constant(Int16Type.INSTANCE, 1, SyntheticLocation.INSTANCE);
		ValueExpression three = new Constant(Int16Type.INSTANCE, 3, SyntheticLocation.INSTANCE);
		ValueExpression substringABCD13 = new TernaryExpression(StringType.INSTANCE, ABConcatCD, one, three,
				StringSubstring.INSTANCE, SyntheticLocation.INSTANCE);

		ValueExpression assignExpr = new BinaryExpression(StringType.INSTANCE, substringABCD13, c, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);

		SubstringDomain result = new SubstringDomain().assign(x, assignExpr, null, null);

		
		Constant BCC = new Constant(StringType.INSTANCE, "bcc", SyntheticLocation.INSTANCE);

		assertTrue(result.getState(x).contains(BCC));
	}
	
	@Test
	public void testExtr7() throws SemanticException {
		Constant AB = new Constant(StringType.INSTANCE, "ab", SyntheticLocation.INSTANCE);
		ValueExpression ABConcatY = new BinaryExpression(StringType.INSTANCE, AB, y, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
		
		ValueExpression one = new Constant(Int16Type.INSTANCE, 1, SyntheticLocation.INSTANCE);
		ValueExpression three = new Constant(Int16Type.INSTANCE, 3, SyntheticLocation.INSTANCE);
		ValueExpression assignExpr = new TernaryExpression(StringType.INSTANCE, ABConcatY, one, three,
				StringSubstring.INSTANCE, SyntheticLocation.INSTANCE);

		SubstringDomain result = new SubstringDomain().assign(x, assignExpr, null, null);

		assertTrue(result.isBottom());
	}
	
	@Test
	public void testExtr8() throws SemanticException {
		ValueExpression replaceABAC = new TernaryExpression(StringType.INSTANCE, ab, a, c, StringReplace.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression ABConcatX = new BinaryExpression(StringType.INSTANCE, ab, x, StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);


		ValueExpression assignExpr = new BinaryExpression(StringType.INSTANCE, replaceABAC, ABConcatX,
				StringConcat.INSTANCE, SyntheticLocation.INSTANCE);

		SubstringDomain result = domainE.assign(z, assignExpr, null, null);

		Constant CBAB = new Constant(StringType.INSTANCE, "cbab", SyntheticLocation.INSTANCE);
		assertTrue(result.getState(z).contains(CBAB));
	}
}
