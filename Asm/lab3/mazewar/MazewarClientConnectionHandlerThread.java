import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class MazewarClientConnectionHandlerThread extends Thread {
	private MazewarClient mazewarClient;

	public MazewarClientConnectionHandlerThread(MazewarClient mazewarClient) {
		super("MazewarClientConnectionHandlerThread");
		this.mazewarClient = mazewarClient;
	}

	public void run() {
		System.out.println("Started MazewarClientConnectionHandlerThread");
		
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(mazewarClient.port);
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not open server socket on port " + mazewarClient.port);
			System.exit(-1);
		}
		
		while (!mazewarClient.playerShutdown.containsKey(mazewarClient.playerID)) {
			try {
				Socket socket = serverSocket.accept();
				(new MazewarClientGameListenerThread(mazewarClient, -1, socket)).start();
			}
			catch (IOException e) {
				System.err.println("ERROR: Could not accept incoming connection");
			}
		}
		
		try {
			serverSocket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close server socket");
		}
		
		System.out.println("Exitted MazewarClientConnectionHandlerThread");
	}
}
