package it.unive.lisa.outputs;

public class OutputDumpingException extends RuntimeException {

	private static final long serialVersionUID = -6224754350892145958L;

	public OutputDumpingException() {
		super();
	}

	public OutputDumpingException(String message, Throwable cause) {
		super(message, cause);
	}

	public OutputDumpingException(String message) {
		super(message);
	}

	public OutputDumpingException(Throwable cause) {
		super(cause);
	}

}
