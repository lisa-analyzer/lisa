package it.unive.lisa.analysis.string.tarsis;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Variable;
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

	private final BinaryExpression expr = new BinaryExpression(
		BoolType.INSTANCE,
		new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE),
		new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE),
		StringContains.INSTANCE,
		SyntheticLocation.INSTANCE);

	@Test
	public void test01()
			throws SemanticException {
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

		Tarsis domain = new Tarsis();

		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test02()
			throws SemanticException {
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

		Tarsis domain = new Tarsis();

		assertEquals(Satisfiability.NOT_SATISFIED, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test03()
			throws SemanticException {
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

		Tarsis domain = new Tarsis();

		assertEquals(Satisfiability.SATISFIED, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test04()
			throws SemanticException {
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

		Tarsis domain = new Tarsis();

		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test05()
			throws SemanticException {
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

		Tarsis domain = new Tarsis();

		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test06()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.topString();
		RegexAutomaton a2 = RegexAutomaton.string("a");
		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test07()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("a");
		RegexAutomaton a2 = RegexAutomaton.topString();
		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test08()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.topString().concat(RegexAutomaton.string("a"));
		RegexAutomaton a2 = RegexAutomaton.string("a");
		assertEquals(Satisfiability.SATISFIED, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test09()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("a");
		RegexAutomaton a2 = RegexAutomaton.topString().concat(RegexAutomaton.string("a"));

		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test10()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.topString().concat(RegexAutomaton.string("a"));
		RegexAutomaton a2 = RegexAutomaton.topString().concat(RegexAutomaton.string("b"));
		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test11()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.topString().concat(RegexAutomaton.string("a"));
		RegexAutomaton a2 = RegexAutomaton.string("ba");
		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test12()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("ba");
		RegexAutomaton a2 = RegexAutomaton.topString().concat(RegexAutomaton.string("a"));
		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test13()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("ba");
		RegexAutomaton a2 = RegexAutomaton.topString().concat(RegexAutomaton.string("c"));
		assertEquals(Satisfiability.NOT_SATISFIED, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void containsTestSelfContains()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("abc");

		// "abc".contanins("abc") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, a));
	}

	@Test
	public void containsEmptyStringConstantString()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("abc");
		RegexAutomaton search = RegexAutomaton.emptyStr();

		// "abc".contanins("") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsEmptyStringFiniteStrings()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("a", "b", "c");
		RegexAutomaton search = RegexAutomaton.emptyStr();

		// "abc".contanins("") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsEmptyStringLoops()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("abc").star();
		RegexAutomaton search = RegexAutomaton.emptyStr();

		// abc*.contanins("") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa001()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "sansa", "manga");
		RegexAutomaton search = RegexAutomaton.string("an");

		// {panda, sansa, manga}.contains(an) = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa002()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "sansa", "manga");
		RegexAutomaton search = RegexAutomaton.strings("an", "p");

		// {panda, sansa, manga}.contains(an, p) = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa003()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "sansa", "manga");
		RegexAutomaton search = RegexAutomaton.string("koala");

		// {"panda", "sansa", "manga"}.contains("koala") = false
		assertEquals(Satisfiability.NOT_SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa004()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda!mc", "mc!papanda", "polo!mc!panda");
		RegexAutomaton search = RegexAutomaton.strings("panda", "mc");

		// {"panda!mc", "mc!papanda", "polo!mc!panda"}.contains(panda, mc) =
		// true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa005()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda!mc", "mc!papanda", "polopanda");
		RegexAutomaton search = RegexAutomaton.strings("panda", "mc");

		// {"panda!mc", "mc!papanda", "polopanda"}.contains(panda, mc) = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa006()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "pandone", "pandina", "pandetta");
		RegexAutomaton search = RegexAutomaton.strings("pa", "pan");

		// {"panda", "pandone", "pandina", "pandetta"}.contains("pa", "pan") =
		// true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa007()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "ronda", "manga", "pandetta");
		RegexAutomaton search = RegexAutomaton.string("an");

		// {"panda", "ronda", "manga", "pandetta"}.contains("an") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa008()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton
			.strings("pandaat", "pandamat", "pansarat", "pansasat", "koladat", "kolabato", "kosalata", "kosanaat");
		RegexAutomaton search = RegexAutomaton.string("at");

		// {"pandaat", "pandamat", "pansarat","pansasat",
		// "koladat", "kolabato", "kosalata", "kosanaat"}.contains("at") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa009()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("pandk", "panck", "panrk");
		RegexAutomaton search = RegexAutomaton.string("an");

		// {"pandk", "panck", "panrk"}.contains("an") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa010()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("pan", "pandk", "panck", "panrk");
		RegexAutomaton search = RegexAutomaton.string("k");

		// {"pan", "pandk", "panck", "panrk"}.contains("k") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));

	}

	@Test
	public void containsTestOldFa011()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("pan", "pandk", "panck", "panrw");
		RegexAutomaton search = RegexAutomaton.string("k");

		// {"pan", "pandk", "panck", "panrw"}.contains("k") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa012()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("panda");
		RegexAutomaton search = RegexAutomaton.string("da");

		// {"panda"}.contains("da") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa013()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "nda", "a");
		RegexAutomaton search = RegexAutomaton.strings("nda", "a");

		// {"panda", "nda", "a"}.contains("nda", "a") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa014()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "anda");
		RegexAutomaton search = RegexAutomaton.strings("nda", "a");

		// {"panda", "anda"}.contains("nda", "a") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa015()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "anda", "orda");
		RegexAutomaton search = RegexAutomaton.strings("nda", "a");

		// {"panda", "anda", "orda"}.contains("nda", "a") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa016()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "koala");
		RegexAutomaton search = RegexAutomaton.strings("nda", "ala");

		// {"panda", "koala"}.contains("nda", "ala") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa017()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "anda", "nda");
		RegexAutomaton search = RegexAutomaton.string("nda");

		// {"panda", "anda", "nda"}.contains("nda") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa019()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "pand", "nd");
		RegexAutomaton search = RegexAutomaton.string("panda");

		// {"panda", "pand", "nd"}.contains("panda") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa020()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "pand", "nd");
		RegexAutomaton search = RegexAutomaton.strings("panda", "anda", "da");

		// {"panda", "pand", "nd"}.contains("panda", "anda", "da") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa021()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "pand", "nd");
		RegexAutomaton search = RegexAutomaton.strings("panda", "anda", "da", "d");

		// {"panda", "pand", "nd"}.contains("panda", "anda", "da", "da") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa022()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "panda", "panda");
		RegexAutomaton search = RegexAutomaton.string("panda");

		// {"panda"}.contains("panda") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa023()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "pandapanda");
		RegexAutomaton search = RegexAutomaton.string("panda");

		// {"panda", "pandapanda"}.contains("panda") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa024()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("panda", "pandapanda");
		RegexAutomaton search = RegexAutomaton.string("pandapanda");

		// {"panda", "pandapanda"}.contains("pandapanda") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa025()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("ordine");
		RegexAutomaton search = RegexAutomaton.strings("ine", "dine");

		// {"ordine"}.contains("ine", "dine") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa026()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("ordine", "sordine");
		RegexAutomaton search = RegexAutomaton.strings("ine", "dine");

		// {"ordine", "sordine"}.contains("ine", "dine") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa027()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("ordine", "sordine");
		RegexAutomaton search = RegexAutomaton.string("r");

		// {"ordine", "sordine"}.contains("r") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa028()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("a").star();
		RegexAutomaton search = RegexAutomaton.string("a");

		// {a*}.contains("a") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa029()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("a").star();

		// {a*}.contains(a*) = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, a));
	}

	@Test
	public void containsTestOldFa030()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.emptyStr();
		RegexAutomaton search = RegexAutomaton.string("e");

		// {""}.contains("e") = false
		assertEquals(Satisfiability.NOT_SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa031()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("idea");
		RegexAutomaton search = RegexAutomaton.string("idea");

		// {"idea"}.contains("idea") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa033()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("idea2");
		RegexAutomaton search = RegexAutomaton.string("idea");

		// {"idea2"}.contains("idea") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa034()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.strings("idea", "riveda", "intrinseca");
		RegexAutomaton search = RegexAutomaton.strings("ea", "va", "ca");

		// {"idea", "riveda", "intrinseca"}.contains("ea", "va", "ca") = top
		assertEquals(Satisfiability.UNKNOWN, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa035()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("pandapanda");
		RegexAutomaton search = RegexAutomaton.strings("da", "nda");

		// {"pandapanda"}.contains("da", "nda") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

	@Test
	public void containsTestOldFa036()
			throws SemanticException {
		Tarsis domain = new Tarsis();
		RegexAutomaton a = RegexAutomaton.string("pandapanda");
		RegexAutomaton search = RegexAutomaton.strings("ap", "p");

		// {"pandapanda"}.contains("p", "ap") = true
		assertEquals(Satisfiability.SATISFIED, domain.contains(a, search));
	}

}
