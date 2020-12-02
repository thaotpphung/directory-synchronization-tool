package dirSyncServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * A server to receive and store backup information from a client computer
 * @author Thao Phung 
 * @version 10/10/2018
 */

public class TCPServerFile {
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private DataInputStream inStream = null;
	private String base = "E:/backup_home/";

	/**
	 * Default constructor
	 */
	public TCPServerFile() {
	}

	/**
	 * Creates a base folder to store data and a new socket to wait for client
	 * connection
	 * 
	 * @throws IOException
	 */
	public void createSocket() {
		try {
			// create Server and start listening
			boolean success = (new File(base)).mkdirs();
			if (!success) {
				System.out.println("base existed");
			}
			serverSocket = new ServerSocket(3339);
			// accept the connection
			socket = serverSocket.accept();
			// fetch the streams
			inStream = new DataInputStream(socket.getInputStream());
			System.out.println("Connected");
		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	/**
	 * Receives initial files from client after rebuild all directory to finish the initial setup
	 * 
	 * @throws SocketException
	 */
	public void initialSetup() {
		while (true) {
			byte[] data = null;
			final int MAX_BUFFER = 1000; // set default max buffer size to be 1000 bytes - a typical value for a TCP payload, since the common MTU of the underlying ethernet is 1500 bytes.
			try {
				// read the size of the file coming from Server
				// read the data from client
				long pathSize = inStream.readLong();
				if (pathSize == -1) {
					break;
				}
				byte[] readBuffer = new byte[(int) pathSize];

				inStream.read(readBuffer, 0, (int) pathSize);
				byte[] temp = new byte[(int) pathSize];
				System.arraycopy(readBuffer, 0, temp, 0, (int) pathSize);

				String path = new String(temp, "UTF-8");

				path = base + path;

				long fileSize = inStream.readLong();

				int bufferSize = 0;

				// decide the data reading bufferSize
				if (fileSize > MAX_BUFFER)
					bufferSize = MAX_BUFFER;
				else
					bufferSize = (int) fileSize;

				data = new byte[bufferSize];

				// insert the path/name of your target file
				FileOutputStream fileOut = new FileOutputStream(path, false);

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
				String relative = new File(base).toURI().relativize(new File(path).toURI()).getPath();
				System.out.println("Received file " + relative + ", File Size is: " + fileSize
						+ ", number of bytes read are: " + totalBytesRead);
				fileOut.close();
			} catch (SocketException se) {
				System.out.println("Socket closed");
				break;

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Receives information about the directory structure from the client and rebuild in server
	 */
	public void buildDirectory() {

		while (socket.isConnected()) {
			try {

				long pathSize = inStream.readLong();
				byte[] temp = new byte[(int) pathSize];

				inStream.read(temp, 0, (int) pathSize);
				String newDir = new String(temp, "UTF-8");

				if (newDir.equals("Done")) {
					break;
				}
				System.out.println("Received directory name:" + newDir);

				newDir = base + newDir;
				boolean success = (new File(newDir)).mkdirs();
				if (!success) {
					System.out.println("folder existed");
				}

			} catch (SocketException se) {
				System.out.println("Socket Exception");
			} catch (IOException i) {
				i.printStackTrace();
			}
		}

	}

	/**
	 * A thread that synchronously receives files from client
	 */
	private class receiveFileThread implements Runnable {
		private Socket threadSocket;

		public receiveFileThread(Socket socket) {
			threadSocket = socket;
		}

		public void run() {
			byte[] data = null;
			final int MAX_BUFFER = 1000; // set default max buffer size to be 1000 bytes - a typical value for a TCP payload, since the common MTU of the underlying ethernet is 1500 bytes.
			try {
				DataInputStream inStream = new DataInputStream(threadSocket.getInputStream());

				// read the size of the file <- coming from Server
				long pathSize = inStream.readLong();
				// read the data from client
				byte[] readBuffer = new byte[(int) pathSize];

				inStream.read(readBuffer, 0, (int) pathSize);
				byte[] temp = new byte[(int) pathSize];
				System.arraycopy(readBuffer, 0, temp, 0, (int) pathSize);

				String path = new String(temp, "UTF-8");

				path = base + path;

				long fileSize = inStream.readLong();

				int bufferSize = 0;

				// decide the data reading bufferSize
				if (fileSize > MAX_BUFFER)
					bufferSize = MAX_BUFFER;
				else
					bufferSize = (int) fileSize;

				data = new byte[bufferSize];

				// insert the path/name of your target file
				FileOutputStream fileOut = new FileOutputStream(path, false);

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

				String relative = new File(base).toURI().relativize(new File(path).toURI()).getPath();

				System.out.print("Received file" + relative + "File Size is: " + fileSize
						+ ", number of bytes read are: " + totalBytesRead + ", path is:" + path + " using ");
				fileOut.close();
				inStream.close();
				threadSocket.close();
			} catch (SocketException se) {
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println(Thread.currentThread());
		}
	}

	/**
	 * A thread that synchronously receives information about directory from client
	 */
	private class receiveDirThread implements Runnable {
		private Socket threadSocket;
		public receiveDirThread(Socket socket) {
			threadSocket = socket;
		}

		public void run() {
			try {
				DataInputStream inStream = new DataInputStream(threadSocket.getInputStream());
				long pathSize = inStream.readLong();
				byte[] temp = new byte[(int) pathSize];

				inStream.read(temp, 0, (int) pathSize);
				String newDir = new String(temp, "UTF-8");

				System.out.print("Received directory name :" + newDir + " using ");

				newDir = base + newDir;
				boolean success = (new File(newDir)).mkdirs();
				if (!success) {
					System.out.println("folder existed");
				}

				inStream.close();
				threadSocket.close();
			} catch (SocketException se) {
				System.out.println("Socket Exception subthread");
			} catch (IOException i) {
				i.printStackTrace();
			}
			System.out.println(Thread.currentThread());

		}
	}

	/**
	 * receives information about modified files or directory and create a thread to deal with it
	 */
	public void syncFile() {
		try {
			long code = inStream.readLong();
			Socket threadSocket = serverSocket.accept();

			if (code == 1) {
				Thread thread = new Thread(new receiveFileThread(threadSocket));
				thread.start();
			} else if (code == 2) {
				Thread thread = new Thread(new receiveDirThread(threadSocket));
				thread.start();
			}
		} catch (IOException e) {
		}
	}

}