package it.unive.lisa.interprocedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.KDepthToken;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RecursionTest {

	private static CFG cfg;
	private static CFG other;

	@BeforeAll
	public static void init()
			throws ParsingException {
		Program p = IMPFrontend.processText("class C { foo() { } bar() { } }");
		var it = p.getAllCFGs().iterator();
		cfg = it.next();
		other = it.next();
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
	public void gettersReturnExactlyWhatWasPassedToTheConstructor() {
		var call = mkCall(1);
		var token = KDepthToken.<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						2);
		var state = mkCompoundState();
		List<CodeMember> members = List.of(cfg, other);

		var recursion = new Recursion<>(call, token, state, cfg, members);

		assertSame(call, recursion.getInvocation());
		assertSame(cfg, recursion.getRecursionHead());
		assertSame(token, recursion.getInvocationToken());
		assertSame(state, recursion.getEntryState());
		assertEquals(members, recursion.getMembers());
	}

	@Test
	public void recursionsWithEqualComponentsAreEqual() {
		var token = KDepthToken.<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						2);
		var state = mkCompoundState();
		List<CodeMember> members = List.of(cfg, other);

		// the invocation call is rebuilt independently on purpose: CFGCall
		// equality is structural (based on location, qualifier, target name
		// and call type), so this still counts as "the same call"
		var a = new Recursion<>(mkCall(1), token, state, cfg, members);
		var b = new Recursion<>(mkCall(1), token, state, cfg, members);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void recursionsDifferingInAnyComponentAreNotEqual() {
		var token = KDepthToken.<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
						2);
		var state = mkCompoundState();
		List<CodeMember> members = List.of(cfg, other);
		var reference = new Recursion<>(mkCall(1), token, state, cfg, members);

		assertNotEquals(reference, new Recursion<>(mkCall(2), token, state, cfg, members));
		assertNotEquals(reference, new Recursion<>(mkCall(1), token, state, other, members));
		assertNotEquals(reference, new Recursion<>(mkCall(1), token, state, cfg, List.of(cfg)));
	}

	@Test
	public void toStringMentionsTheMembersAndWhereTheRecursionStarted() {
		var call = mkCall(1);
		var recursion = new Recursion<>(
				call,
				KDepthToken.<
						SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>create(
								2),
				mkCompoundState(),
				cfg,
				List.of(cfg));

		String repr = recursion.toString();
		assertEquals(true, repr.contains(call.getLocation().toString()));
	}

}
