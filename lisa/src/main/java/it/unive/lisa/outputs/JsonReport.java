package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import it.unive.lisa.checks.warnings.Warning;

public class JsonReport {
	
	private final Set<JsonWarning> findings;
	
	private final Set<String> files;
	
	public JsonReport() {
		this(Collections.emptyList(), Collections.emptyList());
	}
	
	public JsonReport(Collection<Warning> warnings, Collection<String> files) {
       	this.findings = new TreeSet<>();
       	this.files = new TreeSet<>(files);
       	for (Warning warn : warnings)
       		findings.add(new JsonWarning(warn));
	}
	
	public Collection<JsonWarning> getFindings() {
		return findings;
	}
	
	public Set<String> getFiles() {
		return files;
	}
	
	public void dump(Writer writer) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.writeValue(writer, this);
	}
	
	public static JsonReport read(Reader reader) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(reader, JsonReport.class);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((findings == null) ? 0 : findings.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonReport other = (JsonReport) obj;
		if (findings == null) {
			if (other.findings != null)
				return false;
		} else if (!findings.equals(other.findings))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JsonAnalysisReport [findings=" + findings + ", files=" + files + "]";
	}
	
	public static class JsonWarning implements Comparable<JsonWarning> {
		private String message;
		
		public JsonWarning() {
			this.message = null;
		}
		
		public JsonWarning(Warning warning) {
			this.message = warning.toString();
		}
		
		public String getMessage() {
			return message;
		}
		
		public void setMessage(String message) {
			this.message = message;
		}
		
		@Override
		public String toString() {
			return getMessage();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((message == null) ? 0 : message.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			JsonWarning other = (JsonWarning) obj;
			if (message == null) {
				if (other.message != null)
					return false;
			} else if (!message.equals(other.message))
				return false;
			return true;
		}

		@Override
		public int compareTo(JsonWarning o) {
			return message.compareTo(o.message);
		}
	}
}
