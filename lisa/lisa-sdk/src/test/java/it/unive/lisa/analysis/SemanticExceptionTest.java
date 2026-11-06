package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class SemanticExceptionTest {

	@Test
	public void testNoArgConstructorHasNoMessageOrCause() {
		SemanticException e = new SemanticException();
		assertNull(e.getMessage());
		assertNull(e.getCause());
	}

	@Test
	public void testMessageOnlyConstructor() {
		SemanticException e = new SemanticException("boom");
		assertEquals("boom", e.getMessage());
		assertNull(e.getCause());
	}

	@Test
	public void testCauseOnlyConstructorWrapsTheCauseInTheMessage() {
		RuntimeException cause = new RuntimeException("inner");
		SemanticException e = new SemanticException(cause);
		assertSame(cause, e.getCause());
	}

	@Test
	public void testMessageAndCauseConstructor() {
		RuntimeException cause = new RuntimeException("inner");
		SemanticException e = new SemanticException("boom", cause);
		assertEquals("boom", e.getMessage());
		assertSame(cause, e.getCause());
	}

	@Test
	public void testWrapperCarriesTheSemanticExceptionAsItsCauseWithAFixedMessage() {
		SemanticException cause = new SemanticException("boom");
		SemanticExceptionWrapper wrapper = new SemanticExceptionWrapper(cause);
		assertSame(cause, wrapper.getCause());
		assertEquals("A semantic exception happened", wrapper.getMessage());
	}

}
