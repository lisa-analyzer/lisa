package it.unive.lisa.util.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.TreeSet;

/**
 * A file manager that provides standard functionalities for communicating with
 * the file system.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FileManager {

	private static File workdir;

	private static final Collection<String> createdFiles = new TreeSet<>();

	/**
	 * Sets the working directory that will be used as root folder for the
	 * creation of all files.
	 * 
	 * @param workdir the working directory
	 */
	public static void setWorkdir(String workdir) {
		FileManager.workdir = Paths.get(workdir).toFile();
	}

	/**
	 * Clears the list of names of created files.
	 */
	public static void clearCreatedFiles() {
		createdFiles.clear();
	}

	/**
	 * Yields the collection of file names that have been created by this
	 * manager from the last call to {@link #clearCreatedFiles()}.
	 * 
	 * @return the names of the created files
	 */
	public static Collection<String> createdFiles() {
		return createdFiles;
	}

	/**
	 * Creates a UTF-8 encoded file with the given name. If name is a path, all
	 * missing directories will be created as well. The given name will be
	 * joined with the workdir used to initialize this file manager, thus
	 * raising an exception if {@code name} is absolute.
	 * 
	 * @param name the name of the file to create
	 * 
	 * @return a {@link Writer} instance that can write to the created file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */
	public static Writer mkOutputFile(String name) throws IOException {
		return mkOutputFile(cleanupForDotFile(name), false);
	}

	/**
	 * Creates a UTF-8 encoded file with the given name. If name is a path, all
	 * missing directories will be created as well. The given name will be
	 * joined with the workdir used to initialize this file manager, thus
	 * raising an exception if {@code name} is absolute.
	 * 
	 * @param name the name of the file to create
	 * @param bom  if {@code true}, the bom marker {@code \ufeff} will be
	 *                 written to the file
	 * 
	 * @return a {@link Writer} instance that can write to the created file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */
	public static Writer mkOutputFile(String name, boolean bom) throws IOException {
		File file = new File(workdir, name);

		if (!file.getAbsoluteFile().getParentFile().exists())
			file.getAbsoluteFile().getParentFile().mkdirs();

		Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8.newEncoder());
		if (bom)
			writer.write('\ufeff');

		createdFiles.add(name);

		return writer;
	}

	/**
	 * Creates a UTF-8 encoded file with the given name, appending the
	 * {@code dot} extension. If name is a path, all missing directories will be
	 * created as well. The name will be stripped of any characters that might
	 * cause problems in the file name. The given name will be joined with the
	 * workdir used to initialize this file manager, thus raising an exception
	 * if {@code name} is absolute.
	 * 
	 * @param name the name of the file to create
	 * 
	 * @return a {@link Writer} instance that can write to the created file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */
	public static Writer mkDotFile(String name) throws IOException {
		return mkOutputFile(cleanupForDotFile(name) + ".dot", false);
	}

	private static String cleanupForDotFile(String name) {
		String result = name.replace(' ', '_');
		result = result.replace("::", ".");
		return result;
	}
}
