package it.unive.lisa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class LiSARunInfoTest {

	// builds a program with one unit, one global, and one cfg containing two
	// top-level statements (a NoOp, and a Return wrapping a nested
	// VariableRef expression), so that statements/expressions counts can be
	// told apart meaningfully
	private static Application mkApplication() {
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("fake", 1, 0), p, "fake", false);
		p.addUnit(unit);

		Global g = new Global(new SourceCodeLocation("fake", 2, 0), unit, "g", false);
		unit.addGlobal(g);

		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(
				new SourceCodeLocation("fake", 3, 0),
				unit,
				false,
				"foo");
		CFG cfg = new CFG(descriptor);
		cfg.addNode(new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)), true);
		cfg.addNode(
				new Return(
						cfg,
						new SourceCodeLocation("fake", 5, 0),
						new VariableRef(cfg, new SourceCodeLocation("fake", 6, 0), "x")));
		unit.addCodeMember(cfg);

		return new Application(p);
	}

	@Test
	public void countsReflectApplicationContents() {
		Application app = mkApplication();
		DateTime start = new DateTime(0);
		DateTime end = new DateTime(0);

		LiSARunInfo info = new LiSARunInfo(
				Arrays.asList(new Message("w1"), new Message("w2")),
				Arrays.asList(new Message("n1")),
				Arrays.asList("a.json", "b.json"),
				app,
				start,
				end);

		assertEquals(1, info.programs);
		assertEquals(1, info.units);
		assertEquals(1, info.globals);
		assertEquals(1, info.members);
		assertEquals(1, info.cfgs);
		assertEquals(2, info.warnings);
		assertEquals(1, info.notices);
		assertEquals(2, info.files);
		// NoOp and Return are both top-level statements; the VariableRef
		// nested inside Return is the only inner expression
		assertEquals(2, info.statements);
		assertEquals(1, info.expressions);
		assertEquals(VersionInfo.VERSION, info.version);
	}

	@Test
	public void emptyApplicationYieldsZeroCounts() {
		DateTime now = new DateTime();
		LiSARunInfo info = new LiSARunInfo(
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyList(),
				new Application(),
				now,
				now);

		assertEquals(0, info.programs);
		assertEquals(0, info.units);
		assertEquals(0, info.globals);
		assertEquals(0, info.members);
		assertEquals(0, info.cfgs);
		assertEquals(0, info.statements);
		assertEquals(0, info.expressions);
	}

	@Test
	public void equalsAndHashCodeFollowAllPublicFields() {
		Application app = mkApplication();
		DateTime instant = new DateTime(0);

		LiSARunInfo first = new LiSARunInfo(Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), app, instant, instant);
		LiSARunInfo second = new LiSARunInfo(Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), app, instant, instant);

		assertEquals(first, first);
		assertEquals(first, second);
		assertEquals(second, first);
		assertEquals(first.hashCode(), second.hashCode());

		LiSARunInfo differentWarnings = new LiSARunInfo(Arrays.asList(new Message("w")), Collections.emptyList(),
				Collections.emptyList(), app, instant, instant);
		assertFalse(first.equals(differentWarnings));
	}

	@Test
	public void sameCodeAndResultsIgnoresTimingAndVersion() {
		Application app = mkApplication();

		LiSARunInfo early = new LiSARunInfo(Arrays.asList(new Message("w")), Collections.emptyList(),
				Collections.emptyList(), app, new DateTime(0), new DateTime(1000));
		LiSARunInfo late = new LiSARunInfo(Arrays.asList(new Message("w")), Collections.emptyList(),
				Collections.emptyList(), app, new DateTime(50_000), new DateTime(999_999));

		// same code metrics and results, but different start/end/duration
		assertFalse(early.start.equals(late.start) && early.end.equals(late.end));
		assertTrue(early.sameCodeAndResults(late));
		assertTrue(late.sameCodeAndResults(early));

		LiSARunInfo differentWarnings = new LiSARunInfo(Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), app, new DateTime(0), new DateTime(1000));
		assertFalse(early.sameCodeAndResults(differentWarnings));
		assertFalse(early.sameCodeAndResults(null));
	}

	@Test
	public void toPropertyBagContainsAllPublicInstanceFields() {
		Application app = mkApplication();
		DateTime now = new DateTime();
		LiSARunInfo info = new LiSARunInfo(Arrays.asList(new Message("w")), Collections.emptyList(),
				Collections.emptyList(), app, now, now);

		Map<String, String> bag = info.toPropertyBag();

		assertEquals(String.valueOf(info.programs), bag.get("programs"));
		assertEquals(String.valueOf(info.units), bag.get("units"));
		assertEquals(String.valueOf(info.globals), bag.get("globals"));
		assertEquals(String.valueOf(info.members), bag.get("members"));
		assertEquals(String.valueOf(info.cfgs), bag.get("cfgs"));
		assertEquals(String.valueOf(info.statements), bag.get("statements"));
		assertEquals(String.valueOf(info.expressions), bag.get("expressions"));
		assertEquals(String.valueOf(info.warnings), bag.get("warnings"));
		assertEquals(String.valueOf(info.notices), bag.get("notices"));
		assertEquals(String.valueOf(info.files), bag.get("files"));
		assertEquals(info.version, bag.get("version"));
		assertEquals(info.start, bag.get("start"));
		assertEquals(info.end, bag.get("end"));
		assertEquals(info.duration, bag.get("duration"));
	}

	@Test
	public void durationIsFormattedThroughPeriodFormat() {
		// 1 minute and 500 milliseconds apart
		LiSARunInfo info = new LiSARunInfo(Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), new Application(), new DateTime(0), new DateTime(60_500));

		assertNotNull(info.duration);
		assertTrue(info.duration.contains("1m"));
		assertTrue(info.duration.contains("500ms"));
	}

}
