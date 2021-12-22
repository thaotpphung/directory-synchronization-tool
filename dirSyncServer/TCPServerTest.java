package project2;

/**
 * Test class of TCPServer
 * @author ThaoPhung
 * @version 11/30/2019
 */
import java.io.File;
import java.util.ArrayList;

import junit.framework.TestCase;

public class TCPServerTest extends TestCase {
	ArrayList<FileInfo> cliList = new ArrayList<FileInfo>();
	ArrayList<FileInfo> serList = new ArrayList<FileInfo>();
	
	public void testReceiveFileInfo()
	{
		TCPServer fileServer = new TCPServer();
		fileServer.createSocket();
		cliList = fileServer.receiveFileInfo();	
		File cliinfo = new File ("clientinfo.ser");
		assertTrue(cliinfo.exists());
	}

	public void testReceiveFiles()
	{
		TCPServer fileServer = new TCPServer();
		fileServer.createSocket();
		cliList = fileServer.receiveFileInfo();	
		File home = new File ("home");
		if ( !home.exists())
		{
			fileServer.sendMessage("setup home");
			cliList = fileServer.processCliFileInfo();
			fileServer.receiveFiles(cliList);
		}
		fileServer.closeConnection();
		assertTrue(home.exists());
	}	
	
}
