package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WitnessWriter {

	private static final ObjectWriter WRITER = new ObjectMapper(YAMLFactory.builder()
			.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
			.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
			.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
			.enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
			.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
			.build())
					.writer()
					.without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

	private WitnessWriter() {
	}

	public static String write(
			Witness witness)
			throws JsonProcessingException {
		return WRITER.writeValueAsString(witness.getEntries());
	}

	public static void write(
			Witness witness,
			OutputStream out)
			throws IOException {
		WRITER.writeValue(out, witness.getEntries());
	}

	public static void write(
			Witness witness,
			Path file)
			throws IOException {
		try (OutputStream out = Files.newOutputStream(file)) {
			write(witness, out);
		}
	}
}
