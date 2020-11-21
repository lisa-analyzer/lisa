package it.unive.lisa;

public class AnalysisExecutionException extends RuntimeException {

	public AnalysisExecutionException() {
		super();
	}

	public AnalysisExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public AnalysisExecutionException(String message) {
		super(message);
	}

	public AnalysisExecutionException(Throwable cause) {
		super(cause);
	}
}
