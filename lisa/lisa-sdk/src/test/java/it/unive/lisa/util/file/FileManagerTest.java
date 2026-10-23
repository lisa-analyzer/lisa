package it.unive.lisa.util.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileManagerTest {

	private static final String TESTDIR = "tmp-testdir";

	@BeforeEach
	public void init() {
		File dir = new File(TESTDIR);
		if (dir.exists())
			fail("The directory already exists");
	}

	@AfterEach
	public void cleanup() {
		File dir = new File(TESTDIR);

		try {
			FileManager.forceDeleteFolder(TESTDIR);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Deleting an empty directory led to an exception: " + e.getMessage());
		}

		if (dir.exists())
			fail("The directory has not been deleted");
	}

	@Test
	public void testDeleteNonExistingFolder() {
		// this just ensures that cleanup() runs
	}

	@Test
	public void testDeleteEmptyFolder() {
		File dir = new File(TESTDIR);
		dir.mkdir();
		if (!dir.exists())
			fail("The directory has not been created");
	}

	@Test
	public void testCreateFile() {
		FileManager manager = new FileManager(TESTDIR);
		String name = "foo.txt";
		try {
			manager.mkOutputFile(name, w -> w.write("foo"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		File dir = new File(TESTDIR);
		if (!dir.exists())
			fail("The working directory has not been created");

		File file = new File(dir, name);
		if (!file.exists())
			fail("The file has not been created");

		assertEquals(manager.createdFiles().size(), 1, "FileManager did not track the correct number of files");
		assertEquals(manager.createdFiles().iterator().next(), name, "FileManager did not track the created file");
	}

	@Test
	public void testCreateFileWithBom() {
		FileManager manager = new FileManager(TESTDIR);
		String name = "foo.txt";
		try {
			manager.mkOutputFile(name, true, w -> w.write("foo"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		File dir = new File(TESTDIR);
		if (!dir.exists())
			fail("The working directory has not been created");

		File file = new File(dir, name);
		if (!file.exists())
			fail("The file has not been created");

		assertEquals(manager.createdFiles().size(), 1, "FileManager did not track the correct number of files");
		assertEquals(manager.createdFiles().iterator().next(), name, "FileManager did not track the created file");
	}

	@Test
	public void testCreateFileInSubfolder() {
		FileManager manager = new FileManager(TESTDIR);
		try {
			manager.mkOutputFile("sub", "foo.txt", w -> w.write("foo"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		File dir = new File(TESTDIR);
		if (!dir.exists())
			fail("The working directory has not been created");

		File sub = new File(dir, "sub");
		if (!sub.exists())
			fail("The subfolder has not been created");

		File file = new File(sub, "foo.txt");
		if (!file.exists())
			fail("The file has not been created");

		assertEquals(1, manager.createdFiles().size(), "FileManager did not track the correct number of files");
		assertEquals("sub/foo.txt", manager.createdFiles().iterator().next(),
				"FileManager did not track the created file");
	}

	@Test
	public void testDotFileNameSanitization() {
		FileManager manager = new FileManager(TESTDIR);
		try {
			manager.mkDotFile("foo()  bar::jar", w -> w.write("foo"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		File dir = new File(TESTDIR);
		if (!dir.exists())
			fail("The working directory has not been created");

		File file = new File(dir, "foo()__bar.jar.dot");
		if (!file.exists())
			fail("The file has not been created");

		assertEquals(manager.createdFiles().size(), 1, "FileManager did not track the correct number of files");
		assertEquals(manager.createdFiles().iterator().next(), file.getName(),
				"FileManager did not track the created file");
	}

	@Test
	public void testFileNameWithUnixSlashes() {
		FileManager manager = new FileManager(TESTDIR);
		try {
			manager.mkOutputFile("foo/bar.txt", w -> w.write("foo"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		File dir = new File(TESTDIR);
		if (!dir.exists())
			fail("The working directory has not been created");

		File file = new File(dir, "foo_bar.txt");
		if (!file.exists())
			fail("The file has not been created");

		assertEquals(manager.createdFiles().size(), 1, "FileManager did not track the correct number of files");
		assertEquals(
				manager.createdFiles().iterator().next(),
				file.getName(),
				"FileManager did not track the created file");
	}

	@Test
	public void testMkJsonFileAppendsExtension() {
		FileManager manager = new FileManager(TESTDIR);
		try {
			manager.mkJsonFile("report", w -> w.write("{}"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		assertTrue(new File(TESTDIR, "report.json").exists(), "The json file has not been created");
	}

	@Test
	public void testMkGraphmlFileAppendsExtension() {
		FileManager manager = new FileManager(TESTDIR);
		try {
			manager.mkGraphmlFile("graph", w -> w.write("<graphml/>"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		assertTrue(new File(TESTDIR, "graph.graphml").exists(), "The graphml file has not been created");
	}

	@Test
	public void testMkHtmlFileAppendsExtension() {
		FileManager manager = new FileManager(TESTDIR);
		try {
			manager.mkHtmlFile("page", w -> w.write("<html/>"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		assertTrue(new File(TESTDIR, "page.html").exists(), "The html file has not been created");
	}

	@Test
	public void testGenerateSupportFilesDoesNothingByDefault() {
		FileManager manager = new FileManager(TESTDIR);
		try {
			manager.generateSupportFiles();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Generating support files should not fail even without a working directory");
		}
		assertTrue(manager.createdFiles().isEmpty(), "No files should be created when usedHtmlViewer() was not called");
	}

	@Test
	public void testGenerateSupportFilesAfterUsedHtmlViewer() {
		FileManager manager = new FileManager(TESTDIR);
		manager.usedHtmlViewer();
		try {
			manager.generateSupportFiles();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Generating support files failed");
		}

		assertTrue(
				new File(TESTDIR, Paths.get("assets", "style.css").toString()).exists(),
				"The style.css support file has not been created");
		assertTrue(
				new File(TESTDIR, Paths.get("assets", "d3.v7.min.js").toString()).exists(),
				"The d3.v7.min.js support file has not been created");
		assertTrue(
				new File(TESTDIR, Paths.get("assets", "d3-graphviz.min.js").toString()).exists(),
				"The d3-graphviz.min.js support file has not been created");
		assertEquals(3, manager.createdFiles().size());
	}

	@Test
	public void testAbsoluteLookingNameIsTreatedAsRelative() {
		// the javadoc documents that name is always joined with the workdir,
		// even if it looks like an absolute path: no exception is raised, no
		// file is created outside of the working directory, and the leading
		// slash is sanitized away like any other illegal character
		FileManager manager = new FileManager(TESTDIR);
		try {
			manager.mkOutputFile("/tmp/should-not-escape.txt", w -> w.write("foo"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		assertTrue(!new File("/tmp/should-not-escape.txt").exists(),
				"The file must not be created outside of the working directory");
		assertTrue(new File(TESTDIR, "_tmp_should-not-escape.txt").exists(),
				"The file should have been created inside the working directory, with its slashes sanitized");
		assertEquals(1, manager.createdFiles().size());
	}

	@Test
	public void testFileNameWithWindowsSlashes() {
		FileManager manager = new FileManager(TESTDIR);
		try {
			manager.mkOutputFile("foo\\bar.txt", w -> w.write("foo"));
		} catch (IOException e) {
			e.printStackTrace();
			fail("The file has not been created");
		}

		File dir = new File(TESTDIR);
		if (!dir.exists())
			fail("The working directory has not been created");

		File file = new File(dir, "foo_bar.txt");
		if (!file.exists())
			fail("The file has not been created");

		assertEquals(manager.createdFiles().size(), 1, "FileManager did not track the correct number of files");
		assertEquals(manager.createdFiles().iterator().next(), file.getName(),
				"FileManager did not track the created file");
	}

}
