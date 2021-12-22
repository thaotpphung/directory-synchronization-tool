package project2;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * A TCP server to sync "home" folder between local and remote computer
 * 
 * @author Thao Phung
 * @version 11/30/2018
 */
public class TCPServer {
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private DataInputStream inStream = null;
	private DataOutputStream outStream = null;
	private final String CLI_INFO = "clientInfo.ser";
	private final String SER_INFO = "serverInfo.ser";

	/**
	 * Constructor to create the Server
	 */
	public TCPServer() {
	}

	/**
	 * create connection to Client
	 */
	public void createSocket() {
		try {
			// create Server and start listening
			serverSocket = new ServerSocket(5735);
			// accept the connection
			socket = serverSocket.accept();
			// fetch the streams
			inStream = new DataInputStream(socket.getInputStream());
			outStream = new DataOutputStream(socket.getOutputStream());
			System.out.println("Connected");
		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	/**
	 * receive file info from Client
	 * 
	 * @return the file info list of Client
	 */
	public ArrayList<FileInfo> receiveFileInfo() {
		File fileInfo = new File(CLI_INFO);
		if (fileInfo.exists()) {
			try {
				Files.delete(Paths.get(CLI_INFO));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		byte[] data = null;
		final int MAX_BUFFER = 1000;
		try {
			FileOutputStream fileOut = null;
			// read the size of the file <- coming from Server
			long fileSize = inStream.readLong();
			int bufferSize = 0;
			// decide the data reading bufferSize
			if (fileSize > MAX_BUFFER)
				bufferSize = MAX_BUFFER;
			else
				bufferSize = (int) fileSize;
			data = new byte[bufferSize];
			// insert the path/name of your target file
			fileOut = new FileOutputStream(CLI_INFO, true); // path.toString()
			// now read the file coming from Server & save it onto disk
			long totalBytesRead = 0;
			while (true) {
				// read bufferSize number of bytes from Server
				int readBytes = inStream.read(data, 0, bufferSize);
				byte[] arrayBytes = new byte[readBytes];
				System.arraycopy(data, 0, arrayBytes, 0, readBytes);
				totalBytesRead = totalBytesRead + readBytes;
				if (readBytes > 0) {
					// write the data to the file
					fileOut.write(arrayBytes);
					fileOut.flush();
				}
				// stop if fileSize number of bytes are read
				if (totalBytesRead == fileSize)
					break;
				// update fileSize for the last remaining block of data
				if ((fileSize - totalBytesRead) < MAX_BUFFER)
					bufferSize = (int) (fileSize - totalBytesRead);
				// reinitialize the data buffer
				data = new byte[bufferSize];
			}
			System.out.println("Received info file\n");
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// read file info to an ArrayList
		ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
		FileScanner scanner = new FileScanner();
		scanner.openReadFile(Paths.get(CLI_INFO));
		try {
			System.out.println("Client file info");
			fileList = scanner.readFile(Paths.get(CLI_INFO));
		} catch (IOException e) {
			e.printStackTrace();
		}
		scanner.closeReadFile();
		return fileList;
	}

	/**
	 * read the file info to a list of FileInfo objects and create new
	 * directories if necessary
	 * 
	 * @return the ArrayList of FileInfo objects
	 */
	public ArrayList<FileInfo> processCliFileInfo() {
		// read info file to an array list of files
		FileScanner scanner = new FileScanner();
		scanner.openReadFile(Paths.get(CLI_INFO));
		ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
		try {
			System.out.println("processing client file info");
			System.out.println("Client file info:");
			fileList = scanner.readFile(Paths.get(CLI_INFO));
		} catch (IOException e) {
			e.printStackTrace();
		}
		scanner.closeReadFile();
		// create directory
		for (int i = 0; i < fileList.size(); i++) {
			Path path = Paths.get(fileList.get(i).getPath());
			File file = new File(fileList.get(i).getPath());
			if (!file.exists()) {
				if (path.toString().contains("\\")) {
					path = Paths.get(path.toString().replace('\\', '/'));
				}
				if (!(fileList.get(i).getFileName().contains("."))) {
					try {
						Files.createDirectories(path);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return fileList;
	}

	/**
	 * receive original files from Client
	 * 
	 * @param fileList
	 *            list of the files to be received
	 */
	public void receiveFiles(ArrayList<FileInfo> fileList) {
		byte[] data = null;
		final int MAX_BUFFER = 1000;
		try {
			FileOutputStream fileOut = null;
			for (int i = 0; i < fileList.size(); i++) {
				Path path = Paths.get(fileList.get(i).getPath());
				if (path.toString().contains("\\")) {
					path = Paths.get(path.toString().replace('\\', '/'));
				}
				if (fileList.get(i).getFileName().contains(".")) {
					// read the size of the file <- coming from Server
					long fileSize = inStream.readLong();
					System.out.println(
							"file size of  " + fileList.get(i).getFileName()
									+ " is " + fileSize);
					int bufferSize = 0;
					// decide the data reading bufferSize
					if (fileSize > MAX_BUFFER)
						bufferSize = MAX_BUFFER;
					else
						bufferSize = (int) fileSize;
					data = new byte[bufferSize];
					// insert the path/name of your target file
					fileOut = new FileOutputStream(path.toString(), true);
					// now read the file coming from Server & save it onto disk
					long totalBytesRead = 0;
					while (true) {
						// read bufferSize number of bytes from Server
						int readBytes = inStream.read(data, 0, bufferSize);
						byte[] arrayBytes = new byte[readBytes];
						System.arraycopy(data, 0, arrayBytes, 0, readBytes);
						totalBytesRead = totalBytesRead + readBytes;
						if (readBytes > 0) {
							// write the data to the file
							fileOut.write(arrayBytes);
							fileOut.flush();
						}
						// stop if fileSize number of bytes are read
						if (totalBytesRead == fileSize)
							break;
						// update fileSize for the last remaining block of data
						if ((fileSize - totalBytesRead) < MAX_BUFFER)
							bufferSize = (int) (fileSize - totalBytesRead);
						// reinitialize the data buffer
						data = new byte[bufferSize];
					}
					System.out.println(
							"received file " + fileList.get(i).getFileName());
				}
			}
			System.out.println();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * make Server file info
	 * 
	 * @return ArrayList of the files in the "home" directory of Server
	 * @precondition "home" folder needs to exists
	 */
	public ArrayList<FileInfo> makeFileInfo() {
		File file = new File(SER_INFO);
		if (file.exists()) {
			try {
				Files.delete(Paths.get(SER_INFO));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileScanner scanner = new FileScanner();
		// scan file
		scanner.openWriteFile(Paths.get(SER_INFO));
		Path path = Paths.get("home");
		try {
			scanner.scan(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		scanner.closeWriteFile();
		// read file
		scanner.openReadFile(Paths.get(SER_INFO));
		ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
		try {
			System.out.println("Server file info");
			fileList = scanner.readFile(Paths.get(SER_INFO));

		} catch (IOException e) {
			e.printStackTrace();
		}
		scanner.closeReadFile();
		return fileList;
	}

	/**
	 * receive files of a given paths from the Client
	 * 
	 * @param paths
	 *            the path of the file needed to be received
	 */
	public void receiveUpdateFile(String paths) {
		// read the given string into an array of paths
		String[] newFileList;
		newFileList = paths.trim().split("\\s+");
		for (int i = 0; i < newFileList.length; i++) {
			System.out.println(newFileList[i]);
		}
		byte[] data = null;
		final int MAX_BUFFER = 1000;
		try {
			FileOutputStream fileOut = null;
			for (int i = 0; i < newFileList.length; i++) {
				// change path to correct format
				Path path = Paths.get(newFileList[i]);
				if (path.toString().contains("\\")) {
					path = Paths.get(path.toString().replace('\\', '/'));
				}
				// check if a file exists, if yes delete that file
				// to be overriden
				File file = new File(path.toString());
				if (file.exists()) {
					Files.delete(Paths.get(newFileList[i]));
				}
				// read the size of the file <- coming from Server
				long fileSize = inStream.readLong();
				System.out.println(
						"file size of  " + newFileList[i] + " is " + fileSize);
				int bufferSize = 0;
				// decide the data reading bufferSize
				if (fileSize > MAX_BUFFER)
					bufferSize = MAX_BUFFER;
				else
					bufferSize = (int) fileSize;
				data = new byte[bufferSize];
				// insert the path/name of your target file
				fileOut = new FileOutputStream(path.toString(), true); // path.toString()
				// now read the file coming from Server & save it onto disk
				long totalBytesRead = 0;
				while (true) {
					// read bufferSize number of bytes from Server
					int readBytes = inStream.read(data, 0, bufferSize);
					byte[] arrayBytes = new byte[readBytes];
					System.arraycopy(data, 0, arrayBytes, 0, readBytes);
					totalBytesRead = totalBytesRead + readBytes;
					if (readBytes > 0) {
						// write the data to the file
						fileOut.write(arrayBytes);
						fileOut.flush();
					}
					// stop if fileSize number of bytes are read
					if (totalBytesRead == fileSize)
						break;
					// update fileSize for the last remaining block of data
					if ((fileSize - totalBytesRead) < MAX_BUFFER)
						bufferSize = (int) (fileSize - totalBytesRead);
					// reinitialize the data buffer
					data = new byte[bufferSize];
				}
				System.out.println("received file " + newFileList[i]);
			}
			System.out.println();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * send a message to the Client
	 * 
	 * @param Amessage
	 *            the message to be sent
	 */
	public void sendMessage(String Amessage) {
		String message = Amessage;
		try {
			if (message != null && message.length() > 0) {
				outStream.write(message.getBytes("UTF-8"));
			}
			System.out.println("message " + message + " sent\n");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * update the files that were changed in Client
	 * 
	 * @param cliList
	 *            the Client file list
	 * @param serList
	 *            the server file list
	 */
	public void update(ArrayList<FileInfo> serList,
			ArrayList<FileInfo> cliList) {
		int cliSize = cliList.size();
		// string to hold the file name of the files which need to be updated
		String newCliFileList = "";
		for (int i = 0; i < cliSize; i++) {
			if (cliList.get(i).getFileName().contains(".")) {
				//if (cliList.get(i).getSize() != serList.get(i).getSize()) {
				if (!(cliList.get(i).getLastModified().equals(serList.get(i).getLastModified()))) {
					System.out.println("file" + cliList.get(i).getFileName()
							+ " at client changed");
					newCliFileList = newCliFileList + cliList.get(i).getPath();
				}
			}
		}
		// send the string with the path of the files which need to be updated
		sendMessage(newCliFileList);
		if (!newCliFileList.equals("")) {
			receiveUpdateFile(newCliFileList);
		}
	}

	/**
	 * receive new files and then update the files which were changed in Client
	 * 
	 * @param serList
	 *            Server file list
	 * @param cliList
	 *            Client file list
	 */
	public void updateWithNewFile(ArrayList<FileInfo> serList,
			ArrayList<FileInfo> cliList) {
		int cliSize = cliList.size();
		String newCliFileList = "";
		for (int i = 0; i < cliSize; i++) {
			if (cliList.get(i).getFileName().contains(".")) {
				File file = new File(cliList.get(i).getPath());
				if (!file.exists()) {
					newCliFileList = newCliFileList + cliList.get(i).getPath();
				}
			}
		}
		sendMessage(newCliFileList);
		receiveUpdateFile(newCliFileList);
		serList = makeFileInfo();
		update(serList, cliList);
	}

	/**
	 * close connection between CLient and Server
	 */
	public void closeConnection() {
		try {
			this.serverSocket.close();
			this.socket.close();
			this.inStream.close();
			this.outStream.close();
			System.out.println("Close connection");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
