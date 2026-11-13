package it.unive.lisa.outputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.LiSAReport;
import it.unive.lisa.LiSARunInfo;
import it.unive.lisa.ReportingTool;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.program.Application;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JSONReportDumperTest {

	@TempDir
	Path tempDir;

	@Test
	public void dumpWritesAWellFormedJsonFileNamedReportJson() throws IOException {
		LiSAConfiguration conf = new LiSAConfiguration();
		LiSARunInfo info = new LiSARunInfo(
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyList(),
				new Application(),
				new DateTime(),
				new DateTime());
		LiSAReport report = new LiSAReport(
				conf, info, List.of(), List.of(), List.of());

		FileManager fileManager = new FileManager(tempDir.toString());
		ReportingTool tool = new ReportingTool(conf, fileManager);

		new JSONReportDumper().dump(new Application(), report, tool, fileManager);

		Path reportFile = tempDir.resolve(JSONReportDumper.REPORT_NAME);
		assertTrue(Files.exists(reportFile), "report.json was not created");
		String content = Files.readString(reportFile);
		assertTrue(content.trim().startsWith("{"));
		assertTrue(fileManager.createdFiles().contains(JSONReportDumper.REPORT_NAME));
	}

	@Test
	public void reportNameIsReportJson() {
		assertEquals("report.json", JSONReportDumper.REPORT_NAME);
	}

}
