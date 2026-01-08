package it.unive.lisa.events;

import it.unive.lisa.CheckTool;

public class FailingListener
		implements
		EventListener {

	public volatile boolean errorHandled = false;

	@Override
	public void onEvent(
			Event e,
			CheckTool tool) {
		throw new RuntimeException("boom");
	}

	@Override
	public void onError(
			Event event,
			Exception error,
			CheckTool tool) {
		errorHandled = true;
	}
}
