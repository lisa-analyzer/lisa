package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.context.KDepthToken;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import java.util.List;

/**
 * Utilities for building {@link SemanticOracle}s that expose an
 * interprocedural context, to test context-sensitive heap domains.
 */
final class ContextOracles {

	private ContextOracles() {
	}

	/**
	 * Builds a non-starting context made of a single call at the given line.
	 * 
	 * @param line the line of the call
	 * 
	 * @return the context
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static ScopeId<?> context(
			int line) {
		CFGCall call = new CFGCall(
				TestParameterProvider.cfg,
				new SourceCodeLocation("ctx", line, 0),
				CallType.STATIC,
				"C",
				"foo",
				List.of(TestParameterProvider.cfg));
		// the state is not used by k-depth tokens
		KDepthToken token = KDepthToken.create(1);
		return token.push(call, null);
	}

	/**
	 * Builds an oracle exposing the given context.
	 * 
	 * @param context the context
	 * 
	 * @return the oracle
	 */
	static SemanticOracle oracle(
			ScopeId<?> context) {
		return new TestParameterProvider.FakeOracle() {

			@Override
			public ScopeId<?> getContext() {
				return context;
			}
		};
	}
}
