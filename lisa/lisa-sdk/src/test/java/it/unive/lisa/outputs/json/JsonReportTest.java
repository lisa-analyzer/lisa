package it.unive.lisa.outputs.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.LiSAReport;
import it.unive.lisa.LiSARunInfo;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.outputs.messages.Message;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class JsonReportTest {

	@Test
	public void emptyReportHasNoWarningsNoticesOrFiles() {
		JsonReport report = new JsonReport();
		assertTrue(report.getWarnings().isEmpty());
		assertTrue(report.getNotices().isEmpty());
		assertTrue(report.getFiles().isEmpty());
		assertTrue(report.getConfiguration().isEmpty());
		assertTrue(report.getInfo().isEmpty());
	}

	@Test
	public void warningsAndNoticesAreClonedAsJsonMessagesWithTheirTaggedText() {
		LiSAConfiguration conf = new LiSAConfiguration();
		LiSARunInfo info = new LiSARunInfo(
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyList(),
				new it.unive.lisa.program.Application(),
				new DateTime(),
				new DateTime());
		List<Message> warnings = Arrays.asList(new Message("w1"), new Message("w2"));
		List<Message> notices = List.of(new Message("n1"));
		LiSAReport report = new LiSAReport(conf, info, warnings, notices, List.of("out.txt"));

		JsonReport json = new JsonReport(report);
		assertEquals(2, json.getWarnings().size());
		assertEquals(1, json.getNotices().size());
		assertTrue(json.getWarnings().stream().anyMatch(m -> m.getMessage().equals(warnings.get(0).toString())));
		assertTrue(json.getFiles().contains("out.txt"));
	}

	@Test
	public void dumpAndReadRoundTripPreservesAllTheFields() throws IOException {
		LiSAConfiguration conf = new LiSAConfiguration();
		LiSARunInfo info = new LiSARunInfo(
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyList(),
				new it.unive.lisa.program.Application(),
				new DateTime(),
				new DateTime());
		LiSAReport report = new LiSAReport(
				conf, info, List.of(new Message("w1")), List.of(new Message("n1")), List.of("out.txt"));
		JsonReport original = new JsonReport(report);

		StringWriter writer = new StringWriter();
		original.dump(writer);
		String dumped = writer.toString();

		// the dump must be well-formed, indented json
		assertTrue(dumped.trim().startsWith("{"));
		assertTrue(dumped.contains("\n  "), "expected indented output");

		JsonReport reRead = JsonReport.read(new StringReader(dumped));
		assertEquals(original.getFiles(), reRead.getFiles());
		assertEquals(
				original.getWarnings().stream().map(JsonReport.JsonMessage::getMessage).sorted().toList(),
				reRead.getWarnings().stream().map(JsonReport.JsonMessage::getMessage).sorted().toList());
		assertEquals(original.getConfiguration(), reRead.getConfiguration());
	}

	@Test
	public void additionalInfoIsOmittedFromTheDumpWhenEmpty() throws IOException {
		LiSAConfiguration conf = new LiSAConfiguration();
		LiSARunInfo info = new LiSARunInfo(
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyList(),
				new it.unive.lisa.program.Application(),
				new DateTime(),
				new DateTime());
		LiSAReport report = new LiSAReport(
				conf, info, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		// LiSAReport's own additionalInfo map starts empty
		JsonReport json = new JsonReport(report);

		StringWriter writer = new StringWriter();
		json.dump(writer);
		assertTrue(
				!writer.toString().contains("\"additionalInfo\""),
				"an empty additionalInfo object should be filtered out of the dump");
	}

	@Test
	public void jsonMessageDefaultConstructorStartsWithANullMessage() {
		JsonReport.JsonMessage m = new JsonReport.JsonMessage();
		assertEquals(null, m.getMessage());
		m.setMessage("hi");
		assertEquals("hi", m.getMessage());
		assertEquals("hi", m.toString());
	}

	@Test
	public void jsonMessageEqualsAndHashCodeAndCompareToAreBasedOnTheMessageText() {
		JsonReport.JsonMessage a = new JsonReport.JsonMessage(new Message("foo"));
		JsonReport.JsonMessage b = new JsonReport.JsonMessage();
		b.setMessage(new Message("foo").toString());
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(0, a.compareTo(b));

		JsonReport.JsonMessage c = new JsonReport.JsonMessage(new Message("bar"));
		assertTrue(a.compareTo(c) != 0);
	}

}
