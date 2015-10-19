package mj.ocraptor.file_handler.events;

import java.io.File;

public abstract class FileHandler {
	public abstract void handleFile(File file);
	public abstract void handleDir(File dir);
}
