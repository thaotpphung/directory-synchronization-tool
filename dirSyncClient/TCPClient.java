package project2;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * A TCP client to sync "home" folder between local and remote computer
 * 
 * @author Thao Phung
 * @version 12/1/2018
 */
 
public class TCPClient {
	private Socket socket = null;
	private DataInputStream inStream = null;
	private DataOutputStream outStream = null;
	private final String FILE_INFO = "clientInfo.ser";

	/**
	 * Constructor to create Client
	 */
	public TCPClient() {
	}

	/**
	 * create connection to Server
	 */
	public void createSocket() {
		try {
			// connect to localHost at given port #
			socket = new Socket("localHost", 5766);
			System.out.println("Connected");
			// fetch the streams
			inStream = new DataInputStream(socket.getInputStream());
			outStream = new DataOutputStream(socket.getOutputStream());
		} catch (Exception u) {
			u.printStackTrace();
		}
	}

	/**
	 * scan and make a file info of "home" folder
	 * 
	 * @return an ArrayList of the FileInfo objects
	 * @precondition "home" folder needs to exists
	 */
	public ArrayList<FileInfo> makeFileInfo() {
		FileScanner scanner = new FileScanner();
		// scan "home" and make file info
		scanner.openWriteFile(Paths.get(FILE_INFO));
		Path path = Paths.get("home");
		try {
			scanner.scan(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		scanner.closeWriteFile();
		// read file info to an ArrayList of object FileInfo
		scanner.openReadFile(Paths.get(FILE_INFO));
		ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
		try {
			fileList = scanner.readFile(Paths.get(FILE_INFO));
			for ( int i = 0; i< fileList.size(); i++)
			{
				System.out.println(fileList.get(i).getFileName() + " " + fileList.get(i).getLastModified());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		scanner.closeReadFile();
		return fileList;
	}

	/**
	 * send file info to the Server
	 */
	public void sendFileInfo() {
		final int MAX_BUFFER = 1000;
		byte[] data = null;
		int bufferSize = 0;
		try {
			FileInputStream fileInput = null;
			System.out.println("send file info to server");
			/* send File info to Server */
			File fileInfo = new File(FILE_INFO);
			fileInput = new FileInputStream(fileInfo);
			// get the file length
			long fileSize = fileInfo.length();
			System.out.println("File info size is : " + fileSize + " bytes");
			// first send the size of the file to the client
			outStream.writeLong(fileSize);
			outStream.flush();
			// Now send the file contents
			if (fileSize > MAX_BUFFER)
				bufferSize = MAX_BUFFER;
			else
				bufferSize = (int) fileSize;
			data = new byte[bufferSize];
			long totalBytesRead = 0;
			while (true) {
				// read upto MAX_BUFFER number of bytes from file
				int readBytes = fileInput.read(data);
				// send readBytes number of bytes to the client
				outStream.write(data);
				outStream.flush();
				// stop if EOF
				if (readBytes == -1)// EOF
					break;
				totalBytesRead = totalBytesRead + readBytes;
				// stop if fileLength number of bytes are read
				if (totalBytesRead == fileSize)
					break;
				//// update fileSize for the last remaining block of data
				if ((fileSize - totalBytesRead) < MAX_BUFFER)
					bufferSize = (int) (fileSize - totalBytesRead);
				// reinitialize the data buffer
				data = new byte[bufferSize];
			}
			fileInput.close();
			System.out.println("sent file info");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * send the files inside "home" folder to Server
	 * 
	 * @param fileList
	 *            list of File Info objects
	 */
	public void sendFiles(ArrayList<FileInfo> fileList) {
		final int MAX_BUFFER = 1000;
		byte[] data = null;
		int bufferSize = 0;
		try {
			FileInputStream fileInput = null;
			/* send files inside "home" */
			for (int i = 0; i < fileList.size(); i++) {
				Path path = Paths.get(fileList.get(i).getPath());
				if (fileList.get(i).getFileName().contains(".")) {
					// write the filename below in the File constructor
					File file = new File(path.toString());
					fileInput = new FileInputStream(file);
					// get the file length
					long fileSize = file.length();
					System.out.println(
							"file size of " + fileList.get(i).getFileName()
									+ " is " + fileSize);
					// first send the size of the file to the client
					outStream.writeLong(fileSize);
					outStream.flush();
					// Now send the file contents
					if (fileSize > MAX_BUFFER)
						bufferSize = MAX_BUFFER;
					else
						bufferSize = (int) fileSize;
					data = new byte[bufferSize];
					long totalBytesRead = 0;
					while (true) {
						// read upto MAX_BUFFER number of bytes from file
						int readBytes = fileInput.read(data);
						// send readBytes number of bytes to the client
						outStream.write(data);
						outStream.flush();
						// stop if EOF
						if (readBytes == -1)// EOF
							break;
						totalBytesRead = totalBytesRead + readBytes;
						// stop if fileLength number of bytes are read
						if (totalBytesRead == fileSize)
							break;
						//// update fileSize for the last remaining block of
						//// data
						if ((fileSize - totalBytesRead) < MAX_BUFFER)
							bufferSize = (int) (fileSize - totalBytesRead);
						// reinitialize the data buffer
						data = new byte[bufferSize];
					}
					System.out.println("send " + fileList.get(i).getFileName());
				}
			}
			System.out.println();
			fileInput.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * receive message from the Server
	 * 
	 * @return the message received from the Server
	 */
	public String receiveMessage() {
		byte[] readBuffer = new byte[200];
		String message = "";
		int num;
		try {
			num = inStream.read(readBuffer);
			if (num > 0) {
				byte[] arrayBytes = new byte[num];
				System.arraycopy(readBuffer, 0, arrayBytes, 0, num);
				message = new String(arrayBytes, "UTF-8");
				System.out.println("Received message :" + message + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return message;
	}

	/**
	 * send the files that were changed in Client
	 * 
	 * @param paths
	 *            the paths of the files that were changed
	 */
	private void sendUpdateFile(String paths) {
		// check if no file was changed
		if (paths.equals("")) {
			return;
		}
		// some file were changed
		String[] newFileList;
		// read the string into an array of paths
		newFileList = paths.trim().split("\\s+");
		final int MAX_BUFFER = 1000;
		byte[] data = null;
		int bufferSize = 0;
		try {
			FileInputStream fileInput = null;
			for (int i = 0; i < newFileList.length; i++) {
				File file = new File(newFileList[i]);
				fileInput = new FileInputStream(file);
				// get the file length
				long fileSize = file.length();
				System.out.println(
						"file " + newFileList[i] + " size is :" + fileSize);
				// first send the size of the file to the client
				outStream.writeLong(fileSize);
				outStream.flush();
				// Now send the file contents
				if (fileSize > MAX_BUFFER)
					bufferSize = MAX_BUFFER;
				else
					bufferSize = (int) fileSize;
				data = new byte[bufferSize];
				long totalBytesRead = 0;
				while (true) {
					// read upto MAX_BUFFER number of bytes from file
					int readBytes = fileInput.read(data);
					// send readBytes number of bytes to the client
					outStream.write(data);
					outStream.flush();
					// stop if EOF
					if (readBytes == -1)// EOF
						break;
					totalBytesRead = totalBytesRead + readBytes;
					// stop if fileLength number of bytes are read
					if (totalBytesRead == fileSize)
						break;
					//// update fileSize for the last remaining block of data
					if ((fileSize - totalBytesRead) < MAX_BUFFER)
						bufferSize = (int) (fileSize - totalBytesRead);
					// reinitialize the data buffer
					data = new byte[bufferSize];
				}
				System.out.println("sent file" + newFileList[i]);
			}
			System.out.println();
			fileInput.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * update by sending the files that were changed in Client
	 */
	public void update() {
		String paths;
		paths = receiveMessage();
		if (!paths.equals("")) {
			sendUpdateFile(paths);
		}
	}

	/**
	 * update by send new files and then update the files which were changed in
	 * Client
	 */
	public void updateWithNewFile() {
		String newPaths;
		newPaths = receiveMessage();
		sendUpdateFile(newPaths);
		update();
	}

	/**
	 * close the connection between Client and Server
	 */
	public void closeConnection() {
		try {
			this.socket.close();
			this.outStream.close();
			this.inStream.close();
			System.out.println("connection closed");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendNewUpdateFile() {
		// TODO Auto-generated method stub

	}
}
