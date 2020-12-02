package dirSyncClient;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * An object that contain infomation about a directory or file
 * 
 * @author Thao Phung
 * @version 10/10/2018
 */
public class FileInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	private String fileName; 
	private String path; 
	private long size; 
	private String lastModified; 
	private String absPath;
	private static String basePath;

	/**
	 * Constructor for FileInfo class
	 * 
	 * @param fileName     name of the file
	 * @param path         path leads to the file
	 * @param size         file's size
	 * @param lastModified file's last modified time
	 */
	public FileInfo(Path fileName, Path path, long size, FileTime lastModified) {
		this.fileName = fileName.toString();
		this.path = new File(basePath).toURI().relativize(new File(path.toString()).toURI()).getPath().toString();
		this.size = size;
		this.lastModified = lastModified.toString();
		this.absPath = path.toAbsolutePath().toString();
	}
	/**
	 * get file name
	 * 
	 * @return file's name
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
	 * get last modified time
	 * 
	 * @return last modified
	 */
	public String getLastModified() {
		return lastModified;
	}

	/**
	 * get absolute path
	 */
	public String getAbsPath() {
		return absPath;
	}

	/**
	 * set new base path for home directory
	 * @param newbasePath the new base path
	 */
	public static void setBasePath(String newbasePath) {
		basePath = newbasePath;
	}

}
