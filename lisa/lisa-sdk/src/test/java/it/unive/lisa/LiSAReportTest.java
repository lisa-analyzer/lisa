package it.unive.lisa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.program.Application;
import it.unive.lisa.util.representation.StringRepresentation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class LiSAReportTest {

	private static LiSARunInfo mkInfo() {
		// a fixed instant so that two separately-built infos are equal
		DateTime instant = new DateTime(0);
		return new LiSARunInfo(
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyList(),
				new Application(),
				instant,
				instant);
	}

	private static LiSAReport mkReport(
			LiSAConfiguration conf,
			List<Message> warnings,
			List<Message> notices,
			List<String> files) {
		return new LiSAReport(conf, mkInfo(), warnings, notices, files);
	}

	@Test
	public void equalsIsReflexive() {
		LiSAReport report = mkReport(
				new LiSAConfiguration(),
				Arrays.asList(new Message("w")),
				Arrays.asList(new Message("n")),
				Arrays.asList("a.json"));
		assertEquals(report, report);
	}

	@Test
	public void equalsIsSymmetricForEquivalentReports() {
		LiSAConfiguration conf = new LiSAConfiguration();
		List<Message> warnings = Arrays.asList(new Message("w"));
		List<Message> notices = Arrays.asList(new Message("n"));
		List<String> files = Arrays.asList("a.json");

		LiSAReport first = mkReport(conf, warnings, notices, files);
		LiSAReport second = mkReport(conf, warnings, notices, files);

		assertEquals(first, second);
		assertEquals(second, first);
		assertEquals(first.hashCode(), second.hashCode());
	}

	@Test
	public void differingWarningsBreakEquality() {
		LiSAConfiguration conf = new LiSAConfiguration();
		List<Message> notices = Arrays.asList(new Message("n"));
		List<String> files = Arrays.asList("a.json");

		LiSAReport first = mkReport(conf, Arrays.asList(new Message("w1")), notices, files);
		LiSAReport second = mkReport(conf, Arrays.asList(new Message("w2")), notices, files);

		assertFalse(first.equals(second));
	}

	@Test
	public void gettersReturnConstructorArguments() {
		LiSAConfiguration conf = new LiSAConfiguration();
		List<Message> warnings = Arrays.asList(new Message("w"));
		List<Message> notices = Arrays.asList(new Message("n"));
		List<String> files = Arrays.asList("a.json");
		LiSARunInfo info = mkInfo();

		LiSAReport report = new LiSAReport(conf, info, warnings, notices, files);

		assertEquals(conf, report.getConfiguration());
		assertEquals(info, report.getRunInfo());
		assertEquals(warnings, report.getWarnings());
		assertEquals(notices, report.getNotices());
		assertEquals(files, report.getCreatedFiles());
	}

	@Test
	public void additionalInfoStartsEmptyAndIsMutable() {
		LiSAReport report = mkReport(new LiSAConfiguration(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList());

		assertNotNull(report.getAdditionalInfo());
		assertTrue(report.getAdditionalInfo().isEmpty());

		report.getAdditionalInfo().put("key", new StringRepresentation("value"));
		assertEquals(1, report.getAdditionalInfo().size());
		assertEquals(new StringRepresentation("value"), report.getAdditionalInfo().get("key"));
	}

	@Test
	public void toStringContainsWarningsAndNotices() {
		LiSAReport report = mkReport(
				new LiSAConfiguration(),
				Arrays.asList(new Message("a warning")),
				Arrays.asList(new Message("a notice")),
				Arrays.asList("dump.json"));

		String repr = report.toString();
		assertTrue(repr.contains("a warning"));
		assertTrue(repr.contains("a notice"));
		assertTrue(repr.contains("dump.json"));
	}

}
