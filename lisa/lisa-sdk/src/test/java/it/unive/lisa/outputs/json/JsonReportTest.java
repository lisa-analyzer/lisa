package it.unive.lisa.outputs.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.LiSAReport;
import it.unive.lisa.LiSARunInfo;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.outputs.messages.CFGMessage;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
	public void warningsAndNoticesAreClonedAsStructuredJsonMessages() {
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
		assertTrue(json.getWarnings().stream()
				.anyMatch(m -> m.getMessage().equals("w1") && m.getTargetKind().equals("GENERIC")));
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
				original.getWarnings().stream().map(JsonReport.JsonMessage::getMessage).sorted()
						.collect(Collectors.toList()),
				reRead.getWarnings().stream().map(JsonReport.JsonMessage::getMessage).sorted()
						.collect(Collectors.toList()));
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
	public void jsonMessageEqualsAndHashCodeAndCompareToAreBasedOnTheMessageFields() {
		JsonReport.JsonMessage a = new JsonReport.JsonMessage(new Message("foo"));
		JsonReport.JsonMessage b = new JsonReport.JsonMessage();
		b.setMessage("foo");
		b.setTargetKind("GENERIC");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(0, a.compareTo(b));

		JsonReport.JsonMessage c = new JsonReport.JsonMessage(new Message("bar"));
		assertTrue(a.compareTo(c) != 0);

		JsonReport.JsonMessage d = new JsonReport.JsonMessage(new Message("foo", true, null));
		assertTrue(a.compareTo(d) != 0);
		assertTrue(!a.equals(d));
	}

	@Test
	public void structuredMessagesSurviveTheRoundTrip() throws IOException {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		SourceCodeLocation loc = new SourceCodeLocation("file", 1, 2);
		CFG cfg = new CFG(new CodeMemberDescriptor(loc, program, false, "foo"));
		ReproducingInput input = new ReproducingInput(
				new SourceCodeLocation("file", 3, 4),
				"parameter x of foo",
				null,
				0,
				"42");
		Message definite = new CFGMessage(cfg, "w1", true, List.of(input));
		Message possible = new Message("w2");
		Statement st = new NoOp(cfg, new SourceCodeLocation("file", 5, 6));
		cfg.addNode(st, true);
		Message onStatement = new StatementMessage(st, "w3", new UniqueScope<>(), false, null);
		Message refuted = new StatementMessage(st, "w4", null, Violation.reach())
				.verified(VerificationResult.REFUTED);

		LiSAConfiguration conf = new LiSAConfiguration();
		LiSARunInfo info = new LiSARunInfo(
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyList(),
				new it.unive.lisa.program.Application(),
				new DateTime(),
				new DateTime());
		LiSAReport report = new LiSAReport(
				conf, info, List.of(definite, possible, onStatement, refuted), List.of(definite),
				Collections.emptyList());
		JsonReport original = new JsonReport(report);

		JsonReport.JsonMessage structured = original.getWarnings().stream()
				.filter(m -> m.getMessage().equals("w1")).findFirst().get();
		assertEquals(loc.toString(), structured.getLocation());
		assertEquals(cfg.getDescriptor().getFullSignatureWithParNames(), structured.getCfg());
		assertEquals(null, structured.getText());
		assertEquals("CFG", structured.getTargetKind());
		assertTrue(structured.isDefinite());
		assertEquals(1, structured.getReproducingInputs().size());
		assertEquals("42", structured.getReproducingInputs().get(0).getValue());
		assertEquals("parameter x of foo", structured.getReproducingInputs().get(0).getSource());

		JsonReport.JsonMessage statement = original.getWarnings().stream()
				.filter(m -> m.getMessage().equals("w3")).findFirst().get();
		assertEquals(cfg.getDescriptor().getFullSignatureWithParNames(), statement.getCfg());
		assertEquals(st.toString(), statement.getText());
		assertEquals("STATEMENT", statement.getTargetKind());
		assertEquals(new UniqueScope<>().toString(), statement.getContext());
		assertEquals(new UniqueScope<>().getHash(), statement.getContextHash());
		assertEquals(null, structured.getContextHash());
		assertEquals(null, structured.getContext());
		assertTrue(!statement.isDefinite());
		assertTrue(!statement.isRefuted());

		JsonReport.JsonMessage refutedJson = original.getWarnings().stream()
				.filter(m -> m.getMessage().equals("w4")).findFirst().get();
		assertTrue(refutedJson.isRefuted());
		assertTrue(!refutedJson.isDefinite());

		StringWriter writer = new StringWriter();
		original.dump(writer);
		JsonReport reRead = JsonReport.read(new StringReader(writer.toString()));
		assertEquals(original.getWarnings(), reRead.getWarnings());
		assertEquals(original.getNotices(), reRead.getNotices());
		assertEquals(original, reRead);
	}

	@Test
	public void emptyFieldsOfMessagesAreOmittedFromTheDump() throws IOException {
		LiSAConfiguration conf = new LiSAConfiguration();
		LiSARunInfo info = new LiSARunInfo(
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyList(),
				new it.unive.lisa.program.Application(),
				new DateTime(),
				new DateTime());
		LiSAReport report = new LiSAReport(
				conf, info, List.of(new Message("w1")), Collections.emptyList(), Collections.emptyList());
		StringWriter writer = new StringWriter();
		new JsonReport(report).dump(writer);
		String dumped = writer.toString();
		assertTrue(dumped.contains("\"targetKind\" : \"GENERIC\""));
		assertTrue(dumped.contains("\"definite\" : false"));
		assertTrue(!dumped.contains("\"location\""));
		assertTrue(!dumped.contains("\"cfg\""));
		assertTrue(!dumped.contains("\"text\""));
		assertTrue(!dumped.contains("\"context\""));
		assertTrue(!dumped.contains("\"contextHash\""));
		assertTrue(!dumped.contains("\"reproducingInputs\""));
		// the refuted flag is dumped only when set
		assertTrue(!dumped.contains("\"refuted\""));
	}

}
