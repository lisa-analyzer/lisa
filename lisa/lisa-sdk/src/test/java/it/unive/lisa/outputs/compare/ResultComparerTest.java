package it.unive.lisa.outputs.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.file.FileManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResultComparerTest {

	private final ResultComparer comparer = new ResultComparer();

	@TempDir
	File tmp;

	@AfterEach
	public void cleanup()
			throws IOException {
		FileManager.forceDeleteFolder("rc-test-tmp");
	}

	@BeforeEach
	public void setup()
			throws IOException {
		FileManager.forceDeleteFolder("rc-test-tmp");
	}

	private File write(
			String name,
			String content)
			throws IOException {
		File f = new File(tmp, name);
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	@Test
	public void identicalTraceFilesHaveNoDiff()
			throws IOException {
		File left = write("left.trace", "a\nb\nc\n");
		File right = write("right.trace", "a\nb\nc\n");
		assertFalse(comparer.matchTraceFiles(left, right));
	}

	@Test
	public void differingLineIsDetected()
			throws IOException {
		File left = write("left.trace", "a\nb\nc\n");
		File right = write("right.trace", "a\nX\nc\n");
		assertTrue(comparer.matchTraceFiles(left, right));
	}

	// regression test: matchTraceFiles used to use a non-short-circuiting
	// "&" in its merge loop, which silently consumed and discarded the
	// first "extra" line of whichever file was longer, causing two files
	// differing by exactly one trailing line to be reported as identical
	@Test
	public void oneExtraTrailingLineOnTheRightIsDetected()
			throws IOException {
		File left = write("left.trace", "a\nb\nc\n");
		File right = write("right.trace", "a\nb\nc\nd\n");
		assertTrue(comparer.matchTraceFiles(left, right));
	}

	@Test
	public void oneExtraTrailingLineOnTheLeftIsDetected()
			throws IOException {
		File left = write("left.trace", "a\nb\nc\nd\n");
		File right = write("right.trace", "a\nb\nc\n");
		assertTrue(comparer.matchTraceFiles(left, right));
	}

	@Test
	public void multipleExtraTrailingLinesAreAllDetected()
			throws IOException {
		File left = write("left.trace", "a\n");
		File right = write("right.trace", "a\nb\nc\nd\n");
		assertTrue(comparer.matchTraceFiles(left, right));
	}

	@Test
	public void isJsonGraphMatchesOnlyTheGraphJsonSuffix() {
		assertTrue(comparer.isJsonGraph("foo.graph.json"));
		assertFalse(comparer.isJsonGraph("foo.json"));
		assertFalse(comparer.isJsonGraph("foo.graph"));
	}

	@Test
	public void isVisualizationFileMatchesTheDocumentedExtensions() {
		for (String ext : new String[] { "dot", "graphml", "png", "html", "js", "css" })
			assertTrue(comparer.isVisualizationFile("foo." + ext), ext + " should be a visualization file");
		assertFalse(comparer.isVisualizationFile("foo.json"));
		assertFalse(comparer.isVisualizationFile("foo.txt"));
	}

	@Test
	public void customFileCompareThrowsByDefault() {
		assertThrows(
				UnsupportedOperationException.class,
				() -> comparer.customFileCompare(new File("a"), new File("b")));
	}

	@Test
	public void defaultIgnoredRunInfoKeysAreTimestampsAndVersion() {
		assertEquals(Set.of("duration", "start", "end", "version"), comparer.ignoredRunInfoKeys());
	}

	@Test
	public void defaultIgnoredConfigurationKeysIsEmpty() {
		assertTrue(comparer.ignoredConfigurationKeys().isEmpty());
	}

	@Test
	public void defaultBehaviorFlagsAreAllEnabledExceptFailFast() {
		assertTrue(comparer.shouldCompareConfigurations());
		assertTrue(comparer.shouldCompareRunInfos());
		assertTrue(comparer.shouldCompareWarnings());
		assertTrue(comparer.shouldCompareNotices());
		assertTrue(comparer.shouldCompareFiles());
		assertTrue(comparer.shouldCompareFileContents());
		assertTrue(comparer.shouldCompareAdditionalInfo());
		assertFalse(comparer.shouldFailFast());
		assertTrue(comparer.verboseLabelDiff());
	}

}
