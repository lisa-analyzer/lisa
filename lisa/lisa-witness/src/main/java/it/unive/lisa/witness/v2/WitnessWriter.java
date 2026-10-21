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

/**
 * A writer of YAML witnesses. A witness is written as the list of its entries,
 * the top-level structure of the witness format. The text has no document start
 * marker, it quotes strings only when YAML requires it, and it always quotes
 * strings that look like numbers, such as the format version {@code "2.4"}.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class WitnessWriter {

	/**
	 * The writer producing the YAML text. The type keys of the format, such as
	 * {@code waypoint} and {@code invariant}, are written as ordinary keys and
	 * not as YAML tags. Closing the stream it writes to is left to the caller.
	 */
	private static final ObjectWriter WRITER = new ObjectMapper(YAMLFactory.builder()
			.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
			.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
			.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
			.enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
			.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
			.build())
					.writer()
					.without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

	/**
	 * This class only has static methods, and it is never instantiated.
	 */
	private WitnessWriter() {
	}

	/**
	 * Writes the given witness as YAML text.
	 * 
	 * @param witness the witness to write
	 * 
	 * @return the YAML text of the witness
	 * 
	 * @throws JsonProcessingException if the witness cannot be converted to
	 *                                     YAML
	 */
	public static String write(
			Witness witness)
			throws JsonProcessingException {
		return WRITER.writeValueAsString(witness.getEntries());
	}

	/**
	 * Writes the given witness as YAML text, encoded in UTF-8, to the given
	 * stream. The stream is not closed.
	 * 
	 * @param witness the witness to write
	 * @param out     the stream to write to
	 * 
	 * @throws IOException if the stream cannot be written
	 */
	public static void write(
			Witness witness,
			OutputStream out)
			throws IOException {
		WRITER.writeValue(out, witness.getEntries());
	}

	/**
	 * Writes the given witness as YAML text, encoded in UTF-8, to the given
	 * file. The file is created if it does not exist, and replaced if it does.
	 * 
	 * @param witness the witness to write
	 * @param file    the file to write to
	 * 
	 * @throws IOException if the file cannot be written
	 */
	public static void write(
			Witness witness,
			Path file)
			throws IOException {
		try (OutputStream out = Files.newOutputStream(file)) {
			write(witness, out);
		}
	}
}
