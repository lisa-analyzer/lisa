package it.unive.lisa.util.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
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
