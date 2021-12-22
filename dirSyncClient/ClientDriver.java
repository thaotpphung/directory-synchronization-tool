package project2;

/**
 * Driver Class to demonstrate how TCPClient works
 */
import java.util.ArrayList;

public class ClientDriver { 
	public static void main(String[] args) throws Exception {
		ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
		TCPClient fileClient = new TCPClient();
		// create connection
		fileClient.createSocket();
		// make file info of Client
		fileList = fileClient.makeFileInfo();
		// send file info to Server
		fileClient.sendFileInfo();
		// receive a message from the Server
		String message = fileClient.receiveMessage();
		 if ( message.equals("setup home"))
		 {
			 fileClient.sendFiles(fileList);
			 fileClient.closeConnection();
		 } else if (message.equals("normal sync"))
		 {
			 fileClient.update();
			 System.out.println("finish sync");
			 fileClient.closeConnection();
		 } else if ( message.equals("new file sync"))
		 {
			 fileClient.updateWithNewFile();
			 System.out.println("finish sync");
			 fileClient.closeConnection();
		 }
	}
} 

 