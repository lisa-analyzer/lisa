package it.unive.lisa.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import it.unive.lisa.checks.syntactic.CheckTool;

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
