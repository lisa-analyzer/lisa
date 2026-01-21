package it.unive.lisa.outputs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.LiSA;
import it.unive.lisa.LiSAReport;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.util.file.FileManager;
import it.unive.lisa.util.representation.StringRepresentation;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AdditionalInfoTest {

	private static final ClassUnit unit = new ClassUnit(
			SyntheticLocation.INSTANCE,
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"Testing",
			false);

	@Test
	public void testNoInfo()
			throws FileNotFoundException,
			IOException {
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "simpleIf"));
		Ret ret = new Ret(cfg, SyntheticLocation.INSTANCE);
		cfg.addNode(ret, true);

		Program program = cfg.getProgram();

		LiSAConfiguration conf = new LiSAConfiguration();
		conf.workdir = "tmp";
		conf.outputs.add(new JSONReportDumper());
		LiSA lisa = new LiSA(conf);
		LiSAReport report = lisa.run(program);

		assertEquals(0, report.getAdditionalInfo().size());
		try (FileReader reader = new FileReader("tmp/" + JSONReportDumper.REPORT_NAME)) {
			JsonReport jsonReport = JsonReport.read(reader);
			assertEquals(0, jsonReport.getAdditionalInfo().getFields().size());
		}
	}

	@Test
	public void testInfo()
			throws FileNotFoundException,
			IOException {
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "simpleIf"));
		Ret ret = new Ret(cfg, SyntheticLocation.INSTANCE);
		cfg.addNode(ret, true);

		Program program = cfg.getProgram();

		LiSAConfiguration conf = new LiSAConfiguration();
		conf.workdir = "tmp";
		conf.outputs.add(new JSONReportDumper());
		LiSA lisa = new LiSA(conf);
		Consumer<LiSAReport> filler = r -> r.getAdditionalInfo().put("key", new StringRepresentation("value"));
		LiSAReport report = lisa.run(filler, program);

		assertEquals(1, report.getAdditionalInfo().size());
		try (FileReader reader = new FileReader("tmp/" + JSONReportDumper.REPORT_NAME)) {
			JsonReport jsonReport = JsonReport.read(reader);
			assertEquals(1, jsonReport.getAdditionalInfo().getFields().size());
		}
	}

	@BeforeEach
	@AfterEach
	public void cleanUp()
			throws IOException {
		FileManager.forceDeleteFolder("tmp");
	}

}
