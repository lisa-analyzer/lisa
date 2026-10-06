package it.unive.lisa.program.cfg.statement.call;

import static it.unive.lisa.program.cfg.statement.call.CallFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.call.CallFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestInterproceduralAnalysis;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class UnresolvedCallTest {

	private static AnalysisState<TestAbstractState> state() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	private static class ResolvingInterprocedural
			extends
			TestInterproceduralAnalysis<TestAbstractState, AbstractDomain<TestAbstractState>> {

		private final Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> analysis = new Analysis<>(
				new TestAbstractDomain());
		private Call resolved;
		private CallResolutionException toThrow;
		private UnresolvedCall lastResolveRequest;

		@Override
		public Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> getAnalysis() {
			return analysis;
		}

		@Override
		public Call resolve(
				UnresolvedCall call,
				Set<Type>[] types,
				SymbolAliasing aliasing)
				throws CallResolutionException {
			lastResolveRequest = call;
			if (toThrow != null)
				throw toThrow;
			return resolved;
		}

		@Override
		public AnalysisState<TestAbstractState> getAbstractResultOf(
				OpenCall call,
				AnalysisState<TestAbstractState> entryState,
				ExpressionSet[] parameters,
				StatementStore<TestAbstractState> expressions) {
			// the resolved OpenCall's own forwardSemanticsAux runs as part of
			// resolving this UnresolvedCall, and needs a non-null,
			// non-void-looking compute() result so that its own meta variable
			// actually gets registered - that registration is what this test
			// checks propagates up to the UnresolvedCall
			return entryState.withExecutionExpression(new Variable(Untyped.INSTANCE, "ret_value", LOC));
		}
	}

	@Test
	public void defaultStaticTypeIsUntyped() {
		UnresolvedCall call = new UnresolvedCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo");
		assertSame(Untyped.INSTANCE, call.getStaticType());
	}

	@Test
	public void compareCallAuxAlwaysReturnsZero() {
		UnresolvedCall a = new UnresolvedCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo");
		UnresolvedCall b = new UnresolvedCall(newCfg("caller2"), LOC, CallType.STATIC, null, "foo");
		assertTrue(a.compareCallAux(b) == 0);
	}

	@Test
	public void forwardSemanticsAuxDelegatesToTheResolvedCallAndMergesItsMetaVariables() throws SemanticException {
		CFG caller = newCfg("caller");
		OpenCall resolvedTarget = new OpenCall(caller, LOC, CallType.STATIC, null, "foo");
		UnresolvedCall call = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");

		ResolvingInterprocedural interprocedural = new ResolvingInterprocedural();
		interprocedural.resolved = resolvedTarget;

		AnalysisState<TestAbstractState> entry = state();
		AnalysisState<TestAbstractState> result = call.forwardSemanticsAux(
				interprocedural, entry, new ExpressionSet[0], new StatementStore<>(entry));

		assertSame(call, interprocedural.lastResolveRequest);
		assertTrue(result.getExecutionExpressions().contains(new Variable(Untyped.INSTANCE, "ret_value", LOC)));
		assertTrue(resolvedTarget.getMetaVariables().contains(resolvedTarget.getMetaVariable()));
		assertTrue(call.getMetaVariables().contains(resolvedTarget.getMetaVariable()));
	}

	@Test
	public void forwardSemanticsAuxWrapsAResolutionFailureIntoASemanticException() {
		CFG caller = newCfg("caller");
		UnresolvedCall call = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		ResolvingInterprocedural interprocedural = new ResolvingInterprocedural();
		interprocedural.toThrow = new CallResolutionException("no match");

		AnalysisState<TestAbstractState> entry = state();
		assertThrows(SemanticException.class,
				() -> call.forwardSemanticsAux(interprocedural, entry, new ExpressionSet[0],
						new StatementStore<>(entry)));
	}

}
