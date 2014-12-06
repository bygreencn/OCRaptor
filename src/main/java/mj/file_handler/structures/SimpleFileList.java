package mj.file_handler.structures;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SimpleFileList extends FileList {
	ArrayList<File> list;

	/**
	 *
	 */
	public SimpleFileList() {
		this.list = new ArrayList<File>();
	}

	/**
	 *
	 *
	 */
	public void addAll(List<File> files) {
		if (files != null) {
			this.list.addAll(files);
		}
	}

	/**
	 * @param directory
	 * @return
	 */
	@Override
	public List<File> getFiles() {
		return this.list;
	}

	/**
	 * @return
	 */
	@Override
	public int size() {
		return this.list.size();
	}

}
