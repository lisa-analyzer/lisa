package it.unive.lisa.util.testing;

public class TestException
		extends
		RuntimeException {

	private static final long serialVersionUID = 2135849687916385496L;

	public TestException(
			String message) {
		super(message);
	}

	public TestException(
			String message,
			Throwable cause) {
		super(message, cause);
	}
}
