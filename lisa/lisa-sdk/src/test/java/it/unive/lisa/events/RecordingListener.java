package it.unive.lisa.events;

import it.unive.lisa.checks.syntactic.CheckTool;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingListener
		implements
		EventListener {
	public final List<String> events = new CopyOnWriteArrayList<>();

	@Override
	public void onEvent(
			Event event,
			CheckTool tool) {
		events.add(event.toString());
	}
}
