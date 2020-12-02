package dirSyncServer;

public class ServerDriver {
	public static void main(String[] args) {
		TCPServerFile fileServer = new TCPServerFile();
		fileServer.createSocket(); // set up base directory and connect to client 
		fileServer.buildDirectory(); // recreat directory structure
		fileServer.initialSetup(); // initial setup 
		while (true) {
			fileServer.syncFile();
		}
	}
}
