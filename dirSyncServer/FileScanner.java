package project2;

/**
 * This class implements a scan tool that read all directory from "home"
 * @author Thao Phung
 * @version 10/11/18
 */
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class FileScanner {
	private static ObjectOutputStream output; // outputs data to file
	private static ObjectInputStream input; // inputs data to read info

	/**
	 * A method that open a file to write info
	 * @precondition user is allow to open or create file
	 */
	public void openWriteFile(Path path) {
		try {
			output = new ObjectOutputStream(Files.newOutputStream(path));
		} catch (IOException ioException) {
			System.err.println("Error opening file. Terminating.");
			System.exit(1); // terminate the program
		}
	}

	/**
	 * This method finds all the directory and files inside home (including
	 * subdirectory) and write store information in a FileInfo object
	 * 
	 * @param path
	 *            path to home directory
	 * @throws IOException
	 * @precondition open files before scan
	 */
	public void scan(Path path) throws IOException {
		if (Files.exists(path)) // if path exists, output info about it
		{

			FileInfo newFile = new FileInfo(path.getFileName(), path, Files.size(path),
					Files.getLastModifiedTime(path));

			output.writeObject(newFile);

			if (Files.isDirectory(path)) // search all files in the subdirectory using DFS
			{
				// object for iterating through a directory's contents
				DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);

				for (Path p : directoryStream)
					scan(p);
			}
		} else // not file or directory, output error message
		{
			System.out.printf("%s does not exist%n", path);
		}
	}

	/**
	 * Close File
	 */
	public void closeWriteFile() {
		if (output != null)
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public  void openReadFile(Path path)
	   {
	      try // open file
	      {
	         input = new ObjectInputStream(          
	            Files.newInputStream(path));
	      } 
	      catch (IOException ioException)
	      {
	         System.err.println("Error opening file.");
	         System.exit(1);
	      } 
	   }
	

	/**
	 * Read object and output to the screen
	 * 
	 * @throws IOException
	 * @precondition file exist
	 */
	public ArrayList<FileInfo> readFile(Path path) throws IOException
	{
		
		ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
		input = new ObjectInputStream(Files.newInputStream(path));

		try {
			while (true) // loop until there is an EOFException
			{
				FileInfo record = (FileInfo) input.readObject();
				fileList.add(record);
				
				// display record contents
				System.out.printf("%-20s%-50s%-5d%s%n", record.getFileName(), record.getPath(), record.getSize(),
						record.getLastModified());
			}
		} catch (EOFException endOfFileException) {
			// System.out.printf("%nNo more records%n");
		} catch (ClassNotFoundException classNotFoundException) {
			System.err.println("Invalid object type. Terminating.");
		}
		System.out.println();
		return fileList;
	}
	


	public void closeReadFile()
	{	
		try
		{
			if (input != null)
				input.close();
		} 
		catch (IOException ioException)
		{
			System.err.println("Error closing file. Terminating.");
			System.exit(1);
		} 
	} 
}
