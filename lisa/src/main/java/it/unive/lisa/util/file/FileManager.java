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

import org.apache.commons.io.FileUtils;

/**
 * A file manager that provides standard functionalities for communicating with
 * the file system.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FileManager {

	private final File workdir;

	private final Collection<String> createdFiles = new TreeSet<>();

	/**
	 * Builds a new manager that will produce files in the given
	 * {@code workdir}.
	 * 
	 * @param workdir the path to the directory where files will be created by
	 *                    this manager
	 */
	public FileManager(String workdir) {
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
	public void mkOutputFile(String name, WriteAction filler) throws IOException {
		mkOutputFile(name, false, filler);
	}

	/**
	 * Creates a UTF-8 encoded file with the given name, appending the
	 * {@code dot} extension. If name is a path, all missing directories will be
	 * created as well. The name will be stripped of any characters that might
	 * cause problems in the file name. The given name will be joined with the
	 * workdir used to initialize this file manager, thus raising an exception
	 * if {@code name} is absolute. {@code filler} will then be used to write to
	 * the writer.
	 * 
	 * @param name   the name of the file to create
	 * @param filler the callback to write to the file
	 * 
	 * @throws IOException if something goes wrong while creating the file
	 */
	public void mkDotFile(String name, WriteAction filler) throws IOException {
		mkOutputFile(cleanupForDotFile(name) + ".dot", false, filler);
	}

	@FunctionalInterface
	public interface WriteAction {
		void perform(Writer writer) throws IOException;
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
	public void mkOutputFile(String name, boolean bom, WriteAction filler) throws IOException {
		File file = new File(workdir, name);

		File parent = file.getAbsoluteFile().getParentFile();
		if (!parent.exists() && !parent.mkdirs())
			throw new IOException("Unable to create directory structure for " + file);

		createdFiles.add(name);
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8.newEncoder())) {
			if (bom)
				writer.write('\ufeff');
			filler.perform(writer);
		}
	}

	private static String cleanupForDotFile(String name) {
		String result = name.replace(' ', '_');
		result = result.replace("::", ".");
		return result;
	}

	public static void forceDeleteFolder(String path) throws IOException {
		File workdir = new File(path);
		if (workdir.exists())
			FileUtils.forceDelete(workdir);
	}
}
