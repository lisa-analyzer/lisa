package it.unive.lisa.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class EventQueueTest {

	private static class StringEvent
			implements
			Event {
		private final String description;

		private StringEvent(
				String description) {
			this.description = description;
		}

		@Override
		public String toString() {
			return description;
		}
	}

	@Test
	public void synchronous() throws InterruptedException {
		RecordingListener listener = new RecordingListener();

		EventQueue queue = new EventQueue(
				List.of(listener),
				List.of(),
				null);

		queue.post(new StringEvent("main"));
		queue.post(new StringEvent("x = 1"));

		assertEquals(
				List.of("main", "x = 1"),
				listener.events);

		queue.close();
	}

	@Test
	public void asynchronous() throws Exception {
		RecordingListener listener = new RecordingListener();

		EventQueue queue = new EventQueue(
				List.of(),
				List.of(listener),
				null);

		queue.post(new StringEvent("main"));
		queue.post(new StringEvent("x = 1"));

		queue.join();

		assertEquals(
				List.of("main", "x = 1"),
				listener.events);

		queue.close();
	}

	@Test
	public void syncAndAsync() throws Exception {
		RecordingListener sync = new RecordingListener();
		RecordingListener async = new RecordingListener();

		EventQueue queue = new EventQueue(
				List.of(sync),
				List.of(async),
				null);

		queue.post(new StringEvent("test"));
		queue.join();

		assertEquals(List.of("test"), sync.events);
		assertEquals(List.of("test"), async.events);

		queue.close();
	}

	@Test
	public void listenerExceptionsDoNotBreakDispatch() throws Exception {
		FailingListener failing = new FailingListener();
		RecordingListener good = new RecordingListener();

		EventQueue queue = new EventQueue(
				List.of(failing, good),
				List.of(),
				null);

		queue.post(new StringEvent("safe"));

		assertTrue(failing.errorHandled);
		assertEquals(List.of("safe"), good.events);

		queue.close();
	}

	@Test
	public void closeWaitsForAsyncProcessing() throws Exception {
		RecordingListener listener = new RecordingListener();

		EventQueue queue = new EventQueue(
				List.of(),
				List.of(listener),
				null);

		for (int i = 0; i < 10000; i++)
			queue.post(new StringEvent("event" + i));
		queue.close();

		// if close did not wait, some events would be missing
		assertEquals(10000, listener.events.size());
		List<String> expected = new java.util.ArrayList<>(100);
		for (int i = 0; i < 10000; i++)
			expected.add("event" + i);
		assertEquals(expected, listener.events);
	}
}
