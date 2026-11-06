package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HistoryStateTest {

	private final Identifier x = new Variable(Int32Type.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>> state(
			SignLattice sign) {
		return new SimpleAbstractState<>(
				Monolith.SINGLETON,
				new ValueEnvironment<>(SignLattice.TOP).putState(x, sign),
				new TypeEnvironment<>(new TypeSet()));
	}

	private final SimpleAbstractState<Monolith,
			ValueEnvironment<SignLattice>,
			TypeEnvironment<TypeSet>> pos = state(SignLattice.POS);

	private final SimpleAbstractState<Monolith,
			ValueEnvironment<SignLattice>,
			TypeEnvironment<TypeSet>> neg = state(SignLattice.NEG);

	private final SimpleAbstractState<Monolith,
			ValueEnvironment<SignLattice>,
			TypeEnvironment<TypeSet>> zero = state(SignLattice.ZERO);

	@Test
	public void headYieldsTheCurrentState() {
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> h = new HistoryState<>(pos);
		assertEquals(pos, h.head());
	}

	@Test
	public void aFreshHistoryHasOnlyOneEntry() {
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> h = new HistoryState<>(pos);
		List<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> seen = new ArrayList<>();
		for (SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>> s : h)
			seen.add(s);
		assertEquals(List.of(pos), seen);
	}

	@Test
	public void withHeadReplacesTheCurrentStateOnly()
			throws SemanticException {
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> h = new HistoryState<>(pos).upchainAux(new HistoryState<>(neg));
		HistoryState<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> replaced = h
						.withHead(zero);
		assertEquals(zero, replaced.head());

		List<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> seen = new ArrayList<>();
		for (SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>> s : replaced)
			seen.add(s);
		assertEquals(List.of(pos, zero), seen);
	}

	@Test
	public void upchainPushesTheJoinOnTopKeepingThisAsHistory()
			throws SemanticException {
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> older = new HistoryState<>(pos);
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> newer = new HistoryState<>(neg);
		HistoryState<
				SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> merged = older
						.upchainAux(newer);

		assertEquals(pos.lub(neg), merged.head());

		Iterator<SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> it = merged
				.iterator();
		assertEquals(pos, it.next());
		assertEquals(pos.lub(neg), it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void upchainIgnoresTheOtherStateOwnHistory()
			throws SemanticException {
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> otherWithHistory = new HistoryState<>(pos)
						.upchainAux(new HistoryState<>(zero));
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> merged = new HistoryState<>(neg).upchainAux(otherWithHistory);

		List<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> seen = new ArrayList<>();
		for (SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>> s : merged)
			seen.add(s);
		// only neg (this' own history) and the join are present: zero (only
		// reachable through otherWithHistory's own history) must not appear
		assertEquals(List.of(neg, neg.lub(otherWithHistory.head())), seen);
	}

	@Test
	public void isTopRequiresBothCurrentTopAndNoHistory()
			throws SemanticException {
		SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>> top = pos.top();
		assertTrue(new HistoryState<>(top).isTop());
		assertFalse(new HistoryState<>(top).upchainAux(new HistoryState<>(pos)).isTop());
		assertFalse(new HistoryState<>(pos).isTop());
	}

	@Test
	public void isBottomRequiresBothCurrentBottomAndNoHistory()
			throws SemanticException {
		SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>> bottom = pos.bottom();
		assertTrue(new HistoryState<>(bottom).isBottom());
		assertFalse(new HistoryState<>(bottom).upchainAux(new HistoryState<>(pos)).isBottom());
	}

	@Test
	public void lessOrEqualLooksOnlyAtTheCurrentState()
			throws SemanticException {
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> h = new HistoryState<>(pos);
		assertTrue(h.lessOrEqualAux(new HistoryState<>(pos.top())));
		assertFalse(h.lessOrEqualAux(new HistoryState<>(neg)));
	}

	@Test
	public void equalsRequiresBothCurrentAndHistoryToMatch()
			throws SemanticException {
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> a = new HistoryState<>(pos).upchainAux(new HistoryState<>(neg));
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> b = new HistoryState<>(pos).upchainAux(new HistoryState<>(neg));
		HistoryState<SimpleAbstractState<Monolith,
				ValueEnvironment<SignLattice>,
				TypeEnvironment<TypeSet>>> c = new HistoryState<>(neg);
		assertEquals(a, b);
		assertFalse(a.equals(c));
	}

}
