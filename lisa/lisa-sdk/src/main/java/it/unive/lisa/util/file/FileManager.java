package it.unive.lisa.util.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 * A file manager that provides standard functionalities for communicating with
 * the file system.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FileManager {

	private final File workdir;

	private final Collection<String> createdFiles = new TreeSet<>();

	private boolean usedHtmlViewer = false;

	/**
	 * Builds a new manager that will produce files in the given
	 * {@code workdir}.
	 * 
	 * @param workdir the path to the directory where files will be created by
	 *                    this manager
	 */
	public FileManager(
			String workdir) {
		this.workdir = Paths.get(workdir).toFile();
	}

	/**
	 * Yields the collection of file names that have been created by this
	 * manager.
	 * 
	 * @return the names of the created files
	 */
	public Collection<String> createdFiles() {
		return createdFiles;
	}

	/**
	 * Takes note that the at least one of the dumped files needs cytoscape
	 * support for non-compund graphs to be correctly visualized. This will have
	 * an effect on the files produced by {@link #generateSupportFiles()}.
	 */
	public void usedHtmlViewer() {
		usedHtmlViewer = true;
	}

	/**
	 * Creates a UTF-8 encoded file with the given name. If name is a path, all
	 * missing directories will be created as well. The given name will be
	 * joined with the workdir used to initialize this file manager, thus
	 * raising an exception if {@code name} is absolute. {@code filler} will
	 * then be used to write to the writer.
	 * 
	 * @param name   the name of the file to create
	 * @param filler the callback to write to the file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */
	public void mkOutputFile(
			String name,
			WriteAction filler)
			throws IOException {
		mkOutputFile(name, false, filler);
	}

	/**
	 * Creates a UTF-8 encoded file with the given name. If name is a path, all
	 * missing directories will be created as well. The given name will be
	 * joined with the workdir used to initialize this file manager, thus
	 * raising an exception if {@code name} is absolute. {@code filler} will
	 * then be used to write to the writer.
	 * 
	 * @param path   the sub-path, relative to the workdir, where the file
	 *                   should be created
	 * @param name   the name of the file to create
	 * @param filler the callback to write to the file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */
	public void mkOutputFile(
			String path,
			String name,
			WriteAction filler)
			throws IOException {
		mkOutputFile(path, name, false, filler);
	}

	/**
	 * Creates a UTF-8 encoded file with the given name, appending the
	 * {@code dot} extension. The name will be stripped of any characters that
	 * might cause problems in the file name. The given name will be joined with
	 * the workdir used to initialize this file manager, thus raising an
	 * exception if {@code name} is absolute. {@code filler} will then be used
	 * to write to the writer.
	 * 
	 * @param name   the name of the file to create
	 * @param filler the callback to write to the file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */
	public void mkDotFile(
			String name,
			WriteAction filler)
			throws IOException {
		mkOutputFile(cleanupCFGName(name) + ".dot", false, filler);
	}

	/**
	 * Creates a UTF-8 encoded file with the given name, appending the
	 * {@code json} extension. The name will be stripped of any characters that
	 * might cause problems in the file name. The given name will be joined with
	 * the workdir used to initialize this file manager, thus raising an
	 * exception if {@code name} is absolute. {@code filler} will then be used
	 * to write to the writer.
	 * 
	 * @param name   the name of the file to create
	 * @param filler the callback to write to the file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */

	public void mkJsonFile(
			String name,
			WriteAction filler)
			throws IOException {
		mkOutputFile(cleanupCFGName(name) + ".json", false, filler);
	}

	/**
	 * Creates a UTF-8 encoded file with the given name, appending the
	 * {@code graphml} extension. The name will be stripped of any characters
	 * that might cause problems in the file name. The given name will be joined
	 * with the workdir used to initialize this file manager, thus raising an
	 * exception if {@code name} is absolute. {@code filler} will then be used
	 * to write to the writer.
	 * 
	 * @param name   the name of the file to create
	 * @param filler the callback to write to the file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */

	public void mkGraphmlFile(
			String name,
			WriteAction filler)
			throws IOException {
		mkOutputFile(cleanupCFGName(name) + ".graphml", false, filler);
	}

	/**
	 * Creates a UTF-8 encoded file with the given name, appending the
	 * {@code html} extension. The name will be stripped of any characters that
	 * might cause problems in the file name. The given name will be joined with
	 * the workdir used to initialize this file manager, thus raising an
	 * exception if {@code name} is absolute. {@code filler} will then be used
	 * to write to the writer.
	 * 
	 * @param name   the name of the file to create
	 * @param filler the callback to write to the file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */
	public void mkHtmlFile(
			String name,
			WriteAction filler)
			throws IOException {
		mkOutputFile(cleanupCFGName(name) + ".html", false, filler);
	}

	/**
	 * A functional interface for a write operation that can throw
	 * {@link IOException}s.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface WriteAction {

		/**
		 * Performs the operation on the given writer.
		 * 
		 * @param writer the object to use for writing
		 * 
		 * @throws IOException if an error happens while writing
		 */
		void perform(
				Writer writer)
				throws IOException;

	}

	/**
	 * Creates a UTF-8 encoded file with the given name. If name is a path, all
	 * missing directories will be created as well. The given name will be
	 * joined with the workdir used to initialize this file manager, thus
	 * raising an exception if {@code name} is absolute. {@code filler} will
	 * then be used to write to the writer.
	 * 
	 * @param name   the name of the file to create
	 * @param bom    if {@code true}, the bom marker {@code \ufeff} will be
	 *                   written to the file
	 * @param filler the callback to write to the file
	 * 
	 * @throws IOException if something goes wrong while creating or writing to
	 *                         the file
	 */
	public void mkOutputFile(
			String name,
			boolean bom,
			WriteAction filler)
			throws IOException {
		mkOutputFile(null, name, bom, filler);
	}

	/**
	 * Creates a UTF-8 encoded file with the given name. If name is a path, all
	 * missing directories will be created as well. The given name will be
	 * joined with the workdir used to initialize this file manager, thus
	 * raising an exception if {@code name} is absolute. {@code filler} will
	 * then be used to write to the writer.
	 * 
	 * @param path   the sub-path, relative to the workdir, where the file
	 *                   should be created
	 * @param name   the name of the file to create
	 * @param bom    if {@code true}, the bom marker {@code \ufeff} will be
	 *                   written to the file
	 * @param filler the callback to write to the file
	 * 
	 * @throws IOException if something goes wrong while creating or writing to
	 *                         the file
	 */
	public void mkOutputFile(
			String path,
			String name,
			boolean bom,
			WriteAction filler)
			throws IOException {
		File parent = workdir;
		if (path != null)
			parent = new File(workdir, cleanFileName(path, true));
		File file = new File(parent, cleanFileName(name, false));

		if (!parent.exists() && !parent.mkdirs())
			throw new IOException("Unable to create directory structure for " + file);

		createdFiles.add(FilenameUtils.separatorsToUnix(workdir.toPath().relativize(file.toPath()).toString()));
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8.newEncoder())) {
			if (bom)
				writer.write('\ufeff');
			filler.perform(writer);
		}
	}

	private final static int[] illegalChars = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
			20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124 };

	private static String cleanFileName(
			String name,
			boolean keepDirSeparator) {
		// https://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars
		StringBuilder cleanName = new StringBuilder();
		int len = name.codePointCount(0, name.length());
		for (int i = 0; i < len; i++) {
			int c = name.codePointAt(i);
			// 57 is / and 92 is \
			if ((keepDirSeparator && (c == 57 || c == 92)) || Arrays.binarySearch(illegalChars, c) < 0)
				cleanName.appendCodePoint(c);
			else
				cleanName.appendCodePoint('_');

		}
		return cleanName.toString();
	}

	private static String cleanupCFGName(
			String name) {
		String result = name.replace(' ', '_');
		result = result.replace("::", ".");
		return result;
	}

	/**
	 * Deletes a folder with all of its contents if it exists.
	 * 
	 * @param path the path to the folder
	 * 
	 * @throws IOException if an error happens while deleting
	 */
	public static void forceDeleteFolder(
			String path)
			throws IOException {
		File workdir = new File(path);
		if (workdir.exists())
			FileUtils.forceDelete(workdir);
	}

	/**
	 * Generates, inside the working directory, all supporting files needed for
	 * correct visualization of dumped files. The generated files depend on the
	 * which of the <code>usedX</code> methods exposed by this class have been
	 * invoked.
	 * 
	 * @throws IOException if an error happens during the generation
	 */
	public void generateSupportFiles()
			throws IOException {
		List<String> files = new ArrayList<>();
		if (usedHtmlViewer) {
			files.add("assets/d3.v7.min.js");
			files.add("assets/d3-graphviz.min.js");
		}

		for (String file : files)
			try (InputStream stream = getClass().getClassLoader().getResourceAsStream("html-graph/" + file)) {
				String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
				int pos = file.lastIndexOf("/");
				if (pos == -1)
					mkOutputFile(file, false, writer -> writer.write(content));
				else
					mkOutputFile(
							file.substring(0, pos),
							file.substring(pos + 1),
							false,
							writer -> writer.write(content));
			}
	}

}
