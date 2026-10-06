package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.type.Type;

/** Shared helpers for building minimal {@link CFG}s in call/-package tests. */
final class CallFixtures {

	static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private CallFixtures() {
	}

	static CFG newCfg(
			String name) {
		return newCfg(name, it.unive.lisa.type.Untyped.INSTANCE);
	}

	static CFG newCfg(
			String name,
			Type returnType) {
		ClassUnit unit = new ClassUnit(
				LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "unit-" + name, false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, name, returnType));
	}

}
