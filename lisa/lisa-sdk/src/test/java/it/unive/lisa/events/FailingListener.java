package it.unive.lisa.events;

import it.unive.lisa.ReportingTool;

public class FailingListener
		implements
		EventListener {

	public volatile boolean errorHandled = false;

	@Override
	public void onEvent(
			Event e,
			ReportingTool tool) {
		throw new RuntimeException("boom");
	}

	@Override
	public void onError(
			Event event,
			Exception error,
			ReportingTool tool) {
		errorHandled = true;
	}
}
