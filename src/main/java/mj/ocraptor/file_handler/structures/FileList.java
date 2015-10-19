package mj.ocraptor.file_handler.structures;

import java.io.File;
import java.util.List;

public abstract class FileList {
	public abstract List<File> getFiles();
	public abstract int size(); 
}
