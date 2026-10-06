package it.unive.lisa.interprocedural.inlining;

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

public class CallStackIdTest {

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

	private CallStackId<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> emptyId() {
		return CallStackId.create();
	}

	@Test
	public void freshIdIsStartingIdAndEmpty() {
		var id = emptyId();
		assertTrue(id.isStartingId());
		assertEquals(0, id.size());
		assertEquals("<empty>", id.toString());
	}

	@Test
	public void pushGrowsTheStackByOneAndKeepsFullHistory() {
		var id = emptyId();
		id = id.push(mkCall(1), mkCompoundState());
		id = id.push(mkCall(2), mkCompoundState());
		id = id.push(mkCall(3), mkCompoundState());

		assertEquals(3, id.size());
		assertEquals(mkCall(1), id.getCalls().get(0).getLeft());
		assertEquals(mkCall(2), id.getCalls().get(1).getLeft());
		assertEquals(mkCall(3), id.getCalls().get(2).getLeft());
	}

	@Test
	public void getCallIndexesFromTheStartOfTheStack() {
		var id = emptyId().push(mkCall(1), mkCompoundState()).push(mkCall(2), mkCompoundState());
		assertEquals(mkCall(1), id.getCall(0).getLeft());
		assertEquals(mkCall(2), id.getCall(1).getLeft());
	}

	@Test
	public void getCallFromEndIndexesFromTheTopOfTheStack() {
		var id = emptyId().push(mkCall(1), mkCompoundState()).push(mkCall(2), mkCompoundState());
		assertEquals(mkCall(2), id.getCallFromEnd(0).getLeft());
		assertEquals(mkCall(1), id.getCallFromEnd(1).getLeft());
	}

	@Test
	public void getReversedCallsIsTheOppositeOrderOfGetCalls() {
		var id = emptyId().push(mkCall(1), mkCompoundState()).push(mkCall(2), mkCompoundState())
				.push(mkCall(3), mkCompoundState());
		var forward = id.getCalls();
		var backward = id.getReversedCalls();

		assertEquals(forward.size(), backward.size());
		for (int i = 0; i < forward.size(); i++)
			assertEquals(forward.get(i), backward.get(backward.size() - 1 - i));
	}

	@Test
	public void popRemovesEntriesFromTheTopOfTheStack() {
		var id = emptyId().push(mkCall(1), mkCompoundState()).push(mkCall(2), mkCompoundState())
				.push(mkCall(3), mkCompoundState());

		var popped = id.pop(1);

		assertEquals(2, popped.size());
		assertEquals(mkCall(1), popped.getCall(0).getLeft());
		assertEquals(mkCall(2), popped.getCall(1).getLeft());
	}

	@Test
	public void poppingEverythingYieldsAnEmptyStack() {
		var id = emptyId().push(mkCall(1), mkCompoundState()).push(mkCall(2), mkCompoundState());
		assertEquals(0, id.pop(2).size());
	}

	@Test
	public void twoIdenticalCallStacksAreEqual() {
		var a = emptyId().push(mkCall(1), mkCompoundState()).push(mkCall(2), mkCompoundState());
		var b = emptyId().push(mkCall(1), mkCompoundState()).push(mkCall(2), mkCompoundState());
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void differentCallStacksAreNotEqual() {
		var a = emptyId().push(mkCall(1), mkCompoundState());
		var b = emptyId().push(mkCall(2), mkCompoundState());
		assertNotEquals(a, b);

		var shorter = emptyId().push(mkCall(1), mkCompoundState());
		var longer = emptyId().push(mkCall(1), mkCompoundState()).push(mkCall(2), mkCompoundState());
		assertNotEquals(shorter, longer);
	}

	@Test
	public void startingIdOfANonEmptyStackIsEmpty() {
		var id = emptyId().push(mkCall(1), mkCompoundState());
		assertTrue(id.startingId().isStartingId());
	}

}
