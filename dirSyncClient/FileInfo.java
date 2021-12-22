package project2;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * An object that contain infomation about a directory or file
 * 
 * @author Thao Phung
 * @version 10/11/18
 */
public class FileInfo implements Serializable {	
	private static final long serialVersionUID = 1L;
	private String fileName; // file name
	private String path; // file path
	private long size; // file size
	private long lastModified;// last modified

	/**
	 * Constructor
	 * 
	 * @param fileName
	 *            file name
	 * @param path
	 *            file path
	 * @param size
	 *            file size
	 * @param lastModified
	 *            file last modified
	 */
	public FileInfo(Path fileName, Path path, long size, FileTime lastModified) {
		this.fileName = fileName.toString();
		this.path = path.toString();
		this.size = size;
		this.lastModified = lastModified.toMillis();
	}

	/**
	 * Get file name
	 * 
	 * @return file name
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * get file path
	 * 
	 * @return file path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * get file size
	 * 
	 * @return file size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * get last modified
	 * 
	 * @return last modified
	 */
	public long getLastModified() {
		return lastModified;
	}

}
