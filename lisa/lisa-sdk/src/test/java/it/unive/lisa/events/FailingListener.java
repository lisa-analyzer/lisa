package it.unive.lisa.events;

public class FailingListener
		implements
		EventListener {

	public volatile boolean errorHandled = false;

	@Override
	public void onEvent(
			Event e) {
		throw new RuntimeException("boom");
	}

	@Override
	public void onError(
			Event event,
			Exception error) {
		errorHandled = true;
	}
}
