package it.unive.lisa.interprocedural.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class KDepthTokenTest {

	private static CFG cfg;

	@BeforeAll
	public static void init()
			throws ParsingException {
		Program p = IMPFrontend.processText("class C { foo() { } }");
		cfg = p.getAllCFGs().iterator().next();
	}

	private CFGCall mkCall(
			int line) {
		return new CFGCall(
				cfg,
				new SourceCodeLocation("test", line, 0),
				CallType.INSTANCE,
				"C",
				"foo",
				List.of(cfg));
	}

	private AnalysisState<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkState() {
		return new AnalysisState<>(
				new ProgramState<>(DefaultConfiguration.defaultAbstractDomain().makeLattice(), new ExpressionSet()));
	}

	private CompoundState<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkCompoundState() {
		var state = mkState();
		return CompoundState.of(state, new StatementStore<>(state));
	}

	@Test
	public void freshTokenIsStartingIdAndHasEmptyRepresentation() {
		KDepthToken<?> token = KDepthToken.create(3);
		assertTrue(token.isStartingId());
		assertEquals("<empty>", token.toString());
	}

	@Test
	public void kZeroIsContextInsensitiveAndNeverTracksCalls() {
		var empty = KDepthToken.<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						0);
		var afterPushes = empty;
		for (int i = 0; i < 5; i++)
			afterPushes = afterPushes.push(mkCall(i), mkCompoundState());
		assertTrue(afterPushes.isStartingId());
		assertEquals(empty, afterPushes);
	}

	@Test
	public void positiveKKeepsOnlyTheLastKCalls() {
		var token = KDepthToken.<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						2);
		token = token.push(mkCall(1), mkCompoundState());
		token = token.push(mkCall(2), mkCompoundState());
		token = token.push(mkCall(3), mkCompoundState());

		// only calls at lines 2 and 3 should remain: the oldest (line 1) was
		// dropped once the depth bound of 2 was exceeded
		var expected = KDepthToken
				.<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						2)
				.push(mkCall(2), mkCompoundState()).push(mkCall(3), mkCompoundState());
		assertEquals(expected, token);
	}

	@Test
	public void negativeKKeepsTheWholeCallChain() {
		var token = KDepthToken.<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						-1);
		for (int i = 0; i < 10; i++)
			token = token.push(mkCall(i), mkCompoundState());

		var replay = KDepthToken.<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						-1);
		for (int i = 0; i < 10; i++)
			replay = replay.push(mkCall(i), mkCompoundState());

		// nothing should have been dropped: replaying the same 10 calls on a
		// fresh unlimited token yields an equal token
		assertEquals(token, replay);
		assertNotEquals(
				KDepthToken.<
						SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
								-1),
				token);
	}

	@Test
	public void equalityIgnoresTheDepthBoundAndOnlyLooksAtTheCallChain() {
		var narrow = KDepthToken
				.<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						1)
				.push(mkCall(7), mkCompoundState());
		var wide = KDepthToken
				.<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						100)
				.push(mkCall(7), mkCompoundState());

		assertEquals(narrow, wide);
		assertEquals(narrow.hashCode(), wide.hashCode());
	}

	@Test
	public void differentCallChainsAreNotEqual() {
		var a = KDepthToken
				.<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						5)
				.push(mkCall(1), mkCompoundState());
		var b = KDepthToken
				.<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						5)
				.push(mkCall(2), mkCompoundState());

		assertNotEquals(a, b);
	}

	@Test
	public void startingIdOfANonEmptyTokenIsEmpty() {
		var token = KDepthToken
				.<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						3)
				.push(mkCall(1), mkCompoundState());
		assertTrue(token.startingId().isStartingId());
	}

}
