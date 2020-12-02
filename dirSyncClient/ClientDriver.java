package dirSyncClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientDriver {
	public static void main(String[] args) {
		Path homeDir = Paths.get("/Users/ttp/eclipse-workspace/dirSync/src/home"); 
		Path oldLog = Paths.get("OldLog/log.ser");  
		Path newLog = Paths.get("NewLog/log.ser");

		ScanTool scanTool = new ScanTool();
		TCPClientFile fileClient = new TCPClientFile();
		fileClient.createLogFolder("OldLog", "NewLog"); // create folder to store file information
		fileClient.createSocket(); // connect to server

		initialScan(oldLog, homeDir, scanTool);
		initialSetup(oldLog, fileClient);
		
		// every 10s, scan for new changes and creates a thread to send those changes to server
		while (true) { 
			try {
				// scan for new changes
				scanTool.openFile(newLog);
				scanTool.scan(homeDir);
				scanTool.closeFile();

				// sync file: sends new changes to server
				fileClient.syncFile(oldLog, newLog);

				// create new file log
				Files.delete(oldLog);
				Files.copy(newLog, oldLog);
				Thread.sleep(10 * 1000);

			} catch (IOException e) {
				System.out.println("IOException in Main");
			} catch (InterruptedException ie) {
				System.out.println("Interupted Exception in Main");
			}
		}
	}

	/**
	 * set base path for home directory, scan and store home content in a FileInfo object
	 * @param oldLog 
	 * @param homeDir
	 * @param scanTool
	 */
	private static void initialScan(Path oldLog, Path homeDir, ScanTool scanTool) {
		scanTool.openFile(oldLog);
		FileInfo.setBasePath(homeDir.toString());
		try {
			scanTool.scan(homeDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		scanTool.closeFile();
	}

	/**
	 * send initial directory structure and files to server for set up
	 * @param oldLog
	 * @param fileClient
	 */
	private static void initialSetup(Path oldLog, TCPClientFile fileClient) {
		try {
			fileClient.sendDirectoryStructure(oldLog); 
			fileClient.sendFile(oldLog); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
