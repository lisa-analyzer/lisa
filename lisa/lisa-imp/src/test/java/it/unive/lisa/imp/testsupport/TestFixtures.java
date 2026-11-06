package it.unive.lisa.imp.testsupport;

import it.unive.lisa.imp.IMPFeatures;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;

// minimal, self-contained fixtures for building a syntactically valid
// CFG/Program: IMP has real, concrete IMPTypeSystem/IMPFeatures classes, so
// (unlike lisa-program, which has no frontend of its own) there is no need
// for hand-rolled test fakes of these
public final class TestFixtures {

	private TestFixtures() {
	}

	public static final SourceCodeLocation LOCATION = new SourceCodeLocation("test", 1, 1);

	public static final Program PROGRAM = new Program(new IMPFeatures(), new IMPTypeSystem());

	public static final ClassUnit UNIT = new ClassUnit(LOCATION, PROGRAM, "TestUnit", false);

	public static final CFG CFG;

	static {
		PROGRAM.addUnit(UNIT);
		CFG = new CFG(new CodeMemberDescriptor(LOCATION, UNIT, false, "test"));
	}

}
