package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WitnessReader {

	private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

	private WitnessReader() {
	}

	public static Witness read(
			String text)
			throws WitnessReadException {
		try {
			return MAPPER.readValue(text, Witness.class);
		} catch (JsonProcessingException e) {
			throw invalid(e);
		}
	}

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

	public static Witness read(
			Path file)
			throws IOException,
			WitnessReadException {
		try (InputStream in = Files.newInputStream(file)) {
			return read(in);
		}
	}

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
