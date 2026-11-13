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

/**
 * A builder of violation witnesses. Each shortcut method adds a segment made of
 * a single follow waypoint, which is the most common case. Any other segment,
 * for instance with avoid waypoints, a cycle waypoint or the two targets of a
 * data race, is added with {@link #segment(Consumer)}. The metadata of the
 * witness, with its format version, identifier and creation time, is filled
 * when the witness is built.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class ViolationWitnessBuilder
		extends
		WitnessBuilder {

	/**
	 * The segments of the witness, in the order they are added.
	 */
	private final List<Segment> segments = new ArrayList<>();

	/**
	 * Builds the builder of a violation witness for the given task.
	 * 
	 * @param task     the task of the witness
	 * @param producer the tool producing the witness
	 */
	public ViolationWitnessBuilder(
			Task task,
			Producer producer) {
		super(task, producer);
	}

	/**
	 * Builds the builder of a violation witness for the task made of the given
	 * input files and property file. The input files are read and hashed by
	 * {@link TaskLoader}.
	 * 
	 * @param inputs       the input files of the task, as given to the verifier
	 * @param propertyFile the file containing the specification of the task
	 * @param language     the language of the task
	 * @param dataModel    the data model of the task, or {@code null} if the
	 *                         language does not need one
	 * @param producer     the tool producing the witness
	 * 
	 * @return the builder
	 * 
	 * @throws IOException if an input file or the property file cannot be read
	 */
	public static ViolationWitnessBuilder forInputs(
			List<Path> inputs,
			Path propertyFile,
			Language language,
			DataModel dataModel,
			Producer producer)
			throws IOException {
		return new ViolationWitnessBuilder(TaskLoader.load(inputs, propertyFile, language, dataModel), producer);
	}

	/**
	 * Adds a segment made of a followed assumption waypoint.
	 * 
	 * @param location the location of the waypoint
	 * @param value    the expression that holds at the location
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public ViolationWitnessBuilder assumption(
			Location location,
			String value) {
		return segment(s -> s.assumption(Action.FOLLOW, location, value));
	}

	/**
	 * Adds a segment made of a followed assumption waypoint, executed by the
	 * given thread.
	 * 
	 * @param location the location of the waypoint
	 * @param value    the expression that holds at the location
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public ViolationWitnessBuilder assumption(
			Location location,
			String value,
			int threadId) {
		return segment(s -> s.assumption(Action.FOLLOW, location, value, threadId));
	}

	/**
	 * Adds a segment made of a followed branching waypoint.
	 * 
	 * @param location the location of the condition
	 * @param taken    whether the condition evaluates to {@code true}
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public ViolationWitnessBuilder branching(
			Location location,
			boolean taken) {
		return segment(s -> s.branching(Action.FOLLOW, location, taken));
	}

	/**
	 * Adds a segment made of a followed branching waypoint, executed by the
	 * given thread.
	 * 
	 * @param location the location of the condition
	 * @param taken    whether the condition evaluates to {@code true}
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public ViolationWitnessBuilder branching(
			Location location,
			boolean taken,
			int threadId) {
		return segment(s -> s.branching(Action.FOLLOW, location, taken, threadId));
	}

	/**
	 * Adds a segment made of a followed function enter waypoint.
	 * 
	 * @param location the location of the call
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public ViolationWitnessBuilder functionEnter(
			Location location) {
		return segment(s -> s.functionEnter(Action.FOLLOW, location));
	}

	/**
	 * Adds a segment made of a followed function enter waypoint, executed by
	 * the given thread.
	 * 
	 * @param location the location of the call
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public ViolationWitnessBuilder functionEnter(
			Location location,
			int threadId) {
		return segment(s -> s.functionEnter(Action.FOLLOW, location, threadId));
	}

	/**
	 * Adds a segment made of a followed function return waypoint.
	 * 
	 * @param location the location of the call
	 * @param value    the constraint on the returned value, such as
	 *                     {@code \result == 0}
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public ViolationWitnessBuilder functionReturn(
			Location location,
			String value) {
		return segment(s -> s.functionReturn(Action.FOLLOW, location, value));
	}

	/**
	 * Adds a segment made of a followed function return waypoint, executed by
	 * the given thread.
	 * 
	 * @param location the location of the call
	 * @param value    the constraint on the returned value, such as
	 *                     {@code \result == 0}
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public ViolationWitnessBuilder functionReturn(
			Location location,
			String value,
			int threadId) {
		return segment(s -> s.functionReturn(Action.FOLLOW, location, value, threadId));
	}

	/**
	 * Adds a segment made of a target waypoint. This segment must be the last
	 * one of the witness.
	 * 
	 * @param location the location of the violation
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task
	 */
	public ViolationWitnessBuilder target(
			Location location) {
		return segment(s -> s.target(location));
	}

	/**
	 * Adds a segment made of a target waypoint, executed by the given thread.
	 * This segment must be the last one of the witness.
	 * 
	 * @param location the location of the violation
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task
	 */
	public ViolationWitnessBuilder target(
			Location location,
			int threadId) {
		return segment(s -> s.target(location, threadId));
	}

	/**
	 * Adds a segment whose waypoints are added, in order, by the given code.
	 * 
	 * @param waypoints the code adding the waypoints of the segment
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if a waypoint breaks a rule of the
	 *                                     format, or if the waypoints do not
	 *                                     form a segment
	 */
	public ViolationWitnessBuilder segment(
			Consumer<SegmentBuilder> waypoints) {
		SegmentBuilder segment = new SegmentBuilder(task);
		waypoints.accept(segment);
		segments.add(segment.build());
		return this;
	}

	/**
	 * Builds the witness made of the segments added so far, with new metadata.
	 * Each call builds a new witness, with its own identifier and creation
	 * time.
	 * 
	 * @return the witness
	 * 
	 * @throws InvalidWitnessException if the segments do not form a violation
	 *                                     sequence, for instance if the segment
	 *                                     with a target is not the last one
	 */
	public ViolationWitness build() {
		return new ViolationWitness(new ViolationSequence(Metadata.create(producer, task), segments));
	}
}
