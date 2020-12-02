package dirSyncClient;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A client that sends directoriess and files to a backup server computer
 * 
 * @author Thao Phung
 * @version 10/10/2018
 */

public class TCPClientFile {
	private Socket socket = null;
	private DataOutputStream outStream = null;
	private final String SERVER_IP = "192.168.0.22";
	private final int PORT = 3339;
	private static ObjectInputStream input; // inputs data to read info

	/**
	 * Creates folders to store old and new information
	 * 
	 * @param oldLog folder to store old information
	 * @param newLog folder to store new information
	 */
	public void createLogFolder(String oldLog, String newLog) {
		if (!Files.exists(Paths.get(oldLog))) {
			(new File(oldLog)).mkdirs();
		}
		if (!Files.exists(Paths.get(newLog))) {
			(new File(newLog)).mkdirs();
		}
	}

	/**
	 * Default constructor
	 */
	public TCPClientFile() {
	}

	/**
	 * Create a socket to connect to server
	 */
	public void createSocket() {
		try {
			// connect to localHost at given port #
			socket = new Socket(SERVER_IP, PORT);

			System.out.println("Connected");
			// fetch the streams
			outStream = new DataOutputStream(socket.getOutputStream());
		} catch (Exception u) {
			u.printStackTrace();
		}
	}

