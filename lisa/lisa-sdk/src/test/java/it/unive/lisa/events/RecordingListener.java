package it.unive.lisa.events;

import it.unive.lisa.ReportingTool;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingListener
		implements
		EventListener {
	public final List<String> events = new CopyOnWriteArrayList<>();

	@Override
	public void onEvent(
			Event event,
			ReportingTool tool) {
		events.add(event.toString());
	}
}
