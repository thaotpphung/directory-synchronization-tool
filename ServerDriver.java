package directory_sync_tool;

import java.io.File;
import java.util.ArrayList;

public class ServerDriver {
	public static void main(String args[]) {
		ArrayList<FileInfo> cliList = new ArrayList<FileInfo>();
		ArrayList<FileInfo> serList = new ArrayList<FileInfo>();
		// create connection
		TCPServer fileServer = new TCPServer();
		fileServer.createSocket();
		// receive file info of Client
		cliList = fileServer.receiveFileInfo();
		File home = new File("home");
		// check if "home" exists in Server
		if (!home.exists()) {
			// set up "home" in Server
			fileServer.sendMessage("setup home");
			cliList = fileServer.processCliFileInfo();
			fileServer.receiveFiles(cliList);
			fileServer.closeConnection();
		} else {
			// update "home"
			serList = fileServer.makeFileInfo();
			fileServer.processCliFileInfo();
			System.out.println("start sync");
			// update when new files were created in Client 
			// and files were changed/not changed in Client
			if (serList.size() < cliList.size()) {
				fileServer.sendMessage("new file sync");
				fileServer.updateWithNewFile(serList, cliList);
				System.out.println("finish sync");
				fileServer.closeConnection();
			// update when files were changed in Client
			} else if (serList.size() == cliList.size()) {
				fileServer.sendMessage("normal sync");
				fileServer.update(serList, cliList);
				System.out.println("finish sync");
				fileServer.closeConnection();	
			}
		}
	}
}