	/**
	 * Send directory's path to server
	 * 
	 * @param path directory's path
	 */
	public void sendDir(String path) {
		try {
			System.out.println("Sent " + path);
			outStream.writeLong(path.getBytes("UTF-8").length);
			outStream.flush();

			outStream.write(path.getBytes("UTF-8"));
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send files data to server
	 * 
	 * @param record file's information
	 */
	public void sendFile(FileInfo record) {
		final int MAX_BUFFER = 1000;
		byte[] data = null;
		int bufferSize = 0;

		try {
			// write the filename below in the File constructor
			File file = new File(record.getAbsPath());
			byte[] filePath = record.getPath().getBytes("UTF-8");

			outStream.writeLong(filePath.length);
			outStream.flush();

			outStream.write(filePath);
			outStream.flush();

			FileInputStream fileInput = new FileInputStream(file);
			// get the file length
			long fileSize = file.length();

			System.out.printf("Sending file %s with size %d\n", record.getPath(), record.getSize());
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send directory structure to server
	 * 
	 * @param path serialized file information's path
	 * @throws IOException
	 */
	public void sendDirectoryStructure(Path path) throws IOException {
		input = new ObjectInputStream(Files.newInputStream(path));
		System.out.println("Sending directory Structure");
		try {
			while (true) // loop until there is an EOFException
			{
				FileInfo record = (FileInfo) input.readObject();

				// display record contents

				if (Files.isDirectory(Paths.get(record.getAbsPath()))) {
					sendDir(record.getPath());
				}
			}
		} catch (EOFException endOfFileException) {
			sendDir("Done");
			System.out.println("Done building directory\n");
		} catch (ClassNotFoundException classNotFoundException) {
			System.err.println("Invalid object type. Terminating.");
		} finally {
			input.close();
		}

	}

	/**
	 * Send files to server after server done creating directory structure
	 * 
	 * @param path serialized file information's path
	 * @throws IOException
	 */
	public void sendFile(Path path) throws IOException {
		input = new ObjectInputStream(Files.newInputStream(path));

		try {
			while (true) // loop until there is an EOFException
			{
				FileInfo record = (FileInfo) input.readObject();

				// display record contents

				if (!Files.isDirectory(Paths.get(record.getAbsPath()))) {
					sendFile(record);
				}
			}
		} catch (EOFException endOfFileException) {
			System.out.println("Initial setup completed\n");
			outStream.writeLong(-1);
		} catch (ClassNotFoundException classNotFoundException) {
			System.err.println("Invalid object type. Terminating.");
		} finally {
			input.close();
		}

	}

	/**
	 * After the initial setup, send files or folder that are new or modified to
	 * server
	 * 
	 * @param oldLog folder that store old information
	 * @param newLog folder that store new information
	 */
	public void syncFile(Path oldLog, Path newLog) {
		try {
			ObjectInputStream newInput = new ObjectInputStream(Files.newInputStream(newLog));
			try {
				while (true) // loop until there is an EOFException
				{
					FileInfo newRecord = (FileInfo) newInput.readObject();
					ObjectInputStream oldInput = new ObjectInputStream(Files.newInputStream(oldLog));

					try {
						while (true) {
							FileInfo oldRecord = (FileInfo) oldInput.readObject();
							// file is modified
							if (newRecord.getPath().equals(oldRecord.getPath())
									&& !newRecord.getLastModified().equals(oldRecord.getLastModified())) {
								if (!Files.isDirectory(Paths.get(newRecord.getPath()))) {
									System.out.println(newRecord.getFileName() + " is changed, uploading");
									Thread t = new Thread(new sendFileThread(newRecord));
									t.start();
								}
								break;
							} else if (newRecord.getPath().equals(oldRecord.getPath())
									&& newRecord.getLastModified().equals(oldRecord.getLastModified())) { // file does
								// not
								// changed
								break;
							}

						}

					} catch (EOFException endOfFileException) {
						// cannot find newRecord in old record => new file or new directory
						System.out.println(newRecord.getFileName() + " is new, uploading");
						Thread t = new Thread(new sendFileThread(newRecord));
						t.start();
					} finally {
						oldInput.close();
					}
				}

			} catch (EOFException endOfFileException) {
				System.out.println("Done checking new files and folder\n");
			} catch (ClassNotFoundException classE) {
				System.out.println("Class Ex");
			} finally {
				newInput.close();
			}

		} catch (

		IOException IOe) {

		}
	}

	/**
	 * Close connection
	 */
	public void closeSocket() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A thread that send files or directory to server
	 */
	private class sendFileThread implements Runnable {
		private FileInfo file;
		private DataOutputStream threadStream;
		private Socket threadSocket = null;

		public sendFileThread(FileInfo file) {
			this.file = file;
		}

		public void run() {
			try {

				if (!Files.isDirectory(Paths.get(file.getAbsPath()))) {
					outStream.writeLong(1);
					outStream.flush();
					threadSocket = new Socket(SERVER_IP, PORT);
					String path = file.getPath();
					final int MAX_BUFFER = 1000;
					byte[] data = null;
					int bufferSize = 0;
					try {
						threadStream = new DataOutputStream(threadSocket.getOutputStream());
						// write the filename below in the File constructor
						File sendFile = new File(file.getAbsPath());
						FileInputStream fileInput = new FileInputStream(sendFile);
						byte[] filePath = path.getBytes("UTF-8");
						threadStream.writeLong(filePath.length);
						threadStream.flush();
						threadStream.write(filePath);
						threadStream.flush();

						// get the file length
						long fileSize = sendFile.length();

						System.out.printf("Sending file %s with size %d using ", file.getPath(), file.getSize());
						// first send the size of the file to the client
						threadStream.writeLong(fileSize);
						threadStream.flush();

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
							threadStream.write(data);
							threadStream.flush();

							// stop if EOF
							if (readBytes == -1) {// EOF
								break;
							}
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
					} catch (FileNotFoundException e) {
						System.out.println("Error");
					} finally {
						threadStream.close();
						threadSocket.close();
					}

				} else {
					outStream.writeLong(2);
					outStream.flush();
					threadSocket = new Socket(SERVER_IP, PORT);
					try {
						threadStream = new DataOutputStream(threadSocket.getOutputStream());
						String path = file.getPath();
						System.out.print("Sent " + path + " using ");
						threadStream.writeLong(path.getBytes("UTF-8").length);
						threadStream.flush();

						threadStream.write(path.getBytes("UTF-8"));
						threadStream.flush();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						threadStream.close();
						threadSocket.close();
					}
				}
				System.out.println(Thread.currentThread());
				// Thread.sleep(3000);
				// } catch (InterruptedException e) {
				// System.out.println("Subthread interrupted");
			} catch (IOException IOe) {
				System.out.println("IO EX");
			}
		}
	}

}