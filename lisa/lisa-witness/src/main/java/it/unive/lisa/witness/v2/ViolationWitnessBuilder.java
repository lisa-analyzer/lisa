package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.metadata.TaskLoader;
import it.unive.lisa.witness.v2.violation.Action;
import it.unive.lisa.witness.v2.violation.Segment;
import it.unive.lisa.witness.v2.violation.ViolationSequence;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// the shortcuts add a segment with a single follow waypoint; segment(...) adds any other segment
public final class ViolationWitnessBuilder
		extends
		WitnessBuilder {

	private final List<Segment> segments = new ArrayList<>();

	public ViolationWitnessBuilder(
			Task task,
			Producer producer) {
		super(task, producer);
	}

	public static ViolationWitnessBuilder forInputs(
			List<Path> inputs,
			Path propertyFile,
			Language language,
			DataModel dataModel,
			Producer producer)
			throws IOException {
		return new ViolationWitnessBuilder(TaskLoader.load(inputs, propertyFile, language, dataModel), producer);
	}

	public ViolationWitnessBuilder assumption(
			Location location,
			String value) {
		return segment(s -> s.assumption(Action.FOLLOW, location, value));
	}

	public ViolationWitnessBuilder assumption(
			Location location,
			String value,
			int threadId) {
		return segment(s -> s.assumption(Action.FOLLOW, location, value, threadId));
	}

	public ViolationWitnessBuilder branching(
			Location location,
			boolean taken) {
		return segment(s -> s.branching(Action.FOLLOW, location, taken));
	}

	public ViolationWitnessBuilder branching(
			Location location,
			boolean taken,
			int threadId) {
		return segment(s -> s.branching(Action.FOLLOW, location, taken, threadId));
	}

	public ViolationWitnessBuilder functionEnter(
			Location location) {
		return segment(s -> s.functionEnter(Action.FOLLOW, location));
	}

	public ViolationWitnessBuilder functionEnter(
			Location location,
			int threadId) {
		return segment(s -> s.functionEnter(Action.FOLLOW, location, threadId));
	}

	public ViolationWitnessBuilder functionReturn(
			Location location,
			String value) {
		return segment(s -> s.functionReturn(Action.FOLLOW, location, value));
	}

	public ViolationWitnessBuilder functionReturn(
			Location location,
			String value,
			int threadId) {
		return segment(s -> s.functionReturn(Action.FOLLOW, location, value, threadId));
	}

	public ViolationWitnessBuilder target(
			Location location) {
		return segment(s -> s.target(location));
	}

	public ViolationWitnessBuilder target(
			Location location,
			int threadId) {
		return segment(s -> s.target(location, threadId));
	}

	public ViolationWitnessBuilder segment(
			Consumer<SegmentBuilder> waypoints) {
		SegmentBuilder segment = new SegmentBuilder(task);
		waypoints.accept(segment);
		segments.add(segment.build());
		return this;
	}

	public ViolationWitness build() {
		return new ViolationWitness(new ViolationSequence(Metadata.create(producer, task), segments));
	}
}
