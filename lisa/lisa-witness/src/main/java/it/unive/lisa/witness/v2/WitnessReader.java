package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A reader of YAML witnesses, in version
 * {@value it.unive.lisa.witness.v2.metadata.Metadata#FORMAT_VERSION} of the
 * witness format. A witness is read into the objects of this package through
 * their constructors, which perform the same checks as when a witness is built
 * by hand. Keys that are not part of the format are rejected. When a witness is
 * not valid, the message of the exception reports the line where the problem
 * was found, if known.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class WitnessReader {

	/**
	 * The mapper reading the YAML text into the objects of this package. It
	 * keeps the default settings of Jackson, which reject unknown keys.
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

	/**
	 * This class only has static methods, and it is never instantiated.
	 */
	private WitnessReader() {
	}

	/**
	 * Reads a witness from the given YAML text.
	 * 
	 * @param text the YAML text of the witness
	 * 
	 * @return the witness, either a {@link ViolationWitness} or a
	 *             {@link CorrectnessWitness}
	 * 
	 * @throws WitnessReadException if the text is not a valid witness
	 */
	public static Witness read(
			String text)
			throws WitnessReadException {
		try {
			return MAPPER.readValue(text, Witness.class);
		} catch (JsonProcessingException e) {
			throw invalid(e);
		}
	}

	/**
	 * Reads a witness from the given stream. The stream is read until its end,
	 * and it is closed once the witness has been read.
	 * 
	 * @param in the stream to read the YAML text of the witness from
	 * 
	 * @return the witness, either a {@link ViolationWitness} or a
	 *             {@link CorrectnessWitness}
	 * 
	 * @throws IOException          if the stream cannot be read
	 * @throws WitnessReadException if the text is not a valid witness
	 */
	public static Witness read(
			InputStream in)
			throws IOException,
			WitnessReadException {
		try {
			return MAPPER.readValue(in, Witness.class);
		} catch (JsonProcessingException e) {
			throw invalid(e);
		}
	}

	/**
	 * Reads a witness from the given file.
	 * 
	 * @param file the file containing the YAML text of the witness
	 * 
	 * @return the witness, either a {@link ViolationWitness} or a
	 *             {@link CorrectnessWitness}
	 * 
	 * @throws IOException          if the file cannot be read
	 * @throws WitnessReadException if the text is not a valid witness
	 */
	public static Witness read(
			Path file)
			throws IOException,
			WitnessReadException {
		try (InputStream in = Files.newInputStream(file)) {
			return read(in);
		}
	}

	/**
	 * Builds the exception reporting that a witness is not valid. Jackson wraps
	 * the {@link InvalidWitnessException} thrown by the constructors of the
	 * witness objects, possibly more than once. Its message is used when it is
	 * found among the causes of {@code e}, and the message of {@code e}
	 * otherwise. The message is prefixed with the line where the problem was
	 * found, if known.
	 * 
	 * @param e the error raised while reading the witness
	 * 
	 * @return the exception to throw, with {@code e} as its cause
	 */
	private static WitnessReadException invalid(
			JsonProcessingException e) {
		String message = e.getOriginalMessage();
		for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause())
			if (cause instanceof InvalidWitnessException)
				message = cause.getMessage();
		JsonLocation where = e.getLocation();
		String line = where == null || where.getLineNr() < 1 ? "" : "line " + where.getLineNr() + ": ";
		return new WitnessReadException(line + message, e);
	}
}
