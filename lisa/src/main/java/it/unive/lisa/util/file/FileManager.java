package it.unive.lisa.util.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;


public class FileManager {
	
	public static Writer mkOutputFile(String name) throws IOException {
		return mkOutputFile(name, false);
	}
	
	public static Writer mkOutputFile(String name, boolean bom) throws IOException {
		File file = new File(name);
		
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		
		Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8.newEncoder());
		if (bom) 
			writer.write('\ufeff');

		return writer;
	}
}
