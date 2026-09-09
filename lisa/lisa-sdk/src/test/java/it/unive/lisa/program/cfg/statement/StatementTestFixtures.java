package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestInterproceduralAnalysis;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;

/**
 * Shared fixtures for building {@link CFG}s, entry states and a working
 * {@link TestInterproceduralAnalysis} usable to actually exercise
 * {@code forwardSemantics}/{@code backwardSemantics} against
 * {@link TestAbstractDomain} (whose {@code assign}/{@code smallStepSemantics}
 * are identity operations that only affect the analysis state's computed
 * expressions, via {@link Analysis}).
 */
final class StatementTestFixtures {

	static final SourceCodeLocation LOC = new SourceCodeLocation("test", 1, 1);

	private StatementTestFixtures() {
	}

	static CFG newCfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "C", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, true, "m"));
	}

	static TestInterproceduralAnalysis<TestAbstractState, TestAbstractDomain> interprocedural() {
		return new TestInterproceduralAnalysis<TestAbstractState, TestAbstractDomain>() {
			private final Analysis<TestAbstractState, TestAbstractDomain> analysis = new Analysis<>(
					new TestAbstractDomain());

			@Override
			public Analysis<TestAbstractState, TestAbstractDomain> getAnalysis() {
				return analysis;
			}
		};
	}

	static AnalysisState<TestAbstractState> emptyState() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	static StatementStore<TestAbstractState> store(
			AnalysisState<TestAbstractState> state) {
		return new StatementStore<>(state);
	}

}
