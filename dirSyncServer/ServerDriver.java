package project2;

import java.io.File;
import java.util.ArrayList;

public class ServerDriver {
	public static void main(String args[]) {
		ArrayList<FileInfo> cliList = new ArrayList<FileInfo>();
		ArrayList<FileInfo> serList = new ArrayList<FileInfo>();
		// create connection
		TCPServer fileServer = new TCPServer();
		fileServer.createSocket();
		// receive initial file info list from Client
		cliList = fileServer.receiveFileInfo();
		File home = new File("home");
		// check if "home" exists in Server
		if (!home.exists()) {  // home doesn't exist in Server -> set up "home" mode
			// send code "setup home" to client to begin the setup process
			fileServer.sendMessage("setup home");
			// read the file info from client to a list of FileInfo objects and create new directories if necessary
			cliList = fileServer.processCliFileInfo();   
			// receive original files from clients
			fileServer.receiveFiles(cliList);
			fileServer.closeConnection();
		} else {  // home existes in Server -> update "home" mode
			// scan Server and make a new file info list
			serList = fileServer.makeFileInfo();
			// read the file info from client to a list of FileInfo objects and create new directories if necessary
			fileServer.processCliFileInfo();
			System.out.println("start sync");
			// compare 2 file info lists and sync server with client
			if (serList.size() < cliList.size()) {  // when new files were created in client - code: "new file sync"
				// send message to let client know to send back new files
				fileServer.sendMessage("new file sync");
				fileServer.updateWithNewFile(serList, cliList);
				System.out.println("finish sync");
				fileServer.closeConnection();
			} else if (serList.size() == cliList.size()) {  // when no file was created in client but some files have changed - code: "normal sync"
				fileServer.sendMessage("normal sync");
				fileServer.update(serList, cliList);
				System.out.println("finish sync");
				fileServer.closeConnection();	
			}
		}
	}
}
