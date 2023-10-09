package it.unive.lisa.analysis.string.tarsis;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ContainsTest {

	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	@Test
	public void test01() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<RegularExpression>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new Atom("a")));
		delta.add(new Transition<>(st[0], st[2], new Atom("b")));
		delta.add(new Transition<>(st[1], st[3], new Atom("a")));
		delta.add(new Transition<>(st[2], st[3], new Atom("c")));

		RegexAutomaton a = new RegexAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<RegularExpression>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], new Atom("a")));

		RegexAutomaton a2 = new RegexAutomaton(states2, delta2);

		Tarsis t1 = new Tarsis(a);
		Tarsis t2 = new Tarsis(a2);

		assertEquals(Satisfiability.UNKNOWN,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test02() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<RegularExpression>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new Atom("a")));
		delta.add(new Transition<>(st[0], st[2], new Atom("b")));
		delta.add(new Transition<>(st[1], st[3], new Atom("a")));
		delta.add(new Transition<>(st[2], st[3], new Atom("c")));

		RegexAutomaton a = new RegexAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<RegularExpression>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], new Atom("d")));

		RegexAutomaton a2 = new RegexAutomaton(states2, delta2);

		Tarsis t1 = new Tarsis(a);
		Tarsis t2 = new Tarsis(a2);

		assertEquals(Satisfiability.NOT_SATISFIED,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test03() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<RegularExpression>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new Atom("a")));
		delta.add(new Transition<>(st[0], st[2], new Atom("b")));
		delta.add(new Transition<>(st[1], st[3], new Atom("a")));
		delta.add(new Transition<>(st[2], st[3], new Atom("c")));

		RegexAutomaton a = new RegexAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<RegularExpression>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], Atom.EPSILON));

		RegexAutomaton a2 = new RegexAutomaton(states2, delta2);

		Tarsis t1 = new Tarsis(a);
		Tarsis t2 = new Tarsis(a2);

		assertEquals(Satisfiability.SATISFIED,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test04() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<RegularExpression>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new Atom("a")));
		delta.add(new Transition<>(st[0], st[2], new Atom("b")));
		delta.add(new Transition<>(st[1], st[3], new Atom("a")));
		delta.add(new Transition<>(st[2], st[3], new Atom("c")));

		RegexAutomaton a = new RegexAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[3];
		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, true);
		st2[2] = new State(6, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<RegularExpression>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], new Atom("a")));
		delta2.add(new Transition<>(st2[0], st2[2], new Atom("d")));

		RegexAutomaton a2 = new RegexAutomaton(states2, delta2);

		Tarsis t1 = new Tarsis(a);
		Tarsis t2 = new Tarsis(a2);

		assertEquals(Satisfiability.UNKNOWN,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test05() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<RegularExpression>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new Atom("a")));
		delta.add(new Transition<>(st[0], st[2], new Atom("b")));
		delta.add(new Transition<>(st[1], st[3], new Atom("a")));
		delta.add(new Transition<>(st[2], st[3], new Atom("c")));

		RegexAutomaton a = new RegexAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[1];
		st2[0] = new State(4, true, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<RegularExpression>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[0], new Atom("a")));

		RegexAutomaton a2 = new RegexAutomaton(states2, delta2);

		Tarsis t1 = new Tarsis(a);
		Tarsis t2 = new Tarsis(a2);

		assertEquals(Satisfiability.UNKNOWN,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test06() throws SemanticException {
		Tarsis t1 = new Tarsis(RegexAutomaton.topString());
		Tarsis t2 = new Tarsis(RegexAutomaton.string("a"));
		assertEquals(Satisfiability.UNKNOWN,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test07() throws SemanticException {
		Tarsis t1 = new Tarsis(RegexAutomaton.string("a"));
		Tarsis t2 = new Tarsis(RegexAutomaton.topString());
		assertEquals(Satisfiability.UNKNOWN,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test08() throws SemanticException {
		Tarsis t1 = new Tarsis(RegexAutomaton.topString().concat(RegexAutomaton.string("a")));
		Tarsis t2 = new Tarsis(RegexAutomaton.string("a"));
		assertEquals(Satisfiability.SATISFIED,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test09() throws SemanticException {
		Tarsis t1 = new Tarsis(RegexAutomaton.string("a"));
		Tarsis t2 = new Tarsis(RegexAutomaton.topString().concat(RegexAutomaton.string("a")));

		assertEquals(Satisfiability.UNKNOWN,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test10() throws SemanticException {
		Tarsis t1 = new Tarsis(RegexAutomaton.topString().concat(RegexAutomaton.string("a")));
		Tarsis t2 = new Tarsis(RegexAutomaton.topString().concat(RegexAutomaton.string("b")));
		assertEquals(Satisfiability.UNKNOWN,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test11() throws SemanticException {
		Tarsis t1 = new Tarsis(RegexAutomaton.topString().concat(RegexAutomaton.string("a")));
		Tarsis t2 = new Tarsis(RegexAutomaton.string("ba"));
		assertEquals(Satisfiability.UNKNOWN,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test12() throws SemanticException {
		Tarsis t1 = new Tarsis(RegexAutomaton.string("ba"));
		Tarsis t2 = new Tarsis(RegexAutomaton.topString().concat(RegexAutomaton.string("a")));
		assertEquals(Satisfiability.UNKNOWN,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void test13() throws SemanticException {
		Tarsis t1 = new Tarsis(RegexAutomaton.string("ba"));
		Tarsis t2 = new Tarsis(RegexAutomaton.topString().concat(RegexAutomaton.string("c")));
		assertEquals(Satisfiability.NOT_SATISFIED,
				t1.satisfiesBinaryExpression(StringContains.INSTANCE, t1, t2, null, oracle));
	}

	@Test
	public void containsTestSelfContains() {
		Tarsis a = new Tarsis(RegexAutomaton.string("abc"));

		// "abc".contanins("abc") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(a));
	}

	@Test
	public void containsEmptyStringConstantString() {
		Tarsis a = new Tarsis(RegexAutomaton.string("abc"));
		Tarsis search = new Tarsis(RegexAutomaton.emptyStr());

		// "abc".contanins("") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsEmptyStringFiniteStrings() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("a", "b", "c"));
		Tarsis search = new Tarsis(RegexAutomaton.emptyStr());

		// "abc".contanins("") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsEmptyStringLoops() {
		Tarsis a = new Tarsis(RegexAutomaton.string("abc").star());
		Tarsis search = new Tarsis(RegexAutomaton.emptyStr());

		// abc*.contanins("") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa001() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "sansa", "manga"));
		Tarsis search = new Tarsis(RegexAutomaton.string("an"));

		// {panda, sansa, manga}.contains(an) = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa002() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "sansa", "manga"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("an", "p"));

		// {panda, sansa, manga}.contains(an, p) = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa003() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "sansa", "manga"));
		Tarsis search = new Tarsis(RegexAutomaton.string("koala"));

		// {"panda", "sansa", "manga"}.contains("koala") = false
		assertEquals(Satisfiability.NOT_SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa004() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda!mc", "mc!papanda", "polo!mc!panda"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("panda", "mc"));

		// {"panda!mc", "mc!papanda", "polo!mc!panda"}.contains(panda, mc) =
		// true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa005() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda!mc", "mc!papanda", "polopanda"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("panda", "mc"));

		// {"panda!mc", "mc!papanda", "polopanda"}.contains(panda, mc) = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa006() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "pandone", "pandina", "pandetta"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("pa", "pan"));

		// {"panda", "pandone", "pandina", "pandetta"}.contains("pa", "pan") =
		// true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa007() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "ronda", "manga", "pandetta"));
		Tarsis search = new Tarsis(RegexAutomaton.string("an"));

		// {"panda", "ronda", "manga", "pandetta"}.contains("an") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa008() {
		Tarsis a = new Tarsis(
				RegexAutomaton.strings("pandaat", "pandamat", "pansarat", "pansasat", "koladat", "kolabato",
						"kosalata", "kosanaat"));

		Tarsis search = new Tarsis(RegexAutomaton.string("at"));

		// {"pandaat", "pandamat", "pansarat","pansasat",
		// "koladat", "kolabato", "kosalata", "kosanaat"}.contains("at") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa009() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("pandk", "panck", "panrk"));
		Tarsis search = new Tarsis(RegexAutomaton.string("an"));

		// {"pandk", "panck", "panrk"}.contains("an") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa010() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("pan", "pandk", "panck", "panrk"));
		Tarsis search = new Tarsis(RegexAutomaton.string("k"));

		// {"pan", "pandk", "panck", "panrk"}.contains("k") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));

	}

	@Test
	public void containsTestOldFa011() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("pan", "pandk", "panck", "panrw"));
		Tarsis search = new Tarsis(RegexAutomaton.string("k"));

		// {"pan", "pandk", "panck", "panrw"}.contains("k") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa012() {
		Tarsis a = new Tarsis(RegexAutomaton.string("panda"));
		Tarsis search = new Tarsis(RegexAutomaton.string("da"));

		// {"panda"}.contains("da") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa013() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "nda", "a"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("nda", "a"));

		// {"panda", "nda", "a"}.contains("nda", "a") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa014() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "anda"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("nda", "a"));

		// {"panda", "anda"}.contains("nda", "a") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa015() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "anda", "orda"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("nda", "a"));

		// {"panda", "anda", "orda"}.contains("nda", "a") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa016() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "koala"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("nda", "ala"));

		// {"panda", "koala"}.contains("nda", "ala") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa017() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "anda", "nda"));
		Tarsis search = new Tarsis(RegexAutomaton.string("nda"));

		// {"panda", "anda", "nda"}.contains("nda") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa019() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "pand", "nd"));
		Tarsis search = new Tarsis(RegexAutomaton.string("panda"));

		// {"panda", "pand", "nd"}.contains("panda") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa020() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "pand", "nd"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("panda", "anda", "da"));

		// {"panda", "pand", "nd"}.contains("panda", "anda", "da") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa021() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "pand", "nd"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("panda", "anda", "da", "d"));

		// {"panda", "pand", "nd"}.contains("panda", "anda", "da", "da") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa022() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "panda", "panda"));
		Tarsis search = new Tarsis(RegexAutomaton.string("panda"));

		// {"panda"}.contains("panda") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa023() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "pandapanda"));
		Tarsis search = new Tarsis(RegexAutomaton.string("panda"));

		// {"panda", "pandapanda"}.contains("panda") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa024() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("panda", "pandapanda"));
		Tarsis search = new Tarsis(RegexAutomaton.string("pandapanda"));

		// {"panda", "pandapanda"}.contains("pandapanda") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa025() {
		Tarsis a = new Tarsis(RegexAutomaton.string("ordine"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("ine", "dine"));

		// {"ordine"}.contains("ine", "dine") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa026() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("ordine", "sordine"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("ine", "dine"));

		// {"ordine", "sordine"}.contains("ine", "dine") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa027() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("ordine", "sordine"));
		Tarsis search = new Tarsis(RegexAutomaton.string("r"));

		// {"ordine", "sordine"}.contains("r") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa028() {
		Tarsis a = new Tarsis(RegexAutomaton.string("a").star());
		Tarsis search = new Tarsis(RegexAutomaton.string("a"));

		// {a*}.contains("a") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa029() {
		Tarsis a = new Tarsis(RegexAutomaton.string("a").star());

		// {a*}.contains(a*) = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(a));
	}

	@Test
	public void containsTestOldFa030() {
		Tarsis a = new Tarsis(RegexAutomaton.emptyStr());
		Tarsis search = new Tarsis(RegexAutomaton.string("e"));

		// {""}.contains("e") = false
		assertEquals(Satisfiability.NOT_SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa031() {
		Tarsis a = new Tarsis(RegexAutomaton.string("idea"));
		Tarsis search = new Tarsis(RegexAutomaton.string("idea"));

		// {"idea"}.contains("idea") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa033() {
		Tarsis a = new Tarsis(RegexAutomaton.string("idea2"));
		Tarsis search = new Tarsis(RegexAutomaton.string("idea"));

		// {"idea2"}.contains("idea") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa034() {
		Tarsis a = new Tarsis(RegexAutomaton.strings("idea", "riveda", "intrinseca"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("ea", "va", "ca"));

		// {"idea", "riveda", "intrinseca"}.contains("ea", "va", "ca") = top
		assertEquals(Satisfiability.UNKNOWN, a.contains(search));
	}

	@Test
	public void containsTestOldFa035() {
		Tarsis a = new Tarsis(RegexAutomaton.string("pandapanda"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("da", "nda"));

		// {"pandapanda"}.contains("da", "nda") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}

	@Test
	public void containsTestOldFa036() {
		Tarsis a = new Tarsis(RegexAutomaton.string("pandapanda"));
		Tarsis search = new Tarsis(RegexAutomaton.strings("ap", "p"));

		// {"pandapanda"}.contains("p", "ap") = true
		assertEquals(Satisfiability.SATISFIED, a.contains(search));
	}
}
