package it.unive.lisa.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import org.junit.jupiter.api.Test;

public class ApplicationTest {

	private static Program programWith(
			String unitName,
			String cfgName,
			boolean entrypoint) {
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, unitName, false);
		p.addUnit(unit);
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(new SourceCodeLocation("f", 1, 0), unit, false,
				cfgName);
		CFG cfg = new CFG(descriptor);
		unit.addCodeMember(cfg);
		if (entrypoint)
			p.addEntryPoint(cfg);
		return p;
	}

	@Test
	public void allCFGsAndEntryPointsAreMergedAcrossPrograms() {
		Program p1 = programWith("u1", "foo", true);
		Program p2 = programWith("u2", "bar", false);
		Application app = new Application(p1, p2);

		assertEquals(2, app.getAllCFGs().size());
		assertEquals(1, app.getEntryPoints().size());
		assertEquals(2, app.getAllCodeCodeMembers().size());
	}

	@Test
	public void resultsAreCachedAfterTheFirstComputation() {
		Program p1 = programWith("u1", "foo", true);
		Application app = new Application(p1);
		assertSame(app.getAllCFGs(), app.getAllCFGs());
		assertSame(app.getEntryPoints(), app.getEntryPoints());
		assertSame(app.getAllCodeCodeMembers(), app.getAllCodeCodeMembers());
	}

	@Test
	public void getProgramsReturnsThemInConstructionOrder() {
		Program p1 = programWith("u1", "foo", false);
		Program p2 = programWith("u2", "bar", false);
		Application app = new Application(p1, p2);
		assertTrue(app.getPrograms()[0] == p1 && app.getPrograms()[1] == p2);
	}

}
